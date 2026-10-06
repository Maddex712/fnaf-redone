param([string]$AppDirectory=(Join-Path $PSScriptRoot 'build/dist/FNaF Redone'))
$ErrorActionPreference='Stop'
$build=Join-Path $PSScriptRoot 'build'
$tests=Join-Path $build 'tests'
New-Item -ItemType Directory -Force $tests | Out-Null
$classpath="$(Join-Path $AppDirectory 'app')/*;$tests"
& javac --release 17 -encoding UTF-8 -cp $classpath -d $tests (Join-Path $PSScriptRoot 'tests/PackagedUpdaterTest.java') (Join-Path $PSScriptRoot 'tests/UpdaterParentProbe.java') (Join-Path $PSScriptRoot 'tests/UpdaterLaunchProbe.java')
if($LASTEXITCODE -ne 0){throw 'Packaged updater test compilation failed'}
& (Join-Path $AppDirectory 'runtime/bin/java.exe') -cp $classpath dev.thor.fnaf.PackagedUpdaterTest $AppDirectory
if($LASTEXITCODE -ne 0){throw 'Packaged updater round trip failed'}
