# v1.2.0 リリースチェックリスト

v1.2.0 は Windows Client for NFS 結合テストのマイルストーンです。

## ソース確認

```powershell
git status -sb
.\scripts\compile.ps1
.\scripts\test.ps1
```

単体テストの期待結果:

```text
TEST PASSED: 9 tests
```

## Windows Client for NFS 確認

前提条件を確認します。

```powershell
Get-Command mount.exe
Get-Service NfsClnt
```

mount スモークテストを実行します。

```powershell
.\scripts\test-windows-nfs-client.ps1
```

期待結果:

```text
WINDOWS NFS CLIENT TEST PASSED
```

スクリプト終了後、テスト用 mount drive と一時 TinyWinNFS サーバープロセスが残っていてはいけません。

## 任意のクライアント確認

VMware 上の QNX 4.25 は、従来クライアント互換性の確認として扱います。

Linux VM mount テストは追加検証として使用できます。

WSL は任意であり、v1.2.0 リリースの必須条件ではありません。

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

- v1.2.0 milestone の issue がすべて close されていること。
- release tag は `v1.2.0` であること。
- release title は `TinyWinNFS Server 1.2.0` であること。
- `dist\installer\TinyWinNfsSetup.exe` を添付すること。
- release asset の SHA256 がローカルファイルと一致することを確認すること。
