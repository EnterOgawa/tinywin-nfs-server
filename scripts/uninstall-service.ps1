$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$serviceExe = Join-Path $root "service\winsw\nfs-server.exe"
$serviceName = "TinyWinNfsServer"
$legacyServiceNames = @("OgawaNfsServer", "QnxNfsServer")

function Test-Administrator {
	$principal = New-Object Security.Principal.WindowsPrincipal([Security.Principal.WindowsIdentity]::GetCurrent())
	return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

if( !(Test-Administrator)) {
	throw "Administrator privileges are required to uninstall the service."
}

if( !(Test-Path -LiteralPath $serviceExe)) {
	throw "WinSW executable is not found: $serviceExe"
}

$existing = Get-Service -Name $serviceName -ErrorAction SilentlyContinue

if( $existing -eq $null) {
	$deletedLegacy = $false

	foreach( $legacyServiceName in $legacyServiceNames) {
		$legacy = Get-Service -Name $legacyServiceName -ErrorAction SilentlyContinue

		if( $legacy -eq $null) {
			continue
		}

		if( $legacy.Status -ne "Stopped") {
			Stop-Service -Name $legacyServiceName -Force
			$legacy.WaitForStatus("Stopped", [TimeSpan]::FromSeconds(30))
		}

		sc.exe delete $legacyServiceName | Out-Host
		$deletedLegacy = $true
	}

	if( $deletedLegacy) {
		exit 0
	}

	Write-Host "Service does not exist: $serviceName"
	exit 0
}

if( $existing.Status -ne "Stopped") {
	& $serviceExe stop
}

& $serviceExe uninstall
