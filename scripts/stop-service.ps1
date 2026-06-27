$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$serviceName = "TinyWinNfsServer"
$serviceNames = @("TinyWinNfsServer", "OgawaNfsServer", "QnxNfsServer")

$existing = $null

foreach( $candidate in $serviceNames) {
	$existing = Get-Service -Name $candidate -ErrorAction SilentlyContinue

	if( $existing -ne $null) {
		$serviceName = $candidate
		break
	}
}

if( $existing -eq $null) {
	throw "Service is not installed: TinyWinNfsServer"
}

if( $existing.Status -eq "Stopped") {
	Get-Service -Name $serviceName
	exit 0
}

Stop-Service -Name $serviceName
Get-Service -Name $serviceName
