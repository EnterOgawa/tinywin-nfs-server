param(
	[ValidateSet("UDP", "TCP")]
	[string]$Transport = "UDP",
	[switch]$RestartHandlePersistence,
	[switch]$VerifyFileIntegrity
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$javac = "C:\develop\tools\eclipse202503\eclipse\jdk-21.0.8+9\bin\javac.exe"
$java = Join-Path $root "runtime\bin\java.exe"
$developerJava = "C:\develop\tools\eclipse202503\eclipse\jdk-21.0.8+9\bin\java.exe"
$bin = Join-Path $root "bin"
$testBin = Join-Path $root "work\tmp\test-bin"
$smokeTest = Join-Path $root "test\jp\co\enterogawa\nfs\ServiceSmokeTest.java"
$appJar = Join-Path $root "app\tinywin-nfs-server.jar"
$dataRoot = if( [string]::IsNullOrWhiteSpace($env:TINYWIN_NFS_DATA)) { Join-Path $env:ProgramData "EnterOgawa\TinyWinNFS Server" } else { $env:TINYWIN_NFS_DATA }
$configPath = Join-Path $dataRoot "conf\nfs-server.properties"
$legacyConfigPath = Join-Path $root "conf\nfs-server.properties"
$serviceConfigRoot = $dataRoot
$serviceConfigPath = $configPath
$stateFile = Join-Path $env:TEMP "tinywin-nfs-handle-persistence.txt"

if( !(Test-Path -LiteralPath $configPath) -and (Test-Path -LiteralPath $legacyConfigPath)) {
	$serviceConfigRoot = $root
	$serviceConfigPath = $legacyConfigPath
	$configPath = $legacyConfigPath
}

if( (Test-Path -LiteralPath $appJar) -and (Test-Path -LiteralPath $java)) {
	Push-Location $root
	try {
		if( $VerifyFileIntegrity) {
			& $java -cp $appJar jp.co.enterogawa.nfs.ServiceSmokeTest verify-file-integrity $configPath

			if( $LASTEXITCODE -ne 0) {
				throw "service file integrity test failed: $LASTEXITCODE"
			}

			exit 0
		} elseif( $RestartHandlePersistence) {
			& $java -cp $appJar jp.co.enterogawa.nfs.ServiceSmokeTest prepare-handle-persistence $stateFile

			if( $LASTEXITCODE -ne 0) {
				throw "service handle persistence prepare failed: $LASTEXITCODE"
			}

			& (Join-Path $root "scripts\restart-service.ps1") | Out-Null
			& $java -cp $appJar jp.co.enterogawa.nfs.ServiceSmokeTest verify-handle-persistence $stateFile

			if( $LASTEXITCODE -ne 0) {
				throw "service handle persistence verify failed: $LASTEXITCODE"
			}

			Remove-Item -LiteralPath $stateFile -Force -ErrorAction SilentlyContinue
			exit 0
		} else {
			if( $Transport -eq "TCP") {
				& $java -cp $appJar jp.co.enterogawa.nfs.ServiceSmokeTest tcp
			} else {
				& $java -cp $appJar jp.co.enterogawa.nfs.ServiceSmokeTest
			}

			if( $LASTEXITCODE -ne 0) {
				throw "service smoke test failed: $LASTEXITCODE"
			}

			exit 0
		}
	} finally {
		Pop-Location
	}
}

if( !(Test-Path -LiteralPath $javac)) {
	throw "javac.exe is not found: $javac"
}

if( !(Test-Path -LiteralPath $smokeTest)) {
	throw "Smoke test source is not found: $smokeTest"
}

if( !(Test-Path -LiteralPath $developerJava)) {
	throw "java.exe is not found: $developerJava"
}

& (Join-Path $root "scripts\compile.ps1")

if( !(Test-Path -LiteralPath $testBin)) {
	New-Item -ItemType Directory -Path $testBin | Out-Null
}

& $javac --release 21 -encoding UTF-8 -cp $bin -d $testBin $smokeTest

if( $LASTEXITCODE -ne 0) {
	throw "javac smoke test failed: $LASTEXITCODE"
}

Push-Location $root
try {
	if( $VerifyFileIntegrity) {
		Pop-Location
		Push-Location $serviceConfigRoot
		& $developerJava -cp "$bin;$testBin" jp.co.enterogawa.nfs.ServiceSmokeTest verify-file-integrity $serviceConfigPath

		if( $LASTEXITCODE -ne 0) {
			throw "service file integrity test failed: $LASTEXITCODE"
		}
	} elseif( $RestartHandlePersistence) {
		& $developerJava -cp "$bin;$testBin" jp.co.enterogawa.nfs.ServiceSmokeTest prepare-handle-persistence $stateFile

		if( $LASTEXITCODE -ne 0) {
			throw "service handle persistence prepare failed: $LASTEXITCODE"
		}

		& (Join-Path $root "scripts\restart-service.ps1") | Out-Null
		& $developerJava -cp "$bin;$testBin" jp.co.enterogawa.nfs.ServiceSmokeTest verify-handle-persistence $stateFile

		if( $LASTEXITCODE -ne 0) {
			throw "service handle persistence verify failed: $LASTEXITCODE"
		}

		Remove-Item -LiteralPath $stateFile -Force -ErrorAction SilentlyContinue
	} else {
		if( $Transport -eq "TCP") {
			& $developerJava -cp "$bin;$testBin" jp.co.enterogawa.nfs.ServiceSmokeTest tcp
		} else {
			& $developerJava -cp "$bin;$testBin" jp.co.enterogawa.nfs.ServiceSmokeTest
		}

		if( $LASTEXITCODE -ne 0) {
			throw "service smoke test failed: $LASTEXITCODE"
		}
	}
} finally {
	Pop-Location
}
