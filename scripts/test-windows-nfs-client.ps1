param(
	[string]$DriveLetter = "Z",
	[string]$ServerHost = "127.0.0.1",
	[string]$ExportName = "export",
	[ValidateSet("UDP", "TCP")]
	[string]$Transport = "UDP",
	[switch]$KeepWork
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$java = "C:\develop\tools\eclipse202503\eclipse\jdk-21.0.8+9\bin\java.exe"
$bin = Join-Path $root "bin"
$workRoot = Join-Path $root "work\tmp\windows-nfs-client-test"
$exportRoot = Join-Path $workRoot "export"
$dataRoot = Join-Path $workRoot "data"
$configPath = Join-Path $workRoot "nfs-server.properties"
$logPath = Join-Path $workRoot "nfs-server.log"
$mountExe = Join-Path $env:SystemRoot "System32\mount.exe"
$umountExe = Join-Path $env:SystemRoot "System32\umount.exe"
$nfsAdminExe = Join-Path $env:SystemRoot "System32\nfsadmin.exe"
$driveName = $DriveLetter.TrimEnd(":")
$drivePath = $driveName + ":"
$mountPath = $drivePath + "\"
$serverProcess = $null
$mounted = $false
$originalProtocol = $null

if( !$PSBoundParameters.ContainsKey("ExportName")) {
	$ExportName = "export-$($Transport.ToLowerInvariant())-$(Get-Date -Format 'yyyyMMddHHmmss')"
}

function Assert-CommandPath {
	param(
		[string]$Path,
		[string]$Name
	)

	if( !(Test-Path -LiteralPath $Path)) {
		throw "$Name is not found: $Path"
	}
}

function Wait-UdpPorts {
	param(
		[int[]]$Ports,
		[int]$TimeoutSeconds = 15
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

function Wait-TcpPorts {
	param(
		[int[]]$Ports,
		[int]$TimeoutSeconds = 15
	)

	$deadline = (Get-Date).AddSeconds($TimeoutSeconds)

	while( (Get-Date) -lt $deadline) {
		$missing = @()

		foreach( $port in $Ports) {
			$endpoint = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue

			if( $endpoint -eq $null) {
				$missing += $port
			}
		}

		if( $missing.Count -eq 0) {
			return
		}

		Start-Sleep -Milliseconds 500
	}

	throw "TCP ports did not start listening: $($Ports -join ', ')"
}

function Get-NfsClientProtocol {
	$output = & $nfsAdminExe client
	$line = $output | Where-Object { $_ -match "^\s*Protocol\s*:" } | Select-Object -First 1

	if( [string]::IsNullOrWhiteSpace($line)) {
		return "TCP+UDP"
	}

	return ($line -split ":", 2)[1].Trim()
}

function Set-NfsClientProtocol {
	param(
		[string]$Protocol
	)

	& $nfsAdminExe client config "protocol=$Protocol" | Out-Host

	if( $LASTEXITCODE -ne 0) {
		throw "nfsadmin client config protocol=$Protocol failed: $LASTEXITCODE"
	}
}

function Wait-DriveReleased {
	param(
		[int]$TimeoutSeconds = 15
	)

	$deadline = (Get-Date).AddSeconds($TimeoutSeconds)

	while( (Get-Date) -lt $deadline) {
		if( !(Get-PSDrive -Name $driveName -ErrorAction SilentlyContinue)) {
			return
		}

		Start-Sleep -Milliseconds 500
	}

	throw "Drive $drivePath was not released."
}

function Invoke-Umount {
	if( $script:mounted) {
		& $umountExe $drivePath | Out-Null

		if( $LASTEXITCODE -ne 0) {
			throw "Windows NFS umount failed: $LASTEXITCODE"
		}

		$script:mounted = $false
		Wait-DriveReleased
	}
}

function Remove-TestPath {
	param(
		[string]$Path
	)

	if( Test-Path -LiteralPath $Path) {
		Remove-Item -LiteralPath $Path -Recurse -Force
	}
}

function Write-TestConfig {
	$exportPath = $exportRoot.Replace("\", "/")
	$configText = @"
portmap.port=111
nfs.port=2049
mount.port=20048
exports.count=1
exports.1.name=/$ExportName
exports.1.path=$exportPath
exports.1.writable=true
uid=-2
gid=-2
file.mode=0666
directory.mode=0777
block.size=4096
read.size=8192
filename.charset=Shift_JIS
"@
	Set-Content -LiteralPath $configPath -Value $configText -Encoding ASCII
}

function Invoke-Mount {
	$remote = "\\$ServerHost\$ExportName"
	& $mountExe -o anon,nolock,rsize=8,wsize=8,lang=shift-jis $remote $drivePath

	if( $LASTEXITCODE -ne 0) {
		throw "Windows NFS mount failed: $LASTEXITCODE"
	}

	$script:mounted = $true
}

function Invoke-WindowsNfsChecks {
	$stamp = Get-Date -Format "yyyyMMdd_HHmmss"
	$baseName = "win-nfs-client-test-$stamp"
	$jpName = "日本語-$stamp.txt"
	$mountedFile = Join-Path $mountPath "$baseName.txt"
	$renamedFile = Join-Path $mountPath "$baseName-renamed.txt"
	$mountedDir = Join-Path $mountPath "$baseName-dir"
	$mountedJpFile = Join-Path $mountPath $jpName
	$localFile = Join-Path $exportRoot "$baseName-renamed.txt"
	$localDir = Join-Path $exportRoot "$baseName-dir"
	$localJpFile = Join-Path $exportRoot $jpName

	"created-from-windows-nfs-client" | Set-Content -LiteralPath $mountedFile -Encoding ASCII
	$createdReadBack = (Get-Content -LiteralPath $mountedFile -Raw).Trim()

	if( $createdReadBack -ne "created-from-windows-nfs-client") {
		throw "created file readback mismatch: $createdReadBack"
	}

	"updated-from-windows-nfs-client" | Set-Content -LiteralPath $mountedFile -Encoding ASCII
	$updatedReadBack = (Get-Content -LiteralPath $mountedFile -Raw).Trim()

	if( $updatedReadBack -ne "updated-from-windows-nfs-client") {
		throw "updated file readback mismatch: $updatedReadBack"
	}

	Rename-Item -LiteralPath $mountedFile -NewName "$baseName-renamed.txt"

	if( !(Test-Path -LiteralPath $localFile)) {
		throw "renamed file is not visible on server: $localFile"
	}

	$localContent = (Get-Content -LiteralPath $localFile -Raw).Trim()

	if( $localContent -ne "updated-from-windows-nfs-client") {
		throw "server-side file content mismatch: $localContent"
	}

	New-Item -ItemType Directory -Path $mountedDir | Out-Null

	if( !(Test-Path -LiteralPath $localDir)) {
		throw "created directory is not visible on server: $localDir"
	}

	"jp-name-from-windows" | Set-Content -LiteralPath $mountedJpFile -Encoding ASCII

	if( !(Test-Path -LiteralPath $localJpFile)) {
		throw "Japanese filename is not visible on server: $localJpFile"
	}

	Remove-Item -LiteralPath $renamedFile -Force
	Remove-Item -LiteralPath $mountedDir -Force
	Remove-Item -LiteralPath $mountedJpFile -Force

	if( (Test-Path -LiteralPath $localFile) -or (Test-Path -LiteralPath $localDir) -or (Test-Path -LiteralPath $localJpFile)) {
		throw "cleanup through Windows NFS mount did not remove all test paths"
	}
}

function Wait-LogPattern {
	param(
		[string]$Pattern,
		[string]$Description,
		[int]$TimeoutSeconds = 10
	)

	$deadline = (Get-Date).AddSeconds($TimeoutSeconds)

	while( (Get-Date) -lt $deadline) {
		if( Test-Path -LiteralPath $logPath) {
			$logText = Get-Content -LiteralPath $logPath -Raw

			if( $logText -match $Pattern) {
				return
			}
		}

		Start-Sleep -Milliseconds 250
	}

	throw "$Description was not observed in server log."
}

function Assert-NfsV3Log {
	if( !(Test-Path -LiteralPath $logPath)) {
		throw "server log was not created: $logPath"
	}

	Wait-LogPattern -Pattern "program=100005 version=3" -Description "MOUNT v3 request"
	Wait-LogPattern -Pattern "program=100003 version=3" -Description "NFSv3 request"

	if( $Transport -eq "TCP") {
		Wait-LogPattern -Pattern "server=nfs-mount-tcp.*program=100005 version=3" -Description "MOUNT v3 request over TCP"
		Wait-LogPattern -Pattern "server=nfs-tcp.*program=100003 version=3" -Description "NFSv3 request over TCP"
	}
}

try {
	Assert-CommandPath -Path $java -Name "java.exe"
	Assert-CommandPath -Path $mountExe -Name "mount.exe"
	Assert-CommandPath -Path $umountExe -Name "umount.exe"
	Assert-CommandPath -Path $nfsAdminExe -Name "nfsadmin.exe"

	$nfsClient = Get-Service -Name NfsClnt -ErrorAction SilentlyContinue

	if( $nfsClient -eq $null) {
		throw "Client for NFS is not installed."
	}

	if( $nfsClient.Status -ne "Running") {
		throw "Client for NFS service is not running: $($nfsClient.Status)"
	}

	if( Get-PSDrive -Name $driveName -ErrorAction SilentlyContinue) {
		throw "Drive $drivePath is already in use."
	}

	foreach( $port in @(111, 2049, 20048)) {
		if( Get-NetUDPEndpoint -LocalPort $port -ErrorAction SilentlyContinue) {
			throw "UDP port $port is already in use."
		}

		if( Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) {
			throw "TCP port $port is already in use."
		}
	}

	& (Join-Path $root "scripts\compile.ps1")

	Remove-TestPath -Path $workRoot
	New-Item -ItemType Directory -Path $exportRoot | Out-Null
	New-Item -ItemType Directory -Path $dataRoot | Out-Null
	Write-TestConfig

	$arguments = @(
		"-Dfile.encoding=UTF-8",
		"-Dtinywin.nfs.data=$dataRoot",
		"-Dtinywin.nfs.log=$logPath",
		"-Dtinywin.nfs.requestLog=true",
		"-cp",
		$bin,
		"jp.co.enterogawa.nfs.NfsServerMain",
		$configPath
	)
	$serverProcess = Start-Process -FilePath $java -ArgumentList $arguments -WorkingDirectory $root -WindowStyle Hidden -PassThru
	Wait-UdpPorts -Ports @(111, 2049, 20048)
	Wait-TcpPorts -Ports @(111, 2049, 20048)
	$originalProtocol = Get-NfsClientProtocol
	Set-NfsClientProtocol -Protocol $Transport
	Start-Sleep -Seconds 2
	Invoke-Mount
	Invoke-WindowsNfsChecks
	Assert-NfsV3Log

	Write-Host "PASS: Windows Client for NFS mount"
	Write-Host "PASS: Windows Client for NFS $Transport transport"
	Write-Host "PASS: Windows Client for NFS v3 RPC"
	Write-Host "PASS: Windows Client for NFS create/read/update/rename/delete"
	Write-Host "PASS: Windows Client for NFS directory create/delete"
	Write-Host "PASS: Windows Client for NFS Japanese filename"
	Write-Host "WINDOWS NFS CLIENT TEST PASSED"
} finally {
	$cleanupErrors = @()

	try {
		Invoke-Umount
	} catch {
		$cleanupErrors += $_.Exception.Message
	}

	try {
		if( $serverProcess -ne $null) {
			$running = Get-Process -Id $serverProcess.Id -ErrorAction SilentlyContinue

			if( $running -ne $null) {
				Stop-Process -Id $serverProcess.Id -Force
			}
		}
	} catch {
		$cleanupErrors += $_.Exception.Message
	}

	try {
		if( $originalProtocol -ne $null) {
			Set-NfsClientProtocol -Protocol $originalProtocol
		}
	} catch {
		$cleanupErrors += $_.Exception.Message
	}

	try {
		if( !$KeepWork) {
			Remove-TestPath -Path $workRoot
		}
	} catch {
		$cleanupErrors += $_.Exception.Message
	}

	if( $cleanupErrors.Count -gt 0) {
		throw "Cleanup failed: $($cleanupErrors -join '; ')"
	}
}
