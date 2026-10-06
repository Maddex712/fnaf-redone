param([switch]$Package,[string]$OutputDirectory=(Join-Path $PSScriptRoot 'build/dist'))
$ErrorActionPreference='Stop'
$root=$PSScriptRoot
$version=(Get-Content (Join-Path $root 'VERSION') -Raw).Trim()
$out=Join-Path $root 'build'
$deps=Join-Path $out 'deps'
$classes=Join-Path $out 'classes'
New-Item -ItemType Directory -Force $deps,$classes | Out-Null
function Check($name){if($LASTEXITCODE -ne 0){throw "$name failed ($LASTEXITCODE)"}}
$downloads=@(
    @{File='jna-5.14.0.jar';Url='https://repo.maven.apache.org/maven2/net/java/dev/jna/jna/5.14.0/jna-5.14.0.jar';Hash='34ed1e1f27fa896bca50dbc4e99cf3732967cec387a7a0d5e3486c09673fe8c6'},
    @{File='json-20240303.jar';Url='https://repo.maven.apache.org/maven2/org/json/json/20240303/json-20240303.jar';Hash='3cf6cd6892e32e2b4c1c39e0f52f5248a2f5b37646fdfbb79a66b46b618414ed'}
)
foreach($dep in $downloads){$path=Join-Path $deps $dep.File;if(!(Test-Path $path)){Invoke-WebRequest $dep.Url -OutFile $path};if((Get-FileHash $path -Algorithm SHA256).Hash -ne $dep.Hash){throw "Dependency checksum mismatch: $($dep.File)"}}
& python (Join-Path $root 'generate.py');Check 'Generate shared sources'
$sources=@(Get-ChildItem (Join-Path $root 'src'),(Join-Path $out 'generated') -Recurse -Filter '*.java' | ForEach-Object FullName)
& javac --release 17 -encoding UTF-8 -cp "$deps/*" -d $classes @sources;Check 'Compile desktop client'
Copy-Item (Join-Path $root '../app/src/main/assets') $classes -Recurse -Force
$inputDir=Join-Path $out 'input'
New-Item -ItemType Directory -Force $inputDir | Out-Null
& jar --create --file (Join-Path $inputDir 'fnaf-redone.jar') --main-class dev.thor.fnaf.MainActivity -C $classes .;Check 'Create desktop JAR'
Copy-Item "$deps/*.jar" $inputDir -Force
if($Package){
    $dest=$OutputDirectory
    if(Test-Path (Join-Path $dest 'FNaF Redone')){throw 'App image already exists; move it aside before packaging again.'}
    & jpackage --type app-image --name 'FNaF Redone' --app-version $version --vendor 'FNaF Redone contributors' --dest $dest --input $inputDir --main-jar fnaf-redone.jar --main-class dev.thor.fnaf.MainActivity --java-options '-Xmx768m' --add-modules java.base,java.desktop,java.logging,java.prefs,java.management,jdk.unsupported,jdk.crypto.ec --jlink-options '--strip-debug --no-man-pages --no-header-files'
    Check 'Package Windows app image'
    $bundle=Join-Path $dest 'FNaF Redone'
    Copy-Item (Join-Path $root 'README.md') (Join-Path $bundle 'START-HERE.md')
    foreach($notice in @('THIRD-PARTY.md','JNA-LICENSE.txt','JSON-LICENSE.txt','APACHE-2.0.txt')){Copy-Item (Join-Path $root $notice) $bundle}
    Copy-Item (Join-Path $root '../LICENSE'),(Join-Path $root '../CREDITS.md') $bundle
    Write-Output (Join-Path $dest 'FNaF Redone/FNaF Redone.exe')
}
