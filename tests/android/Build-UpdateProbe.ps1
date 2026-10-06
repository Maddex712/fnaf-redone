param([Parameter(Mandatory=$true)][string]$Toolchain,[Parameter(Mandatory=$true)][string]$SigningKey,[string]$KeyAlias='thor')
$ErrorActionPreference='Stop'
$repo=(Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$out=Join-Path $repo 'build/android-update-device'
New-Item -ItemType Directory -Force $out,(Join-Path $out 'classes'),(Join-Path $out 'dex') | Out-Null
$androidJar=Get-ChildItem (Join-Path $Toolchain 'platform') -Filter android.jar -Recurse | Select-Object -First 1 -ExpandProperty FullName
$aapt=Get-ChildItem (Join-Path $Toolchain 'build') -Filter aapt2.exe -Recurse | Select-Object -First 1 -ExpandProperty FullName
$tools=Split-Path $aapt
$env:JAVA_HOME=Split-Path (Split-Path (Get-Command javac.exe).Source)
function Check {if($LASTEXITCODE -ne 0){throw 'Probe build failed'}}
function Finish-Apk($apk,$dex,$name){
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip=[IO.Compression.ZipFile]::Open($apk,[IO.Compression.ZipArchiveMode]::Update)
    try{[IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,$dex,'classes.dex') | Out-Null}finally{$zip.Dispose()}
    & (Join-Path $tools 'zipalign.exe') -f 4 $apk (Join-Path $out 'aligned.apk');Check
    & (Join-Path $tools 'apksigner.bat') sign --ks $SigningKey --ks-key-alias $KeyAlias --ks-pass env:FNAF_SIGNING_PASSWORD --key-pass env:FNAF_SIGNING_PASSWORD --out (Join-Path $out $name) (Join-Path $out 'aligned.apk');Check
}
# A private older-version fixture uses the current updater to exercise a real forward install.
# It is emulator-only, never uploaded; the final public APK still comes from Build.ps1.
& $aapt link -I $androidJar --manifest (Join-Path $repo 'app/src/main/AndroidManifest.xml') -A (Join-Path $repo 'app/src/main/assets') --version-code 4 --version-name '0.1.3-preview' -o (Join-Path $out 'baseline-unsigned.apk') (Join-Path $repo 'build/res.zip');Check
Finish-Apk (Join-Path $out 'baseline-unsigned.apk') (Join-Path $repo 'build/dex/classes.dex') 'baseline.apk'
$manifest=Join-Path $out 'AndroidManifest.xml'
'<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="dev.thor.fnaf.updatetest"><uses-sdk android:minSdkVersion="26" android:targetSdkVersion="35"/><application android:label="Update verification"/><instrumentation android:name="dev.thor.fnaf.UpdateProbe" android:targetPackage="dev.thor.fnaf.preview"/></manifest>' | Set-Content $manifest
& $aapt link -I $androidJar --manifest $manifest -o (Join-Path $out 'probe-unsigned.apk');Check
& javac --release 8 -cp "$androidJar;$(Join-Path $repo 'build/classes')" -d (Join-Path $out 'classes') (Join-Path $PSScriptRoot 'UpdateProbe.java');Check
& jar --create --file (Join-Path $out 'classes.jar') -C (Join-Path $out 'classes') .;Check
& (Join-Path $tools 'd8.bat') --min-api 26 --lib $androidJar --output (Join-Path $out 'dex') (Join-Path $out 'classes.jar');Check
Finish-Apk (Join-Path $out 'probe-unsigned.apk') (Join-Path $out 'dex/classes.dex') 'probe.apk'
Write-Output $out
