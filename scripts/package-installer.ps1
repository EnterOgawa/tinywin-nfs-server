param(
	[string]$JdkPath = "",
	[string]$SwtJarPath = "",
	[string]$IsccPath = ""
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$isccCandidates = @(
	"C:\Program Files (x86)\Inno Setup 6\ISCC.exe",
	"C:\Program Files\Inno Setup 6\ISCC.exe"
)
if( [string]::IsNullOrWhiteSpace($IsccPath)) {
	$IsccPath = $env:TINYWIN_NFS_ISCC
}

if( [string]::IsNullOrWhiteSpace($IsccPath)) {
	$IsccPath = $isccCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
}

$iscc = $IsccPath
$iss = Join-Path $root "installer\tinywin-nfs-server.iss"
$output = Join-Path $root "dist\installer\TinyWinNfsSetup.exe"

if( [string]::IsNullOrWhiteSpace($iscc) -or !(Test-Path -LiteralPath $iscc -PathType Leaf)) {
	throw "Inno Setup compiler ISCC.exe is not found."
}

if( !(Test-Path -LiteralPath $iss)) {
	throw "Inno Setup script is not found: $iss"
}

& (Join-Path $root "scripts\package-manager.ps1") -JdkPath $JdkPath -SwtJarPath $SwtJarPath

& $iscc $iss

if( $LASTEXITCODE -ne 0) {
	throw "Inno Setup compilation failed: $LASTEXITCODE"
}

if( !(Test-Path -LiteralPath $output)) {
	throw "Installer was not created: $output"
}

Write-Host "Installer created: $output"
Write-Host "Corresponding sources: $(Join-Path $root 'dist\installer\TinyWinNfsSources.zip')"
Write-Host "Sign the installer manually and publish it with the corresponding source archive."
