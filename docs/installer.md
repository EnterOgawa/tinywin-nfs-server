# Inno Setup インストーラー

## ビルド

```powershell
.\scripts\package-installer.ps1
```

出力先:

```text
dist\installer\TinyWinNfsSetup.exe
```

## インストーラーの動作

インストーラーには、管理ツール、Java runtime、WinSW サービスラッパー、スクリプト、既定設定、ドキュメントを含めます。

インストール時の処理:

- デスクトップショートカットを作成します。
- ファイルを配置する前に既存の `TinyWinNfsServer` Windows サービスを停止および削除し、その後で再インストールします。
- `TinyWinNfsServer` をインストールする前に、旧 `OgawaNfsServer` または `QnxNfsServer` サービスを停止および削除します。
- UDP/TCP `111`、`2049`、`20048` に対する Windows ファイアウォールルールを追加します。
- サービスタスクが選択されている場合、インストール後に `TinyWinNfsServer` を起動します。
- インストール済みショートカットがコマンドプロンプトを表示せず UAC 経由で起動できるように、`TinyWinNfsManager.exe` を Windows の `RUNASADMIN` 互換設定へ登録します。
- 可変設定、既定 export、TinyWinNFS サーバーログを `C:\ProgramData\EnterOgawa\TinyWinNFS Server` 配下に保存します。
- `ProgramData` 側の設定がまだ存在しない場合、既存の `C:\Program Files\EnterOgawa\TinyWinNFS Server\conf\nfs-server.properties` を `ProgramData` へ移行します。

サービス登録とファイアウォール変更には権限が必要なため、インストーラーは管理者権限を要求します。

## アップグレード確認

リリース前に、直前の公開インストーラーからのアップグレード動作を確認します。

1. 直前のリリースをインストールします。
2. 少なくとも 1 つの書込可能 export を設定します。
3. `TinyWinNfsServer` が実行中であることを確認します。
4. 以下を実行します。

```powershell
.\scripts\smoke-service.ps1 -VerifyFileIntegrity
```

5. 既存インストールの上から新しいパッケージをインストールします。
6. サービスが停止、置換、再起動されたことを確認します。
7. `C:\ProgramData\EnterOgawa\TinyWinNFS Server\conf\nfs-server.properties` に設定済み export が残っていることを確認します。
8. 以下を実行します。

```powershell
.\scripts\smoke-service.ps1
.\scripts\smoke-service.ps1 -Transport TCP
.\scripts\smoke-service.ps1 -VerifyFileIntegrity
```

アップグレード後に、古い `TinyWinNfsServer`、`OgawaNfsServer`、`QnxNfsServer` サービスが残っていてはいけません。

## アイコン

アプリケーションとセットアップのアイコンは以下から生成します。

```text
assets\tinywin-nfs-server.ico
```

アイコンを再生成する場合は以下を実行します。

```powershell
.\scripts\generate-icon.ps1
```
