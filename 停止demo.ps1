# 停止本项目的服务，保留数据库、日志和缓存卷，不关闭其他 Docker 项目。
$ErrorActionPreference='Stop'
$composeArguments=@('-p','rentalsys-demo','-f',(Join-Path $PSScriptRoot 'deploy/compose.yaml'))
$environmentFile=Join-Path $PSScriptRoot '.env'
if(Test-Path -LiteralPath $environmentFile){$composeArguments+=@('--env-file',$environmentFile)}
docker compose @composeArguments --profile web stop
if($LASTEXITCODE -ne 0){throw '服务停止失败'}
Write-Host '项目已停止，业务数据和日志已保留。'
