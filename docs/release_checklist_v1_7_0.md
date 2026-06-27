# v1.7.0 リリースチェックリスト

v1.7.0 は、QNX 4.25 と Windows Client for NFS 向けのリンク互換性リリースです。

## 範囲

- NFSv2/NFSv3 `READLINK` の安定したステータス処理。
- NFSv3 `READLINK` のクライアント許可リスト確認。
- NFSv2/NFSv3 `SYMLINK` における Windows 権限不足と不正なリンク先のエラー処理。
- 壊れた symlink に対する `READLINK` 回帰テスト。
- NFSv3 `MKNOD` は `NOTSUPP` として明示的に未対応のままにします。

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

各通信方式の期待結果:

```text
WINDOWS NFS CLIENT TEST PASSED
```

## QNX 確認

QNX 4.25 上で以下を確認します。

- NFSv2/UDP で export をマウントします。
- 通常ファイル、ディレクトリ、symlink 項目を含むディレクトリツリーをコピーします。
- 通常ファイルとディレクトリが正しくコピーされることを確認します。
- symlink 項目が実際の Windows symlink になるか、壊れた代替ファイルを作らずにその項目だけ失敗することを確認します。
- QNX からコピー済みツリーを削除します。
- Windows export にコピー済み内容や `.nfsX*` の残骸が残らないことを確認します。
- `logs/nfs-server.log` に現在の parse-error や予期しない変更操作失敗がないことを確認します。

## README 確認

release tag を作成する前に以下を確認します。

- README が v1.7.0 の対応範囲を記載していること。
- リンク互換性の制限が明示されていること。
- 未対応項目が明示されていること: NFSv3 `MKNOD`、NLM/file locking、NFSv4。
- QNX 4.25 が NFSv2/UDP 検証対象として記載されていること。
- Windows Client for NFS が NFSv3 UDP/TCP 検証対象として記載されていること。
- インストール済み runtime に `runtime\conf\security\java.security` が含まれること。
- インストール済みフォルダから `TinyWinNfsManager.exe` が `Failed to launch JVM` なしで起動すること。
- インストール済み Start Menu/Desktop の管理ツールショートカットが `TinyWinNfsManager.exe` を参照すること。
- インストール済み管理ツール実行ファイルに `RUNASADMIN` 互換レジストリ値が設定されていること。
- 非管理者の直接起動で設定保存した場合、一時ファイルパスではなく管理者権限が必要である旨のメッセージが表示されること。

## リリースメタデータ

- release tag は `v1.7.0` であること。
- release title は `TinyWinNFS Server v1.7.0` であること。
- installer asset は `TinyWinNfsSetup.exe` であること。
- GitHub release note にインストーラー SHA256 を含めること。
