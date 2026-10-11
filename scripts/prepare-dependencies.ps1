param([switch]$IncludeWinSW)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot "build-environment.ps1")
$components = Get-TinyWinNfsComponents
$swt = Join-Path $root "lib\org.eclipse.swt.win32.win32.x86_64.jar"
Get-TinyWinNfsPinnedArtifact -Artifact $components.swt.binary -Destination $swt

# サービスラッパーはパッケージ作成時、または明示指定時だけ取得する。
if( $IncludeWinSW) {
	& (Join-Path $PSScriptRoot "download-winsw.ps1")
}

Write-Host "Dependencies verified: SWT $($components.swt.version)"
