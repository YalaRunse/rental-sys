param([switch]$Force)
# 始终按锁文件在临时 Node 容器构建，不使用 Windows 上可能过期的 node_modules。
$ErrorActionPreference='Stop'
$projectDirectory=Split-Path -Parent $PSScriptRoot
$uiDirectory=Join-Path $projectDirectory 'ydl-ui'
$runDirectory=Join-Path $projectDirectory '.local-run'
New-Item -ItemType Directory -Path $runDirectory -Force | Out-Null
$inputs=@(Get-ChildItem (Join-Path $uiDirectory 'src') -File -Recurse)
$inputs+=Get-ChildItem (Join-Path $uiDirectory 'public') -File -Recurse
foreach($name in @('package.json','package-lock.json','babel.config.js','vue.config.js','jsconfig.json')){$inputs+=Get-Item (Join-Path $uiDirectory $name)}
$inputs+=Get-Item $PSCommandPath
$hashes=$inputs|Sort-Object FullName|ForEach-Object{$_.FullName+':'+(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash}
$sha=[Security.Cryptography.SHA256]::Create()
$fingerprint=[BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes(($hashes -join "`n")))).Replace('-','')
$sha.Dispose()
$fingerprintPath=Join-Path $runDirectory 'ui-fingerprint.txt'
$distDirectory=Join-Path $uiDirectory 'dist'
if(-not $Force -and (Test-Path (Join-Path $distDirectory 'index.html')) -and (Test-Path $fingerprintPath) -and (Get-Content $fingerprintPath -Raw).Trim() -eq $fingerprint){Write-Host '前端源码和锁文件未变，复用生产构建。';return}
$buildName='rentalsys-ui-build-'+[Guid]::NewGuid().ToString('N').Substring(0,10)
$stagingDirectory=Join-Path $runDirectory ($buildName+'-dist')
try {
    # 源码只读挂载；npm ci 只写容器文件系统，下载缓存放在项目专属卷。
    docker run --name $buildName --pull never --mount "type=bind,source=$uiDirectory,target=/source,readonly" --mount 'type=volume,source=rentalsys-npm-cache,target=/root/.npm' -w /app node:22-bookworm sh -c 'cp /source/package.json /source/package-lock.json . && npm ci --no-audit --no-fund && cp -r /source/src /source/public /source/babel.config.js /source/vue.config.js /source/jsconfig.json . && npm run build'
    if($LASTEXITCODE -ne 0){throw '锁文件依赖安装或前端构建失败，原有 dist 保留'}
    New-Item -ItemType Directory -Path $stagingDirectory -Force | Out-Null
    docker cp "${buildName}:/app/dist/." $stagingDirectory
    if($LASTEXITCODE -ne 0 -or -not (Test-Path (Join-Path $stagingDirectory 'index.html'))){throw '前端构建产物提取失败'}
    # 删除前核对最终绝对路径，仅替换本项目可再生成的 dist 目录。
    $resolvedDist=[IO.Path]::GetFullPath($distDirectory)
    $expectedDist=[IO.Path]::GetFullPath((Join-Path $projectDirectory 'ydl-ui/dist'))
    if($resolvedDist -ne $expectedDist){throw 'dist 路径校验失败'}
    if(Test-Path -LiteralPath $resolvedDist){Remove-Item -LiteralPath $resolvedDist -Recurse -Force}
    Move-Item -LiteralPath $stagingDirectory -Destination $resolvedDist
    [IO.File]::WriteAllText($fingerprintPath,$fingerprint,[Text.UTF8Encoding]::new($false))
} finally {
    docker rm -f $buildName | Out-Null
}
Write-Host '前端锁文件安装与生产构建完成。'
