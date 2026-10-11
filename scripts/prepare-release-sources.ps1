param(
	[string]$JdkPath = "",
	[string]$Destination = ""
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot "build-environment.ps1")
$environment = Get-TinyWinNfsBuildEnvironment -JdkPath $JdkPath
$components = Get-TinyWinNfsComponents

# runtime と対応ソースの版が一致する JDK だけを配布に使用する。
if( $environment.Release.JAVA_VERSION -ne $components.runtime.version -or
	$environment.Release.IMPLEMENTOR -ne $components.runtime.implementor -or
	$environment.Release.IMPLEMENTOR_VERSION -ne $components.runtime.implementorVersion -or
	$environment.Release.OS_ARCH -ne $components.runtime.architecture) {
	throw "Release packaging requires $($components.runtime.implementorVersion) $($components.runtime.architecture). Update the component manifest and corresponding sources together."
}

if( [string]::IsNullOrWhiteSpace($Destination)) {
	$Destination = Join-Path $root "dist\installer\TinyWinNfsSources.zip"
} elseif( ![IO.Path]::IsPathRooted($Destination)) {
	$Destination = Join-Path $root $Destination
}

Assert-TinyWinNfsWorkspacePath -Path $Destination
$stage = Join-Path $root "work\tmp\release-sources"
Assert-TinyWinNfsWorkspacePath -Path $stage
if( Test-Path -LiteralPath $stage) {
	Remove-Item -LiteralPath $stage -Recurse -Force
}

New-Item -ItemType Directory -Path $stage -Force | Out-Null
$cache = Join-Path $root "work\tmp\dependencies"

# Java、ネイティブ部品、ビルドスクリプトを含む対応ソースを揃える。
foreach( $componentName in @("swt", "winsw", "runtime")) {
	$sourceDirectory = Join-Path $stage $componentName
	New-Item -ItemType Directory -Path $sourceDirectory -Force | Out-Null
	foreach( $artifact in $components.$componentName.sources) {
		$cached = Join-Path $cache $artifact.fileName
		Get-TinyWinNfsPinnedArtifact -Artifact $artifact -Destination $cached
		Copy-Item -LiteralPath $cached -Destination (Join-Path $sourceDirectory $artifact.fileName)
	}
}

Copy-Item -LiteralPath (Join-Path $root "licenses") -Destination (Join-Path $stage "licenses") -Recurse
Copy-Item -LiteralPath (Join-Path $root "dependencies\components.json") -Destination $stage
Copy-Item -LiteralPath (Join-Path $root "docs\source_distribution.md") -Destination (Join-Path $stage "SOURCES.md")
Copy-Item -LiteralPath (Join-Path $environment.Jdk "release") -Destination (Join-Path $stage "jdk-release.txt")
Copy-Item -LiteralPath (Join-Path $environment.Jdk "legal") -Destination (Join-Path $stage "runtime-legal") -Recurse
# runtime image を再生成できるよう、本製品のソースとビルド入力も含める。
foreach( $name in @("src", "test", "scripts", "dependencies", "assets", "conf", "installer", "launcher", "sample", ".settings")) {
	Copy-Item -LiteralPath (Join-Path $root $name) -Destination (Join-Path $stage $name) -Recurse
}

foreach( $name in @("LICENSE", "THIRD_PARTY_NOTICES.md", "README.md", "CONTRIBUTING.md", "SECURITY.md", "CHANGELOG.md", ".project", ".classpath")) {
	Copy-Item -LiteralPath (Join-Path $root $name) -Destination $stage
}

# 可変データやサービスログを混入させず、配布用の入力だけをコピーする。
New-Item -ItemType Directory -Path (Join-Path $stage "service\winsw") -Force | Out-Null
foreach( $name in @("nfs-server.xml", "nfs-server.package.xml")) {
	Copy-Item -LiteralPath (Join-Path $root "service\winsw\$name") -Destination (Join-Path $stage "service\winsw\$name")
}

Copy-Item -LiteralPath (Join-Path $root "docs") -Destination (Join-Path $stage "docs") -Recurse
New-Item -ItemType Directory -Path (Join-Path $stage "export") | Out-Null
Copy-Item -LiteralPath (Join-Path $root "export\README.txt") -Destination (Join-Path $stage "export\README.txt")

New-Item -ItemType Directory -Path (Split-Path -Parent $Destination) -Force | Out-Null
$temporaryArchive = $Destination + "." + [Guid]::NewGuid().ToString("N") + ".zip"
try {
	Compress-Archive -Path (Join-Path $stage "*") -DestinationPath $temporaryArchive -CompressionLevel NoCompression
	Move-Item -LiteralPath $temporaryArchive -Destination $Destination -Force
} finally {
	if( Test-Path -LiteralPath $temporaryArchive) {
		Remove-Item -LiteralPath $temporaryArchive -Force
	}
}

Get-FileHash -LiteralPath $Destination -Algorithm SHA256
