$ErrorActionPreference = "Stop"

function Test-Administrator {
	$principal = New-Object Security.Principal.WindowsPrincipal([Security.Principal.WindowsIdentity]::GetCurrent())
	return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

if( !(Test-Administrator)) {
	throw "Administrator privileges are required to add firewall rules."
}

$rules = @(
	@{ Name = "TinyWinNFS-Portmap-UDP-111"; Port = 111 },
	@{ Name = "TinyWinNFS-NFS-UDP-2049"; Port = 2049 },
	@{ Name = "TinyWinNFS-Mount-UDP-20048"; Port = 20048 }
)

foreach( $rule in $rules) {
	$existing = Get-NetFirewallRule -DisplayName $rule.Name -ErrorAction SilentlyContinue

	if( $existing -ne $null) {
		continue
	}

	New-NetFirewallRule `
		-DisplayName $rule.Name `
		-Direction Inbound `
		-Action Allow `
		-Protocol UDP `
		-LocalPort $rule.Port | Out-Host
}
