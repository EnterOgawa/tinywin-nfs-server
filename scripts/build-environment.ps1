$tinyWinNfsBuildRoot = Split-Path -Parent $PSScriptRoot

function Get-TinyWinNfsComponents {
	Get-Content -LiteralPath (Join-Path $tinyWinNfsBuildRoot "dependencies\components.json") -Raw -Encoding UTF8 | ConvertFrom-Json
}

function Assert-TinyWinNfsWorkspacePath {
	param([string]$Path)

	$workspace = [IO.Path]::GetFullPath($tinyWinNfsBuildRoot).TrimEnd('\')
	$resolved = [IO.Path]::GetFullPath($Path)

	# プロジェクト配下の具体的な出力先だけを許可する。
	if( !$resolved.StartsWith($workspace + '\', [StringComparison]::OrdinalIgnoreCase)) {
		throw "Output path is outside the project: $resolved"
	}

	$ancestor = $resolved
	# リンクを経由してプロジェクト外へ書き込まない。
	while( $ancestor -and !$ancestor.Equals($workspace, [StringComparison]::OrdinalIgnoreCase)) {
		if( (Test-Path -LiteralPath $ancestor) -and ((Get-Item -LiteralPath $ancestor -Force).Attributes -band [IO.FileAttributes]::ReparsePoint)) {
			throw "Output path contains a link: $ancestor"
		}

		$ancestor = Split-Path -Parent $ancestor
	}
}

function Get-TinyWinNfsBuildEnvironment {
	param(
		[string]$JdkPath = "",
		[string]$SwtJarPath = "",
		[switch]$RequireSwt
	)

	# 引数、環境変数、ローカルの指定 JDK、PATH の順で選ぶ。
	if( [string]::IsNullOrWhiteSpace($JdkPath)) {
		$JdkPath = $env:TINYWIN_NFS_JDK
	}

	if( [string]::IsNullOrWhiteSpace($JdkPath)) {
		$JdkPath = $env:JAVA_HOME
	}

	if( [string]::IsNullOrWhiteSpace($JdkPath)) {
		$localJdk = "C:\develop\tools\eclipse202503\eclipse\jdk-21.0.11+10"
		if( Test-Path -LiteralPath $localJdk) {
			$JdkPath = $localJdk
		} else {
			$compiler = Get-Command javac.exe -ErrorAction SilentlyContinue | Select-Object -First 1
			if( $null -ne $compiler) {
				$JdkPath = Split-Path -Parent (Split-Path -Parent $compiler.Source)
			}
		}
	}

	if( [string]::IsNullOrWhiteSpace($JdkPath)) {
		throw "Set TINYWIN_NFS_JDK or JAVA_HOME to a Java 21 JDK, or pass -JdkPath."
	}

	$jdk = [IO.Path]::GetFullPath($JdkPath)
	$java = Join-Path $jdk "bin\java.exe"
	$javac = Join-Path $jdk "bin\javac.exe"
	$releasePath = Join-Path $jdk "release"
	if( !(Test-Path -LiteralPath $java) -or !(Test-Path -LiteralPath $javac) -or !(Test-Path -LiteralPath $releasePath)) {
		throw "Java 21 JDK is not found: $jdk"
	}

	$release = @{}
	# JDK 自身の版情報から確認し、別の Java を起動しない。
	foreach( $line in (Get-Content -LiteralPath $releasePath -Encoding UTF8)) {
		if( $line -match '^([A-Z_]+)="(.*)"$') {
			$release[$Matches[1]] = $Matches[2]
		}
	}

	if( $release.JAVA_VERSION -notmatch '^21(\.|$)') {
		throw "Java 21 is required: $($release.JAVA_VERSION)"
	}

	if( $RequireSwt) {
		if( [string]::IsNullOrWhiteSpace($SwtJarPath)) {
			$SwtJarPath = $env:TINYWIN_NFS_SWT_JAR
		}

		if( [string]::IsNullOrWhiteSpace($SwtJarPath)) {
			$SwtJarPath = Join-Path $tinyWinNfsBuildRoot "lib\org.eclipse.swt.win32.win32.x86_64.jar"
		}

		if( !(Test-Path -LiteralPath $SwtJarPath -PathType Leaf)) {
			throw "SWT jar is not found. Run scripts\prepare-dependencies.ps1 or pass -SwtJarPath."
		}

		$SwtJarPath = [IO.Path]::GetFullPath($SwtJarPath)
		$components = Get-TinyWinNfsComponents
		if( (Get-FileHash -LiteralPath $SwtJarPath -Algorithm SHA256).Hash -ne $components.swt.binary.sha256) {
			throw "SWT jar does not match dependencies\components.json: $SwtJarPath"
		}
	}

	[pscustomobject]@{ Jdk = $jdk; Java = $java; Javac = $javac; SwtJar = $SwtJarPath; Release = $release }
}

function Get-TinyWinNfsPinnedArtifact {
	param($Artifact, [string]$Destination)

	Assert-TinyWinNfsWorkspacePath -Path $Destination
	if( $Artifact.sha256 -notmatch '^[0-9a-fA-F]{64}$' -or $Artifact.url -notmatch '^https://') {
		throw "Invalid dependency entry: $($Artifact.fileName)"
	}

	if( (Test-Path -LiteralPath $Destination -PathType Leaf) -and (Get-FileHash -LiteralPath $Destination -Algorithm SHA256).Hash -eq $Artifact.sha256) {
		return
	}

	New-Item -ItemType Directory -Path (Split-Path -Parent $Destination) -Force | Out-Null
	$temporary = $Destination + "." + [Guid]::NewGuid().ToString("N") + ".tmp"
	try {
		# Windows PowerShell 5.1 でも HTTPS の取得条件を揃える。
		[Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12
		Invoke-WebRequest -Uri $Artifact.url -OutFile $temporary -UseBasicParsing
		if( (Get-FileHash -LiteralPath $temporary -Algorithm SHA256).Hash -ne $Artifact.sha256) {
			throw "SHA-256 mismatch: $($Artifact.fileName)"
		}

		Move-Item -LiteralPath $temporary -Destination $Destination -Force
	} finally {
		if( Test-Path -LiteralPath $temporary) {
			Remove-Item -LiteralPath $temporary -Force
		}
	}
}
