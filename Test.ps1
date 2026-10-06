$ErrorActionPreference = 'Stop'
$destination = Join-Path $PSScriptRoot 'build/jvm-tests'
New-Item -ItemType Directory -Force $destination | Out-Null
$suites = @('GameState', 'Progress', 'NightIntro', 'TitleEffects', 'NightOutro', 'ScreenMode')
$sources = @()
foreach ($suite in $suites) {
    $sources += Join-Path $PSScriptRoot "app/src/main/java/dev/thor/fnaf/$suite.java"
    $sources += Join-Path $PSScriptRoot "tests/${suite}Test.java"
}
$sources += Join-Path $PSScriptRoot 'app/src/main/java/dev/thor/fnaf/NightSkipChord.java'
& javac --release 8 -d $destination @sources
if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed' }
foreach ($suite in $suites) {
    & java -cp $destination "dev.thor.fnaf.${suite}Test"
    if ($LASTEXITCODE -ne 0) { throw "$suite tests failed" }
}
