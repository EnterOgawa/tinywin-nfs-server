# インストール・更新・アンインストール確認

この文書は、TinyWinNFS Server v2.0.0 正式版で確認するインストール、上書き更新、アンインストール、サービス再登録の手順を整理します。

## 前提

- インストーラーは管理者権限を要求します。
- サービス ID は `TinyWinNfsServer` です。
- 旧サービス ID `OgawaNfsServer` / `QnxNfsServer` は移行用として停止・削除対象にします。
- インストーラー署名はユーザーがリリース前に実施します。
- Codex 側では署名処理の自動化や署名用トークン依存処理を行いません。

## 新規インストール確認

| 手順 | 期待結果 |
|---|---|
| インストーラーを起動 | UAC が表示される |
| サービスインストールを選択 | `TinyWinNfsServer` が登録される |
| ファイアウォールルール追加を選択 | UDP/TCP `111`、`2049`、`20048` が許可される |
| 管理ツール起動 | `TinyWinNfsManager.exe` が GUI として起動する |
| 設定ファイル確認 | `ProgramData` 側に `conf\nfs-server.properties` が作成される |
| 既定 export 確認 | `ProgramData` 側に `export` が作成される |

## 上書き更新確認

| 手順 | 期待結果 |
|---|---|
| 旧版をインストール | サービスと `ProgramData` が作成される |
| 共有設定を変更 | `conf\nfs-server.properties` に反映される |
| export に利用者ファイルを置く | 上書き更新後も残る |
| 新版を上書きインストール | 既存サービスが停止、削除、再登録される |
| サービス起動確認 | `TinyWinNfsServer` が実行中になる |
| 設定確認 | 変更済み export が維持される |
| 既定 export 確認 | 利用者ファイルが削除されていない |

## アンインストール確認

| 手順 | 期待結果 |
|---|---|
| アンインストール実行 | `TinyWinNfsServer` が停止、削除される |
| アプリ本体確認 | `Program Files` 側のアプリ本体が削除される |
| `ProgramData` 確認 | 利用者データは自動削除しない |
| 互換レジストリ確認 | `RUNASADMIN` 互換設定が削除される |

`ProgramData` 配下は利用者データを含むため、アンインストーラーで自動削除しません。
完全削除が必要な場合のみ、利用者が内容を確認してから手動削除します。

## サービス再登録確認

管理者 PowerShell で以下を実行します。

```powershell
.\scripts\stop-service.ps1
.\scripts\uninstall-service.ps1
.\scripts\install-service.ps1
.\scripts\start-service.ps1
.\scripts\status-service.ps1
```

確認項目:

- `TinyWinNfsServer` が再登録される。
- 旧サービス ID が残らない。
- WinSW 設定が `ProgramData` 側の設定ファイルを参照している。
- サービスログが `service\winsw` に出力される。

## 動作確認

サービス起動を伴うため、以下はユーザーから明示指示がある場合のみ Codex 側で実行します。

```powershell
.\scripts\smoke-service.ps1
.\scripts\smoke-service.ps1 -Transport TCP
.\scripts\smoke-service.ps1 -VerifyFileIntegrity
.\scripts\smoke-service.ps1 -VerifyLargeTreeIntegrity
```

実マウント確認:

- QNX 4.25: `mount_nfs windows-host:/export /mnt`
- Windows Client for NFS: `.\scripts\test-windows-nfs-client-matrix.ps1`
- Linux/WSL: 任意回帰として `docs\wsl_mount_test.md` の手順
