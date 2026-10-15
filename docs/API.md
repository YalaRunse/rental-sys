# API 文档

基础地址：http://localhost/admin/api（页面同源代理）；后端直连为 http://127.0.0.1:8088/admin/api。

除健康检查和登录外，均要求 Authorization: Bearer <token>。成功使用 HTTP 200；写入接口没有使用 201/204，调用方按统一响应处理。

## 统一协议

~~~json
{"success":true,"code":"OK","message":"操作成功","data":{"records":[],"total":0,"page":1,"size":10},"requestId":"..."}
~~~

错误格式同样包含 success=false、code、message、data=null 和 requestId。响应头 X-Request-Id 与日志关联。业务 code 的精确值以响应与源码为准。

| HTTP 状态 | 含义 |
| --- | --- |
| 400 | 参数缺失、格式错误、分页越界、日期或精度非法 |
| 401 | 未登录、令牌失效、凭证错误或用户停用 |
| 403 | 已登录但权限不足 |
| 404 | 访问的业务记录不存在 |
| 409 | 编号重复、状态冲突、版本过期、超额或幂等冲突 |
| 429 | 登录失败次数达到限制 |
| 500 / 503 | 未预期服务故障 / 会话服务暂不可用 |

列表使用 page=1、size=10、query=""；page 最大 100000，size 为 1—100。房源、合同、账单可传 status，账单额外支持 contractId。操作日志仅支持分页。

## 身份与权限

| 方法 | 路径 | 请求 / 返回 |
| --- | --- | --- |
| GET | /health | 公开探活；数据库与 Redis 正常时 status=UP |
| POST | /auth/login | username、password；返回 token、expiresIn、user |
| GET | /auth/me | 安全身份、roles、permissions |
| POST | /auth/logout | 撤销本次会话，data=null |

~~~json
{"username":"admin","password":"111111"}
~~~

登录账号 5—30 个字母、数字或下划线；登录密码 6—64 字符，兼容原管理员。新建账号密码要求 8—64 字符。默认口令仅用于本机演示。同账号再次登录会撤销上次令牌。

| 角色 | 权限 |
| --- | --- |
| admin | *，可使用所有业务与系统接口 |
| operator | rental:read、rental:write、finance:collect |
| viewer | rental:read |

## 房源与租客

| 方法 | 路径 | 权限 | 用途 |
| --- | --- | --- | --- |
| GET / POST | /houses | read / write | 分页查询 / 新建 |
| PUT / DELETE | /houses/{id} | rental:write | 编辑 / 逻辑归档 |
| GET / POST | /tenants | read / write | 分页查询 / 新建 |
| PUT / DELETE | /tenants/{id} | rental:write | 编辑 / 逻辑归档 |

read / write 分别表示 rental:read / rental:write。

~~~json
{"code":"DEMO-NEW01","title":"示例一居","address":"虚构地址","area":42,"monthlyRent":1800.00,"deposit":1800.00,"status":"VACANT","version":0}
~~~

code：1—32 个字母、数字、横线或下划线；title 最多 80，address 最多 200；面积 1—9999.99，金额最多两位小数，租金大于零、押金可为零。提交状态只能 VACANT / OFFLINE；OCCUPIED 由合同驱动。编辑必须携带查询返回的 version，过期返回 409。

~~~json
{"name":"张示例","phone":"13800000009","email":"sample@example.invalid","remark":"虚构演示资料"}
~~~

姓名最多 40，phone 为 11 位中国大陆手机号格式且唯一，email 最多 100，remark 最多 200。已归档的编码和手机号仍占唯一值，当前没有复用或恢复接口。

## 合同、账单与收款

| 方法 | 路径 | 权限 | 用途 |
| --- | --- | --- | --- |
| GET / POST | /contracts | read / write | 分页查询 / 签约 |
| POST | /contracts/{id}/close | rental:write | 无请求体；结算退租 |
| GET | /bills | rental:read | 分页、状态和合同筛选 |
| GET | /bills/{id}/payments | rental:read | 收款流水数组 |
| POST | /bills/{id}/payments | finance:collect | 登记收款 |
| GET | /dashboard | rental:read | 真实业务汇总 |

~~~json
{"houseId":3,"tenantId":1,"startDate":"2026-10-15","months":6}
~~~

months 为 1—24；日期年份 2000—2100；签约立即占用房源。截止日为 startDate.plusMonths(months).minusDays(1)，账单依照起租日每月到期。租金和押金取房源快照。

~~~json
{"amount":600.00,"requestId":"PAY-unique-request-0001","method":"BANK","note":"线下转账登记"}
~~~

amount 大于零且最多两位小数；requestId 为 8—64 个字母、数字、横线或下划线；method 为 BANK / CASH / OTHER；note 最多 200。重试必须复用原幂等号与原内容；换号会作为另一笔收款处理。相同请求返回同一流水，不同内容占用同一号返回 409。

状态枚举：房源 VACANT / OCCUPIED / OFFLINE；合同 ACTIVE / CLOSED；账单 UNPAID / PARTIAL / PAID / CANCELLED。退租要求已到期账单全清，未来已收款需要先设计退款流程，本版拒绝直接退租。

## 系统管理

全部要求 system:manage（admin 的 * 覆盖）。

| 方法 | 路径 | 请求 / 用途 |
| --- | --- | --- |
| GET / POST | /system/users | 分页查询 / 新建账号 |
| PUT | /system/users/{id}/access | role、status，修改后撤销会话 |
| GET | /system/roles | 角色与权限数组 |
| GET | /system/audits | 按时间倒序的写操作审计 |

~~~json
{"username":"demo_viewer","nickname":"演示观察员","password":"DemoPass123!","role":"viewer","email":"viewer@example.invalid","phone":""}
~~~

~~~json
{"role":"operator","status":"0"}
~~~

status 为字符串 "0"（启用）或 "1"（停用）；不能修改自身角色和启停状态。返回账号模型不包含密码或散列；没有密码重置或账号删除接口。

自动化调用示例见 system-admin/src/test/java/com/rental/RentalApiIT.java；页面菜单“接口说明”也提供简表。
