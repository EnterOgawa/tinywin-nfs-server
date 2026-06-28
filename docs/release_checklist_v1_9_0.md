# v1.9.0 リリースチェックリスト

v1.9.0 は、互換性確認の自動化、診断出力、NFS手続き網羅性の整理を中心にした運用品質改善リリースです。

## 範囲

- Windows Client for NFS 結合テストのMarkdownレポート出力。
- 管理ツールからの診断パッケージ出力。
- NFSv2/NFSv3/MOUNT 手続きカバレッジ表の整備。
- NFSv3 `FSINFO` / `FSSTAT` / `PATHCONF` 応答精度の改善。
- 書込、rename、削除、大量ツリー操作の回帰確認強化。

## ローカル確認

```powershell
git diff --check
.\scripts\compile.ps1
.\scripts\test.ps1
```

期待結果:

```text
TEST PASSED: 15 tests
```

PowerShell構文確認:

```powershell
$null = [scriptblock]::Create((Get-Content -Raw .\scripts\test-windows-nfs-client.ps1))
$null = [scriptblock]::Create((Get-Content -Raw .\scripts\smoke-service.ps1))
```

## パッケージ確認

```powershell
.\scripts\package-manager.ps1
.\scripts\package-installer.ps1
```

確認項目:

- `dist\TinyWinNfsManager\TinyWinNfsManager.exe` が作成される。
- `dist\installer\TinyWinNfsSetup.exe` が作成される。
- インストーラーの `ProductVersion` が `1.9.0` である。
- `docs\nfs_procedure_coverage.md` と `docs\release_checklist_v1_9_0.md` が配布物に含まれる。

## 署名確認

リリース用インストーラーは署名済みであることを必須とします。

```powershell
Get-AuthenticodeSignature .\dist\installer\TinyWinNfsSetup.exe
```

確認項目:

- `Status` が `Valid` である。
- `SignerCertificate` が想定したコード署名証明書である。
- 署名後の `TinyWinNfsSetup.exe` を最終配布物とする。
- 未署名または署名検証失敗の場合、tag push、GitHub Release 作成、release asset アップロードは行わない。

v1.9.0 リリース確認結果:

- `Status`: `Valid`
- `SignerCertificate`: `ENTER SYSTEM, K.K.`
- SHA256: `2695EA74C440D0B4626554EA14CFEBF1EBD5C2D1F85D39E73E5F9E2E77CC41F0`

## サービス確認

サービス起動を伴うため、ユーザーから明示指示がある場合のみ実施します。

```powershell
.\scripts\smoke-service.ps1
.\scripts\smoke-service.ps1 -Transport TCP
.\scripts\smoke-service.ps1 -VerifyFileIntegrity
.\scripts\smoke-service.ps1 -VerifyLargeTreeIntegrity
```

`-VerifyLargeTreeIntegrity` では、複数階層、28ファイル、rename、削除、残骸確認が成功することを確認します。

## Windows Client for NFS 確認

Windows Client for NFS が有効な環境で実施します。

```powershell
.\scripts\test-windows-nfs-client.ps1
.\scripts\test-windows-nfs-client.ps1 -Transport TCP
.\scripts\test-windows-nfs-client.ps1 -Transport TCP -SkipProtocolChange
```

確認項目:

- テスト成功時に `WINDOWS NFS CLIENT TEST PASSED` が出る。
- `work\analysis\windows-nfs-client` 配下にMarkdownレポートが出力される。
- レポートに `NfsClnt` 状態、`nfsadmin client`、protocol、mount対象、操作結果が記録される。
- 失敗時にも failure と原因候補がレポートに残る。

## 管理ツール確認

インストール済み管理ツール、または `dist\TinyWinNfsManager\TinyWinNfsManager.exe` で確認します。

- サービス タブに `診断出力` が表示される。
- `診断出力` 実行後、`C:\ProgramData\EnterOgawa\TinyWinNFS Server\diagnostics` 配下にZIPが作成される。
- ZIPに `summary.txt`、設定ファイル、設定バックアップ、TinyWinNFSログ、WinSWログが含まれる。
- export 配下の共有ファイル本体が含まれない。

## QNX 4.25 実機確認

QNX 4.25 は重要な互換性確認対象です。

- `mount_nfs windows-host:/export /mnt` でマウントできる。
- QNX側からファイル作成、更新、コピー、rename、削除ができる。
- 大量ディレクトリコピー後にサーバー側ファイル数と内容が崩れない。
- 削除後に `.nfsX*` 形式の残骸ディレクトリが残らない。

## ユーザー動作確認ゲート

ユーザー側で以下を確認するまで、`git push`、tag push、GitHub Release 作成などの公開操作は行いません。

- インストール済み環境で管理ツールが起動する。
- 設定保存とサービス再起動ができる。
- QNX 4.25 または Windows Client for NFS で基本操作が成功する。
- 診断パッケージを作成できる。

## 公開手順

ユーザーから release 指示があった後に実行します。

```powershell
git push origin main
git tag -a v1.9.0 -m "TinyWinNFS Server v1.9.0"
git push origin v1.9.0
```

GitHub Release:

- release tag は `v1.9.0`。
- release title は `TinyWinNFS Server v1.9.0`。
- release note は日本語で記載する。
- installer asset は署名済みの `TinyWinNfsSetup.exe`。
- release note にインストーラー SHA256 を含める。
