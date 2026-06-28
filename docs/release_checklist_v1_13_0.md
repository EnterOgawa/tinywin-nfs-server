# v1.13.0 リリースチェックリスト

v1.13.0 は、Windows Client for NFS、QNX 4.25、Linux/WSL の互換性検証を整理し、NFSエラー応答と相互編集の回帰確認を強化するリリースです。

## 範囲

- Windows Client for NFS の UDP/TCP 検証マトリクス追加。
- Windows Client for NFS レポートへの復旧案追加。
- QNX 4.25 の実機確認ケースと制約の切り分けを文書化。
- Linux/WSL の任意回帰マトリクスを文書化。
- NFSv2/NFSv3 の基本エラー応答と属性応答の単体回帰追加。
- 複数クライアント相当の相互編集回帰追加。
- NFSv3 `RMDIR` の通常ファイル誤削除防止。
- README と CHANGELOG の更新。

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

- `NFS status and attributes` が通る。
- `Cross client edit regression` が通る。
- NFSv3 `RMDIR` が通常ファイルに対して `NOTDIR` を返す。
- `scripts\test-windows-nfs-client-matrix.ps1` が存在する。

## Windows Client for NFS 確認

実マウント確認はユーザーから明示指示がある場合に実施します。

単発 UDP:

```powershell
.\scripts\test-windows-nfs-client.ps1 -Transport UDP
```

単発 TCP:

```powershell
.\scripts\test-windows-nfs-client.ps1 -Transport TCP
```

UDP/TCP マトリクス:

```powershell
.\scripts\test-windows-nfs-client-matrix.ps1
```

確認項目:

- `work\analysis\windows-nfs-client-matrix` に総合レポートが作成される。
- UDP と TCP の transport 別レポートが作成される。
- サーバーログで MOUNT v3 と NFSv3 RPC が観測される。
- create、read、update、rename、delete、directory create/delete、日本語ファイル名が成功する。
- 失敗時レポートに復旧案が含まれる。

## QNX 4.25 実機確認

実機確認はユーザー側で実施します。

確認項目:

- `NFSfsys` が起動している。
- `mount_nfs windows-host:/export /mnt` が成功する。
- QNX 側から小さいファイルを作成し、Windows 側で内容を確認できる。
- Windows 側で編集した内容を QNX 側から確認できる。
- rename、delete、mkdir、rmdir が成功する。
- 多数ファイルを含むディレクトリコピー後、Windows 側の件数と合計サイズが一致する。
- 削除後に `.nfsX*` 形式の残骸が残らない。
- symlink を含むコピーでは、Windows 側権限どおりに成功または明示失敗し、通常ファイルへの代替作成がない。

## Linux/WSL 任意回帰

WSL は VMware 環境への影響があるため任意です。実行はユーザーから明示指示がある場合に限定します。

確認対象:

- NFSv2/UDP
- NFSv3/UDP
- NFSv3/TCP

確認項目:

- mount、read、write、rename、delete が成功する。
- ディレクトリ作成と削除が成功する。
- Windows 側 export フォルダで内容と件数を確認できる。

## パッケージ確認

```powershell
.\scripts\package-manager.ps1
.\scripts\package-installer.ps1
```

確認項目:

- `dist\TinyWinNfsManager\TinyWinNfsManager.exe` が作成される。
- `dist\installer\TinyWinNfsSetup.exe` が作成される。
- インストーラーの `ProductVersion` が `1.13.0` である。
- `docs\release_checklist_v1_13_0.md` が配布物に含まれる。
- `scripts\test-windows-nfs-client-matrix.ps1` が配布物に含まれる。

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
git tag -a v1.13.0 -m "TinyWinNFS Server v1.13.0"
git push origin v1.13.0
```

GitHub Release:

- release tag は `v1.13.0`。
- release title は `TinyWinNFS Server v1.13.0`。
- release note は日本語で記載する。
- installer asset は署名済みの `TinyWinNfsSetup.exe`。
- release note にインストーラー SHA256 を含める。
