param(
	[string]$DriveLetter = "Z",
	[string]$ServerHost = "127.0.0.1",
	[string]$ExportName = "export",
	[ValidateSet("UDP", "TCP")]
	[string]$Transport = "UDP",
	[switch]$SkipProtocolChange,
	[switch]$KeepWork,
	[string]$ReportPath = ""
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
$reportLines = [System.Collections.Generic.List[string]]::new()
$testStartedAt = Get-Date
$testResult = "FAILED"
$testFailure = ""
$cleanupErrors = @()

if( !$PSBoundParameters.ContainsKey("ExportName")) {
	$ExportName = "export-$($Transport.ToLowerInvariant())-$(Get-Date -Format 'yyyyMMddHHmmss')"
}

if( [string]::IsNullOrWhiteSpace($ReportPath)) {
	$reportRoot = Join-Path $root "work\analysis\windows-nfs-client"
	$ReportPath = Join-Path $reportRoot ("windows-nfs-client-{0}-{1}.md" -f $Transport.ToLowerInvariant(), (Get-Date -Format "yyyyMMdd-HHmmss"))
}

function Add-ReportLine {
	param(
		[string]$Line = ""
	)

	$script:reportLines.Add($Line) | Out-Null
}

function Add-ReportSection {
	param(
		[string]$Title
	)

	Add-ReportLine ""
	Add-ReportLine "## $Title"
	Add-ReportLine ""
}

function Add-ReportValue {
	param(
		[string]$Name,
		[string]$Value
	)

	Add-ReportLine "- ${Name}: $Value"
}

function Add-ReportCodeBlock {
	param(
		[string]$Title,
		[string]$Text
	)

	Add-ReportLine ""
	Add-ReportLine "### $Title"
	Add-ReportLine ""
	Add-ReportLine '```text'
	Add-ReportLine $Text.TrimEnd()
	Add-ReportLine '```'
}

function Add-RecoveryHints {
	Add-ReportSection -Title "復旧案"
	Add-ReportLine '- `NfsClnt` が停止している場合は Windows を再起動し、`Get-Service NfsClnt` と `nfsadmin client` を確認します。'
	Add-ReportLine '- UDP/TCP `111`、`2049`、`20048` が使用中の場合は、TinyWinNFS サービスまたは他の NFS サーバーを停止します。'
	Add-ReportLine ('- ドライブが使用中の場合は `umount {0}` を実行し、ドライブ解放後に再実行します。' -f $drivePath)
	Add-ReportLine "- mount は成功するが NFSv3 ログが出ない場合は、前回 mount のキャッシュが残っていないか確認し、unmount 後に再実行します。"
	Add-ReportLine '- Access denied が出る場合は、`uid=-2`、`gid=-2`、`file.mode=0666`、`directory.mode=0777`、`filename.charset=Shift_JIS` を確認します。'
}

function Write-TestReport {
	$reportDirectory = Split-Path -Parent $ReportPath

	if( ![string]::IsNullOrWhiteSpace($reportDirectory) -and !(Test-Path -LiteralPath $reportDirectory)) {
		New-Item -ItemType Directory -Path $reportDirectory | Out-Null
	}

	$lines = [System.Collections.Generic.List[string]]::new()
	$lines.Add("# Windows Client for NFS 結合テストレポート") | Out-Null
	$lines.Add("") | Out-Null
	$lines.Add("- result: $script:testResult") | Out-Null
	$lines.Add("- started: $($script:testStartedAt.ToString('yyyy-MM-dd HH:mm:ss'))") | Out-Null
	$lines.Add("- finished: $((Get-Date).ToString('yyyy-MM-dd HH:mm:ss'))") | Out-Null
	$lines.Add("- transport: $Transport") | Out-Null
	$lines.Add("- server: $ServerHost") | Out-Null
	$lines.Add("- export: /$ExportName") | Out-Null
	$lines.Add("- drive: $drivePath") | Out-Null

	if( ![string]::IsNullOrWhiteSpace($script:testFailure)) {
		$lines.Add("- failure: $script:testFailure") | Out-Null
	}

	if( $script:cleanupErrors.Count -gt 0) {
		$lines.Add("- cleanup: $($script:cleanupErrors -join '; ')") | Out-Null
	}

	if( ![string]::IsNullOrWhiteSpace($script:testFailure) -or $script:cleanupErrors.Count -gt 0) {
		Add-RecoveryHints
	}

	$lines.AddRange($script:reportLines)
	Set-Content -LiteralPath $ReportPath -Value $lines -Encoding UTF8
	Write-Host "Report: $ReportPath"
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

function Get-ServiceStatusText {
	param(
		[string]$Name
	)

	$service = Get-Service -Name $Name -ErrorAction SilentlyContinue

	if( $service -eq $null) {
		return "Not installed"
	}

	return $service.Status.ToString()
}

function Assert-NfsClientReady {
	$status = Get-ServiceStatusText -Name "NfsClnt"

	if( $status -eq "Not installed") {
		throw "Client for NFS is not installed. Enable Windows Client for NFS, then run this test again."
	}

	if( $status -ne "Running") {
		$message = @"
Client for NFS service is not running: $status
Do not stop or restart NfsClnt from this test script.
Try restarting Windows, then verify with:
  Get-Service NfsClnt
  nfsadmin client
"@
		throw ($message.Trim())
	}

	try {
		$output = & $nfsAdminExe client 2>&1

		if( $LASTEXITCODE -ne 0) {
			throw ($output -join [Environment]::NewLine)
		}
	} catch {
		throw "nfsadmin client failed. Windows Client for NFS may be in an unusable state. Restart Windows and run the test again. Detail: $($_.Exception.Message)"
	}
}

function Assert-DriveAvailable {
	if( Get-PSDrive -Name $driveName -ErrorAction SilentlyContinue) {
		throw "Drive $drivePath is already in use. Unmount it first with: umount $drivePath"
	}
}

function Use-TestProtocol {
	if( $SkipProtocolChange) {
		Write-Host "INFO: Skipping Windows Client for NFS protocol change. Current protocol: $(Get-NfsClientProtocol)"
		Add-ReportValue -Name "protocol change" -Value "skipped"
		return
	}

	$currentProtocol = Get-NfsClientProtocol
	Add-ReportValue -Name "original protocol" -Value $currentProtocol

	if( $currentProtocol -eq $Transport) {
		Write-Host "INFO: Windows Client for NFS protocol is already $Transport"
		Add-ReportValue -Name "protocol change" -Value "not required"
		return
	}

	Write-Host "INFO: Changing Windows Client for NFS protocol from $currentProtocol to $Transport"
	Set-NfsClientProtocol -Protocol $Transport
	Start-Sleep -Seconds 2
	Write-Host "INFO: Windows Client for NFS protocol is now $(Get-NfsClientProtocol)"
	Add-ReportValue -Name "protocol change" -Value "$currentProtocol -> $Transport"
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
write.size=8192
directory.preferred.size=4096
max.file.size=9223372036854775807
time.delta.nanos=1000000
pathconf.link.max=1024
pathconf.name.max=255
filename.charset=Shift_JIS
"@
	Set-Content -LiteralPath $configPath -Value $configText -Encoding ASCII
}

function Invoke-Mount {
	$remote = "\\$ServerHost\$ExportName"
	Add-ReportValue -Name "mount remote" -Value $remote
	& $mountExe -o anon,nolock,rsize=8,wsize=8,lang=shift-jis $remote $drivePath

	if( $LASTEXITCODE -ne 0) {
		throw "Windows NFS mount failed: $LASTEXITCODE"
	}

	$script:mounted = $true
	Add-ReportValue -Name "mount" -Value "succeeded"
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

	Add-ReportValue -Name "create/read" -Value "succeeded"

	"updated-from-windows-nfs-client" | Set-Content -LiteralPath $mountedFile -Encoding ASCII
	$updatedReadBack = (Get-Content -LiteralPath $mountedFile -Raw).Trim()

	if( $updatedReadBack -ne "updated-from-windows-nfs-client") {
		throw "updated file readback mismatch: $updatedReadBack"
	}

	Add-ReportValue -Name "update/read" -Value "succeeded"

	Rename-Item -LiteralPath $mountedFile -NewName "$baseName-renamed.txt"

	if( !(Test-Path -LiteralPath $localFile)) {
		throw "renamed file is not visible on server: $localFile"
	}

	$localContent = (Get-Content -LiteralPath $localFile -Raw).Trim()

	if( $localContent -ne "updated-from-windows-nfs-client") {
		throw "server-side file content mismatch: $localContent"
	}

	Add-ReportValue -Name "rename/server visibility" -Value "succeeded"

	New-Item -ItemType Directory -Path $mountedDir | Out-Null

	if( !(Test-Path -LiteralPath $localDir)) {
		throw "created directory is not visible on server: $localDir"
	}

	Add-ReportValue -Name "directory create" -Value "succeeded"

	"jp-name-from-windows" | Set-Content -LiteralPath $mountedJpFile -Encoding ASCII

	if( !(Test-Path -LiteralPath $localJpFile)) {
		throw "Japanese filename is not visible on server: $localJpFile"
	}

	Add-ReportValue -Name "Japanese filename" -Value "succeeded"

	Remove-Item -LiteralPath $renamedFile -Force
	Remove-Item -LiteralPath $mountedDir -Force
	Remove-Item -LiteralPath $mountedJpFile -Force

	if( (Test-Path -LiteralPath $localFile) -or (Test-Path -LiteralPath $localDir) -or (Test-Path -LiteralPath $localJpFile)) {
		throw "cleanup through Windows NFS mount did not remove all test paths"
	}

	Add-ReportValue -Name "cleanup through mount" -Value "succeeded"
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
	Add-ReportValue -Name "NFS version" -Value "v3 observed"

	if( $Transport -eq "TCP") {
		Wait-LogPattern -Pattern "server=nfs-mount-tcp.*program=100005 version=3" -Description "MOUNT v3 request over TCP"
		Wait-LogPattern -Pattern "server=nfs-tcp.*program=100003 version=3" -Description "NFSv3 request over TCP"
		Add-ReportValue -Name "transport log" -Value "TCP observed"
	} else {
		Add-ReportValue -Name "transport log" -Value "UDP observed"
	}
}

try {
	Add-ReportSection -Title "入力"
	Add-ReportValue -Name "drive" -Value $drivePath
	Add-ReportValue -Name "server" -Value $ServerHost
	Add-ReportValue -Name "export" -Value "/$ExportName"
	Add-ReportValue -Name "transport" -Value $Transport
	Add-ReportValue -Name "skip protocol change" -Value $SkipProtocolChange.ToString()
	Add-ReportValue -Name "keep work" -Value $KeepWork.ToString()

	Assert-CommandPath -Path $java -Name "java.exe"
	Assert-CommandPath -Path $mountExe -Name "mount.exe"
	Assert-CommandPath -Path $umountExe -Name "umount.exe"
	Assert-CommandPath -Path $nfsAdminExe -Name "nfsadmin.exe"

	Add-ReportSection -Title "環境"
	Add-ReportValue -Name "java" -Value $java
	Add-ReportValue -Name "mount.exe" -Value $mountExe
	Add-ReportValue -Name "umount.exe" -Value $umountExe
	Add-ReportValue -Name "nfsadmin.exe" -Value $nfsAdminExe
	Add-ReportValue -Name "NfsClnt" -Value (Get-ServiceStatusText -Name "NfsClnt")
	Add-ReportCodeBlock -Title "nfsadmin client" -Text ((& $nfsAdminExe client 2>&1) -join [Environment]::NewLine)

	$nfsClient = Get-Service -Name NfsClnt -ErrorAction SilentlyContinue

	if( $nfsClient -eq $null) {
		throw "Client for NFS is not installed."
	}

	Assert-NfsClientReady
	Assert-DriveAvailable
	Add-ReportValue -Name "drive availability" -Value "available"

	Add-ReportSection -Title "ポート"
	foreach( $port in @(111, 2049, 20048)) {
		if( Get-NetUDPEndpoint -LocalPort $port -ErrorAction SilentlyContinue) {
			throw "UDP port $port is already in use. Stop TinyWinNFS service or any other NFS server before running this test."
		}

		if( Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) {
			throw "TCP port $port is already in use. Stop TinyWinNFS service or any other NFS server before running this test."
		}

		Add-ReportValue -Name "port $port" -Value "available"
	}

	& (Join-Path $root "scripts\compile.ps1")
	Add-ReportValue -Name "compile" -Value "succeeded"

	Remove-TestPath -Path $workRoot
	New-Item -ItemType Directory -Path $exportRoot | Out-Null
	New-Item -ItemType Directory -Path $dataRoot | Out-Null
	Write-TestConfig
	Add-ReportSection -Title "テスト設定"
	Add-ReportValue -Name "work root" -Value $workRoot
	Add-ReportValue -Name "export root" -Value $exportRoot
	Add-ReportValue -Name "data root" -Value $dataRoot
	Add-ReportValue -Name "config" -Value $configPath

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
	Add-ReportValue -Name "server process" -Value $serverProcess.Id.ToString()
	Wait-UdpPorts -Ports @(111, 2049, 20048)
	Wait-TcpPorts -Ports @(111, 2049, 20048)
	Add-ReportValue -Name "server ports" -Value "listening"
	$originalProtocol = Get-NfsClientProtocol
	Use-TestProtocol
	Add-ReportSection -Title "操作結果"
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
	$script:testResult = "PASSED"
} catch {
	$script:testFailure = $_.Exception.Message
	Add-ReportSection -Title "失敗"
	Add-ReportValue -Name "message" -Value $_.Exception.Message
	throw
} finally {
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
		if( $originalProtocol -ne $null -and !$SkipProtocolChange) {
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

	$script:cleanupErrors = $cleanupErrors
	Write-TestReport

	if( $cleanupErrors.Count -gt 0) {
		throw "Cleanup failed: $($cleanupErrors -join '; ')"
	}
}
