param(
	[int]$Files = 1000,
	[int]$Directories = 10,
	[int]$Depth = 1,
	[int]$MinSize = 1024,
	[int]$MaxSize = 8192,
	[int]$Loops = 1,
	[switch]$LongRun,
	[int]$DurationSeconds = 0,
	[switch]$WriteSync,
	[bool]$WriteCacheEnabled = $true,
	[int]$WriteCacheMaxOpen = 64,
	[int]$WriteCacheIdleMillis = 3000,
	[int]$UdpWorkers = 8,
	[int]$UdpQueueSize = 1024,
	[int]$TcpTimeoutMillis = 30000,
	[string]$Out = "work\analysis\v1.12.0-benchmark"
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot "build-environment.ps1")
$buildEnvironment = Get-TinyWinNfsBuildEnvironment -RequireSwt
$javac = $buildEnvironment.Javac
$java = $buildEnvironment.Java
$swtJar = $buildEnvironment.SwtJar
$bin = Join-Path $root "bin"
$testBin = Join-Path $root "work\tmp\test-bin"
$outputPath = if( [System.IO.Path]::IsPathRooted($Out)) { $Out } else { Join-Path $root $Out }

if( !(Test-Path -LiteralPath $javac)) {
	throw "javac.exe is not found: $javac"
}

if( !(Test-Path -LiteralPath $java)) {
	throw "java.exe is not found: $java"
}

if( !(Test-Path -LiteralPath $swtJar)) {
	throw "SWT jar is not found: $swtJar"
}

& (Join-Path $root "scripts\compile.ps1")

Assert-TinyWinNfsWorkspacePath -Path $testBin
if( Test-Path -LiteralPath $testBin) {
	Remove-Item -LiteralPath $testBin -Recurse -Force
}

New-Item -ItemType Directory -Path $testBin | Out-Null
$testFiles = Get-ChildItem -LiteralPath (Join-Path $root "test") -Recurse -Filter *.java | ForEach-Object { $_.FullName }

& $javac --release 21 -encoding UTF-8 -cp "$bin;$swtJar" -d $testBin $testFiles

if( $LASTEXITCODE -ne 0) {
	throw "javac test failed: $LASTEXITCODE"
}

$benchmarkArgs = @(
	"--files", $Files,
	"--directories", $Directories,
	"--depth", $Depth,
	"--min-size", $MinSize,
	"--max-size", $MaxSize,
	"--loops", $Loops,
	"--duration-seconds", $DurationSeconds,
	"--write-sync", $WriteSync.IsPresent.ToString().ToLowerInvariant(),
	"--write-cache-enabled", $WriteCacheEnabled.ToString().ToLowerInvariant(),
	"--write-cache-max-open", $WriteCacheMaxOpen,
	"--write-cache-idle-millis", $WriteCacheIdleMillis,
	"--udp-workers", $UdpWorkers,
	"--udp-queue-size", $UdpQueueSize,
	"--tcp-timeout-millis", $TcpTimeoutMillis,
	"--out", $outputPath
)

if( $LongRun.IsPresent) {
	$benchmarkArgs += "--long-run"
}

& $java -cp "$bin;$testBin" jp.co.enterogawa.nfs.LocalRpcBenchmark @benchmarkArgs

if( $LASTEXITCODE -ne 0) {
	throw "local RPC benchmark failed: $LASTEXITCODE"
}
