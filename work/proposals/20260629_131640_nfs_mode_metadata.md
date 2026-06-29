# NFS modeメタデータ対応 提案書

## 変更対象

| ファイル | 変更内容 |
|---|---|
| `src/jp/co/enterogawa/nfs/export/NfsModeStore.java` | NFS mode を export 相対パス単位で保持する永続ストアを追加 |
| `src/jp/co/enterogawa/nfs/program/NfsV2Program.java` | `SETATTR mode` の保存、属性応答への反映、削除/rename時の追従を追加 |
| `src/jp/co/enterogawa/nfs/program/NfsV3Program.java` | `SETATTR mode` の保存、属性応答への反映、削除/rename時の追従を追加 |
| `src/jp/co/enterogawa/nfs/server/NfsServer.java` | v2/v3 で同じ `NfsModeStore` を共有 |
| `test/jp/co/enterogawa/nfs/AllTests.java` | NFSv2/NFSv3 の `SETATTR mode` 回帰確認を追加 |
| `scripts/install-service.ps1` | ProgramData 配下の `data` フォルダ作成を追加 |
| `installer/tinywin-nfs-server.iss` | インストーラーで `data` フォルダを作成 |
| `README.md` / `docs/*.md` / `CHANGELOG.md` | mode メタデータと v2.1.0 未リリース内容を文書化 |

## 変更内容

### `NfsV2Program.java` / `NfsV3Program.java`

```diff
+ modeStore.setMode( path, mode) ;
+ permissionMode = modeStore.getMode( path, permissionMode) ;
+ modeStore.remove( path) ;
+ modeStore.move( source.getPath(), target.getPath()) ;
```

### `NfsModeStore.java`

```diff
+ modes.properties に `export名/相対パス = mode` 相当の情報を保存する。
+ `chmod` / `SETATTR mode` 後の `GETATTR` で変更後 mode を返す。
```

## 期待効果

- QNX 側から `chmod` した結果が、QNX の一覧表示や後続 `GETATTR` に反映される。
- Windows ACL の完全変換は行わず、NFS クライアント向け属性として保持する。
- 削除や rename に合わせて mode メタデータも削除または移動される。

## 確認方針

- `.\scripts\compile.ps1`
- `.\scripts\test.ps1`
- 必要に応じて QNX 実機で `chmod 755 file` 後の一覧表示を確認する。
