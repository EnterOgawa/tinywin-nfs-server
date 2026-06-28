param(
	[string]$DriveLetter = "Z",
	[string]$ServerHost = "127.0.0.1",
	[string]$ExportNamePrefix = "export",
	[ValidateSet("UDP", "TCP")]
	[string[]]$Transport = @("UDP", "TCP"),
	[switch]$SkipProtocolChange,
	[switch]$KeepWork,
	[string]$ReportPath = ""
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$caseScript = Join-Path $root "scripts\test-windows-nfs-client.ps1"
$startedAt = Get-Date
$stamp = $startedAt.ToString("yyyyMMdd-HHmmss")
$reportRoot = Join-Path $root "work\analysis\windows-nfs-client-matrix"
$caseRoot = Join-Path $reportRoot "cases-$stamp"
$results = [System.Collections.Generic.List[object]]::new()

if( [string]::IsNullOrWhiteSpace($ReportPath)) {
	$ReportPath = Join-Path $reportRoot "windows-nfs-client-matrix-$stamp.md"
}

function New-ReportDirectory {
	param(
		[string]$Path
	)

	if( !(Test-Path -LiteralPath $Path)) {
		New-Item -ItemType Directory -Path $Path | Out-Null
	}
}

function Invoke-Case {
	param(
		[string]$CaseTransport
	)

	$exportName = "{0}-{1}-{2}" -f $ExportNamePrefix, $CaseTransport.ToLowerInvariant(), $stamp
	$caseReport = Join-Path $caseRoot ("windows-nfs-client-{0}.md" -f $CaseTransport.ToLowerInvariant())
	$caseArguments = @{
		DriveLetter = $DriveLetter
		ServerHost = $ServerHost
		ExportName = $exportName
		Transport = $CaseTransport
		ReportPath = $caseReport
	}

	if( $SkipProtocolChange) {
		$caseArguments.SkipProtocolChange = $true
	}

	if( $KeepWork) {
		$caseArguments.KeepWork = $true
	}

	try {
		& $caseScript @caseArguments
		$results.Add([pscustomobject]@{
			Transport = $CaseTransport
			NfsVersion = "NFSv3"
			Result = "PASSED"
			Report = $caseReport
			Failure = ""
		}) | Out-Null
	} catch {
		$results.Add([pscustomobject]@{
			Transport = $CaseTransport
			NfsVersion = "NFSv3"
			Result = "FAILED"
			Report = $caseReport
			Failure = $_.Exception.Message
		}) | Out-Null
	}
}

function Write-MatrixReport {
	$reportDirectory = Split-Path -Parent $ReportPath
	New-ReportDirectory -Path $reportDirectory

	$lines = [System.Collections.Generic.List[string]]::new()
	$lines.Add("# Windows Client for NFS 検証マトリクス") | Out-Null
	$lines.Add("") | Out-Null
	$lines.Add("- started: $($startedAt.ToString('yyyy-MM-dd HH:mm:ss'))") | Out-Null
	$lines.Add("- finished: $((Get-Date).ToString('yyyy-MM-dd HH:mm:ss'))") | Out-Null
	$lines.Add("- server: $ServerHost") | Out-Null
	$lines.Add("- drive: $DriveLetter") | Out-Null
	$lines.Add("- export prefix: $ExportNamePrefix") | Out-Null
	$lines.Add("") | Out-Null
	$lines.Add("## 実行結果") | Out-Null
	$lines.Add("") | Out-Null
	$lines.Add("| NFS | transport | result | report | failure |") | Out-Null
	$lines.Add("|---|---|---|---|---|") | Out-Null

	foreach( $result in $results) {
		$failure = $result.Failure.Replace("|", "\|").Replace("`r", " ").Replace("`n", " ")
		$lines.Add(('| {0} | {1} | {2} | `{3}` | {4} |' -f $result.NfsVersion, $result.Transport, $result.Result, $result.Report, $failure)) | Out-Null
	}

	$lines.Add('| NFSv2 | UDP/TCP | 対象外 | 単体テスト、QNX 4.25、Linux/WSL任意回帰で確認 | Windows `mount.exe` には portable な NFS version 指定がないため、このマトリクスでは強制できない |') | Out-Null
	$lines.Add("") | Out-Null
	$lines.Add("## 判定方針") | Out-Null
	$lines.Add("") | Out-Null
	$lines.Add("- Windows Client for NFS は、サーバーログで MOUNT v3 と NFSv3 RPC を観測できた場合に NFSv3 と判定します。") | Out-Null
	$lines.Add("- NFSv2 は、ローカル単体テストと QNX 4.25 実機確認、必要に応じた Linux/WSL 任意回帰で確認します。") | Out-Null
	$lines.Add('- UDP/TCP は `nfsadmin client config protocol=...` と TinyWinNFS の `server=nfs-*` ログで確認します。') | Out-Null
	$lines.Add("") | Out-Null
	$lines.Add("## 失敗時の確認") | Out-Null
	$lines.Add("") | Out-Null
	$lines.Add('- `NfsClnt` が停止している場合は Windows を再起動し、`Get-Service NfsClnt` と `nfsadmin client` を確認します。') | Out-Null
	$lines.Add('- UDP/TCP `111`、`2049`、`20048` が使用中の場合は、TinyWinNFS サービスまたは他の NFS サーバーを停止します。') | Out-Null
	$lines.Add(('- ドライブが使用中の場合は `umount {0}` を実行し、ドライブ解放後に再実行します。' -f $DriveLetter)) | Out-Null
	$lines.Add("- mount は成功するが NFSv3 ログが出ない場合は、前回 mount のキャッシュが残っていないか確認し、unmount 後に再実行します。") | Out-Null
	$lines.Add('- Access denied が出る場合は、`uid=-2`、`gid=-2`、`file.mode=0666`、`directory.mode=0777`、`filename.charset=Shift_JIS` を確認します。') | Out-Null
	Set-Content -LiteralPath $ReportPath -Value $lines -Encoding UTF8
	Write-Host "Matrix report: $ReportPath"
}

if( !(Test-Path -LiteralPath $caseScript)) {
	throw "Windows NFS client case script is not found: $caseScript"
}

New-ReportDirectory -Path $caseRoot

foreach( $currentTransport in $Transport) {
	Invoke-Case -CaseTransport $currentTransport
}

Write-MatrixReport

$failed = $results | Where-Object { $_.Result -ne "PASSED" }

if( $failed.Count -gt 0) {
	throw "Windows Client for NFS matrix failed: $($failed.Transport -join ', ')"
}

Write-Host "WINDOWS NFS CLIENT MATRIX PASSED"
