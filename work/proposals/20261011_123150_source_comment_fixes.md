# ソースコメント修正案

## 方針

- コメント、Javadoc のみを修正し、製品の実行ロジックは変更しない。
- 行番号は現在のソースを基準とする。
- 以下は変更箇所のみの抜粋。今回の確認では製品ソースへ適用していない。
- 暗黙コンストラクタに対する Javadoc 警告は、コメント変更と分けて対応方針を判断する。
- 詳細結果: `work/analysis/20261011_123150_source_comments/report.md`。

## 更新履歴の段落構造

対象: `src` 内のクラス Javadoc 49 箇所。代表例は `src/jp/co/enterogawa/nfs/export/NfsModeStore.java:24`。メソッド: なし（クラス説明）。

見本と同様に段落を先に閉じ、`<pre>` を独立させる。

```diff
- * <p>更新履歴：
+ * <p>更新履歴：</p>
- * </p>
```

## boolean 戻り値のリスト構造

対象: `src` 内の boolean 戻り値 Javadoc 71 箇所。代表例は `src/jp/co/enterogawa/nfs/config/NfsExport.java:105`、`isWritable()`。

```diff
-	 * @return	判定結果<ul>
-	 * 			false	: 読込専用<br>
-	 * 			true	: 書込可</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 読込専用</li>
+	 * <li>true: 書込可</li>
+	 * </ul>
```

## 引数コメントの不整合

対象: `src/jp/co/enterogawa/nfs/export/FileHandleTable.java:499`、`resolveStorePath(List<NfsExport> exports)`。

```diff
-	 * @param rootPath	公開ルート
+	 * @param exports	公開定義一覧
```

対象: `src/jp/co/enterogawa/nfs/manager/TinyWinNfsSwtManager.java:818`、`addField(Composite parent, String label)`。

```diff
-	 * @param button	ボタン
```

## 引数・戻り値の説明補完

対象: `src/jp/co/enterogawa/nfs/program/NfsV2Program.java:2446`、`resolveAttributeUid(Path path)`。GID と NFSv3 の同名処理にも同じ説明を補完する。

```diff
+	 * @param path	属性を取得する対象パス
```

対象: `src/jp/co/enterogawa/nfs/manager/TinyWinNfsSwtManager.java:1820`、`writeValidatedConfig(Path configDirectory, List<String> lines)`。

```diff
+	 * @return	既存設定のバックアップパス。バックアップがない場合は null
```

同様に `NfsServerConfig.java:183` の `loadExports` の `configBasePath`、`NfsDiagnostics.java:418` の `ExportReport` コンストラクタの各引数、`TinyWinNfsSwtManager.java:471` の `addTab` と `TinyWinNfsManager.java:592` の `saveConfig` の戻り値説明を補完する。

## クローズ異常の説明

対象: `src/jp/co/enterogawa/nfs/io/WriteFileCache.java:387` / `:396`、`closeQuietly(Entry entry)`。

```diff
-	 * <p>メソッド名称： 静的エントリクローズ</p>
+	 * <p>メソッド名称： クローズ例外を通知しないエントリクローズ</p>
-			// キャッシュ退避時のクローズ異常は次回I/Oで検出する
+			// キャッシュ退避時のクローズ異常は呼出元へ通知しない
```

後続 I/O に異常を伝える機能を追加する場合は、別の機能変更として設計・テストする。コメントの修正と混同しない。

## MOUNT の対応範囲

対象: `src/jp/co/enterogawa/nfs/program/MountV1Program.java:17` / `:19`、クラス Javadoc。クラス名とプロトコル処理は変更しない。

```diff
- * MOUNT Version 1プログラムクラスです。<br><br>
+ * MOUNT Version 1-3プログラムクラスです。<br><br>
- * <p>クラス名称： MOUNT Version 1プログラム</p>
+ * <p>クラス名称： MOUNT Version 1-3プログラム</p>
```

## テストソースの条件コメント

対象: `test/jp/co/enterogawa/nfs/LocalRpcBenchmark.java:117` / `:121`、`run()`。

```diff
+				// 指定した繰り返し回数に達した場合
+				// 長時間測定の終了時刻に達した場合
```

上の 2 行は、それぞれ対応する `if` の直前に置く。ほかの 35 箇所も、分岐条件またはループの検証意図が分かる説明を加える。該当一覧は `control-comment-candidates.csv` を参照する。

## 修正後の確認

- コメント・空白を除いた Java トークンが変更前と一致することを確認する。
- 製品・テスト・見本の Javadoc を同じ厳密設定で再検査する。
- 条件・ループの前置きコメントを構文解析で再確認する。
- 暗黙コンストラクタを明示化する場合は、元の可視性・例外・生成処理を維持し、コメントのみの変更とは別に確認する。
- アプリ・サービス・mount の実行、push・公開はこのコメント修正だけを理由に行わない。
