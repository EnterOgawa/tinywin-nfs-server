# ソースコメント確認結果

## 結論

コメントの整理が必要です。`src` の条件・ループのコメントは揃っていますが、Javadoc の共通書式に HTML 構造の誤りがあり、引数・戻り値の説明不足や実装と異なる説明も残っています。

今回は確認と修正提案のみを実施し、製品ソース、テストソース、見本ソースは変更していません。実行時の不具合を検証したものではなく、アプリ起動、NFS サーバー起動、サービス操作、mount、テスト実行は行っていません。

## 対象・方法

| 項目 | 内容 |
|---|---|
| 規約 | `AGENTS.md`、`sample/AplMain.java` |
| 対象 | `src`: 33 ファイル、`test`: 3 ファイル、`sample`: 1 ファイル |
| JDK | 指定の Java 21.0.11+10 |
| Javadoc | `--release 21 -private -Xdoclint:all -Werror -Xmaxerrs 10000 -Xmaxwarns 10000` |
| 構文解析 | JDK の `JavacTask.parse()` で実際の `if` / 通常 `for` / 拡張 `for` を抽出。型解析・製品クラス実行は行わない |
| 前置きコメント | 空行を除いた直前行が行コメントまたはブロックコメントの終端であるかを確認 |
| 説明の照合 | 検査で指摘されたメソッドと、MOUNT のバージョン処理、書込キャッシュ、mode / UID / GID 解決などの代表箇所を目視確認 |
| 出力 | 永続結果はこのフォルダ、一時ツール・Javadoc 出力は `work/tmp/20261011_123150_source_comments` |

コメントの存在確認は全文の構文解析で行っていますが、全コメントの意味の正しさを自動判定したものではありません。規約の空行・タブなどを含む総合的な整形検査は今回の主対象ではありません。

## 指摘事項

### 1. [P2] Javadoc の共通 HTML 書式が不正

- 更新履歴の `<pre>` を `<p>` に入れており、末尾の `</p>` が不正になります。
- boolean の戻り値で `<ul>` の直下に文章や `<br>` を置いています。各項目を `<li>` にする必要があります。
- `sample/AplMain.java` は更新履歴の段落を `<pre>` より前に閉じており、見本自体は厳密な Javadoc 検査に合格しました。
- 例: `src/jp/co/enterogawa/nfs/export/NfsModeStore.java:24` のクラス Javadoc、`src/jp/co/enterogawa/nfs/config/NfsExport.java:105` の `isWritable()`。

`src` の実際の指摘は、更新履歴の段落構造 49 件、`<ul>` 内の文章 71 件、`<ul>` 内の `<br>` 71 件です。このほか引数名の不整合 2 件があり、位置付きエラーは計 193 件です。`-Werror` による全体エラー 1 件を含め、Javadoc の出力は **194 エラー・45 警告** となりました。

### 2. [P2] 引数名と Javadoc の不整合・説明不足

| ファイル・行 | メソッド | 問題 |
|---|---|---|
| `src/jp/co/enterogawa/nfs/export/FileHandleTable.java:499` | `resolveStorePath(List<NfsExport> exports)` | 実際の引数 `exports` ではなく、存在しない `rootPath` を説明している |
| `src/jp/co/enterogawa/nfs/manager/TinyWinNfsSwtManager.java:818` | `addField(Composite parent, String label)` | 存在しない `button` の `@param` が残っている |
| `src/jp/co/enterogawa/nfs/config/NfsServerConfig.java:183` | `loadExports(Properties properties, Path configBasePath)` | `configBasePath` の `@param` がない |
| `src/jp/co/enterogawa/nfs/program/NfsV2Program.java:2449`、`:2480` | `resolveAttributeUid` / `resolveAttributeGid` | 対象 `path` の `@param` がない |
| `src/jp/co/enterogawa/nfs/program/NfsV3Program.java:1841`、`:1872` | `resolveAttributeUid` / `resolveAttributeGid` | 対象 `path` の `@param` がない |
| `src/jp/co/enterogawa/nfs/diagnostic/NfsDiagnostics.java:418` | `ExportReport` コンストラクタ | 10 個の引数の説明がない |
| `src/jp/co/enterogawa/nfs/manager/TinyWinNfsSwtManager.java:1823` | `writeValidatedConfig` | 戻り値のバックアップパス、バックアップがない場合の `null` が説明されていない |
| `src/jp/co/enterogawa/nfs/manager/TinyWinNfsSwtManager.java:471` | `addTab` | 作成した `TabItem` の `@return` がない |
| `src/jp/co/enterogawa/nfs/manager/TinyWinNfsManager.java:592` | `saveConfig` | 保存の成否を返す boolean の `@return` がない |

