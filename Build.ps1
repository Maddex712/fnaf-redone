param(
    [Parameter(Mandatory=$true)][string]$Toolchain,
    [Parameter(Mandatory=$true)][string]$SigningKey,
    [string]$KeyAlias='thor',
    [string]$OutputApk=(Join-Path $PSScriptRoot '../FNaF-Redone-0.1.2-preview.apk')
)
$ErrorActionPreference='Stop'
$Toolchain=(Resolve-Path $Toolchain).Path
$androidJar=Get-ChildItem (Join-Path $Toolchain 'platform') -Filter android.jar -Recurse | Select-Object -First 1 -ExpandProperty FullName
$aapt=Get-ChildItem (Join-Path $Toolchain 'build') -Filter aapt2.exe -Recurse | Select-Object -First 1 -ExpandProperty FullName
$buildTools=Split-Path $aapt
if (!$androidJar -or !$aapt) { throw 'Android platform 35 and build tools 35 are required. See README.' }
$jdk=Split-Path (Get-Command javac.exe).Source
$env:JAVA_HOME=Split-Path $jdk
$out=Join-Path $PSScriptRoot 'build'
New-Item -ItemType Directory -Force $out,(Join-Path $out 'classes'),(Join-Path $out 'dex'),(Join-Path $out 'tests') | Out-Null
function Check([string]$Name) { if ($LASTEXITCODE -ne 0) { throw "$Name failed with exit code $LASTEXITCODE" } }
$source=Join-Path $PSScriptRoot 'app/src/main'
& (Join-Path $jdk 'javac.exe') --release 8 -d (Join-Path $out 'tests') (Join-Path $source 'java/dev/thor/fnaf/GameState.java') (Join-Path $PSScriptRoot 'tests/GameStateTest.java') (Join-Path $source 'java/dev/thor/fnaf/NightSkipChord.java') (Join-Path $source 'java/dev/thor/fnaf/Progress.java') (Join-Path $PSScriptRoot 'tests/ProgressTest.java') (Join-Path $source 'java/dev/thor/fnaf/NightIntro.java') (Join-Path $PSScriptRoot 'tests/NightIntroTest.java') (Join-Path $source 'java/dev/thor/fnaf/TitleEffects.java') (Join-Path $PSScriptRoot 'tests/TitleEffectsTest.java') (Join-Path $source 'java/dev/thor/fnaf/NightOutro.java') (Join-Path $PSScriptRoot 'tests/NightOutroTest.java')
Check 'Test compilation'
& (Join-Path $jdk 'java.exe') -cp (Join-Path $out 'tests') dev.thor.fnaf.GameStateTest
Check 'State tests'
& (Join-Path $jdk 'java.exe') -cp (Join-Path $out 'tests') dev.thor.fnaf.ProgressTest
Check 'Progress tests'
& (Join-Path $jdk 'java.exe') -cp (Join-Path $out 'tests') dev.thor.fnaf.NightIntroTest
Check 'Intro tests'
& (Join-Path $jdk 'java.exe') -cp (Join-Path $out 'tests') dev.thor.fnaf.TitleEffectsTest
Check 'Title effect tests'
& (Join-Path $jdk 'java.exe') -cp (Join-Path $out 'tests') dev.thor.fnaf.NightOutroTest
Check 'Outro tests'
& (Join-Path $jdk 'javac.exe') --release 8 -d (Join-Path $out 'tests') (Join-Path $source 'java/dev/thor/fnaf/ScreenMode.java') (Join-Path $PSScriptRoot 'tests/ScreenModeTest.java')
Check 'Screen mode test compilation'
& (Join-Path $jdk 'java.exe') -cp (Join-Path $out 'tests') dev.thor.fnaf.ScreenModeTest
Check 'Screen mode tests'
& $aapt compile --dir (Join-Path $source 'res') -o (Join-Path $out 'res.zip')
Check 'Resource compilation'
& $aapt link -I $androidJar --manifest (Join-Path $source 'AndroidManifest.xml') -A (Join-Path $source 'assets') -0 ttf --min-sdk-version 26 --target-sdk-version 35 --version-code 3 --version-name '0.1.2-preview' -o (Join-Path $out 'unsigned.apk') (Join-Path $out 'res.zip')
Check 'Resource linking'
$files=Get-ChildItem (Join-Path $source 'java') -Recurse -Filter '*.java' | ForEach-Object FullName
& (Join-Path $jdk 'javac.exe') --release 8 -cp $androidJar -d (Join-Path $out 'classes') @files
Check 'Java compilation'
& (Join-Path $jdk 'jar.exe') --create --file (Join-Path $out 'classes.jar') -C (Join-Path $out 'classes') .
Check 'Class archive'
& (Join-Path $buildTools 'd8.bat') --min-api 26 --lib $androidJar --output (Join-Path $out 'dex') (Join-Path $out 'classes.jar')
Check 'DEX compilation'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$apk=[IO.Compression.ZipFile]::Open((Join-Path $out 'unsigned.apk'),[IO.Compression.ZipArchiveMode]::Update)
try { [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($apk,(Join-Path $out 'dex/classes.dex'),'classes.dex') | Out-Null } finally { $apk.Dispose() }
& (Join-Path $buildTools 'zipalign.exe') -f 4 (Join-Path $out 'unsigned.apk') (Join-Path $out 'aligned.apk')
Check 'APK alignment'
if(!(Test-Path -LiteralPath $SigningKey)){throw 'Signing key does not exist'}
if(!$env:FNAF_SIGNING_PASSWORD){throw 'Set FNAF_SIGNING_PASSWORD in your environment (never commit it).'}
$final=$OutputApk
& (Join-Path $buildTools 'apksigner.bat') sign --ks $SigningKey --ks-key-alias $KeyAlias --ks-pass env:FNAF_SIGNING_PASSWORD --key-pass env:FNAF_SIGNING_PASSWORD --out $final (Join-Path $out 'aligned.apk')
Check 'APK signing'
& (Join-Path $buildTools 'apksigner.bat') verify --verbose $final
Check 'APK signature verification'
Get-FileHash $final -Algorithm SHA256
