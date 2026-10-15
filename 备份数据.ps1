# 使用容器内文件导出，避免 PowerShell 重定向改写 SQL 编码。
$ErrorActionPreference='Stop'
$backupDirectory=Join-Path $PSScriptRoot '.local-run/backups'
New-Item -ItemType Directory -Path $backupDirectory -Force | Out-Null
$filename='rental-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'.sql'
$destination=Join-Path $backupDirectory $filename
docker exec rentalsys-demo-mysql-1 sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -uroot --single-transaction --no-tablespaces --databases rental-system > /tmp/rental-backup.sql'
if($LASTEXITCODE -ne 0){throw '数据库导出失败'}
docker cp 'rentalsys-demo-mysql-1:/tmp/rental-backup.sql' $destination
if($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $destination) -or (Get-Item -LiteralPath $destination).Length -eq 0){throw '备份提取或校验失败'}
Write-Host "数据库备份已保存：$destination"
