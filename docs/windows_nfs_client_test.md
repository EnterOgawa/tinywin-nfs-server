# Windows Client for NFS マウントテスト

Windows Client for NFS は、v1.2.0 以降の主要なローカル結合テストです。WSL や Hyper-V を使わずに、Windows 標準 NFS クライアントから TinyWinNFS をマウントできることを確認します。

v1.4.0 以降では、サーバーログ上で MOUNT v3 と NFSv3 RPC 要求を観測できることも確認します。
v1.6.0 以降では、同じスクリプトで UDP または TCP 通信方式を検証できます。

QNX 4.25 on VMware は、従来クライアント互換性の基準として扱います。WSL 確認は任意です。

## 前提条件

- Windows Client for NFS が有効であること。
- `NfsClnt` サービスが実行中であること。
- PowerShell が管理者として実行されていること。
- UDP/TCP `111`、`2049`、`20048` が空いていること。
- ドライブ `Z:` が空いていること。別の空きドライブ文字をスクリプトに渡しても構いません。

クライアントを確認します。

```powershell
Get-Command mount.exe
Get-Service NfsClnt
nfsadmin client
```

`NfsClnt` が `Stopped` の場合、この検証スクリプトから `NfsClnt` を停止/再起動しません。
Windows Client for NFS は OS 側 redirector の状態に依存するため、停止状態から復帰できない場合は Windows を再起動してから確認します。

## 自動テスト

実行:

```powershell
.\scripts\test-windows-nfs-client.ps1
```

TCP 通信方式を実行:

```powershell
.\scripts\test-windows-nfs-client.ps1 -Transport TCP
```

UDP と TCP は別々のテスト実行として扱います。Windows Client for NFS は通信方式の切り替え後にマウントや portmap の状態をキャッシュすることがあります。TCP ログ確認で `server=nfs-mount-tcp` を観測できない場合は、前回のマウントが完全に消えてから TCP コマンドを再実行します。

Windows 側の protocol 設定を事前に済ませている場合は、スクリプトから設定変更を行わないようにできます。

```powershell
.\scripts\test-windows-nfs-client.ps1 -Transport TCP -SkipProtocolChange
```

レポート出力先を明示する場合:

```powershell
.\scripts\test-windows-nfs-client.ps1 -ReportPath work\analysis\windows-nfs-client\manual-report.md
```

スクリプトは以下を行います。

- `work\tmp` 配下に一時 export を作成します。
- Windows クライアントテスト設定で TinyWinNFS を起動します。
- Windows Client for NFS が選択した通信方式を使うように設定し、終了時に以前のプロトコル設定を復元します。`-SkipProtocolChange` 指定時は変更しません。
- `\\127.0.0.1\export-udp-20260627153000` のような一意の既定 export 名を `Z:` へ mount します。
- MOUNT v3 と NFSv3 要求がサーバーへ到達したことを確認します。
- create、read、update、rename、delete を確認します。
- ディレクトリの create/delete を確認します。
- `lang=shift-jis` を使って日本語ファイル名を確認します。
- `work\analysis\windows-nfs-client` 配下へMarkdownレポートを出力します。
- ドライブを unmount し、一時サーバーを停止します。

v1.7.0 の単体テストでは、NFSv2/NFSv3 `READLINK`、`SYMLINK`、壊れた symlink の `READLINK`、NFSv3 `MKNOD` 拒否を確認します。Windows Client for NFS の結合確認は、Windows クライアントによる POSIX symlink 作成の挙動が環境依存であるため、マウント、通常ファイル、ディレクトリ、ファイル名エンコードに集中します。

期待結果:

```text
PASS: Windows Client for NFS mount
PASS: Windows Client for NFS UDP transport
PASS: Windows Client for NFS v3 RPC
PASS: Windows Client for NFS create/read/update/rename/delete
PASS: Windows Client for NFS directory create/delete
PASS: Windows Client for NFS Japanese filename
WINDOWS NFS CLIENT TEST PASSED
```

## レポート

既定では、実行ごとに以下の形式でMarkdownレポートを作成します。

```text
work\analysis\windows-nfs-client\windows-nfs-client-udp-YYYYMMDD-HHMMSS.md
```

レポートには以下が含まれます。

- 実行結果、開始/終了時刻、transport、server、export、drive
- `NfsClnt` サービス状態
- `nfsadmin client` の出力
- protocol 変更の有無
- port 使用状況
- mount 対象
- create/read/update/rename/delete の結果
- MOUNT v3 / NFSv3 と UDP/TCP のログ確認結果
- 失敗時の failure と cleanup 結果

別のドライブ文字を使う場合:

```powershell
.\scripts\test-windows-nfs-client.ps1 -DriveLetter Y
```

固定 export 名を使う場合:

```powershell
.\scripts\test-windows-nfs-client.ps1 -ExportName export
```

テスト後に一時作業フォルダを残す場合:

```powershell
.\scripts\test-windows-nfs-client.ps1 -KeepWork
```

## 手動 mount

同等の mount コマンド:

```cmd
mount -o anon,nolock,rsize=8,wsize=8,lang=shift-jis \\127.0.0.1\export Z:
```

Windows の `mount.exe` は、help 出力上で portable な NFS version option を公開していません。v3 の利用は TinyWinNFS ログで確認します。v1.4.0 の成功テストでは、以下を含むログが出ます。

```text
program=100005 version=3
program=100003 version=3
```

TCP 検証では、以下を含むログも必要です。

```text
server=nfs-mount-tcp
server=nfs-tcp
```

unmount:

```cmd
umount Z:
```

TinyWinNFS は NLM を実装していないため、`nolock` が必要です。

## Windows 匿名 UID/GID

Windows Client for NFS の匿名マウントは、多くの場合 `UID=-2`、`GID=-2` を使います。サーバーが `uid=0`、`gid=0`、`file.mode=0644` を返すと、Windows がファイル作成を許可しても、同じファイルへの後続更新を拒否することがあります。

レジストリ変更なしで Windows クライアント結合テストを行う場合は、以下を使用します。

```properties
uid=-2
gid=-2
file.mode=0666
directory.mode=0777
filename.charset=Shift_JIS
```

サンプルプロファイルは `conf\nfs-server-windows-client-test.properties` です。

## トラブルシュート

`mount.exe` が見つからない場合は、Windows Client for NFS を有効にします。

`NfsClnt` が実行されていない場合:

```powershell
Get-Service NfsClnt
nfsadmin client
```

`NfsClnt` が停止状態から起動維持できない場合は、Windows Client for NFS の OS 側状態が壊れている可能性があります。
その場合は Windows を再起動してから確認します。テストスクリプトから `NfsClnt` や `NfsRdr` を停止/再起動する運用は避けます。

```powershell
Restart-Computer
```

ファイル作成後に `Access denied` が発生する場合は、テストサーバーが `UID=-2` / `GID=-2` に対して書込可能属性を返しているか確認します。

mount コマンドが成功しても日本語ファイル名が文字化けする場合は、両側が同じエンコードを使っているか確認します。

- Windows mount option: `lang=shift-jis`
- TinyWinNFS config: `filename.charset=Shift_JIS`
