# NFS UID/GID メタデータ対応提案

## 変更対象

| ファイル | 変更内容 |
|---|---|
| `src/jp/co/enterogawa/nfs/export/NfsModeStore.java` | mode に加えて UID/GID を `modes.properties` へ保存する処理を追加 |
| `src/jp/co/enterogawa/nfs/program/NfsV2Program.java` | `SETATTR uid/gid`、`CREATE`、`MKDIR`、`SYMLINK` の owner/group メタデータ反映を追加 |
| `src/jp/co/enterogawa/nfs/program/NfsV3Program.java` | `SETATTR uid/gid`、`CREATE`、`MKDIR`、`SYMLINK` の owner/group メタデータ反映を追加 |
| `test/jp/co/enterogawa/nfs/AllTests.java` | NFSv2/NFSv3 の `SETATTR uid/gid` と作成時 owner/group の回帰確認を追加 |
| `README.md` / `docs/*.md` / `CHANGELOG.md` | v2.1.0 と NFS mode/UID/GID メタデータの説明へ更新 |

## 変更概要

```diff
+ SETATTR uid/gid を NFS メタデータとして保存する。
+ CREATE / MKDIR 時に UID/GID 指定がない場合、要求元 AUTH_SYS の UID/GID を初期 owner/group として保存する。
+ GETATTR / READDIRPLUS などの属性応答で、保存済み UID/GID を優先して返す。
+ REMOVE / RMDIR / RENAME に合わせて UID/GID メタデータも削除または移動する。
+ Windows ACL や Windows ファイル所有者は変更せず、NFS クライアント向け属性応答として扱う。
```

## 確認観点

- NFSv2 `SETATTR uid/gid` 後の属性応答で UID/GID が維持される。
- NFSv3 `SETATTR uid/gid` 後の `GETATTR` で UID/GID が維持される。
- NFSv2/NFSv3 の `CREATE` / `MKDIR` で、AUTH_SYS の UID/GID が初期 owner/group として返る。
- 削除や rename に合わせて mode/UID/GID メタデータが不整合を起こさない。
- `permission.identity` は保存済み UID/GID がない場合の既定応答として維持される。
