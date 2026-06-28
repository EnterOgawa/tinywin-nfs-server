# v1.14.0 リリースチェックリスト

v1.14.0 は、2.0.0 正式版に向けた仕様完成度、設定互換性、セキュリティ、インストール/更新/アンインストール、ドキュメント構成を最終整理するリリースです。

## 範囲

- 2.0.0 正式版で保証するサポート範囲と対象外機能の整理。
- 設定互換性と `ProgramData` データ配置の固定化。
- allowed clients、AUTH_SYS、Windows 権限、export 境界の整理。
- インストール、上書き更新、アンインストール、サービス再登録手順の整理。
- README と主要 docs の正式版候補構成への更新。
- export 重複、重複パス、ネストパス拒否の単体回帰追加。

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

## ドキュメント確認

| 文書 | 確認 |
|---|---|
| `README.md` | サポート範囲、インストール、設定、mount、検証、制限事項へ辿れる |
| `docs/support_scope.md` | 保証範囲、制限付き機能、対象外機能が明記されている |
| `docs/configuration_compatibility.md` | `ProgramData` 配置と設定キー互換性が明記されている |
| `docs/security_model.md` | allowed clients、AUTH_SYS、Windows 権限の限界が明記されている |
| `docs/install_upgrade_uninstall.md` | 新規、上書き、アンインストール、サービス再登録手順がある |
| `docs/nfs_procedure_coverage.md` | v1.14.0 正式版候補時点の手続き状況になっている |

## パッケージ確認

```powershell
.\scripts\package-manager.ps1
.\scripts\package-installer.ps1
```

確認項目:

- `dist\TinyWinNfsManager\TinyWinNfsManager.exe` が作成される。
- `dist\installer\TinyWinNfsSetup.exe` が作成される。
- インストーラーの `ProductVersion` が `1.14.0` である。
- 新規 docs が配布物に含まれる。
- `scripts\test-windows-nfs-client-matrix.ps1` が配布物に含まれる。

## 実環境確認

実環境確認はユーザーから明示指示がある場合に実施します。

サービス:

```powershell
.\scripts\smoke-service.ps1
.\scripts\smoke-service.ps1 -Transport TCP
.\scripts\smoke-service.ps1 -VerifyFileIntegrity
.\scripts\smoke-service.ps1 -VerifyLargeTreeIntegrity
```

Windows Client for NFS:

```powershell
.\scripts\test-windows-nfs-client-matrix.ps1
```

QNX 4.25:

- `NFSfsys` が起動している。
- `mount_nfs windows-host:/export /mnt` が成功する。
- 作成、更新、rename、delete、ディレクトリコピー、削除後残骸なしを確認する。

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
git tag -a v1.14.0 -m "TinyWinNFS Server v1.14.0"
git push origin v1.14.0
```

GitHub Release:

- release tag は `v1.14.0`。
- release title は `TinyWinNFS Server v1.14.0`。
- release note は日本語で記載する。
- installer asset は署名済みの `TinyWinNfsSetup.exe`。
- release note にインストーラー SHA256 を含める。
