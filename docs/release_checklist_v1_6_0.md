# v1.6.0 リリースチェックリスト

v1.6.0 は、ONC RPC 上の NFSv2/NFSv3 と MOUNT に対する TCP 通信方式マイルストーンです。

## 範囲

- ONC RPC over TCP の record marking。
- 設定済みサービスポートを使用する Portmap、MOUNT、NFS の TCP 待受。
- NFS v2/v3 と MOUNT v1-v3 の Portmap TCP 割り当て。
- UDP と同じ実装を使用した TCP 上の NFSv2/NFSv3 ファイル操作。
- Windows Client for NFS の TCP 確認。
- TCP 対応に伴うファイアウォール、サービス、インストーラー、ドキュメント更新。

## ローカル確認

```powershell
git diff --check
.\scripts\compile.ps1
.\scripts\test.ps1
.\scripts\package-installer.ps1
```

単体テストの期待結果:

```text
TEST PASSED: 13 tests
```

## サービス確認

サービスをインストールまたは更新してから実行します。

```powershell
.\scripts\smoke-service.ps1
.\scripts\smoke-service.ps1 -Transport TCP
.\scripts\smoke-service.ps1 -VerifyFileIntegrity
.\scripts\smoke-service.ps1 -RestartHandlePersistence
```

長時間確認:

```powershell
.\scripts\test-service-stability.ps1 -DurationMinutes 60 -IntervalSeconds 15 -RestartEveryIterations 10
```

## Windows Client for NFS 確認

Windows Client for NFS がインストール済みで、UDP/TCP `111`、`2049`、`20048` が空いている環境で実行します。

```powershell
.\scripts\test-windows-nfs-client.ps1
.\scripts\test-windows-nfs-client.ps1 -Transport TCP
```

2 つのコマンドは独立した確認として実行します。Windows Client for NFS は、通信方式を短時間で切り替えるとマウントや portmap の状態をキャッシュする場合があります。

各通信方式の期待結果:

```text
WINDOWS NFS CLIENT TEST PASSED
```

TCP 実行では、`server=nfs-mount-tcp` と `server=nfs-tcp` のログも確認すること。
既定 export 名は実行ごとに一意にし、UDP/TCP 確認間で Windows Client for NFS の mount handle cache が再利用されることを避けます。

## インストーラー更新確認

1. 最新リリース済みパッケージをインストールします。
2. 少なくとも 1 つの書込可能 export を設定します。
3. サービスが実行中で、UDP スモークテストが成功することを確認します。
4. 既存インストールの上から v1.6.0 パッケージをインストールします。
5. 既存サービスが停止、置換、再起動されたことを確認します。
6. `conf\nfs-server.properties` に設定済み export が残っていることを確認します。
7. UDP スモークテスト、TCP スモークテスト、ファイル整合性スモークテスト、Windows Client for NFS 確認を実行します。

## README 確認

release tag を作成する前に以下を確認します。

- README が UDP/TCP を対応通信方式として記載していること。
- 未対応項目が明示されていること: NLM/file locking、NFSv4、NFSv3 `MKNOD`。
- インストール、更新、検証コマンドがパッケージと一致していること。
- QNX 4.25 が NFSv2/UDP 検証対象として記載されていること。

## リリースメタデータ

- v1.6.0 milestone の issue がすべて close されていること。
- release tag は `v1.6.0` であること。
- release title は `TinyWinNFS Server v1.6.0` であること。
- installer asset は `TinyWinNfsSetup.exe` であること。
- GitHub release note にインストーラー SHA256 を含めること。
