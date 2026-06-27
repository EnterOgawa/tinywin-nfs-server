# サービス安定性テスト

この確認は、NFSv3 と TCP 通信方式のマイルストーン後の運用強化を目的にしています。

TinyWinNFS Server が Windows サービスとしてインストール済みで、設定済み export にテストファイルを作成しても問題ない環境でのみ実行してください。

## 短時間実行

```powershell
.\scripts\test-service-stability.ps1 -DurationMinutes 10 -IntervalSeconds 10
```

スクリプトは以下を繰り返し実行します。

```powershell
.\scripts\smoke-service.ps1 -VerifyFileIntegrity
```

整合性スモークテストでは、サービス RPC 経路を通して create、overwrite、truncate、append、rename overwrite、サーバー側ディスク内容、後片付けを確認します。

## 再起動を含む実行

```powershell
.\scripts\test-service-stability.ps1 -DurationMinutes 60 -IntervalSeconds 15 -RestartEveryIterations 10
```

この実行では、整合性確認の間に定期的なサービス再起動を追加します。UDP/TCP ポートが正しく解放され、再 bind されることをリリース前に確認する場合に使用します。

## 合格基準

- すべての iteration で `PASS: service file integrity` が表示されます。
- 最終行が `SERVICE STABILITY TEST PASSED` です。
- iteration 間で `scripts\status-service.ps1` が `TinyWinNfsServer` running を報告します。
- 実行後、設定済み export フォルダに `service-integrity-*.txt` ファイルが残りません。

## 失敗時の扱い

スクリプトが失敗した場合は、失敗した操作を特定するまでサービスログと export フォルダを残します。関連ログには、クライアントアドレス、RPC 操作、status、path context が含まれているはずです。
