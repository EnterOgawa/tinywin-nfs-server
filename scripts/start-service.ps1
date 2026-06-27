$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$serviceName = "TinyWinNfsServer"
$serviceNames = @("TinyWinNfsServer", "OgawaNfsServer", "QnxNfsServer")
$configPath = Join-Path $root "conf\nfs-server.properties"

function Get-ConfigInt {
	param(
		[string]$Key,
		[int]$DefaultValue
	)

	if( !(Test-Path -LiteralPath $configPath)) {
		return $DefaultValue
	}

	$line = Get-Content -LiteralPath $configPath | Where-Object { $_ -match "^\s*$([regex]::Escape($Key))\s*=" } | Select-Object -First 1

	if( [string]::IsNullOrWhiteSpace($line)) {
		return $DefaultValue
	}

	$value = ($line -split "=", 2)[1].Trim()
	$parsed = 0

	if( [int]::TryParse($value, [ref]$parsed)) {
		return $parsed
	}

	return $DefaultValue
}

function Wait-UdpPorts {
	param(
		[int[]]$Ports,
		[int]$TimeoutSeconds = 30
	)

	$deadline = (Get-Date).AddSeconds($TimeoutSeconds)

	while( (Get-Date) -lt $deadline) {
		$missing = @()

		foreach( $port in $Ports) {
			$endpoint = Get-NetUDPEndpoint -LocalPort $port -ErrorAction SilentlyContinue

			if( $endpoint -eq $null) {
				$missing += $port
			}
		}

		if( $missing.Count -eq 0) {
			return
		}

		Start-Sleep -Milliseconds 500
	}

	throw "UDP ports did not start listening: $($Ports -join ', ')"
}

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

if( $existing.Status -ne "Running") {
	Start-Service -Name $serviceName
	$started = Get-Service -Name $serviceName
	$started.WaitForStatus("Running", [TimeSpan]::FromSeconds(30))
}

$ports = @()
$ports += Get-ConfigInt -Key "portmap.port" -DefaultValue 111
$ports += Get-ConfigInt -Key "nfs.port" -DefaultValue 2049
$ports += Get-ConfigInt -Key "mount.port" -DefaultValue 20048
Wait-UdpPorts $ports
Get-Service -Name $serviceName
