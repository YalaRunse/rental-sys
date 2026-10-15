param([switch]$KeepEnvironment)
# 在隔离的测试项目中执行 Java HTTP 集成测试，不向演示数据库写入测试记录。
$ErrorActionPreference='Stop'
$root=$PSScriptRoot
$compose=Join-Path $root 'deploy\compose.yaml'
& (Join-Path $root '编译项目.ps1')
$oldPort=$env:BACKEND_PORT
$env:BACKEND_PORT='18089'
try {
    docker compose -p rentalsys-qa -f $compose up -d --pull never --wait --wait-timeout 150 mysql redis backend
    if($LASTEXITCODE -ne 0){throw '独立测试环境未就绪'}
    docker run --rm --pull never --network rentalsys-qa_default --mount "type=bind,source=$root,target=/workspace" --mount 'type=volume,source=rentalsys-maven-cache,target=/root/.m2' -w /workspace maven:3.10.0-eclipse-temurin-11-noble mvn -B -ntp -pl system-admin -Pintegration '-Dapi.baseUrl=http://backend:8080/admin/api' org.apache.maven.plugins:maven-failsafe-plugin:3.5.2:integration-test org.apache.maven.plugins:maven-failsafe-plugin:3.5.2:verify
    if($LASTEXITCODE -ne 0){throw '集成测试失败，请查看 system-admin/target/failsafe-reports'}
    Write-Host 'Java 单元测试与独立环境接口测试通过。'
} finally {
    if(-not $KeepEnvironment){docker compose -p rentalsys-qa -f $compose stop}
    if($null -eq $oldPort){Remove-Item Env:BACKEND_PORT -ErrorAction SilentlyContinue}else{$env:BACKEND_PORT=$oldPort}
}
