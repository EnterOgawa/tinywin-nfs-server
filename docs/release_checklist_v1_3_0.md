# v1.3.0 リリースチェックリスト

v1.3.0 は運用強化のマイルストーンです。

## ソース確認

```powershell
git status -sb
.\scripts\compile.ps1
.\scripts\test.ps1
```

単体テストの期待結果:

```text
TEST PASSED: 11 tests
```

## 機能確認

- 不正な export 名が保存/起動前に拒否されること。
- フォルダが存在しない、読み取れない、または必要時に書き込めない場合に拒否されること。
- `allowed.clients` が空欄の場合、すべてのクライアントが許可されること。
- `allowed.clients=127.0.0.1` ではローカルテストを許可し、別の IPv4 アドレスを拒否すること。
- Save + Restart で保存結果と再起動結果が個別にログ表示されること。
- サービス タブにアプリケーションルート、設定ファイル、サービス実行ファイルパスが表示されること。
- 管理者権限がない場合、サービス操作で管理者権限エラーが表示されること。

## ログ確認

要求診断に以下が含まれることを確認します。

- クライアントアドレス
- XID
- RPC program/version/procedure
- status
- 変更操作または access-denied 操作のパス

成功した NFS READ 要求ログは、`-Dtinywin.nfs.debug=true` が設定されていない限り抑制されること。

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

- v1.3.0 milestone の issue がすべて close されていること。
- release tag は `v1.3.0` であること。
- release title は `TinyWinNFS Server 1.3.0` であること。
- `dist\installer\TinyWinNfsSetup.exe` を添付すること。
- release asset の SHA256 がローカルファイルと一致することを確認すること。
