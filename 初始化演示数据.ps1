# 使用已有官方 Node 镜像调用业务 API，Windows 无需安装 Node 或 Python。
$ErrorActionPreference='Stop'
$toolsDirectory=Join-Path $PSScriptRoot 'tools'
$arguments=@('run','--rm','--pull','never','--network','rentalsys-demo_default','--mount',"type=bind,source=$toolsDirectory,target=/tools,readonly")
if($env:DEMO_PASSWORD){$arguments+=@('--env','DEMO_PASSWORD')}
$arguments+=@('node:22-bookworm','node','/tools/seed_demo.cjs')
& docker @arguments
if($LASTEXITCODE -ne 0){throw '示例导入未完成；请检查服务和管理员口令。脚本不会清空现有数据。'}
