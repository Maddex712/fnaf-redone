param([string]$InstallerDirectory = (Join-Path $PSScriptRoot 'build/installer'))
$ErrorActionPreference = 'Stop'
$compiler = Join-Path $env:WINDIR 'Microsoft.NET/Framework64/v4.0.30319/csc.exe'
$InstallerDirectory = (Resolve-Path -LiteralPath $InstallerDirectory).Path
$test = Join-Path $InstallerDirectory 'InstallerTests.exe'
& $compiler /nologo /target:exe /platform:x64 "/win32manifest:$PSScriptRoot/installer/app.manifest" /r:System.Windows.Forms.dll /r:System.Drawing.dll "/r:$InstallerDirectory/freddyfiles.exe" "/out:$test" (Join-Path $PSScriptRoot 'installer/InstallerTests.cs')
if ($LASTEXITCODE -ne 0) { throw 'Installer test compilation failed.' }
& $test $InstallerDirectory (Join-Path $PSScriptRoot 'updater.ps1')
if ($LASTEXITCODE -ne 0) { throw 'Installer tests failed.' }
