package com.rental;

import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** 在独立 MySQL/Redis/Tomcat 环境运行真实 HTTP 测试，不用 Mock 代替事务和并发。 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RentalApiIT {
    private static final ObjectMapper JSON=new ObjectMapper();
    private static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final String BASE=System.getProperty("api.baseUrl","http://127.0.0.1:18089/admin/api");
    private static final String PREFIX="IT"+UUID.randomUUID().toString().replace("-","").substring(0,9);
    private static String token;
    private static long tenant;
    private static long house;
    private static long contract;
    private static long bill;
    private static String viewerToken;
    private static long viewerId;
    private static final String TODAY=LocalDate.now(ZoneId.of("Asia/Hong_Kong")).toString();

    static final class Reply {final int status;final JsonNode body;Reply(int status,JsonNode body){this.status=status;this.body=body;}JsonNode data(){return body.path("data");}}
    static Reply call(String method,String path,String auth,Object input) throws Exception {
        HttpRequest.Builder request=HttpRequest.newBuilder(URI.create(BASE+path)).timeout(Duration.ofSeconds(20)).header("Accept","application/json");
        if(auth!=null)request.header("Authorization","Bearer "+auth);
        if(input!=null)request.header("Content-Type","application/json");
        request.method(method,input==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(input)));
        HttpResponse<String> response=HTTP.send(request.build(),HttpResponse.BodyHandlers.ofString());
        JsonNode body;try{body=JSON.readTree(response.body());}catch(Exception error){throw new AssertionError("非 JSON 响应: "+response.statusCode()+" "+response.body().substring(0,Math.min(300,response.body().length())));}
        return new Reply(response.statusCode(),body);
    }
    static Map<String,Object> houseBody(String code) {Map<String,Object> value=new HashMap<>();value.put("code",code);value.put("title",code+" 测试房源");value.put("address","演示园区测试地址");value.put("area",50);value.put("monthlyRent",1000);value.put("deposit",1000);value.put("status","VACANT");value.put("version",0);return value;}
    static Map<String,Object> paymentBody(String key,String amount) {Map<String,Object> value=new HashMap<>();value.put("requestId",key);value.put("amount",amount);value.put("method","BANK");value.put("note","集成测试流水");return value;}
    static JsonNode require(Reply response,int status) {assertEquals(status,response.status,response.body.toString());return response.data();}
    @BeforeAll static void prepare() throws Exception {
        require(call("GET","/health",null,null),200);
        token=require(call("POST","/auth/login",null,Map.of("username","admin","password","111111")),200).path("token").asText();
        String phone="138"+String.format("%08d",Math.abs(PREFIX.hashCode()%100000000));
        tenant=require(call("POST","/tenants",token,Map.of("name",PREFIX,"phone",phone,"email","test@example.invalid","remark","自动化验证")),200).path("id").asLong();
        house=require(call("POST","/houses",token,houseBody(PREFIX)),200).path("id").asLong();
    }
    @Test @Order(1) void anonymousAndBadCredentials() throws Exception {require(call("GET","/houses",null,null),401);require(call("POST","/auth/login",null,Map.of("username","admin","password","badpass")),401);}
    @Test @Order(2) void validationAndRequestTracing() throws Exception {
        Reply invalid=call("POST","/houses",token,Map.of("code","bad"));require(invalid,400);assertFalse(invalid.body.path("requestId").asText().isEmpty());
        require(call("GET","/houses?page=0",token,null),400);require(call("GET","/houses?size=101",token,null),400);
    }
    @Test @Order(3) void uniqueAndOptimisticLock() throws Exception {
        require(call("POST","/houses",token,houseBody(PREFIX)),409);
        Map<String,Object> changed=houseBody(PREFIX);changed.put("title","已修改");require(call("PUT","/houses/"+house,token,changed),200);
        require(call("PUT","/houses/"+house,token,changed),409);
    }
    @Test @Order(4) void concurrentSigningHasSingleWinner() throws Exception {
        ExecutorService executor=Executors.newFixedThreadPool(8);CountDownLatch ready=new CountDownLatch(8);CountDownLatch start=new CountDownLatch(1);
        List<Future<Reply>> futures=new ArrayList<>();
        try {for(int i=0;i<8;i++)futures.add(executor.submit(()->{ready.countDown();start.await();return call("POST","/contracts",token,Map.of("houseId",house,"tenantId",tenant,"startDate",TODAY,"months",3));}));
            assertTrue(ready.await(10,TimeUnit.SECONDS));start.countDown();int winners=0;
            for(Future<Reply> future:futures){Reply result=future.get(30,TimeUnit.SECONDS);if(result.status==200){winners++;contract=result.data().path("id").asLong();}else require(result,409);}
            assertEquals(1,winners,"同一房源必须只产生一份生效合同");
        } finally {executor.shutdownNow();}
        JsonNode rows=require(call("GET","/bills?contractId="+contract,token,null),200);assertEquals(3,rows.path("total").asInt());bill=rows.path("records").get(0).path("id").asLong();
    }
    @Test @Order(5) void activeReferencesCannotBeDeleted() throws Exception {require(call("DELETE","/houses/"+house,token,null),409);require(call("DELETE","/tenants/"+tenant,token,null),409);require(call("POST","/contracts/"+contract+"/close",token,null),409);}
    @Test @Order(6) void overpaymentDoesNotWriteAnything() throws Exception {
        require(call("POST","/bills/"+bill+"/payments",token,paymentBody(PREFIX+"-over","1000.01")),409);
        assertEquals(0,require(call("GET","/bills/"+bill+"/payments",token,null),200).size());
    }
    @Test @Order(7) void concurrentIdempotentPaymentCreatesOneReceipt() throws Exception {
        ExecutorService executor=Executors.newFixedThreadPool(4);List<Callable<Reply>> tasks=new ArrayList<>();
        for(int i=0;i<4;i++)tasks.add(()->call("POST","/bills/"+bill+"/payments",token,paymentBody(PREFIX+"-partial","400.00")));
        try {Set<Long> receipts=new HashSet<>();for(Future<Reply> future:executor.invokeAll(tasks))receipts.add(require(future.get(),200).path("id").asLong());assertEquals(1,receipts.size());}finally{executor.shutdownNow();}
        assertEquals(1,require(call("GET","/bills/"+bill+"/payments",token,null),200).size());
        require(call("POST","/bills/"+bill+"/payments",token,paymentBody(PREFIX+"-partial","500.00")),409);
    }
    @Test @Order(8) void settleAndCloseReleasesHouse() throws Exception {
        require(call("POST","/bills/"+bill+"/payments",token,paymentBody(PREFIX+"-final","600.00")),200);
        assertEquals("CLOSED",require(call("POST","/contracts/"+contract+"/close",token,null),200).path("status").asText());
        JsonNode houses=require(call("GET","/houses?query="+PREFIX,token,null),200);assertEquals("VACANT",houses.path("records").get(0).path("status").asText());
        long cancelled=0;for(JsonNode row:require(call("GET","/bills?contractId="+contract,token,null),200).path("records"))if("CANCELLED".equals(row.path("status").asText()))cancelled++;assertEquals(2,cancelled);
    }
    @Test @Order(9) void noPasswordLeakAndRoleEnforcement() throws Exception {
        viewerId=require(call("POST","/system/users",token,Map.of("username",PREFIX,"nickname","测试观察员","password","DemoPass123!","role","viewer","email","test@example.invalid","phone","")),200).asLong();
        JsonNode login=require(call("POST","/auth/login",null,Map.of("username",PREFIX,"password","DemoPass123!")),200);viewerToken=login.path("token").asText();assertFalse(login.toString().contains("password"));
        // 同账号再次登录必须撤销旧令牌，旧请求不能通过续期使其复活。
        String replacedToken=viewerToken;
        viewerToken=require(call("POST","/auth/login",null,Map.of("username",PREFIX,"password","DemoPass123!")),200).path("token").asText();
        require(call("GET","/auth/me",replacedToken,null),401);
        require(call("GET","/houses",viewerToken,null),200);require(call("POST","/houses",viewerToken,houseBody(PREFIX+"-deny")),403);
        require(call("GET","/system/users",viewerToken,null),403);
        assertFalse(require(call("GET","/system/users?query="+PREFIX,token,null),200).toString().contains("password"));
    }
    @Test @Order(10) void accessChangeRevokesSession() throws Exception {
        // 同时尝试登录与禁用：无论登录成功发生在哪个时刻，禁用提交后令牌都应不可用。
        ExecutorService executor=Executors.newFixedThreadPool(2);
        CountDownLatch start=new CountDownLatch(1);
        try {
            Future<Reply> login=executor.submit(()->{start.await();return call("POST","/auth/login",null,Map.of("username",PREFIX,"password","DemoPass123!"));});
            Future<Reply> disable=executor.submit(()->{start.await();return call("PUT","/system/users/"+viewerId+"/access",token,Map.of("role","viewer","status","1"));});
            start.countDown();require(disable.get(20,TimeUnit.SECONDS),200);
            Reply result=login.get(20,TimeUnit.SECONDS);
            if(result.status==200)require(call("GET","/auth/me",result.data().path("token").asText(),null),401);
            else require(result,401);
        } finally {executor.shutdownNow();}
        require(call("GET","/houses",viewerToken,null),401);
        require(call("POST","/auth/login",null,Map.of("username",PREFIX,"password","DemoPass123!")),401);
    }
    @Test @Order(11) void logicalDeletionAndAudit() throws Exception {
        require(call("DELETE","/houses/"+house,token,null),200);require(call("DELETE","/tenants/"+tenant,token,null),200);
        assertEquals(0,require(call("GET","/houses?query="+PREFIX,token,null),200).path("total").asInt());
        assertTrue(require(call("GET","/system/audits",token,null),200).path("total").asLong()>0);
    }
    @Test @Order(12) void repeatedLoginFailureIsThrottled() throws Exception {
        Map<String,Object> bad=Map.of("username",PREFIX+"bad","password","WrongPass123!");
        for(int index=0;index<5;index++)require(call("POST","/auth/login",null,bad),401);
        require(call("POST","/auth/login",null,bad),429);
    }
    @AfterAll static void logout() throws Exception {if(token!=null){require(call("POST","/auth/logout",token,null),200);require(call("GET","/auth/me",token,null),401);}}
}
