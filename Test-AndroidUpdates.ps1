$ErrorActionPreference='Stop'
$deps=Join-Path $PSScriptRoot 'desktop/build/deps'
New-Item -ItemType Directory -Force $deps | Out-Null
$json=Join-Path $deps 'json-20240303.jar'
if(!(Test-Path $json)){Invoke-WebRequest 'https://repo.maven.apache.org/maven2/org/json/json/20240303/json-20240303.jar' -OutFile $json}
if((Get-FileHash $json -Algorithm SHA256).Hash -ne '3cf6cd6892e32e2b4c1c39e0f52f5248a2f5b37646fdfbb79a66b46b618414ed'){throw 'JSON dependency checksum mismatch'}
$destination=Join-Path $PSScriptRoot 'build/android-update-tests'
New-Item -ItemType Directory -Force $destination | Out-Null
& javac --release 8 -cp $json -d $destination (Join-Path $PSScriptRoot 'app/src/main/java/dev/thor/fnaf/AndroidReleases.java') (Join-Path $PSScriptRoot 'app/src/main/java/dev/thor/fnaf/AndroidUpdateTransfer.java') (Join-Path $PSScriptRoot 'tests/AndroidUpdatesTest.java')
if($LASTEXITCODE -ne 0){throw 'Android update test compilation failed'}
& java -cp "$destination$([IO.Path]::PathSeparator)$json" dev.thor.fnaf.AndroidUpdatesTest
if($LASTEXITCODE -ne 0){throw 'Android update tests failed'}
