# NFSv2 互換性改善

## 変更概要
- NFSv2 の `READLINK` を追加し、未対応 procedure による失敗を避けるようにした。
- `LOOKUP` の名前検証を追加し、空文字、パス区切り、公開ルート外参照を拒否するようにした。
- `.` と `..` は明示的に許可し、`..` は公開ルートを越えないように丸める。
- NFSv2 処理中の `IOException` は `NFSERR_IO`、不正引数は `NFSERR_INVAL` として応答するようにした。
- RPC 処理中の例外時に無応答でタイムアウトさせず、可能な限り `GARBAGE_ARGS` 応答を返すようにした。
- WinSW 定義の Java 実行ファイルを指定 JDK の絶対パスに変更した。

## 変更対象
- `src/jp/co/enterogawa/nfs/program/NfsV2Program.java`
- `src/jp/co/enterogawa/nfs/rpc/RpcCall.java`
- `src/jp/co/enterogawa/nfs/rpc/UdpRpcServer.java`
- `service/winsw/nfs-server.xml`
- `README.md`

## 確認結果
- 指定 JDK `C:\develop\tools\eclipse202503\eclipse\jdk-21.0.8+9` の `javac.exe` でコンパイル成功。
- NFSサーバー起動、サービス起動、QNX 4.25 からの mount は未実施。
