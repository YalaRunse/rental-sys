param([switch]$Rebuild)
# 一条命令启动后端、MySQL、Redis 和生产静态页面，数据卷继续保留。
$ErrorActionPreference='Stop'
$root=$PSScriptRoot
$compose=Join-Path $root 'deploy/compose.yaml'
$composeArguments=@('-p','rentalsys-demo','-f',$compose)
# 明确读取项目根配置，避免从其他工作目录启动时漏读 .env。
$environmentFile=Join-Path $root '.env'
if(Test-Path -LiteralPath $environmentFile){$composeArguments+=@('--env-file',$environmentFile)}
docker version --format '{{.Server.Version}}'
if($LASTEXITCODE -ne 0){throw '请先启动 Docker Desktop。通信文件故障的恢复方法见 docs/运维与部署.md'}
& (Join-Path $root '编译项目.ps1') -Force:$Rebuild
# 只结束旧启动方式遗留的本项目 Node 进程。
$pidFile=Join-Path $root '.local-run/frontend.pid'
if(Test-Path $pidFile){
  $oldId=[int](Get-Content $pidFile)
  $old=Get-CimInstance Win32_Process -Filter "ProcessId = $oldId"
  if($old -and $old.CommandLine.Contains((Join-Path $root '.local-run/frontend.cjs'))){Stop-Process -Id $oldId}
}
& (Join-Path $root 'tools/build_frontend.ps1') -Force:$Rebuild
docker compose @composeArguments --profile web build frontend
if($LASTEXITCODE -ne 0){throw '前端镜像构建失败'}
docker compose @composeArguments --profile web up -d --pull never --wait --wait-timeout 150
if($LASTEXITCODE -ne 0){throw '服务未就绪，请查看 Compose 日志'}
Write-Host '启动完成：默认页面 http://localhost/login；自定义 WEB_PORT 时请使用对应端口。'
Write-Host '演示管理员：admin / 111111；默认口令沿用原学习项目。'
