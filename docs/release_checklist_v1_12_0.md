# v1.12.0 リリースチェックリスト

v1.12.0 は、大量コピー/削除の性能確認と負荷調整をしやすくするリリースです。

## 範囲

- ローカルRPCベンチマークの追加。
- 長時間負荷ループ用スクリプトの追加。
- UDP RPC ワーカー数、UDP キューサイズ、TCP タイムアウトの設定化。
- NFSv2 大量 READDIR の回帰テスト。
- 書込キャッシュ flush 方針の回帰テスト。
- README と性能確認ドキュメントの更新。

## ローカル確認

```powershell
git diff --check
.\scripts\compile.ps1
.\scripts\test.ps1
```

期待結果:

```text
TEST PASSED: 18 tests
```

## 性能/負荷確認

短時間ベンチマーク:

```powershell
.\scripts\benchmark-local-rpc.ps1 -Files 1000 -Directories 10 -Depth 1
```

長時間負荷ループ:

```powershell
.\scripts\test-long-running.ps1 -DurationMinutes 30 -Files 1000 -Directories 10 -Depth 1
```

確認項目:

- `work\analysis\v1.12.0-benchmark` または指定した出力先に Markdown/CSV が保存される。
- 作成と書込、LOOKUP、READDIR、RENAME、削除が例外なく完了する。
- 長時間負荷ループ中にメモリ使用量が単調に増え続けない。
- `write.sync=false` / `write.sync=true` の差分を必要に応じて比較できる。
- `write.cache.enabled=true` / `false` の差分を必要に応じて比較できる。

## 設定確認

`conf\nfs-server.properties` に以下が含まれることを確認します。

```properties
rpc.udp.workers=8
rpc.udp.queue.size=1024
rpc.tcp.timeout.millis=30000
```

確認項目:

- 無効な `rpc.udp.workers=0` は設定検証で拒否される。
- 無効な `rpc.tcp.timeout.millis=0` は設定検証で拒否される。
- 既存設定ファイルに新項目がなくても既定値で起動できる。

## パッケージ確認

```powershell
.\scripts\package-manager.ps1
.\scripts\package-installer.ps1
```

確認項目:

- `dist\TinyWinNfsManager\TinyWinNfsManager.exe` が作成される。
- `dist\installer\TinyWinNfsSetup.exe` が作成される。
- インストーラーの `ProductVersion` が `1.12.0` である。
- `docs\performance_testing.md` が配布物に含まれる。
- `docs\release_checklist_v1_12_0.md` が配布物に含まれる。

リポジトリ確認項目:

- `scripts\benchmark-local-rpc.ps1` が存在する。
- `scripts\test-long-running.ps1` が存在する。

## 実マウント確認

実機確認はユーザーから明示指示がある場合に実施します。

- QNX 4.25 の `mount_nfs windows-host:/export /mnt` で既存運用が壊れていない。
- Windows Client for NFS の NFSv3/UDP と NFSv3/TCP で既存テストが通る。

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
git tag -a v1.12.0 -m "TinyWinNFS Server v1.12.0"
git push origin v1.12.0
```

GitHub Release:

- release tag は `v1.12.0`。
- release title は `TinyWinNFS Server v1.12.0`。
- release note は日本語で記載する。
- installer asset は署名済みの `TinyWinNfsSetup.exe`。
- release note にインストーラー SHA256 を含める。
