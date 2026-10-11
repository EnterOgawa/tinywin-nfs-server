$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot "build-environment.ps1")
$java = (Get-TinyWinNfsBuildEnvironment -RequireSwt).Java
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
