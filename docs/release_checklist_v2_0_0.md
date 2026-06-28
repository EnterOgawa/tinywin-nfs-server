# v2.0.0 リリースチェックリスト

v2.0.0 は TinyWinNFS Server の正式版です。
NFSv2/NFSv3、MOUNT v1-v3、AUTH_SYS、UDP/TCP、複数 export、read-write 対応を安定版として公開します。

## 範囲

- 正式版サポート範囲と既知制限の確定。
- README と主要 docs の正式版表現への整理。
- 管理ツール、インストーラー、パッケージスクリプトの `2.0.0` 表記への統一。
- 最終回帰テストと、実環境確認結果の記録。
- ユーザー側署名後の署名状態確認。
- GitHub Release `v2.0.0` の公開。

## 対応 issue

| issue | 確認内容 |
|---|---|
| `#86` | 2.0.0正式版の最終回帰テストを実施する |
| `#87` | 正式版サポート範囲と既知制限を確定する |
| `#88` | 2.0.0向けREADMEとdocsを最終更新する |
| `#89` | 正式版インストーラーと署名確認を行う |
| `#90` | GitHub Release v2.0.0を作成する |
| `#91` | 2.0.0 リリースチェックリストを追加する |
| `#92` | 2.0.0 のバージョン番号と配布メタデータを確定する |

## ローカル確認

```powershell
git diff --check
.\scripts\compile.ps1
.\scripts\test.ps1
```

期待結果:

```text
TEST PASSED: 20 tests
```

確認項目:

- `Config validation` で重複 export 名、重複 export パス、ネスト export パスを拒否する。
- `Client access restrictions` が通る。
- `NFS status and attributes` が通る。
- `Cross client edit regression` が通る。
- NFSv2/NFSv3 の通常操作、属性応答、エラー応答の回帰が通る。

## ドキュメント確認

| 文書 | 確認 |
|---|---|
| `README.md` | v2.0.0 正式版の対応範囲、導入、設定、mount、検証、制限事項へ辿れる |
| `docs/support_scope.md` | 正式版の保証範囲、制限付き機能、対象外機能が明記されている |
| `docs/configuration_compatibility.md` | `ProgramData` 配置と設定キー互換性が明記されている |
| `docs/security_model.md` | allowed clients、AUTH_SYS、Windows 権限の限界が明記されている |
| `docs/install_upgrade_uninstall.md` | 新規、上書き、アンインストール、サービス再登録手順がある |
| `docs/nfs_procedure_coverage.md` | v2.0.0 正式版時点の手続き状況になっている |

## パッケージ確認

```powershell
.\scripts\package-manager.ps1
.\scripts\package-installer.ps1
```

確認項目:

- `dist\TinyWinNfsManager\TinyWinNfsManager.exe` が作成される。
- `dist\installer\TinyWinNfsSetup.exe` が作成される。
- 管理ツールの表示バージョンが `2.0.0` である。
- インストーラーの `ProductVersion` が `2.0.0` である。
- 正式版 docs が配布物に含まれる。
- `scripts\test-windows-nfs-client-matrix.ps1` が配布物に含まれる。

## 実環境確認

実環境確認はユーザーから明示指示がある場合に実施します。

サービス:

```powershell
.\scripts\smoke-service.ps1
.\scripts\smoke-service.ps1 -Transport TCP
.\scripts\smoke-service.ps1 -VerifyFileIntegrity
.\scripts\smoke-service.ps1 -VerifyLargeTreeIntegrity
.\scripts\smoke-service.ps1 -RestartHandlePersistence
```

Windows Client for NFS:

```powershell
.\scripts\test-windows-nfs-client-matrix.ps1
```

QNX 4.25:

- `NFSfsys` が起動している。
- `mount_nfs windows-host:/export /mnt` が成功する。
- 作成、更新、rename、delete、ディレクトリコピー、削除後残骸なしを確認する。
- 大量コピーで異常停止やファイル破損がないことを確認する。

## インストール/更新確認

| 確認 | 期待結果 |
|---|---|
| 新規インストール | `TinyWinNfsServer` が登録され、`ProgramData` 側に設定と export が作成される |
| 上書き更新 | 既存設定と export 内容が維持される |
| 旧サービスID | `OgawaNfsServer` / `QnxNfsServer` が残らない |
| アンインストール | サービスとアプリ本体が削除され、利用者データは自動削除されない |

## 署名確認

インストーラー署名はユーザーがリリース前に実施します。
Codex側では署名処理の自動化、署名用bat、証明書、USBトークンに依存する処理を実装・実行しません。

ユーザーから署名完了の明示連絡があった後、可能な範囲で以下を確認します。

```powershell
Get-AuthenticodeSignature .\dist\installer\TinyWinNfsSetup.exe
Get-FileHash .\dist\installer\TinyWinNfsSetup.exe -Algorithm SHA256
```

## 公開ゲート

ユーザー側の動作確認と署名完了の明示連絡があるまで、以下は行いません。

- `git push`
- tag push
- GitHub Release作成
- release assetアップロード

## 公開手順

ユーザーから公開指示があった後に実行します。

```powershell
git push origin main
git tag -a v2.0.0 -m "TinyWinNFS Server v2.0.0"
git push origin v2.0.0
```

GitHub Release:

- release tag は `v2.0.0`。
- release title は `TinyWinNFS Server v2.0.0`。
- release note は日本語で記載する。
- installer asset は署名済みの `TinyWinNfsSetup.exe`。
- release note にインストーラー SHA256 と正式版サポート範囲を含める。
