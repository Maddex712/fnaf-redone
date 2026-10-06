param([switch]$Installed)
$ErrorActionPreference='Stop'
$build=Join-Path $PSScriptRoot 'build'
$testClasses=Join-Path $build 'locator-tests'
New-Item -ItemType Directory -Force $testClasses | Out-Null
& javac --release 17 -encoding UTF-8 -d $testClasses (Join-Path $PSScriptRoot 'src/dev/thor/fnaf/SteamLocator.java') (Join-Path $PSScriptRoot 'tests/SteamLocatorTest.java')
if($LASTEXITCODE -ne 0){throw 'Steam locator test compilation failed'}
$options=@();if($Installed){$options+='--installed'}
& java -cp $testClasses dev.thor.fnaf.SteamLocatorTest @options
if($LASTEXITCODE -ne 0){throw 'Steam locator checks failed'}
