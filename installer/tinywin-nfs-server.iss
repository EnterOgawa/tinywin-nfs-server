#define AppName "TinyWinNFS Server"
#define AppVersion "1.7.0"
#define AppPublisher "EnterOgawa"
#define AppExeName "TinyWinNfsManager.exe"
#define SourceDir "..\dist\TinyWinNfsManager"

[Setup]
AppId={{8F5216BB-CCAB-4E7E-B8FA-3D2F52B2F214}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher={#AppPublisher}
AppCopyright=Copyright 2026 Shunji Ogawa
DefaultDirName={autopf}\EnterOgawa\TinyWinNFS Server
DefaultGroupName=TinyWinNFS Server
DisableProgramGroupPage=yes
OutputDir=..\dist\installer
OutputBaseFilename=TinyWinNfsSetup
SetupIconFile=..\assets\tinywin-nfs-server.ico
UninstallDisplayIcon={app}\{#AppExeName}
LicenseFile=..\LICENSE
InfoBeforeFile=..\THIRD_PARTY_NOTICES.md
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
PrivilegesRequired=admin
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible

[Languages]
Name: "japanese"; MessagesFile: "compiler:Languages\Japanese.isl"

[Tasks]
Name: "desktopicon"; Description: "デスクトップにショートカットを作成する"; GroupDescription: "ショートカット:"
Name: "installservice"; Description: "Windowsサービスをインストールする"; GroupDescription: "サービス:"
Name: "firewallrules"; Description: "Windows Firewallルールを追加する"; GroupDescription: "サービス:"

[Files]
Source: "{#SourceDir}\LICENSE"; DestDir: "{app}"; Flags: ignoreversion
Source: "{#SourceDir}\THIRD_PARTY_NOTICES.md"; DestDir: "{app}"; Flags: ignoreversion
Source: "{#SourceDir}\TinyWinNfsManager-Admin.cmd"; DestDir: "{app}"; Flags: ignoreversion
Source: "{#SourceDir}\TinyWinNfsManager.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "{#SourceDir}\app\*"; DestDir: "{app}\app"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "{#SourceDir}\assets\*"; DestDir: "{app}\assets"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "{#SourceDir}\docs\*"; DestDir: "{app}\docs"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "{#SourceDir}\export\*"; DestDir: "{app}\export"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "{#SourceDir}\runtime\*"; DestDir: "{app}\runtime"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "{#SourceDir}\scripts\*"; DestDir: "{app}\scripts"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "{#SourceDir}\service\*"; DestDir: "{app}\service"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "{#SourceDir}\conf\*"; DestDir: "{app}\conf"; Flags: ignoreversion recursesubdirs createallsubdirs onlyifdoesntexist

[Icons]
Name: "{group}\TinyWinNFS Manager"; Filename: "{app}\{#AppExeName}"; WorkingDir: "{app}"
Name: "{group}\TinyWinNFS Manager (Admin)"; Filename: "{app}\TinyWinNfsManager-Admin.cmd"; WorkingDir: "{app}"; IconFilename: "{app}\{#AppExeName}"
Name: "{autodesktop}\TinyWinNFS Manager"; Filename: "{app}\{#AppExeName}"; WorkingDir: "{app}"; Tasks: desktopicon

[Run]
Filename: "powershell.exe"; Parameters: "-NoProfile -ExecutionPolicy Bypass -File ""{app}\scripts\install-service.ps1"" -Force"; WorkingDir: "{app}"; StatusMsg: "Windowsサービスをインストールしています..."; Tasks: installservice; Flags: runhidden waituntilterminated
Filename: "powershell.exe"; Parameters: "-NoProfile -ExecutionPolicy Bypass -File ""{app}\scripts\add-firewall-rules.ps1"""; WorkingDir: "{app}"; StatusMsg: "Windows Firewallルールを追加しています..."; Tasks: firewallrules; Flags: runhidden waituntilterminated
Filename: "powershell.exe"; Parameters: "-NoProfile -ExecutionPolicy Bypass -File ""{app}\scripts\start-service.ps1"""; WorkingDir: "{app}"; StatusMsg: "Windowsサービスを開始しています..."; Tasks: installservice; Flags: runhidden waituntilterminated
Filename: "{app}\{#AppExeName}"; Description: "TinyWinNFS Managerを起動する"; WorkingDir: "{app}"; Flags: nowait postinstall skipifsilent

[UninstallRun]
Filename: "powershell.exe"; Parameters: "-NoProfile -ExecutionPolicy Bypass -File ""{app}\scripts\uninstall-service.ps1"""; WorkingDir: "{app}"; StatusMsg: "Windowsサービスを削除しています..."; Flags: runhidden waituntilterminated; RunOnceId: "TinyWinNfsServerUninstallService"

[Code]
function RunPowerShell(Command: String): Integer;
var
  ResultCode: Integer;
begin
  if not Exec(
      ExpandConstant('{sys}\WindowsPowerShell\v1.0\powershell.exe'),
      '-NoProfile -ExecutionPolicy Bypass -Command "' + Command + '"',
      '',
      SW_HIDE,
      ewWaitUntilTerminated,
      ResultCode) then
  begin
    Result := -1;
  end
  else
  begin
    Result := ResultCode;
  end;
end;

function PrepareToInstall(var NeedsRestart: Boolean): String;
var
  Command: String;
  ResultCode: Integer;
begin
  Result := '';

  if not WizardIsTaskSelected('installservice') then
  begin
    Exit;
  end;

  Command :=
    'try { ' +
    '$ErrorActionPreference=''Stop''; ' +
    '$names=@(''TinyWinNfsServer'',''OgawaNfsServer'',''QnxNfsServer''); ' +
    'foreach($name in $names){ ' +
    '$svc=Get-Service -Name $name -ErrorAction SilentlyContinue; ' +
    'if($null -ne $svc){ ' +
    'if($svc.Status -ne ''Stopped''){ ' +
    'Stop-Service -Name $name -Force; ' +
    '$svc.WaitForStatus(''Stopped'', [System.TimeSpan]::FromSeconds(30)); ' +
    '} ' +
    'sc.exe delete $name | Out-Null; ' +
    'Start-Sleep -Seconds 2; ' +
    '} ' +
    '} ' +
    'exit 0; ' +
    '} catch { exit 1; }';

  ResultCode := RunPowerShell(Command);

  if ResultCode <> 0 then
  begin
    Result := '既存のWindowsサービスを停止または削除できませんでした。サービス管理画面で TinyWinNfsServer を停止してから再実行してください。';
  end;
end;
