$ErrorActionPreference = "Stop"

$serviceName = "TinyWinNfsServer"
$serviceNames = @("TinyWinNfsServer", "OgawaNfsServer", "QnxNfsServer")
$service = $null

foreach( $candidate in $serviceNames) {
	$service = Get-Service -Name $candidate -ErrorAction SilentlyContinue

	if( $service -ne $null) {
		$serviceName = $candidate
		break
	}
}

if( $service -eq $null) {
	Write-Host "Service is not installed: TinyWinNfsServer"
	exit 0
}

$service
sc.exe queryex $serviceName
