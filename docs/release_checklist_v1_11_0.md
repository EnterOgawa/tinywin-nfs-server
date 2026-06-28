# v1.11.0 リリースチェックリスト

v1.11.0 は、管理ツールの診断、ログ確認、サービス操作、設定移行、マウント支援を改善するリリースです。

## 範囲

- 管理ツールの診断タブ。
- サーバーログ検索、種別フィルタ、自動更新。
- サービス操作結果の表示改善と多重実行抑止。
- 設定インポート、エクスポート、既定値初期化。
- QNX、Windows Client for NFS、Linux/WSL 向け mount 表示。

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
- インストーラーの `ProductVersion` が `1.11.0` である。
- `docs\release_checklist_v1_11_0.md` が配布物に含まれる。

## 管理ツール確認

管理ツールを起動し、以下を確認します。

- `診断` タブでサービス状態、ポート状態、設定診断、export診断が表示される。
- 重大度フィルタで `情報`、`警告`、`エラー` を絞り込める。
- `診断` タブから設定ファイル、ログ、診断パッケージへ移動できる。
- `ログ` タブでサーバーログの検索、エラー、変更操作、RPC/MOUNT フィルタが機能する。
- `ログ` タブの自動更新をON/OFFできる。
- `サービス` タブの操作結果に exitCode、stdout、stderr、失敗理由の目安が表示される。
- サービス操作中に操作ボタンが多重実行できない。

## 設定移行確認

- `設定エクスポート` で現在のUI入力内容を `.properties` として保存できる。
- `設定インポート` で正しい設定ファイルを取り込める。
- 設定インポート前に既存設定のバックアップが作成される。
- 不正な設定ファイルは反映されない。
- `既定に戻す` で既定設定へ戻せる。

## マウント支援確認

- QNX を選択すると `mount_nfs <host>:<export> <mount-point>` が表示される。
- Windows Client for NFS を選択すると `mount -o anon \\<host>\<export> Z:` 形式が表示される。
- Linux/WSL を選択すると `sudo mount -t nfs -o vers=...,proto=... <host>:<export> <mount-point>` が表示される。
- 複数exportの選択を変えると、選択中exportのコマンドへ更新される。
- コマンドコピーは先頭行のみをコピーする。

## QNX 4.25 確認

実機確認はユーザーから明示指示がある場合に実施します。

- 既存の `mount_nfs windows-host:/export /mnt` が引き続き表示される。
- 共有フォルダ変更後、保存して再起動すればQNX側でmountできる。

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
git tag -a v1.11.0 -m "TinyWinNFS Server v1.11.0"
git push origin v1.11.0
```

GitHub Release:

- release tag は `v1.11.0`。
- release title は `TinyWinNFS Server v1.11.0`。
- release note は日本語で記載する。
- installer asset は署名済みの `TinyWinNfsSetup.exe`。
- release note にインストーラー SHA256 を含める。