全件の位置情報は `javadoc-diagnostics.json` を参照してください。45 警告の内訳は、未説明メンバー 22 件、暗黙コンストラクタ 4 件、`@param` 不足 16 件、`@return` 不足 3 件です。

暗黙コンストラクタへの警告は単純な既存コメント修正だけでは解消しません。今後修正する際は、元の公開性を保つ明示コンストラクタの追加などを別途検討し、可視性や挙動を不用意に変えないようにします。

### 3. [P2] 書込キャッシュの異常検出を保証する説明が実装にない

- 対象: `src/jp/co/enterogawa/nfs/io/WriteFileCache.java:396`、`closeQuietly(Entry entry)`。
- コメントは「キャッシュ退避時のクローズ異常は次回I/Oで検出する」としています。
- 実装は `IOException` を受け取るだけで、異常を記録して後続 I/O に伝える処理はありません。期限切れ・容量調整の呼出元では、エントリをキャッシュから除いた後にこのメソッドを呼びます。
- そのため、このコメントのような検出保証はできません。「クローズ異常を呼出元へ通知しない」といった実際の動作を示す説明にする必要があります。
- 同じメソッドの `:387` にある「静的エントリクローズ」も、例外を通知しないクローズを `static` の意味と混同させる名称です。
- この指摘はコメントと実装の不一致です。今回、実環境のディスク異常やファイル破損を再現・確認したわけではありません。

### 4. [P3] MOUNT クラスの対応バージョン説明が古い

- 対象: `src/jp/co/enterogawa/nfs/program/MountV1Program.java:17`、クラス Javadoc。
- 「MOUNT Version 1」とのみ説明していますが、`supportsVersion()` は 1 / 2 / 3 を受け付け、`handleMount()` に v3 の応答分岐もあります。
- クラス名や処理はそのままにし、クラス説明に MOUNT v1-v3 対応を明記するのが適切です。

### 5. [P3] テストソースのコメント不足

| ファイル | `if` / `for` の前置きコメント不足 |
|---|---|
| `test/jp/co/enterogawa/nfs/AllTests.java` | 4 箇所 |
| `test/jp/co/enterogawa/nfs/LocalRpcBenchmark.java` | 26 箇所 |
| `test/jp/co/enterogawa/nfs/ServiceSmokeTest.java` | 7 箇所 |

代表例は `LocalRpcBenchmark.java:117` / `:121` の繰り返し終了判定、`AllTests.java:837` / `:845` の大量ディレクトリエントリ確認です。該当位置は `control-comment-candidates.csv` を参照してください。

テストソースの Javadoc は未説明メンバー 40 件、暗黙コンストラクタ 3 件で **43 警告**。`-Werror` により全体エラー 1 件で終了しました。HTML 構造などの位置付きエラーはありません。

## 揃っていた点

| 対象 | `if` | 通常 `for` | 拡張 `for` | 前置きコメント不足 |
|---|---|---|---|---|
| `src` | 679 | 15 | 60 | 0 |
| `test` | 67 | 17 | 9 | 37 |
| `sample` | 1 | 0 | 0 | 0 |

主要なクラス・メソッドには、見本に沿った区切り線と日本語の名称・説明が付いています。メタデータの UID/GID と既定値の解決、書き込み時の同期指定、許可クライアント判定について、確認した代表箇所の条件コメントに逆転した説明は見つかりませんでした。

## 証跡

- `javadoc-src.log`: 製品ソースの検査出力。
- `javadoc-diagnostics.json`: 製品ソースの位置付きエラー・警告 238 件。
- `javadoc-test.log`: テストソースの検査出力。
- `javadoc-sample.log`: 見本ソースの検査出力。成功時は空。
- `control-comments.tsv`: 構文解析で抽出した 848 個の条件・ループ。
- `control-comment-candidates.csv`: 前置きコメント不足候補 37 個。
- `control-parser.log`: 構文解析診断。今回の解析は成功。
- 修正案: `work/proposals/20261011_123150_source_comment_fixes.md`。

`git diff --name-only -- src` は空で、製品ソースへの変更はありません。以前から存在する未コミット変更は保持しています。commit / push / Release 作成は行っていません。
