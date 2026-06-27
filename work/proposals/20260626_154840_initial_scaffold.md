# 初期プロジェクト構成作成

## 変更概要
- Java 21 の通常 Eclipse Java プロジェクトとして `.project`、`.classpath`、`.settings/org.eclipse.jdt.core.prefs` を追加。
- QNX 4.25 向け NFSv2 read-only MVP の初期実装を `src/jp/co/enterogawa/nfs` 配下に追加。
- `conf/nfs-server.properties` に Portmap / MOUNT / NFS / export の初期設定を追加。
- `service/winsw/nfs-server.xml` に WinSW 用サービス定義例を追加。
- `export/README.txt` にデフォルト公開ディレクトリを追加。

## 主要変更ファイル
- `.project`
- `.classpath`
- `.settings/org.eclipse.jdt.core.prefs`
- `README.md`
- `conf/nfs-server.properties`
- `service/winsw/nfs-server.xml`
- `src/jp/co/enterogawa/nfs/NfsServerMain.java`
- `src/jp/co/enterogawa/nfs/config/NfsServerConfig.java`
- `src/jp/co/enterogawa/nfs/server/NfsServer.java`
- `src/jp/co/enterogawa/nfs/rpc/*.java`
- `src/jp/co/enterogawa/nfs/xdr/*.java`
- `src/jp/co/enterogawa/nfs/export/*.java`
- `src/jp/co/enterogawa/nfs/program/*.java`

## 実装済み機能
- UDP Portmap v2
  - `NULL`
  - `GETPORT`
  - `DUMP`
- UDP MOUNT v1
  - `NULL`
  - `MNT`
  - `DUMP`
  - `UMNT`
  - `UMNTALL`
  - `EXPORT`
- UDP NFSv2
  - `NULL`
  - `ROOT`
  - `GETATTR`
  - `LOOKUP`
  - `READ`
  - `READDIR`
  - `STATFS`
- WRITE 系処理は初期スコープ通り `NFSERR_ROFS` を返す。

## 未確認事項
- `javac` が PATH に存在しないため、コンパイル確認は未実施。
- NFSサーバー起動、QNX 4.25 からの mount、Wireshark 確認は未実施。
- Windows Firewall、管理者権限、UDP 111 / 2049 / 20048 の利用可否は未確認。
