param(
	[int]$DurationMinutes = 30,
	[int]$Files = 1000,
	[int]$Directories = 10,
	[int]$Depth = 1,
	[int]$MinSize = 1024,
	[int]$MaxSize = 8192,
	[int]$UdpWorkers = 8,
	[int]$UdpQueueSize = 1024,
	[string]$Out = "work\analysis\v1.12.0-long-running"
)

$ErrorActionPreference = "Stop"

if( $DurationMinutes -le 0) {
	throw "DurationMinutes must be greater than zero."
}

$root = Split-Path -Parent $PSScriptRoot
$benchmark = Join-Path $root "scripts\benchmark-local-rpc.ps1"
$durationSeconds = $DurationMinutes * 60

if( !(Test-Path -LiteralPath $benchmark)) {
	throw "Local RPC benchmark script is not found: $benchmark"
}

& $benchmark `
	-Files $Files `
	-Directories $Directories `
	-Depth $Depth `
	-MinSize $MinSize `
	-MaxSize $MaxSize `
	-LongRun `
	-DurationSeconds $durationSeconds `
	-UdpWorkers $UdpWorkers `
	-UdpQueueSize $UdpQueueSize `
	-Out $Out

if( $LASTEXITCODE -ne 0) {
	throw "long-running local RPC test failed: $LASTEXITCODE"
}
