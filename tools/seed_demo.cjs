// 通过真实业务 API 创建虚构样例；不会清空原有记录，也不会直接改数据库。
const base=process.env.RENTAL_API||'http://backend:8080/admin/api';
let token;
async function request(method,path,body){
  const response=await fetch(base+path,{method,headers:{'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{})},body:body===undefined?undefined:JSON.stringify(body)});
  const result=await response.json();
  if(!response.ok)throw new Error(result.message||'HTTP '+response.status);
  return result.data;
}
async function main(){
  token=(await request('POST','/auth/login',{username:'admin',password:process.env.DEMO_PASSWORD||'111111'})).token;
  try{
    // 已经导入时保留用户之后的编辑；不把业务数据重置为初始样例。
    if((await request('GET','/houses?query=DEMO-')).total){console.log('演示房源已存在，保留当前数据。');return;}
    const samples=[['DEMO-A101','青秀 · 采光一居',42,1800,'VACANT'],['DEMO-A202','江畔 · 通透两居',68,2600,'VACANT'],['DEMO-B301','万象 · 三房整租',86,3200,'VACANT'],['DEMO-B402','轻寓 · 独立单间',35,1500,'VACANT'],['DEMO-C501','梧桐 · 温馨两居',62,2300,'VACANT'],['DEMO-C602','林间 · 精装一居',48,2100,'OFFLINE']];
    const houses=[],tenants=[];
    for(const [code,title,area,monthlyRent,status] of samples)houses.push(await request('POST','/houses',{code,title,address:'演示路 '+code+' 号（虚构地址）',area,monthlyRent,deposit:monthlyRent,status,version:0}));
    for(const [index,name] of ['陈示例','林演示','王样本'].entries())tenants.push(await request('POST','/tenants',{name,phone:'1380000000'+(index+1),email:'demo'+(index+1)+'@example.invalid',remark:'虚构演示数据'}));
    // 按服务约定生成当月到期账单，不使用依赖时区的 UTC 月份截断。
    const now=new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Hong_Kong',year:'numeric',month:'2-digit',day:'2-digit'}).formatToParts(new Date());
    const part=type=>now.find(value=>value.type===type).value;
    const startDate=part('year')+'-'+part('month')+'-01';
    for(const [index,amount] of [1200,2600].entries()){
      const contract=await request('POST','/contracts',{houseId:houses[index].id,tenantId:tenants[index].id,startDate,months:6});
      const bill=(await request('GET','/bills?contractId='+contract.id)).records[0];
      await request('POST','/bills/'+bill.id+'/payments',{amount,requestId:'DEMO-PAY-'+(index+1),method:'BANK',note:'虚构线下收款演示记录'});
    }
    console.log('已创建 6 套房源、3 位虚构租客、2 份合同、12 张月账单、2 笔收款记录。');
  }finally{await request('POST','/auth/logout');}
}
main().catch(error=>{console.error(error.message);process.exitCode=1;});
