param([switch]$Force)
# Maven 构建不依赖 out/ 下的旧 JAR；相同源码指纹可复用已验证产物。
$ErrorActionPreference='Stop'
$root=$PSScriptRoot
$runDirectory=Join-Path $root '.local-run'
New-Item -ItemType Directory -Path $runDirectory -Force | Out-Null
$inputs=@(Get-ChildItem (Join-Path $root 'system-admin/src') -File -Recurse)
$inputs+=Get-ChildItem (Join-Path $root 'system-admin/web') -File -Recurse
$inputs+=Get-Item (Join-Path $root 'pom.xml'),(Join-Path $root 'system-admin/pom.xml'),$PSCommandPath
$hashes=$inputs|Sort-Object FullName|ForEach-Object{(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash}
$sha=[Security.Cryptography.SHA256]::Create()
$fingerprint=[BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes(($hashes -join "`n")))).Replace('-','')
$sha.Dispose()
$fingerprintPath=Join-Path $runDirectory 'maven-fingerprint.txt'
$war=Join-Path $root 'system-admin/target/admin.war'
if(-not $Force -and (Test-Path $war) -and (Test-Path $fingerprintPath) -and (Get-Content $fingerprintPath -Raw).Trim() -eq $fingerprint){Write-Host '源码未变，复用 Maven 构建结果。';return}
# WAR 文件替换前停止本项目的读取者，防止运行中自动部署半成品。
docker compose -p rentalsys-demo -f (Join-Path $root 'deploy/compose.yaml') stop backend
docker compose -p rentalsys-qa -f (Join-Path $root 'deploy/compose.yaml') stop backend
docker run --rm --pull never --mount "type=bind,source=$root,target=/workspace" --mount 'type=volume,source=rentalsys-maven-cache,target=/root/.m2' -w /workspace maven:3.10.0-eclipse-temurin-11-noble mvn -B -ntp clean package
if($LASTEXITCODE -ne 0){throw 'Maven 编译、单元测试或 WAR 打包失败'}
[IO.File]::WriteAllText($fingerprintPath,$fingerprint,[Text.UTF8Encoding]::new($false))
Write-Host 'Java 编译、单元测试和 WAR 打包完成。'