# Multiple Export Update

## 変更対象

- `src/jp/co/enterogawa/nfs/config/NfsExport.java`
- `src/jp/co/enterogawa/nfs/config/NfsServerConfig.java`
- `src/jp/co/enterogawa/nfs/export/FileHandleTable.java`
- `src/jp/co/enterogawa/nfs/program/MountV1Program.java`
- `src/jp/co/enterogawa/nfs/program/NfsV2Program.java`
- `src/jp/co/enterogawa/nfs/server/NfsServer.java`
- `src/jp/co/enterogawa/nfs/manager/TinyWinNfsSwtManager.java`
- `test/jp/co/enterogawa/nfs/AllTests.java`
- `conf/nfs-server.properties`
- `conf/nfs-server-wsl-test.properties`
- `README.md`
- `docs/manager_app.md`
- `docs/qnx425_mount.md`

## 内容

- 複数のサーバー側共有フォルダを `exports.count` / `exports.N.name` / `exports.N.path` / `exports.N.writable` で設定できるようにした。
- 従来の `export.name` / `export.path` / `export.writable` は先頭共有として引き続き読み込めるようにした。
- MOUNT v1/v2 で指定されたexport名に対応するルートファイルハンドルを返すようにした。
- NFSv2の書込可否を、グローバル設定ではなく共有ごとのWritableで判定するようにした。
- SWTマネージャーのShareタブを共有一覧形式に変更し、Add/Apply/Removeで複数共有を管理できるようにした。
- 複数共有のMOUNTと共有ごとのread-only判定を単体テストに追加した。

## 確認

- `.\scripts\test.ps1`
- `.\scripts\package-manager.ps1`
- `.\scripts\package-installer.ps1`
- `dist\TinyWinNfsManager\TinyWinNfsManager.exe` 起動確認
