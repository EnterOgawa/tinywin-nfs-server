param(
	[int]$DurationMinutes = 60,
	[int]$IntervalSeconds = 15,
	[int]$RestartEveryIterations = 0
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$smokeScript = Join-Path $root "scripts\smoke-service.ps1"
$statusScript = Join-Path $root "scripts\status-service.ps1"
$restartScript = Join-Path $root "scripts\restart-service.ps1"
$deadline = (Get-Date).AddMinutes($DurationMinutes)
$iteration = 0

if( !(Test-Path -LiteralPath $smokeScript)) {
	throw "Smoke test script is not found: $smokeScript"
}

if( $DurationMinutes -le 0) {
	throw "DurationMinutes must be greater than zero."
}

if( $IntervalSeconds -lt 0) {
	throw "IntervalSeconds must be zero or greater."
}

Write-Host "TinyWinNFS service stability test started."
Write-Host "DurationMinutes=$DurationMinutes IntervalSeconds=$IntervalSeconds RestartEveryIterations=$RestartEveryIterations"

while( (Get-Date) -lt $deadline) {
	$iteration++
	Write-Host "Iteration $iteration started: $(Get-Date -Format 'yyyy/MM/dd HH:mm:ss')"

	if( Test-Path -LiteralPath $statusScript) {
		& $statusScript
	}

	& $smokeScript -VerifyFileIntegrity

	if( $LASTEXITCODE -ne 0) {
		throw "Service integrity smoke test failed at iteration $iteration."
	}

	if( $RestartEveryIterations -gt 0 -and ($iteration % $RestartEveryIterations) -eq 0) {
		if( !(Test-Path -LiteralPath $restartScript)) {
			throw "Restart script is not found: $restartScript"
		}

		Write-Host "Restarting service after iteration $iteration."
		& $restartScript
	}

	if( $IntervalSeconds -gt 0 -and (Get-Date) -lt $deadline) {
		Start-Sleep -Seconds $IntervalSeconds
	}
}

Write-Host "SERVICE STABILITY TEST PASSED: $iteration iterations"
