# v1.4.0 リリースチェックリスト

v1.4.0 は UDP 上の NFSv3 / MOUNT v3 マイルストーンです。

## ソース確認

```powershell
git status -sb
.\scripts\compile.ps1
.\scripts\test.ps1
```

単体テストの期待結果:

```text
TEST PASSED: 12 tests
```

## プロトコル確認

- Portmap が NFS v2/v3 の UDP ポートを返すこと。
- Portmap が MOUNT v1/v2/v3 の UDP ポートを返すこと。
- MOUNT v1/v2 が従来どおり固定 32 byte の NFSv2 handle を返すこと。
- MOUNT v3 が opaque file handle と AUTH_NONE/AUTH_SYS flavor を返すこと。
- NFSv2 回帰 procedure が引き続き成功すること。
- NFSv3 procedure が GETATTR、SETATTR、LOOKUP、ACCESS、READ、WRITE、CREATE、MKDIR、REMOVE、RMDIR、RENAME、READDIRPLUS、FSSTAT、FSINFO、PATHCONF、COMMIT で成功すること。

## Windows Client for NFS

管理者 PowerShell で実行します。

```powershell
.\scripts\test-windows-nfs-client.ps1
```

期待結果:

```text
PASS: Windows Client for NFS mount
PASS: Windows Client for NFS v3 RPC
PASS: Windows Client for NFS create/read/update/rename/delete
PASS: Windows Client for NFS directory create/delete
PASS: Windows Client for NFS Japanese filename
WINDOWS NFS CLIENT TEST PASSED
```

スクリプトは、サーバーログに以下の両方が含まれることを確認します。

```text
program=100005 version=3
program=100003 version=3
```

## 対象外

- TCP 通信方式
- NLM/file locking

## インストーラー

```powershell
.\scripts\package-installer.ps1
```

生成されたインストーラーを確認します。

```powershell
(Get-Item .\dist\installer\TinyWinNfsSetup.exe).VersionInfo | Select-Object ProductVersion,ProductName,FileDescription
Get-FileHash .\dist\installer\TinyWinNfsSetup.exe -Algorithm SHA256
```

## GitHub

- v1.4.0 milestone の issue がすべて close されていること。
- release tag は `v1.4.0` であること。
- release title は `TinyWinNFS Server v1.4.0` であること。
- `dist\installer\TinyWinNfsSetup.exe` を添付すること。
- release asset の SHA256 がローカルファイルと一致することを確認すること。
