param(
	[ValidateSet("Unit", "Transport", "All")]
	[string]$Suite = "Unit",
	[string]$JdkPath = "",
	[string]$SwtJarPath = ""
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot "build-environment.ps1")
$buildEnvironment = Get-TinyWinNfsBuildEnvironment -JdkPath $JdkPath -SwtJarPath $SwtJarPath -RequireSwt
$javac = $buildEnvironment.Javac
$java = $buildEnvironment.Java
$swtJar = $buildEnvironment.SwtJar
$src = Join-Path $root "src"
$test = Join-Path $root "test"
$bin = Join-Path $root "bin"
$testBin = Join-Path $root "work\tmp\test-bin"

if( !(Test-Path -LiteralPath $javac)) {
	throw "javac.exe is not found: $javac"
}

if( !(Test-Path -LiteralPath $java)) {
	throw "java.exe is not found: $java"
}

if( !(Test-Path -LiteralPath $swtJar)) {
	throw "SWT jar is not found: $swtJar"
}

Assert-TinyWinNfsWorkspacePath -Path $bin
Assert-TinyWinNfsWorkspacePath -Path $testBin
if( Test-Path -LiteralPath $bin) {
	Remove-Item -LiteralPath $bin -Recurse -Force
}

if( Test-Path -LiteralPath $testBin) {
	Remove-Item -LiteralPath $testBin -Recurse -Force
}

New-Item -ItemType Directory -Path $bin | Out-Null
New-Item -ItemType Directory -Path $testBin | Out-Null

$srcFiles = Get-ChildItem -LiteralPath $src -Recurse -Filter *.java | ForEach-Object { $_.FullName }
$testFiles = Get-ChildItem -LiteralPath $test -Recurse -Filter *.java | ForEach-Object { $_.FullName }

if( $srcFiles.Count -eq 0) {
	throw "Java source files are not found."
}

if( $testFiles.Count -eq 0) {
	throw "Java test files are not found."
}

& $javac --release 21 -encoding UTF-8 -cp $swtJar -d $bin $srcFiles

if( $LASTEXITCODE -ne 0) {
	throw "javac src failed: $LASTEXITCODE"
}

$resources = Get-ChildItem -LiteralPath $src -Recurse -File | Where-Object { $_.Extension -ne ".java" }

foreach( $resource in $resources) {
	$relative = $resource.FullName.Substring($src.Length).TrimStart("\")
	$target = Join-Path $bin $relative
	$targetDir = Split-Path -Parent $target

	if( !(Test-Path -LiteralPath $targetDir)) {
		New-Item -ItemType Directory -Path $targetDir | Out-Null
	}

	Copy-Item -LiteralPath $resource.FullName -Destination $target -Force
}

& $javac --release 21 -encoding UTF-8 -cp "$bin;$swtJar" -d $testBin $testFiles

if( $LASTEXITCODE -ne 0) {
	throw "javac test failed: $LASTEXITCODE"
}

Push-Location $root
try {
	& $java -cp "$bin;$testBin" jp.co.enterogawa.nfs.AllTests ($Suite.ToLowerInvariant())
} finally {
	Pop-Location
}

if( $LASTEXITCODE -ne 0) {
	throw "tests failed: $LASTEXITCODE"
}
