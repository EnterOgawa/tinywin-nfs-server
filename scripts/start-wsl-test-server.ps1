$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$java = "C:\develop\tools\eclipse202503\eclipse\jdk-21.0.8+9\bin\java.exe"
$config = Join-Path $root "conf\nfs-server-wsl-test.properties"
$bin = Join-Path $root "bin"

if( !(Test-Path -LiteralPath $java)) {
	throw "java.exe is not found: $java"
}

if( !(Test-Path -LiteralPath $config)) {
	throw "configuration file is not found: $config"
}

& (Join-Path $root "scripts\compile.ps1")
& $java -cp $bin jp.co.enterogawa.nfs.NfsServerMain $config
