$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$jdk = "C:\develop\tools\eclipse202503\eclipse\jdk-21.0.8+9"
$jar = Join-Path $jdk "bin\jar.exe"
$jpackage = Join-Path $jdk "bin\jpackage.exe"
$javaLauncher = Join-Path $jdk "bin\java.exe"
$swtJar = "C:\develop\tools\eclipse202503\eclipse\plugins\org.eclipse.swt.win32.win32.x86_64_3.129.0.v20250221-1734.jar"
$icon = Join-Path $root "assets\tinywin-nfs-server.ico"
$iconScript = Join-Path $root "scripts\generate-icon.ps1"
$work = Join-Path $root "work\tmp\package"
$input = Join-Path $work "input"
$smokeBin = Join-Path $work "smoke-bin"
$dist = Join-Path $root "dist"
$appName = "TinyWinNfsManager"
$appVersion = "1.2.0"
$appImage = Join-Path $dist $appName
$appJar = Join-Path $input "tinywin-nfs-server.jar"
$smokeTest = Join-Path $root "test\jp\co\enterogawa\nfs\ServiceSmokeTest.java"

if( !(Test-Path -LiteralPath $jar)) {
	throw "jar.exe is not found: $jar"
}

if( !(Test-Path -LiteralPath $jpackage)) {
	throw "jpackage.exe is not found: $jpackage"
}

if( !(Test-Path -LiteralPath $swtJar)) {
	throw "SWT jar is not found: $swtJar"
}

if( !(Test-Path -LiteralPath $icon)) {
	& $iconScript
}

if( !(Test-Path -LiteralPath $icon)) {
	throw "Application icon is not found: $icon"
}

& (Join-Path $root "scripts\compile.ps1")

if( Test-Path -LiteralPath $work) {
	Remove-Item -LiteralPath $work -Recurse -Force
}

if( Test-Path -LiteralPath $appImage) {
	Remove-Item -LiteralPath $appImage -Recurse -Force
}

New-Item -ItemType Directory -Path $input | Out-Null
New-Item -ItemType Directory -Path $smokeBin | Out-Null
Copy-Item -LiteralPath $swtJar -Destination (Join-Path $input "org.eclipse.swt.win32.win32.x86_64.jar") -Force
& (Join-Path $jdk "bin\javac.exe") --release 21 -encoding UTF-8 -cp (Join-Path $root "bin") -d $smokeBin $smokeTest

if( $LASTEXITCODE -ne 0) {
	throw "javac smoke test failed: $LASTEXITCODE"
}

& $jar --create --file $appJar --main-class jp.co.enterogawa.nfs.manager.TinyWinNfsSwtManager -C (Join-Path $root "bin") . -C $smokeBin .

if( $LASTEXITCODE -ne 0) {
	throw "jar failed: $LASTEXITCODE"
}

& $jpackage `
	--type app-image `
	--name $appName `
	--app-version $appVersion `
	--input $input `
	--main-jar "tinywin-nfs-server.jar" `
	--main-class jp.co.enterogawa.nfs.manager.TinyWinNfsSwtManager `
	--icon $icon `
	--dest $dist `
	--java-options "-Dfile.encoding=UTF-8"

if( $LASTEXITCODE -ne 0) {
	throw "jpackage failed: $LASTEXITCODE"
}

Copy-Item -LiteralPath (Join-Path $root "conf") -Destination (Join-Path $appImage "conf") -Recurse -Force
Copy-Item -LiteralPath (Join-Path $root "assets") -Destination (Join-Path $appImage "assets") -Recurse -Force
Copy-Item -LiteralPath $javaLauncher -Destination (Join-Path $appImage "runtime\bin\java.exe") -Force
Copy-Item -LiteralPath (Join-Path $root "export") -Destination (Join-Path $appImage "export") -Recurse -Force
Copy-Item -LiteralPath (Join-Path $root "docs") -Destination (Join-Path $appImage "docs") -Recurse -Force
Copy-Item -LiteralPath (Join-Path $root "service") -Destination (Join-Path $appImage "service") -Recurse -Force
Copy-Item -LiteralPath (Join-Path $root "launcher\TinyWinNfsManager-Admin.cmd") -Destination (Join-Path $appImage "TinyWinNfsManager-Admin.cmd") -Force
Copy-Item -LiteralPath (Join-Path $root "LICENSE") -Destination (Join-Path $appImage "LICENSE") -Force
Copy-Item -LiteralPath (Join-Path $root "THIRD_PARTY_NOTICES.md") -Destination (Join-Path $appImage "THIRD_PARTY_NOTICES.md") -Force

$scriptTarget = Join-Path $appImage "scripts"
$runtimeScripts = @(
	"add-firewall-rules.ps1",
	"install-service.ps1",
	"restart-service.ps1",
	"smoke-service.ps1",
	"start-service.ps1",
	"status-service.ps1",
	"test-windows-nfs-client.ps1",
	"stop-service.ps1",
	"uninstall-service.ps1"
)

New-Item -ItemType Directory -Path $scriptTarget | Out-Null

foreach( $scriptName in $runtimeScripts) {
	Copy-Item -LiteralPath (Join-Path $root "scripts\$scriptName") -Destination (Join-Path $scriptTarget $scriptName) -Force
}

$serviceLogPath = Join-Path $appImage "service\winsw"

if( Test-Path -LiteralPath $serviceLogPath) {
	Get-ChildItem -LiteralPath $serviceLogPath -Filter "*.log" -File | Remove-Item -Force
}

$packageXml = Join-Path $appImage "service\winsw\nfs-server.package.xml"
$targetXml = Join-Path $appImage "service\winsw\nfs-server.xml"
Copy-Item -LiteralPath $packageXml -Destination $targetXml -Force

$winsw = Join-Path $appImage "service\winsw\nfs-server.exe"

if( !(Test-Path -LiteralPath $winsw)) {
	Push-Location $appImage
	try {
		& (Join-Path $appImage "scripts\download-winsw.ps1")
	} finally {
		Pop-Location
	}
}

Write-Host "App image created: $appImage"
Write-Host "Run: $appImage\$appName.exe"
