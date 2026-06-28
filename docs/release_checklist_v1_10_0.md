# v1.10.0 リリースチェックリスト

v1.10.0 は、Windowsファイルシステム制約を前提に、運用前診断、設定ミス検出、診断パッケージ、ドキュメントを強化するリリースです。

## 範囲

- 大文字小文字衝突チェック。
- exportフォルダ診断。
- 設定ミス検出と警告表示。
- 診断パッケージへの環境・サービス・ポート情報追加。
- Windowsファイルシステム制約とQNX運用注意のドキュメント化。

## ローカル確認

```powershell
git diff --check
.\scripts\compile.ps1
.\scripts\test.ps1
```

期待結果:

```text
TEST PASSED: 16 tests
```

## パッケージ確認

```powershell
.\scripts\package-manager.ps1
.\scripts\package-installer.ps1
```

確認項目:

- `dist\TinyWinNfsManager\TinyWinNfsManager.exe` が作成される。
- `dist\installer\TinyWinNfsSetup.exe` が作成される。
- インストーラーの `ProductVersion` が `1.10.0` である。
- `docs\windows_filesystem_constraints.md` と `docs\release_checklist_v1_10_0.md` が配布物に含まれる。

## 診断パッケージ確認

管理ツールの `サービス` タブで `診断出力` を実行します。

確認項目:

- `C:\ProgramData\EnterOgawa\TinyWinNFS Server\diagnostics` 配下にZIPが作成される。
- `summary.txt` にJava、OS、ユーザー、管理者権限、サービス状態が含まれる。
- `diagnostics/report.txt` に設定診断、export診断、ファイル数、ディレクトリ数、総バイト数が含まれる。
- 大文字小文字衝突がある場合、`EXPORT_CASE_COLLISION` が記録される。
- export配下の共有ファイル本体はZIPに含まれない。

## 設定診断確認

以下の設定を確認します。

- Portmap、NFS、MOUNTポートが重複していない。
- 低番ポート利用時に管理者権限が必要であることが診断に出る。
- `filename.charset` が `UTF-8` 以外の場合に情報が出る。
- 許可クライアント未設定のexportが情報として出る。

## QNX 4.25 確認

実機確認はユーザーから明示指示がある場合に実施します。

- `mount_nfs windows-host:/export /mnt` でマウントできる。
- QNX側からファイル作成、更新、コピー、rename、削除ができる。
- コピー元に大文字小文字のみ異なる名前がある場合、運用上の制約として扱える。
- コピー後の診断パッケージでexport統計を確認できる。

## 署名確認

インストーラー署名はユーザーがリリース前に実施します。
Codex側では署名処理の自動化、署名用bat、証明書、USBトークンに依存する処理を実装・実行しません。

ユーザーから署名完了の明示連絡があった後、可能な範囲で以下を確認します。

```powershell
Get-AuthenticodeSignature .\dist\installer\TinyWinNfsSetup.exe
Get-FileHash .\dist\installer\TinyWinNfsSetup.exe -Algorithm SHA256
```

確認項目:

- `Status` が `Valid` である。
- SHA256をリリースノートへ記載する。

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
git tag -a v1.10.0 -m "TinyWinNFS Server v1.10.0"
git push origin v1.10.0
```

GitHub Release:

- release tag は `v1.10.0`。
- release title は `TinyWinNFS Server v1.10.0`。
- release note は日本語で記載する。
- installer asset は署名済みの `TinyWinNfsSetup.exe`。
- release note にインストーラー SHA256 を含める。
