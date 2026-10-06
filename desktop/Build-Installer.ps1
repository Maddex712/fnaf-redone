param(
    [string]$PayloadZip,
    [string]$AppDirectory = (Join-Path $PSScriptRoot 'build/dist/FNaF Redone'),
    [string]$OutputDirectory = (Join-Path $PSScriptRoot 'build/installer')
)
$ErrorActionPreference = 'Stop'
$compiler = Join-Path $env:WINDIR 'Microsoft.NET/Framework64/v4.0.30319/csc.exe'
if (!(Test-Path -LiteralPath $compiler)) { throw 'The Windows .NET Framework compiler is required.' }
New-Item -ItemType Directory -Force $OutputDirectory | Out-Null
$OutputDirectory = (Resolve-Path -LiteralPath $OutputDirectory).Path
if (!$PayloadZip) {
    $PayloadZip = Join-Path $OutputDirectory 'payload.zip'
    if (Test-Path -LiteralPath $PayloadZip) { throw 'Payload ZIP exists; select a fresh output directory or pass -PayloadZip.' }
    Compress-Archive -LiteralPath $AppDirectory -DestinationPath $PayloadZip
}
$PayloadZip = (Resolve-Path -LiteralPath $PayloadZip).Path
$version = (Get-Content (Join-Path $PSScriptRoot 'VERSION') -Raw).Trim()
$hash = (Get-FileHash -LiteralPath $PayloadZip -Algorithm SHA256).Hash.ToLowerInvariant()
$info = Join-Path $OutputDirectory 'BuildInfo.cs'
@"
using System.Reflection;
[assembly: System.Runtime.Versioning.TargetFramework(".NETFramework,Version=v4.8")]
[assembly: AssemblyTitle("FreddyFiles")]
[assembly: AssemblyProduct("FNaF Redone")]
[assembly: AssemblyDescription("FNaF Redone Windows installer")]
[assembly: AssemblyVersion("$version.0")]
[assembly: AssemblyFileVersion("$version.0")]
namespace FreddyFiles { public static class BuildInfo {
    public const string Version = "$version";
    public const string PayloadHash = "$hash";
} }
"@ | Set-Content -LiteralPath $info -Encoding UTF8
$source = Join-Path $PSScriptRoot 'installer/FreddyFiles.cs'
$manifest = Join-Path $PSScriptRoot 'installer/app.manifest'
$refs = @('/r:System.Windows.Forms.dll', '/r:System.Drawing.dll', '/r:System.IO.Compression.dll', '/r:System.IO.Compression.FileSystem.dll')
$uninstaller = Join-Path $OutputDirectory 'uninstall.exe'
& $compiler /nologo /target:winexe /platform:x64 /optimize+ /define:UNINSTALL "/win32manifest:$manifest" @refs "/out:$uninstaller" $source $info
if ($LASTEXITCODE -ne 0) { throw 'Uninstaller compilation failed.' }
$installer = Join-Path $OutputDirectory 'freddyfiles.exe'
& $compiler /nologo /target:winexe /platform:x64 /optimize+ "/win32manifest:$manifest" @refs "/resource:$PayloadZip,payload.zip" "/resource:$uninstaller,uninstall.exe" "/out:$installer" $source $info
if ($LASTEXITCODE -ne 0) { throw 'Installer compilation failed.' }
$installerHash = (Get-FileHash -LiteralPath $installer -Algorithm SHA256).Hash.ToLowerInvariant()
"$installerHash  freddyfiles.exe" | Set-Content -LiteralPath (Join-Path $OutputDirectory 'FreddyFiles-SHA256SUMS.txt') -Encoding ascii
Write-Output $installer
