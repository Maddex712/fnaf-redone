$ErrorActionPreference='Stop'
$build=Join-Path $PSScriptRoot 'build'
$tests=Join-Path $build 'tests'
New-Item -ItemType Directory -Force $tests | Out-Null
$classpath="$(Join-Path $build 'classes');$(Join-Path $build 'deps')/*;$tests"
& javac --release 17 -encoding UTF-8 -cp $classpath -d $tests (Join-Path $PSScriptRoot 'tests/DesktopFramePumpTest.java') (Join-Path $PSScriptRoot 'tests/DesktopSettingsTest.java') (Join-Path $PSScriptRoot 'tests/ControllerReaderTest.java') (Join-Path $PSScriptRoot 'tests/RendererRegressionTest.java') (Join-Path $PSScriptRoot 'tests/AudioThreadTest.java')
if($LASTEXITCODE -ne 0){throw 'Desktop test compilation failed'}
foreach($suite in @('DesktopFramePumpTest','DesktopSettingsTest','ControllerReaderTest','RendererRegressionTest')){
    & java -cp $classpath "dev.thor.fnaf.$suite"
    if($LASTEXITCODE -ne 0){throw "$suite failed"}
}

& java -cp $classpath android.media.AudioThreadTest
if($LASTEXITCODE -ne 0){throw "Audio thread isolation failed"}
