$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$serviceDir = Join-Path $root "service\winsw"
$target = Join-Path $serviceDir "nfs-server.exe"
$url = "https://github.com/winsw/winsw/releases/download/v2.12.0/WinSW-x64.exe"

if( !(Test-Path -LiteralPath $serviceDir)) {
	New-Item -ItemType Directory -Path $serviceDir | Out-Null
}

Invoke-WebRequest -Uri $url -OutFile $target
Get-FileHash -Algorithm SHA256 -LiteralPath $target
