# 栖居租赁管理前端

Vue 3 + Element Plus，服务端接口统一为 /admin/api。页面和同源代理由项目根的启动demo.ps1 构建并启动。

- src/views：登录、布局、真实经营概览、业务资料/合同/账单/账号页面、接口说明。
- src/api：统一 Axios 响应、Bearer 令牌、401 退出处理。
- src/store / router：安全身份恢复和菜单访问控制；真正的权限校验仍由后端执行。
- package-lock.json：精确依赖。容器运行 npm ci，不使用旧的本机 node_modules。

完整启动、测试、界面截图与项目设计请看上一级 README.md。前端沿用 Vue CLI，构建存在旧工具链与组件包体积提示，尚未完成按需组件拆包。
