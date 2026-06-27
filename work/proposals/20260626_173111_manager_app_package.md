# 管理ツール exe パッケージ作成

## 変更概要
- Swing ベースの管理ツール `QnxNfsServerManager` を追加。
- `jpackage` によるダブルクリック起動用 app-image 作成スクリプト `scripts/package-manager.ps1` を追加。
- 配布版用 WinSW 設定 `service/winsw/nfs-server.package.xml` を追加。
- 管理者権限起動用 `launcher/QnxNfsServerManager-Admin.cmd` を追加。
- 管理ツールドキュメント `docs/manager_app.md` を追加。
- 配布版サービス用に `service-runtime` を `jlink` で生成する処理を追加。

## 生成結果
生成先:

```text
dist\QnxNfsServerManager
```

入口:

```text
dist\QnxNfsServerManager\QnxNfsServerManager.exe
dist\QnxNfsServerManager\QnxNfsServerManager-Admin.cmd
```

配布版サービス設定:

```text
service\winsw\nfs-server.xml
```

サービス起動 Java:

```text
service-runtime\bin\java.exe
```

サービス実行 jar:

```text
app\qnx-nfs-server.jar
```

## 確認結果
- `scripts\compile.ps1` 成功。
- `scripts\test.ps1` 成功。
- `scripts\package-manager.ps1` 成功。
- 配布版に `QnxNfsServerManager.exe` が存在することを確認。
- 配布版に `QnxNfsServerManager-Admin.cmd` が存在することを確認。
- 配布版に `service-runtime\bin\java.exe` が存在することを確認。
- 現在起動中の開発フォルダ側サービスに対する `scripts\smoke-service.ps1` 成功。

## 注意
- 既存の開発フォルダ側サービスは停止・再インストールしていない。
- 配布フォルダ側のサービス登録テストは、同一サービス名 `QnxNfsServer` と競合するため未実施。
