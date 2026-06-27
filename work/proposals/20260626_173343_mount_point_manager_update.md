# マウントポイント設定の管理ツール対応

## 変更概要
- 管理ツールに `Mount` タブを追加。
- QNX向け `mount_nfs` コマンドを管理ツール上で生成できるようにした。
- サーバーホスト、サーバー公開名、QNX側マウントポイントを設定可能にした。
- 生成した mount コマンドをクリップボードへコピーできるようにした。
- `conf/nfs-server.properties` に `client.server.host` と `client.mount.point` を追加。
- WSLテスト用設定にも `client.server.host` と `client.mount.point` を追加。
- `docs/qnx425_mount.md` と `docs/manager_app.md` を更新。

## 設定項目
- `export.name`: NFSサーバー側の公開名。例: `/export`
- `export.path`: Windows側の公開フォルダ。例: `export`
- `client.server.host`: QNXから見えるWindowsホスト名またはIP。
- `client.mount.point`: QNX側のローカルマウント先。例: `/mnt`

## 生成されるコマンド例
```text
mount_nfs windows-host:/export /mnt
```

## 確認結果
- `scripts/compile.ps1` 成功。
- `scripts/test.ps1` 成功。
- `scripts/package-manager.ps1` 成功。
- 起動中サービスに対する `scripts/smoke-service.ps1` 成功。
- 配布版 `dist/QnxNfsServerManager` に更新後の設定ファイルが含まれることを確認。
