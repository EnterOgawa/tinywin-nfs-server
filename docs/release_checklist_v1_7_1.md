# v1.7.1 リリースチェックリスト

v1.7.1 は、アプリケーションファイルと可変の実行時データを分離するインストール配置の強化リリースです。

## 範囲

- インストール済み設定を `C:\ProgramData\EnterOgawa\TinyWinNFS Server\conf` 配下に保存します。
- 既定 export フォルダを `C:\ProgramData\EnterOgawa\TinyWinNFS Server\export` 配下に保存します。
- TinyWinNFS サーバーログを `C:\ProgramData\EnterOgawa\TinyWinNFS Server\logs` 配下に保存します。
- 新しい `ProgramData` 設定が存在しない場合のみ、旧 `Program Files` 設定を移行します。
- 設定ファイルが `conf` 配下にある場合、相対 `export.path` をデータルート基準で解決します。

## ローカル確認

```powershell
git diff --check
.\scripts\compile.ps1
.\scripts\test.ps1
.\scripts\package-installer.ps1
```

単体テストの期待結果:

```text
TEST PASSED: 14 tests
```

## インストーラー確認

サービスタスクを有効にしてインストールまたは更新し、以下を確認します。

- `C:\ProgramData\EnterOgawa\TinyWinNFS Server\conf\nfs-server.properties` が存在すること。
- `C:\ProgramData\EnterOgawa\TinyWinNFS Server\export` が存在すること。
- `C:\ProgramData\EnterOgawa\TinyWinNFS Server\logs` が存在すること。
- 更新時に既存の設定済み export が保持されること。
- インストール済み WinSW XML が `ProgramData` 側の設定ファイルを参照していること。
- インストール済み管理ツールのサービス タブに、アプリケーションルート、データルート、設定ファイル、サービス実行ファイル、ログファイルの各パスが個別に表示されること。

## サービス確認

実行:

```powershell
.\scripts\smoke-service.ps1
.\scripts\smoke-service.ps1 -Transport TCP
.\scripts\smoke-service.ps1 -VerifyFileIntegrity
.\scripts\smoke-service.ps1 -RestartHandlePersistence
```

期待結果:

```text
SERVICE SMOKE TEST PASSED
PASS: service file integrity
PASS: service handle persistence after restart
```

## リリースメタデータ

- release tag は `v1.7.1` であること。
- release title は `TinyWinNFS Server v1.7.1` であること。
- installer asset は `TinyWinNfsSetup.exe` であること。
- GitHub release note にインストーラー SHA256 を含めること。
