# v1.3.0 operational hardening

## 変更対象

- `src/jp/co/enterogawa/nfs/config/NfsExport.java`
- `src/jp/co/enterogawa/nfs/config/NfsServerConfig.java`
- `src/jp/co/enterogawa/nfs/export/FileHandleTable.java`
- `src/jp/co/enterogawa/nfs/rpc/RpcProgram.java`
- `src/jp/co/enterogawa/nfs/rpc/RpcRequestContext.java`
- `src/jp/co/enterogawa/nfs/rpc/UdpRpcServer.java`
- `src/jp/co/enterogawa/nfs/program/MountV1Program.java`
- `src/jp/co/enterogawa/nfs/program/NfsV2Program.java`
- `src/jp/co/enterogawa/nfs/server/NfsServer.java`
- `src/jp/co/enterogawa/nfs/manager/TinyWinNfsSwtManager.java`
- `src/jp/co/enterogawa/nfs/manager/messages.properties`
- `src/jp/co/enterogawa/nfs/manager/messages_ja.properties`
- `test/jp/co/enterogawa/nfs/AllTests.java`
- `conf/*.properties`
- `docs/*.md`

## 内容

v1.3.0 は operational hardening として以下を実装する。

- export 設定の保存前/起動前検証強化
- 共有ごとの IPv4 client allow-list
- MOUNT/NFS 要求の接続元制限
- Save + Restart の結果表示改善
- サービス実行ファイル/設定ファイルの表示
- RPC 診断ログの強化
- 成功 READ の通常ログ抑制

## 代表差分

```diff
+ exports.1.allowed.clients=192.168.1.30,127.0.0.1
```

```diff
+ RPC client=<ip>:<port> server=<name> xid=<xid> program=<program> version=<version> procedure=<procedure> accept=<accept> status=<status>
```

```diff
+ NFS WRITE client=<ip> xid=<xid> program=100003 version=2 procedure=8 status=<status> path=<path>
```

## 検証

- `.\scripts\compile.ps1`
- `.\scripts\test.ps1`
- `TEST PASSED: 11 tests`
