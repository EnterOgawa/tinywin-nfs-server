$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot "build-environment.ps1")
$components = Get-TinyWinNfsComponents
$serviceDir = Join-Path $root "service\winsw"
$target = Join-Path $serviceDir "nfs-server.exe"

Assert-TinyWinNfsWorkspacePath -Path $serviceDir
if( !(Test-Path -LiteralPath $serviceDir)) {
	New-Item -ItemType Directory -Path $serviceDir | Out-Null
}

Get-TinyWinNfsPinnedArtifact -Artifact $components.winsw.binary -Destination $target
Get-FileHash -Algorithm SHA256 -LiteralPath $target
