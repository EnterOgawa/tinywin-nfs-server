$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$isccCandidates = @(
	"C:\Program Files (x86)\Inno Setup 6\ISCC.exe",
	"C:\Program Files\Inno Setup 6\ISCC.exe",
	"C:\Program Files (x86)\Inno Setup 5\ISCC.exe",
	"C:\Program Files\Inno Setup 5\ISCC.exe"
)
$iscc = $isccCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
$iss = Join-Path $root "installer\tinywin-nfs-server.iss"
$output = Join-Path $root "dist\installer\TinyWinNfsSetup.exe"

if( [string]::IsNullOrWhiteSpace($iscc)) {
	throw "Inno Setup compiler ISCC.exe is not found."
}

if( !(Test-Path -LiteralPath $iss)) {
	throw "Inno Setup script is not found: $iss"
}

& (Join-Path $root "scripts\package-manager.ps1")

& $iscc $iss

if( !(Test-Path -LiteralPath $output)) {
	throw "Installer was not created: $output"
}

Write-Host "Installer created: $output"
