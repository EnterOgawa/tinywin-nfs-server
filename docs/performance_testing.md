# 性能/負荷確認

v1.12.0 では、実機クライアントに依存しないローカルRPCベンチマークを追加しています。

この確認は Windows サービスを起動せず、ネットワークポートも使用しません。NFSv2 の RPC 手続きをプロセス内で呼び出し、作成、書込、LOOKUP、READDIR、rename、削除の時間を測定します。

## ローカルRPCベンチマーク

```powershell
.\scripts\benchmark-local-rpc.ps1 -Files 1000 -Directories 10 -Depth 1
```

主なパラメータ:

| パラメータ | 既定値 | 説明 |
|---|---:|---|
| `-Files` | `1000` | 1ループで作成するファイル数 |
| `-Directories` | `10` | 1ループで作成するディレクトリ系列数 |
| `-Depth` | `1` | ディレクトリ階層の深さ |
| `-MinSize` | `1024` | ファイル最小サイズ |
| `-MaxSize` | `8192` | ファイル最大サイズ |
| `-Loops` | `1` | 繰り返し回数 |
| `-WriteSync` | `false` | `write.sync=true` 相当で測定 |
| `-WriteCacheEnabled` | `true` | 書込キャッシュ有効/無効 |
| `-WriteCacheMaxOpen` | `64` | 書込キャッシュ最大オープン数 |
| `-WriteCacheIdleMillis` | `3000` | 書込キャッシュのアイドル保持時間 |
| `-UdpWorkers` | `8` | 設定ファイルへ出力する `rpc.udp.workers` |
| `-UdpQueueSize` | `1024` | 設定ファイルへ出力する `rpc.udp.queue.size` |
| `-Out` | `work\analysis\v1.12.0-benchmark` | 結果出力先 |

出力:

| ファイル | 内容 |
|---|---|
| `local-rpc-benchmark-*.md` | 人が確認しやすいMarkdownレポート |
| `local-rpc-benchmark-*.csv` | 比較・集計用CSV |

## 長時間負荷ループ

長時間の create/write/lookup/readdir/rename/delete ループは以下で実行します。

```powershell
.\scripts\test-long-running.ps1 -DurationMinutes 30 -Files 1000 -Directories 10 -Depth 1
```

通常の単体テストには含めません。リリース前や性能設定を変更した場合に、必要な時間だけ実行してください。

## 設定調整の見方

| 設定 | 方針 |
|---|---|
| `write.sync=false` | 既定値です。コピー性能を優先します。 |
| `write.sync=true` | 各WRITEで物理同期を待つため遅くなります。ストレージ同期を優先する運用でのみ使用します。 |
| `write.cache.enabled=true` | 既定値です。大量コピー時の open/close 負荷を下げます。 |
| `write.cache.max.open` | 同時に多数ファイルへ書く場合は増やす候補です。ただしOSのファイルハンドル消費も増えます。 |
| `write.cache.idle.millis` | 短すぎると再openが増えます。長すぎると開いたファイルが残りやすくなります。 |
| `rpc.udp.workers` | UDP要求を並列処理する数です。CPU数、ストレージ速度、クライアントの要求量に合わせて調整します。 |
| `rpc.udp.queue.size` | 瞬間的なUDP要求増加を受ける待ち行列です。大きくしすぎると遅延に気づきにくくなります。 |
| `rpc.tcp.timeout.millis` | TCPクライアントが応答しなくなった時の読み取りタイムアウトです。 |

## 確認基準

最低限、以下を確認します。

- `.\scripts\test.ps1` が成功する。
- ローカルRPCベンチマークが例外なく完了する。
- Markdown/CSVの結果が `work\analysis` 配下に保存される。
- `削除` の件数が作成ファイル数とディレクトリ数に見合っている。
- 同じ条件で設定値だけを変えた場合、作成/書込、READDIR、削除の差分を比較できる。

## 実機確認との関係

このベンチマークは、NFS手続き実装とファイルI/Oの負荷を確認するためのものです。

QNX 4.25、Windows Client for NFS、Linux/WSL の実マウント確認は別扱いです。ユーザー側の動作確認指示がある場合のみ実行します。
