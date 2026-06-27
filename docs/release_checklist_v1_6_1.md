# v1.6.1 リリースチェックリスト

v1.6.1 は、TCP 通信方式マイルストーン後の互換性と性能の安定化リリースです。

## 範囲

- AUTH_SYS UID/GID に基づく `permission.identity=auto` の自動権限 ID。
- クライアント IP 個別プロファイルなしでの Windows Client for NFS 直接マウント互換性。
- QNX 4.25 の大量コピー性能改善。
- QNX `.nfsX*` ディレクトリ削除互換性。
- 書込ファイルキャッシュと非同期書込の既定動作。
- 大量コピー/削除時の高頻度成功ログの削減。

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

期待結果:

```text
SERVICE SMOKE TEST PASSED
PASS: service file integrity
PASS: service handle persistence after restart
```

## Windows Client for NFS 確認

Windows Client for NFS 結合確認を実行します。

```powershell
.\scripts\test-windows-nfs-client.ps1
.\scripts\test-windows-nfs-client.ps1 -Transport TCP
```

2 つのコマンドは独立した確認として実行します。Windows Client for NFS は、通信方式を短時間で切り替えるとマウントや portmap の状態をキャッシュする場合があります。

各通信方式の期待結果:

```text
WINDOWS NFS CLIENT TEST PASSED
```

`permission.identity=auto` が有効な状態で、インストール済みサービスの export も直接確認します。

- Windows Client for NFS で `\\127.0.0.1\export` をマウントします。
- ファイルを作成します。
- 同じファイルに追記します。
- ファイルを rename します。
- ファイルと親テストディレクトリを削除します。
- 一時マウントドライブが残っていないことを確認します。

## QNX 確認

QNX 4.25 上で以下を確認します。

- NFSv2/UDP で export をマウントします。
- QNX から Windows export へ大きなディレクトリツリーをコピーします。
- `.nfsX*` の残骸なしでコピーが完了することを確認します。
- QNX からコピー済みツリーを削除します。
- Windows export にコピー済みツリーが残っていないことを確認します。
- `logs/nfs-server.log` に現在の `status=13`、parse-error、予期しない変更操作失敗がないことを確認します。

## インストーラー更新確認

1. 最新リリース済みパッケージをインストールします。
2. 少なくとも 1 つの書込可能 export を設定します。
3. サービスが実行中で、UDP スモークテストが成功することを確認します。
4. 既存インストールの上から v1.6.1 パッケージをインストールします。
5. 既存サービスが停止、置換、再起動されたことを確認します。
6. `conf\nfs-server.properties` に設定済み export が残っていることを確認します。
7. `permission.identity=auto` が存在することを確認するか、管理ツールから追加します。
8. UDP スモークテスト、TCP スモークテスト、ファイル整合性スモークテスト、Windows Client for NFS 確認を実行します。

## README 確認

release tag を作成する前に以下を確認します。

- README が v1.6.1 の対応範囲を記載していること。
- `permission.identity=auto/fixed` が文書化されていること。
- 未対応項目が明示されていること: NFSv3 `MKNOD`、NLM/file locking、NFSv4。
- QNX 4.25 が NFSv2/UDP 検証対象として記載されていること。
- Windows Client for NFS が NFSv3 UDP/TCP 検証対象として記載されていること。

## リリースメタデータ

- release tag は `v1.6.1` であること。
- release title は `TinyWinNFS Server v1.6.1` であること。
- installer asset は `TinyWinNfsSetup.exe` であること。
- GitHub release note にインストーラー SHA256 を含めること。
