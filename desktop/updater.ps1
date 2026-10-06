param(
    [Parameter(Mandatory=$true)][string]$Install,
    [Parameter(Mandatory=$true)][string]$Staged,
    [Parameter(Mandatory=$true)][string]$Data,
    [Parameter(Mandatory=$true)][int]$ParentId,
    [switch]$NoRestart
)
$ErrorActionPreference='Stop'
$movedOld=$false
$installedNew=$false
$gameExited=$false
$backup=''
function SamePath([string]$a,[string]$b){[string]::Equals([IO.Path]::GetFullPath($a),[IO.Path]::GetFullPath($b),[StringComparison]::OrdinalIgnoreCase)}
function RetryMove([string]$from,[string]$to){
    for($attempt=0;$attempt -lt 30;$attempt++){
        try{[IO.Directory]::Move($from,$to);return}
        catch{if($attempt -eq 29){throw};Start-Sleep -Milliseconds 200}
    }
}
function ValidateBundle([string]$path){
    if((Get-Item -LiteralPath $path).Attributes -band [IO.FileAttributes]::ReparsePoint){throw 'Linked application directories are not supported.'}
    foreach($file in @('FNaF Redone.exe','app/fnaf-redone.jar','app/FNaF Redone.cfg','runtime/bin/java.exe','runtime/bin/server/jvm.dll')){
        if(!(Test-Path -LiteralPath (Join-Path $path $file) -PathType Leaf)){throw "Incomplete application: $file"}
    }
}
function RemovePrevious([string]$path){
    # This is the exact sibling owned by this installation, never a supplied arbitrary deletion path.
    if(!(SamePath $path ($Install+'.previous')) -or !(SamePath ([IO.Path]::GetDirectoryName($path)) ([IO.Path]::GetDirectoryName($Install)))){throw 'Invalid backup location.'}
    if(!(Test-Path -LiteralPath $path)){return}
    ValidateBundle $path
    $marker=Join-Path $path '.fnaf-backup-target.txt'
    if(!(Test-Path -LiteralPath $marker) -or !(SamePath ([IO.File]::ReadAllText($marker).Trim()) $Install)){throw 'Backup folder belongs to another installation. Move it aside before updating.'}
    # Refuse junctions/symlinks anywhere in an older backup before recursive removal.
    if(@(Get-ChildItem -LiteralPath $path -Recurse -Force | Where-Object { $_.Attributes -band [IO.FileAttributes]::ReparsePoint }).Count -gt 0){throw 'Backup contains linked files; move it aside before updating.'}
    Remove-Item -LiteralPath $path -Recurse -Force
}
try{
    $Install=[IO.Path]::GetFullPath($Install).TrimEnd('\')
    $Staged=[IO.Path]::GetFullPath($Staged).TrimEnd('\')
    $Data=[IO.Path]::GetFullPath($Data).TrimEnd('\')
    $parent=[IO.Path]::GetDirectoryName($Install)
    if(!$parent -or !(SamePath ([IO.Path]::GetDirectoryName($Staged)) $parent) -or !([IO.Path]::GetFileName($Staged).StartsWith([IO.Path]::GetFileName($Install)+'.update-',[StringComparison]::OrdinalIgnoreCase))){throw 'Update must be staged beside the application.'}
    if((SamePath $Data $Install) -or $Data.StartsWith($Install+'\',[StringComparison]::OrdinalIgnoreCase)){throw 'Save data must be outside the application folder.'}
    ValidateBundle $Install
    ValidateBundle $Staged
    $backup=$Install+'.previous'
    if((SamePath $Data $backup) -or $Data.StartsWith($backup+'\',[StringComparison]::OrdinalIgnoreCase)){throw 'Save data cannot be inside the backup folder.'}
    Set-Content -LiteralPath (Join-Path $PSScriptRoot 'ready') -Value 'ready' -Encoding ascii
    if(Get-Process -Id $ParentId -ErrorAction SilentlyContinue){
        try{Wait-Process -Id $ParentId -Timeout 120 -ErrorAction Stop}
        catch{if(Get-Process -Id $ParentId -ErrorAction SilentlyContinue){throw 'Game did not close. Update was not installed.'}}
    }
    $gameExited=$true
    RemovePrevious $backup
    RetryMove $Install $backup
    $movedOld=$true
    Set-Content -LiteralPath (Join-Path $backup '.fnaf-backup-target.txt') -Value $Install -Encoding utf8
    RetryMove $Staged $Install
    $installedNew=$true
    ValidateBundle $Install
    if(!$NoRestart){Start-Process -FilePath (Join-Path $Install 'FNaF Redone.exe') -WorkingDirectory $Install}
    Set-Content -LiteralPath (Join-Path $PSScriptRoot 'result.txt') -Value 'Update installed; executable path preserved.'
}catch{
    $failure=$_.Exception.Message
    if($movedOld -and (Test-Path -LiteralPath $backup)){
        try{
            if($installedNew -and (Test-Path -LiteralPath $Install)){RetryMove $Install $Staged}
            RetryMove $backup $Install
            $failure+=' Previous version restored.'
            if(!$NoRestart){Start-Process -FilePath (Join-Path $Install 'FNaF Redone.exe') -WorkingDirectory $Install}
        }catch{$failure+=' Recovery copy is at '+$backup+'. '+$_.Exception.Message}
    }
    if($gameExited -and !$movedOld -and !$NoRestart){try{Start-Process -FilePath (Join-Path $Install 'FNaF Redone.exe') -WorkingDirectory $Install}catch{}}
    Set-Content -LiteralPath (Join-Path $PSScriptRoot 'result.txt') -Value $failure
    if(!$NoRestart){Add-Type -AssemblyName System.Windows.Forms;[Windows.Forms.MessageBox]::Show($failure,'FNaF Redone update') | Out-Null}
    exit 1
}
