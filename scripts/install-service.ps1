param(
	[switch]$Force
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$serviceDir = Join-Path $root "service\winsw"
$serviceExe = Join-Path $serviceDir "nfs-server.exe"
$serviceXml = Join-Path $serviceDir "nfs-server.xml"
$serviceName = "TinyWinNfsServer"
$legacyServiceNames = @("OgawaNfsServer", "QnxNfsServer")
$dataRoot = if( [string]::IsNullOrWhiteSpace($env:TINYWIN_NFS_DATA)) { Join-Path $env:ProgramData "EnterOgawa\TinyWinNFS Server" } else { $env:TINYWIN_NFS_DATA }
$dataConfDir = Join-Path $dataRoot "conf"
$dataExportDir = Join-Path $dataRoot "export"
$dataLogDir = Join-Path $dataRoot "logs"
$dataStoreDir = Join-Path $dataRoot "data"
$dataConfigPath = Join-Path $dataConfDir "nfs-server.properties"

function Test-Administrator {
	$principal = New-Object Security.Principal.WindowsPrincipal([Security.Principal.WindowsIdentity]::GetCurrent())
	return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function Copy-FileIfMissing {
	param(
		[string]$Source,
		[string]$Destination
	)

	if( !(Test-Path -LiteralPath $Source)) {
		return
	}

	if( Test-Path -LiteralPath $Destination) {
		return
	}

	Copy-Item -LiteralPath $Source -Destination $Destination -Force
}

function Initialize-DataLayout {
	New-Item -ItemType Directory -Path $dataConfDir -Force | Out-Null
	New-Item -ItemType Directory -Path $dataExportDir -Force | Out-Null
	New-Item -ItemType Directory -Path $dataLogDir -Force | Out-Null
	New-Item -ItemType Directory -Path $dataStoreDir -Force | Out-Null

	$configCandidates = @(
		(Join-Path $root "conf\nfs-server.properties"),
		(Join-Path $root "defaults\conf\nfs-server.properties")
	)

	foreach( $candidate in $configCandidates) {
		if( Test-Path -LiteralPath $dataConfigPath) {
			break
		}

		Copy-FileIfMissing -Source $candidate -Destination $dataConfigPath
	}

	$sampleConfigCandidates = @(
		(Join-Path $root "conf\nfs-server-windows-client-test.properties"),
		(Join-Path $root "defaults\conf\nfs-server-windows-client-test.properties"),
		(Join-Path $root "conf\nfs-server-wsl-test.properties"),
		(Join-Path $root "defaults\conf\nfs-server-wsl-test.properties")
	)

	foreach( $sampleConfig in $sampleConfigCandidates) {
		$destination = Join-Path $dataConfDir (Split-Path -Leaf $sampleConfig)
		Copy-FileIfMissing -Source $sampleConfig -Destination $destination
	}

	$exportSource = Join-Path $root "export"

	if( Test-Path -LiteralPath $exportSource) {
		Get-ChildItem -LiteralPath $exportSource -File | ForEach-Object {
			Copy-FileIfMissing -Source $_.FullName -Destination (Join-Path $dataExportDir $_.Name)
		}
	}
}

function Get-ExecutablePathFromServicePath {
	param(
		[string]$PathName
	)

	$value = $PathName.Trim()

	if( $value.StartsWith('"')) {
		$endIndex = $value.IndexOf('"', 1)

		if( $endIndex -gt 1) {
			return $value.Substring(1, $endIndex - 1)
		}
	}

	$exeIndex = $value.ToLowerInvariant().IndexOf(".exe")

	if( $exeIndex -ge 0) {
		return $value.Substring(0, $exeIndex + 4)
	}

	return $null
}

function Test-SamePath {
	param(
		[string]$Left,
		[string]$Right
	)

	if( [string]::IsNullOrWhiteSpace($Left) -or [string]::IsNullOrWhiteSpace($Right)) {
		return $false
	}

	$leftPath = [System.IO.Path]::GetFullPath($Left).TrimEnd('\')
	$rightPath = [System.IO.Path]::GetFullPath($Right).TrimEnd('\')
	return $leftPath.Equals($rightPath, [System.StringComparison]::OrdinalIgnoreCase)
}

if( !(Test-Administrator)) {
	throw "Administrator privileges are required to install the service."
}

if( !(Test-Path -LiteralPath $serviceExe)) {
	throw "WinSW executable is not found. Run scripts\download-winsw.ps1 first."
}

if( !(Test-Path -LiteralPath $serviceXml)) {
	throw "WinSW XML is not found: $serviceXml"
}

Initialize-DataLayout

$compileScript = Join-Path $root "scripts\compile.ps1"
$srcDir = Join-Path $root "src"

if( (Test-Path -LiteralPath $compileScript) -and (Test-Path -LiteralPath $srcDir)) {
	& $compileScript
}

$existing = Get-Service -Name $serviceName -ErrorAction SilentlyContinue

if( $existing -ne $null) {
	$existingService = Get-CimInstance Win32_Service -Filter "Name='$serviceName'"
	$existingExe = Get-ExecutablePathFromServicePath $existingService.PathName

	if( (Test-SamePath $existingExe $serviceExe) -and !$Force) {
		Write-Host "Service already exists: $serviceName"
		exit 0
	}

	if( !(Test-SamePath $existingExe $serviceExe) -and !$Force) {
		throw "Service already exists at: $existingExe. Use -Force to replace it."
	}

	if( $existing.Status -ne "Stopped") {
		Stop-Service -Name $serviceName -Force
		$existing.WaitForStatus("Stopped", [TimeSpan]::FromSeconds(30))
	}

	Write-Host "Replacing service: $serviceName"
	sc.exe delete $serviceName | Out-Host
	Start-Sleep -Seconds 2
}

foreach( $legacyServiceName in $legacyServiceNames) {
	$legacy = Get-Service -Name $legacyServiceName -ErrorAction SilentlyContinue

	if( $legacy -eq $null) {
		continue
	}

	if( !$Force) {
		throw "Legacy service already exists: $legacyServiceName. Use -Force to replace it with $serviceName."
	}

	if( $legacy.Status -ne "Stopped") {
		Stop-Service -Name $legacyServiceName -Force
		$legacy.WaitForStatus("Stopped", [TimeSpan]::FromSeconds(30))
	}

	sc.exe delete $legacyServiceName | Out-Host
	Start-Sleep -Seconds 2
}

& $serviceExe install
sc.exe failure $serviceName reset= 86400 actions= restart/10000/restart/30000/""/60000 | Out-Host
sc.exe failureflag $serviceName 1 | Out-Host
