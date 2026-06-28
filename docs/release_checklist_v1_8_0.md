# v1.8.0 リリースチェックリスト

v1.8.0 は、検証安定化と運用品質の改善リリースです。

## 範囲

- Windows Client for NFS 検証スクリプトの前提条件チェックを強化します。
- QNX 4.25 実機回帰確認の手順と記録形式を整備します。
- リリース前にユーザー動作確認を挟む運用を明文化します。
- 管理ツールのサービス診断表示を強化します。
- 設定保存と旧設定移行時のバックアップを追加します。
- ファイル整合性と大量操作の確認観点を拡充します。

## ローカル確認

実装後、まずローカルで以下を確認します。

```powershell
git diff --check
.\scripts\compile.ps1
.\scripts\test.ps1
.\scripts\package-manager.ps1
.\scripts\package-installer.ps1
```

単体テストの期待結果:

```text
TEST PASSED: 15 tests
```

## サービス確認

ユーザーからサービス確認の明示指示がある場合のみ実行します。

```powershell
.\scripts\smoke-service.ps1
.\scripts\smoke-service.ps1 -Transport TCP
.\scripts\smoke-service.ps1 -VerifyFileIntegrity
.\scripts\smoke-service.ps1 -VerifyLargeTreeIntegrity
.\scripts\smoke-service.ps1 -RestartHandlePersistence
```

期待結果:

```text
SERVICE SMOKE TEST PASSED
PASS: service file integrity
PASS: service large tree integrity
PASS: service handle persistence after restart
```

## Windows Client for NFS 確認

Windows Client for NFS が利用可能な状態で、UDP/TCP を独立して確認します。

```powershell
Get-Service NfsClnt
nfsadmin client
.\scripts\test-windows-nfs-client.ps1
.\scripts\test-windows-nfs-client.ps1 -Transport TCP
```

Windows 側で protocol 設定を事前に済ませている場合:

```powershell
.\scripts\test-windows-nfs-client.ps1 -Transport TCP -SkipProtocolChange
```

`NfsClnt` が停止状態から起動維持できない場合は、Windows を再起動してから確認します。
検証スクリプトから `NfsClnt` や `NfsRdr` を停止/再起動しません。

## QNX 4.25 実機確認

`docs/qnx425_mount.md` の「実機回帰確認」に従い、以下を記録します。

```text
QNX 4.25 実機確認:
- mount:
- create/update/read:
- rename/delete:
- directory copy:
- large copy/delete:
- symlink:
- Windows側件数/サイズ:
- 残課題:
```

## ユーザー動作確認ゲート

実装者は、以下の段階で必ず停止します。

1. ローカル実装を完了する。
2. ローカル確認結果を issue コメントまたは作業報告にまとめる。
3. インストーラーまたは配布物をユーザーが動作確認できる状態にする。
4. ユーザーの動作確認完了と明示指示を待つ。

ユーザーから明示指示があるまでは、以下を行いません。

- `git push`
- tag push
- GitHub Release 作成
- milestone close
- リリース完了としての issue close

## 公開手順

ユーザーから release 指示があった後に実行します。

```powershell
git push origin main
git tag -a v1.8.0 -m "TinyWinNFS Server v1.8.0"
git push origin v1.8.0
```

GitHub Release:

- release tag は `v1.8.0`。
- release title は `TinyWinNFS Server v1.8.0`。
- release note は日本語で記載する。
- installer asset は `TinyWinNfsSetup.exe`。
- release note にインストーラー SHA256 を含める。
