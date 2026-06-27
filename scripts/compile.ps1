$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$javac = "C:\develop\tools\eclipse202503\eclipse\jdk-21.0.8+9\bin\javac.exe"
$swtJar = "C:\develop\tools\eclipse202503\eclipse\plugins\org.eclipse.swt.win32.win32.x86_64_3.129.0.v20250221-1734.jar"
$src = Join-Path $root "src"
$bin = Join-Path $root "bin"

if( !(Test-Path -LiteralPath $javac)) {
	throw "javac.exe is not found: $javac"
}

if( !(Test-Path -LiteralPath $swtJar)) {
	throw "SWT jar is not found: $swtJar"
}

if( Test-Path -LiteralPath $bin) {
	Remove-Item -LiteralPath $bin -Recurse -Force
}

New-Item -ItemType Directory -Path $bin | Out-Null

$files = Get-ChildItem -LiteralPath $src -Recurse -Filter *.java | ForEach-Object { $_.FullName }

if( $files.Count -eq 0) {
	throw "Java source files are not found."
}

& $javac --release 21 -encoding UTF-8 -cp $swtJar -d $bin $files

if( $LASTEXITCODE -ne 0) {
	throw "javac failed: $LASTEXITCODE"
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
