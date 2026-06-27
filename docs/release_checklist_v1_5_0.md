# v1.5.0 リリースチェックリスト

v1.5.0 は NFSv3 リリース後の運用安定性マイルストーンです。

## 範囲

- overwrite、truncate、rename、COMMIT の書込経路整合性。
- 複数クライアント編集後の属性とキャッシュの整合性。
- 長時間稼働時のサービス安定性確認。
- RPC エラー処理と運用ログの読みやすさ。
- 複数 export 設定の検証と安全な永続化。
- インストーラー更新、サービス置換、設定保持。
- tag 作成前の README 確認。

## ローカル確認

```powershell
git diff --check
.\scripts\compile.ps1
.\scripts\test.ps1
.\scripts\package-installer.ps1
```

## Windows Client for NFS 確認

Windows Client for NFS がインストール済みで、UDP `111`、`2049`、`20048` が空いている環境で実行します。

```powershell
.\scripts\test-windows-nfs-client.ps1
```

期待結果:

```text
WINDOWS NFS CLIENT TEST PASSED
```

## サービス確認

サービスをインストールまたは更新してから実行します。

```powershell
.\scripts\smoke-service.ps1
.\scripts\smoke-service.ps1 -VerifyFileIntegrity
.\scripts\smoke-service.ps1 -RestartHandlePersistence
```

長時間確認:

```powershell
.\scripts\test-service-stability.ps1 -DurationMinutes 60 -IntervalSeconds 15 -RestartEveryIterations 10
```

## インストーラー更新確認

1. 最新リリース済みパッケージをインストールします。
2. 少なくとも 1 つの書込可能 export を設定します。
3. サービスが実行中で、スモークテストが成功することを確認します。
4. 既存インストールの上から v1.5.0 パッケージをインストールします。
5. 既存サービスが停止、置換、再起動されたことを確認します。
6. `conf\nfs-server.properties` に設定済み export が残っていることを確認します。
7. サービス確認と Windows Client for NFS 確認を再実行します。

## README 確認

release tag を作成する前に以下を確認します。

- README に現在対応しているプロトコルと検証済みクライアントが記載されていること。
- 未対応項目が明示されていること: NLM/file locking、TCP 通信方式、NFSv4。
- インストール、更新、検証コマンドがパッケージと一致していること。
- 複数 export と書込可能共有の説明が管理ツール UI と一致していること。

## リリースメタデータ

- v1.5.0 milestone の issue がすべて close されていること。
- release tag は `v1.5.0` であること。
- release title は `TinyWinNFS Server v1.5.0` であること。
- installer asset は `TinyWinNfsSetup.exe` であること。
- GitHub release note にインストーラー SHA256 を含めること。
