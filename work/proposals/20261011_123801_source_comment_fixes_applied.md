# ソースコメント修正の実施記録

## 方針
- 2026/10/11 のコメント確認で指摘した項目を修正した。
- 修正対象は `src` 33 ファイルと `test` 3 ファイル。見本の `sample/AplMain.java` と既存の他の未コミット変更は保持した。
- 共通Javadocの更新履歴 49 箇所、boolean戻り値 71 箇所、引数・戻り値・内部メンバーの説明不足を修正した。
- テストの条件・ループ前置きコメント 37 箇所を補完し、クローズ異常と MOUNT の対応範囲の説明を実装に合わせた。
- 暗黙コンストラクタ 7 箇所は元の可視性を保った空コンストラクタとして明示した。それ以外の実行コードは変更していない。
- 修正前の基準は今回の作業開始時のスナップショット。既存の変更を含むため、`HEAD` との差分とは区別する。
- 変更対象のメソッド・メンバーと修正前後の行番号を、Javaの構文解析から以下に記録した。
- diff欄は変更行のみを記載している。異なる変更箇所の間の未変更行は省略する。

## 確認結果
- 指定JDKで37ソースのコンパイルに成功。
- 37ソースの `javadoc --release 21 -private -Xdoclint:all -Werror` に成功。エラー・警告ともに0件。
- 条件・ループ848箇所の前置きコメント不足は0件。
- 同じJDK・同じ依存jar・`javac -g:none` で修正前後をコンパイルし、77クラスすべてのSHA-256が一致。実行コード・クラス構造はバイト単位で同一。
- 構文上のメンバー増加は暗黙コンストラクタの明示化7件のみ。既存メンバーの削除はない。
- `git diff --check -- src test` に問題なし。
- アプリ、NFSサーバー、サービス、mount、単体テストは起動・実行していない。
- commit / push / Release作成は行っていない。

詳細な検証記録: `work/analysis/20261011_123801_comment_fixes/verification.md`。

## ファイル別差分

### `src/jp/co/enterogawa/nfs/NfsServerMain.java`

| 対象メソッド・メンバー | 種別 | 修正前行 | 修正後行 |
|---|---|---|---|
| `NfsServerMain` | クラスJavadoc（メソッドなし） | 18 | 18 |
| `NfsServerMain` | クラスJavadoc（メソッドなし） | 23 | 22 |
| `NfsServerMain.NfsServerMain()` | メソッド / コンストラクタ | 29 | 29 |

```diff
- * <p>更新履歴：
+ * <p>更新履歴：</p>
- * </p>
+	//--------------------------------------------------------------------------
+	/**
+	 * インスタンスを生成します。<br><br>
+	 *
+	 * <p>メソッド名称： コンストラクタ</p>
+	 */
+	//--------------------------------------------------------------------------
+	public NfsServerMain() {
+	}
+
```


### `src/jp/co/enterogawa/nfs/config/ConfigBackup.java`

| 対象メソッド・メンバー | 種別 | 修正前行 | 修正後行 |
|---|---|---|---|
| `ConfigBackup` | クラスJavadoc（メソッドなし） | 21 | 21 |
| `ConfigBackup` | クラスJavadoc（メソッドなし） | 26 | 25 |

```diff
- * <p>更新履歴：
+ * <p>更新履歴：</p>
- * </p>
```


### `src/jp/co/enterogawa/nfs/config/NfsExport.java`

| 対象メソッド・メンバー | 種別 | 修正前行 | 修正後行 |
|---|---|---|---|
| `NfsExport` | クラスJavadoc（メソッドなし） | 14 | 14 |
| `NfsExport` | クラスJavadoc（メソッドなし） | 19 | 18 |
| `NfsExport.isWritable()` | メソッド / コンストラクタ | 105 | 104 |
| `NfsExport.allowsClient(String)` | メソッド / コンストラクタ | 134 | 135 |

```diff
- * <p>更新履歴：
+ * <p>更新履歴：</p>
- * </p>
-	 * @return	判定結果<ul>
-	 * 			false	: 読込専用<br>
-	 * 			true	: 書込可</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 読込専用</li>
+	 * <li>true: 書込可</li>
+	 * </ul>
-	 * @return	判定結果<ul>
-	 * 			false	: 拒否<br>
-	 * 			true	: 許可</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 拒否</li>
+	 * <li>true: 許可</li>
+	 * </ul>
```


### `src/jp/co/enterogawa/nfs/config/NfsServerConfig.java`

| 対象メソッド・メンバー | 種別 | 修正前行 | 修正後行 |
|---|---|---|---|
| `NfsServerConfig` | クラスJavadoc（メソッドなし） | 23 | 23 |
| `NfsServerConfig` | クラスJavadoc（メソッドなし） | 28 | 27 |
| `NfsServerConfig.loadExports(Properties, Path)` | メソッド / コンストラクタ | 179 | 179 |
| `NfsServerConfig.containsWhitespace(String)` | メソッド / コンストラクタ | 599 | 599 |
| `NfsServerConfig.hasParentSegment(String)` | メソッド / コンストラクタ | 623 | 625 |
| `NfsServerConfig.isExactIpv4Address(String)` | メソッド / コンストラクタ | 649 | 653 |
| `NfsServerConfig.isExportWritable()` | メソッド / コンストラクタ | 846 | 852 |
| `NfsServerConfig.isClientIdentityEnabled()` | メソッド / コンストラクタ | 887 | 895 |
| `NfsServerConfig.isWriteSync()` | メソッド / コンストラクタ | 1032 | 1042 |
| `NfsServerConfig.isWriteCacheEnabled()` | メソッド / コンストラクタ | 1047 | 1059 |

```diff
- * <p>更新履歴：
+ * <p>更新履歴：</p>
- * </p>
+	 * @param configBasePath	共有の相対パスを解決する基準パス
-	 * @return	判定結果<ul>
-	 * 			false	: なし<br>
-	 * 			true	: あり</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: なし</li>
+	 * <li>true: あり</li>
+	 * </ul>
-	 * @return	判定結果<ul>
-	 * 			false	: なし<br>
-	 * 			true	: あり</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: なし</li>
+	 * <li>true: あり</li>
+	 * </ul>
-	 * @return	判定結果<ul>
-	 * 			false	: IPv4以外<br>
-	 * 			true	: IPv4</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: IPv4以外</li>
+	 * <li>true: IPv4</li>
+	 * </ul>
-	 * @return	判定結果<ul>
-	 * 			false	: 読込専用<br>
-	 * 			true	: 書込可</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 読込専用</li>
+	 * <li>true: 書込可</li>
+	 * </ul>
-	 * @return	判定結果<ul>
-	 * 			false	: 固定<br>
-	 * 			true	: 反映</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 固定</li>
+	 * <li>true: 反映</li>
+	 * </ul>
-	 * @return	判定結果<ul>
-	 * 			false	: 非同期書込<br>
-	 * 			true	: 同期書込</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 非同期書込</li>
+	 * <li>true: 同期書込</li>
+	 * </ul>
-	 * @return	判定結果<ul>
-	 * 			false	: 無効<br>
-	 * 			true	: 有効</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 無効</li>
+	 * <li>true: 有効</li>
+	 * </ul>
```


### `src/jp/co/enterogawa/nfs/config/TinyWinNfsPaths.java`

| 対象メソッド・メンバー | 種別 | 修正前行 | 修正後行 |
|---|---|---|---|
| `TinyWinNfsPaths` | クラスJavadoc（メソッドなし） | 13 | 13 |
| `TinyWinNfsPaths` | クラスJavadoc（メソッドなし） | 18 | 17 |

```diff
- * <p>更新履歴：
+ * <p>更新履歴：</p>
- * </p>
```


### `src/jp/co/enterogawa/nfs/diagnostic/NfsDiagnostics.java`

| 対象メソッド・メンバー | 種別 | 修正前行 | 修正後行 |
|---|---|---|---|
| `NfsDiagnostics` | クラスJavadoc（メソッドなし） | 26 | 26 |
| `NfsDiagnostics` | クラスJavadoc（メソッドなし） | 31 | 30 |
| `ExportReport.ExportReport(NfsExport, boolean, boolean, boolean, boolean, long, long, long, List<CaseCollision>, List<DiagnosticMessage>)` | メソッド / コンストラクタ | 415 | 415 |
| `ExportReport.exists()` | メソッド / コンストラクタ | 450 | 460 |
| `ExportReport.isDirectory()` | メソッド / コンストラクタ | 465 | 477 |
| `ExportReport.isReadable()` | メソッド / コンストラクタ | 480 | 494 |
| `ExportReport.isWritable()` | メソッド / コンストラクタ | 495 | 511 |

```diff
- * <p>更新履歴：
+ * <p>更新履歴：</p>
- * </p>
+		 *
+		 * @param export	診断対象の公開定義
+		 * @param exists	共有フォルダの存在有無
+		 * @param directory	ディレクトリであるか
+		 * @param readable	読み込み可否
+		 * @param writable	書き込み可否
+		 * @param fileCount	通常ファイル数
+		 * @param directoryCount	ディレクトリ数
+		 * @param totalBytes	通常ファイルの合計サイズ
+		 * @param caseCollisions	大文字小文字が衝突する名前の一覧
+		 * @param messages	診断メッセージ一覧
-		 * @return	判定結果<ul>
-		 * 			false	: 不存在<br>
-		 * 			true	: 存在</ul>
+		 * @return	判定結果
+		 * <ul>
+		 * <li>false: 不存在</li>
+		 * <li>true: 存在</li>
+		 * </ul>
-		 * @return	判定結果<ul>
-		 * 			false	: ディレクトリ以外<br>
-		 * 			true	: ディレクトリ</ul>
+		 * @return	判定結果
+		 * <ul>
+		 * <li>false: ディレクトリ以外</li>
+		 * <li>true: ディレクトリ</li>
+		 * </ul>
-		 * @return	判定結果<ul>
-		 * 			false	: 読込不可<br>
-		 * 			true	: 読込可</ul>
+		 * @return	判定結果
+		 * <ul>
+		 * <li>false: 読込不可</li>
+		 * <li>true: 読込可</li>
+		 * </ul>
-		 * @return	判定結果<ul>
-		 * 			false	: 書込不可<br>
-		 * 			true	: 書込可</ul>
+		 * @return	判定結果
+		 * <ul>
+		 * <li>false: 書込不可</li>
+		 * <li>true: 書込可</li>
+		 * </ul>
```


### `src/jp/co/enterogawa/nfs/export/FileHandle.java`

| 対象メソッド・メンバー | 種別 | 修正前行 | 修正後行 |
|---|---|---|---|
| `FileHandle` | クラスJavadoc（メソッドなし） | 14 | 14 |
| `FileHandle` | クラスJavadoc（メソッドなし） | 19 | 18 |
| `FileHandle.equals(Object)` | メソッド / コンストラクタ | 99 | 98 |

```diff
- * <p>更新履歴：
+ * <p>更新履歴：</p>
- * </p>
-	 * @return	判定結果<ul>
-	 * 			false	: 不一致<br>
-	 * 			true	: 等価</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 不一致</li>
+	 * <li>true: 等価</li>
+	 * </ul>
```


### `src/jp/co/enterogawa/nfs/export/FileHandleTable.java`

| 対象メソッド・メンバー | 種別 | 修正前行 | 修正後行 |
|---|---|---|---|
| `FileHandleTable` | クラスJavadoc（メソッドなし） | 30 | 30 |
| `FileHandleTable` | クラスJavadoc（メソッドなし） | 35 | 34 |
| `FileHandleTable.isInExportRoot(Path)` | メソッド / コンストラクタ | 210 | 209 |
| `FileHandleTable.isSameExport(Path, Path)` | メソッド / コンストラクタ | 227 | 228 |
| `FileHandleTable.isWritable(Path)` | メソッド / コンストラクタ | 245 | 248 |
| `FileHandleTable.resolveStorePath(List<NfsExport>)` | メソッド / コンストラクタ | 499 | 504 |

```diff
- * <p>更新履歴：
+ * <p>更新履歴：</p>
- * </p>
-	 * @return	判定結果<ul>
-	 * 			false	: 公開ルート外<br>
-	 * 			true	: 公開ルート内</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 公開ルート外</li>
+	 * <li>true: 公開ルート内</li>
+	 * </ul>
-	 * @return	判定結果<ul>
-	 * 			false	: 異なる公開ルート<br>
-	 * 			true	: 同一公開ルート</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 異なる公開ルート</li>
+	 * <li>true: 同一公開ルート</li>
+	 * </ul>
-	 * @return	判定結果<ul>
-	 * 			false	: 読込専用<br>
-	 * 			true	: 書込可</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 読込専用</li>
+	 * <li>true: 書込可</li>
+	 * </ul>
-	 * @param rootPath	公開ルート
+	 * @param exports	公開定義一覧
```


### `src/jp/co/enterogawa/nfs/export/NfsModeStore.java`

| 対象メソッド・メンバー | 種別 | 修正前行 | 修正後行 |
|---|---|---|---|
| `NfsModeStore` | クラスJavadoc（メソッドなし） | 23 | 23 |
| `NfsModeStore` | クラスJavadoc（メソッドなし） | 28 | 27 |
| `NfsModeStore.removeTreeValues(Map<String, Integer>, String)` | メソッド / コンストラクタ | 507 | 506 |
| `NfsModeStore.moveValues(Map<String, Integer>, String, String)` | メソッド / コンストラクタ | 525 | 526 |

```diff
- * <p>更新履歴：
+ * <p>更新履歴：</p>
- * </p>
-	 * @return	判定結果<ul>
-	 * 			false	: 削除なし<br>
-	 * 			true	: 削除あり</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 削除なし</li>
+	 * <li>true: 削除あり</li>
+	 * </ul>
-	 * @return	判定結果<ul>
-	 * 			false	: 移動なし<br>
-	 * 			true	: 移動あり</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 移動なし</li>
+	 * <li>true: 移動あり</li>
+	 * </ul>
```


### `src/jp/co/enterogawa/nfs/io/WriteFileCache.java`

| 対象メソッド・メンバー | 種別 | 修正前行 | 修正後行 |
|---|---|---|---|
| `WriteFileCache` | クラスJavadoc（メソッドなし） | 22 | 22 |
| `WriteFileCache` | クラスJavadoc（メソッドなし） | 27 | 26 |
| `WriteFileCache.closeQuietly(Entry)` | メソッド / コンストラクタ | 387 | 386 |
| `WriteFileCache.closeQuietly(Entry)` | メソッド / コンストラクタ | 396 | 395 |

```diff
- * <p>更新履歴：
+ * <p>更新履歴：</p>
- * </p>
-	 * <p>メソッド名称： 静的エントリクローズ</p>
+	 * <p>メソッド名称： クローズ例外を通知しないエントリクローズ</p>
-			// キャッシュ退避時のクローズ異常は次回I/Oで検出する
+			// キャッシュ退避時のクローズ異常は呼出元へ通知しない
```


### `src/jp/co/enterogawa/nfs/manager/ManagerMessages.java`

| 対象メソッド・メンバー | 種別 | 修正前行 | 修正後行 |
|---|---|---|---|
| `ManagerMessages` | クラスJavadoc（メソッドなし） | 16 | 16 |
| `ManagerMessages` | クラスJavadoc（メソッドなし） | 21 | 20 |

```diff
- * <p>更新履歴：
+ * <p>更新履歴：</p>
- * </p>
```


### `src/jp/co/enterogawa/nfs/manager/TinyWinNfsManager.java`

| 対象メソッド・メンバー | 種別 | 修正前行 | 修正後行 |
|---|---|---|---|
| `TinyWinNfsManager` | クラスJavadoc（メソッドなし） | 59 | 59 |
| `TinyWinNfsManager` | クラスJavadoc（メソッドなし） | 64 | 63 |
| `TinyWinNfsManager.saveConfig()` | メソッド / コンストラクタ | 589 | 589 |
| `TinyWinNfsManager.isAdministrator()` | メソッド / コンストラクタ | 1266 | 1271 |
| `ProcessResult` | クラスJavadoc（メソッドなし） | 1507 | 1514 |
| `ProcessResult` | クラスJavadoc（メソッドなし） | 1512 | 1518 |
| `ResultCallback` | クラスJavadoc（メソッドなし） | 1576 | 1582 |
| `ResultCallback` | クラスJavadoc（メソッドなし） | 1581 | 1586 |

```diff
- * <p>更新履歴：
+ * <p>更新履歴：</p>
- * </p>
+	 *
+	 * @return	保存結果
+	 * <ul>
+	 * <li>false: 保存失敗</li>
+	 * <li>true: 保存成功</li>
+	 * </ul>
-	 * @return	判定結果<ul>
-	 * 			false	: 一般ユーザー<br>
-	 * 			true	: 管理者</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 一般ユーザー</li>
+	 * <li>true: 管理者</li>
+	 * </ul>
-	 * <p>更新履歴：
+	 * <p>更新履歴：</p>
-	 * </p>
-	 * <p>更新履歴：
+	 * <p>更新履歴：</p>
-	 * </p>
```


### `src/jp/co/enterogawa/nfs/manager/TinyWinNfsSwtManager.java`

| 対象メソッド・メンバー | 種別 | 修正前行 | 修正後行 |
|---|---|---|---|
| `TinyWinNfsSwtManager` | クラスJavadoc（メソッドなし） | 75 | 75 |
| `TinyWinNfsSwtManager` | クラスJavadoc（メソッドなし） | 80 | 79 |
| `TinyWinNfsSwtManager.addTab(TabFolder, String, Composite)` | メソッド / コンストラクタ | 468 | 468 |
| `TinyWinNfsSwtManager.addField(Composite, String)` | メソッド / コンストラクタ | 818 | 817 |
| `TinyWinNfsSwtManager.isValidExportName(String)` | メソッド / コンストラクタ | 1475 | 1474 |
| `TinyWinNfsSwtManager.isExactIpv4Address(String)` | メソッド / コンストラクタ | 1552 | 1553 |
| `TinyWinNfsSwtManager.saveConfig()` | メソッド / コンストラクタ | 1688 | 1691 |
| `TinyWinNfsSwtManager.writeValidatedConfig(Path, List<String>)` | メソッド / コンストラクタ | 1819 | 1825 |
| `TinyWinNfsSwtManager.shouldRequireAdministratorForConfigSave(Path)` | メソッド / コンストラクタ | 2006 | 2012 |
| `TinyWinNfsSwtManager.isProtectedInstallPath(Path)` | メソッド / コンストラクタ | 2027 | 2035 |
| `TinyWinNfsSwtManager.isUnderEnvironmentPath(Path, String)` | メソッド / コンストラクタ | 2047 | 2057 |
| `TinyWinNfsSwtManager.isValidClientMountPoint(String)` | メソッド / コンストラクタ | 2164 | 2176 |
| `TinyWinNfsSwtManager.isValidWindowsMountPoint(String)` | メソッド / コンストラクタ | 2524 | 2538 |
| `TinyWinNfsSwtManager.matchesDiagnosticSeverity(DiagnosticRow)` | メソッド / コンストラクタ | 2840 | 2856 |
| `TinyWinNfsSwtManager.hasAnyExtension(Path, String[])` | メソッド / コンストラクタ | 3139 | 3157 |
| `TinyWinNfsSwtManager.matchesLogFilter(String, int)` | メソッド / コンストラクタ | 3487 | 3507 |
| `TinyWinNfsSwtManager.containsAny(String, String[])` | メソッド / コンストラクタ | 3522 | 3544 |
| `TinyWinNfsSwtManager.isAdministrator()` | メソッド / コンストラクタ | 4104 | 4128 |
| `DiagnosticText` | クラスJavadoc（メソッドなし） | 4397 | 4423 |
| `DiagnosticText` | クラスJavadoc（メソッドなし） | 4402 | 4427 |
| `ProcessResult` | クラスJavadoc（メソッドなし） | 4466 | 4491 |
| `ProcessResult` | クラスJavadoc（メソッドなし） | 4471 | 4495 |
| `DiagnosticRow` | クラスJavadoc（メソッドなし） | 4576 | 4600 |
| `DiagnosticRow` | クラスJavadoc（メソッドなし） | 4581 | 4604 |
| `ShareEntry` | クラスJavadoc（メソッドなし） | 4681 | 4704 |
| `ShareEntry` | クラスJavadoc（メソッドなし） | 4686 | 4708 |
| `ShareEntry.isWritable()` | メソッド / コンストラクタ | 4757 | 4779 |
| `ResultCallback` | クラスJavadoc（メソッドなし） | 4788 | 4812 |
| `ResultCallback` | クラスJavadoc（メソッドなし） | 4793 | 4816 |
| `SwtSelectionListener` | クラスJavadoc（メソッドなし） | 4821 | 4844 |
| `SwtSelectionListener` | クラスJavadoc（メソッドなし） | 4826 | 4848 |

```diff
- * <p>更新履歴：
+ * <p>更新履歴：</p>
- * </p>
+	 * @return	作成したタブ
-	 * @param button	ボタン
-	 * @return	判定結果<ul>
-	 * 			false	: 異常<br>
-	 * 			true	: 正常</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 異常</li>
+	 * <li>true: 正常</li>
+	 * </ul>
-	 * @return	判定結果<ul>
-	 * 			false	: IPv4以外<br>
-	 * 			true	: IPv4</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: IPv4以外</li>
+	 * <li>true: IPv4</li>
+	 * </ul>
-	 * @return	判定結果<ul>
-	 * 			false	: 保存失敗<br>
-	 * 			true	: 保存成功</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 保存失敗</li>
+	 * <li>true: 保存成功</li>
+	 * </ul>
+	 * @return	既存設定のバックアップパス。バックアップがない場合は null
-	 * @return	判定結果<ul>
-	 * 			false	: 不要<br>
-	 * 			true	: 管理者権限が必要</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 不要</li>
+	 * <li>true: 管理者権限が必要</li>
+	 * </ul>
-	 * @return	判定結果<ul>
-	 * 			false	: 非保護パス<br>
-	 * 			true	: 保護パス</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 非保護パス</li>
+	 * <li>true: 保護パス</li>
+	 * </ul>
-	 * @return	判定結果<ul>
-	 * 			false	: 配下ではない<br>
-	 * 			true	: 配下</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 配下ではない</li>
+	 * <li>true: 配下</li>
+	 * </ul>
-	 * @return	判定結果<ul>
-	 * 			false	: 異常<br>
-	 * 			true	: 正常</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 異常</li>
+	 * <li>true: 正常</li>
+	 * </ul>
-	 * @return	判定結果<ul>
-	 * 			false	: その他<br>
-	 * 			true	: Windows mount point</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: その他</li>
+	 * <li>true: Windows mount point</li>
+	 * </ul>
-	 * @return	判定結果<ul>
-	 * 			false	: 不一致<br>
-	 * 			true	: 一致</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 不一致</li>
+	 * <li>true: 一致</li>
+	 * </ul>
-	 * @return	判定結果<ul>
-	 * 			false	: 不一致<br>
-	 * 			true	: 一致</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 不一致</li>
+	 * <li>true: 一致</li>
+	 * </ul>
-	 * @return	判定結果<ul>
-	 * 			false	: 不一致<br>
-	 * 			true	: 一致</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 不一致</li>
+	 * <li>true: 一致</li>
+	 * </ul>
-	 * @return	判定結果<ul>
-	 * 			false	: 含まない<br>
-	 * 			true	: 含む</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 含まない</li>
+	 * <li>true: 含む</li>
+	 * </ul>
-	 * @return	判定結果<ul>
-	 * 			false	: 一般ユーザー<br>
-	 * 			true	: 管理者</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 一般ユーザー</li>
+	 * <li>true: 管理者</li>
+	 * </ul>
-	 * <p>更新履歴：
+	 * <p>更新履歴：</p>
-	 * </p>
-	 * <p>更新履歴：
+	 * <p>更新履歴：</p>
-	 * </p>
-	 * <p>更新履歴：
+	 * <p>更新履歴：</p>
-	 * </p>
-	 * <p>更新履歴：
+	 * <p>更新履歴：</p>
-	 * </p>
-		 * @return	判定結果<ul>
-		 * 			false	: 読込専用<br>
-		 * 			true	: 書込可</ul>
+		 * @return	判定結果
+		 * <ul>
+		 * <li>false: 読込専用</li>
+		 * <li>true: 書込可</li>
+		 * </ul>
-	 * <p>更新履歴：
+	 * <p>更新履歴：</p>
-	 * </p>
-	 * <p>更新履歴：
+	 * <p>更新履歴：</p>
-	 * </p>
```


### `src/jp/co/enterogawa/nfs/program/MountV1Program.java`

| 対象メソッド・メンバー | 種別 | 修正前行 | 修正後行 |
|---|---|---|---|
| `MountV1Program` | クラスJavadoc（メソッドなし） | 17 | 17 |
| `MountV1Program` | クラスJavadoc（メソッドなし） | 19 | 19 |
| `MountV1Program` | クラスJavadoc（メソッドなし） | 23 | 23 |
| `MountV1Program` | クラスJavadoc（メソッドなし） | 28 | 27 |
| `MountV1Program.supportsVersion(int)` | メソッド / コンストラクタ | 120 | 119 |

```diff
- * MOUNT Version 1プログラムクラスです。<br><br>
+ * MOUNT Version 1-3プログラムクラスです。<br><br>
- * <p>クラス名称： MOUNT Version 1プログラム</p>
+ * <p>クラス名称： MOUNT Version 1-3プログラム</p>
- * <p>更新履歴：
+ * <p>更新履歴：</p>
- * </p>
-	 * @return	判定結果<ul>
-	 * 			false	: 未対応<br>
-	 * 			true	: 対応</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 未対応</li>
+	 * <li>true: 対応</li>
+	 * </ul>
```


### `src/jp/co/enterogawa/nfs/program/NfsStatus.java`

| 対象メソッド・メンバー | 種別 | 修正前行 | 修正後行 |
|---|---|---|---|
| `NfsStatus` | クラスJavadoc（メソッドなし） | 11 | 11 |
| `NfsStatus` | クラスJavadoc（メソッドなし） | 16 | 15 |

```diff
- * <p>更新履歴：
+ * <p>更新履歴：</p>
- * </p>
```


### `src/jp/co/enterogawa/nfs/program/NfsV2Program.java`

| 対象メソッド・メンバー | 種別 | 修正前行 | 修正後行 |
|---|---|---|---|
| `NfsV2Program` | クラスJavadoc（メソッドなし） | 56 | 56 |
| `NfsV2Program` | クラスJavadoc（メソッドなし） | 61 | 60 |
| `NfsV2Program.isValidWriteCount(long)` | メソッド / コンストラクタ | 905 | 904 |
| `NfsV2Program.isValidLookupName(String)` | メソッド / コンストラクタ | 1594 | 1595 |
| `NfsV2Program.hasInvalidWindowsNameCharacter(String)` | メソッド / コンストラクタ | 1640 | 1643 |
| `NfsV2Program.isInExportRoot(Path)` | メソッド / コンストラクタ | 1666 | 1671 |
| `NfsV2Program.isInSameExportRoot(Path, Path)` | メソッド / コンストラクタ | 1683 | 1690 |
| `NfsV2Program.isWritable(Path)` | メソッド / コンストラクタ | 1699 | 1708 |
| `NfsV2Program.isClientAllowed(Path)` | メソッド / コンストラクタ | 1715 | 1726 |
| `NfsV2Program.isQnxSillyRenamePath(Path)` | メソッド / コンストラクタ | 1995 | 2008 |
| `NfsV2Program.containsReadDirCookie(List<DirectoryEntry>, int)` | メソッド / コンストラクタ | 2282 | 2297 |
| `NfsV2Program.resolveAttributeUid(Path)` | メソッド / コンストラクタ | 2445 | 2463 |
| `NfsV2Program.resolveAttributeGid(Path)` | メソッド / コンストラクタ | 2476 | 2495 |
| `NfsV2Program.isReadOnlyPath(Path)` | メソッド / コンストラクタ | 2509 | 2528 |
| `SetAttributes` | クラスJavadoc（メソッドなし） | 2706 | 2727 |
| `SetAttributes` | クラスJavadoc（メソッドなし） | 2711 | 2731 |
| `SetAttributes.hasSize()` | メソッド / コンストラクタ | 2766 | 2786 |
| `SetAttributes.hasMode()` | メソッド / コンストラクタ | 2781 | 2803 |
| `SetAttributes.hasUid()` | メソッド / コンストラクタ | 2796 | 2820 |
| `SetAttributes.hasGid()` | メソッド / コンストラクタ | 2811 | 2837 |
| `ResolvedPath` | クラスJavadoc（メソッドなし） | 2952 | 2980 |
| `ResolvedPath` | クラスJavadoc（メソッドなし） | 2957 | 2984 |
| `ResolvedPath.isOk()` | メソッド / コンストラクタ | 3020 | 3047 |
| `DirectoryEntry` | クラスJavadoc（メソッドなし） | 3064 | 3093 |
| `DirectoryEntry` | クラスJavadoc（メソッドなし） | 3069 | 3097 |
| `HardLinkGroup.HardLinkGroup()` | メソッド / コンストラクタ | 3153 | 3182 |

```diff
- * <p>更新履歴：
+ * <p>更新履歴：</p>
- * </p>
-	 * @return	判定結果<ul>
-	 * 			false	: 無効<br>
-	 * 			true	: 有効</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 無効</li>
+	 * <li>true: 有効</li>
+	 * </ul>
-	 * @return	判定結果<ul>
-	 * 			false	: 不正<br>
-	 * 			true	: 正常</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 不正</li>
+	 * <li>true: 正常</li>
+	 * </ul>
-	 * @return	判定結果<ul>
-	 * 			false	: 不正文字なし<br>
-	 * 			true	: 不正文字あり</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 不正文字なし</li>
+	 * <li>true: 不正文字あり</li>
+	 * </ul>
-	 * @return	判定結果<ul>
-	 * 			false	: 公開ルート外<br>
-	 * 			true	: 公開ルート内</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 公開ルート外</li>
+	 * <li>true: 公開ルート内</li>
+	 * </ul>
-	 * @return	判定結果<ul>
-	 * 			false	: 公開ルート外<br>
-	 * 			true	: 同一公開ルート内</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 公開ルート外</li>
+	 * <li>true: 同一公開ルート内</li>
+	 * </ul>
-	 * @return	判定結果<ul>
-	 * 			false	: 読込専用<br>
-	 * 			true	: 書込可</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 読込専用</li>
+	 * <li>true: 書込可</li>
+	 * </ul>
-	 * @return	判定結果<ul>
-	 * 			false	: 拒否<br>
-	 * 			true	: 許可</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 拒否</li>
+	 * <li>true: 許可</li>
+	 * </ul>
-	 * @return	判定結果<ul>
-	 * 			false	: 通常パス<br>
-	 * 			true	: QNX一時リネームパス</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 通常パス</li>
+	 * <li>true: QNX一時リネームパス</li>
+	 * </ul>
-	 * @return	判定結果<ul>
-	 * 			false	: 存在なし<br>
-	 * 			true	: 存在あり</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 存在なし</li>
+	 * <li>true: 存在あり</li>
+	 * </ul>
+	 * @param path	属性を取得する対象パス
+	 * @param path	属性を取得する対象パス
-	 * @return	判定結果<ul>
-	 * 			false	: 書込可能<br>
-	 * 			true	: 読込専用</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 書込可能</li>
+	 * <li>true: 読込専用</li>
+	 * </ul>
-	 * <p>更新履歴：
+	 * <p>更新履歴：</p>
-	 * </p>
-		 * @return	判定結果<ul>
-		 * 			false	: 指定なし<br>
-		 * 			true	: 指定あり</ul>
+		 * @return	判定結果
+		 * <ul>
+		 * <li>false: 指定なし</li>
+		 * <li>true: 指定あり</li>
+		 * </ul>
-		 * @return	判定結果<ul>
-		 * 			false	: 指定なし<br>
-		 * 			true	: 指定あり</ul>
+		 * @return	判定結果
+		 * <ul>
+		 * <li>false: 指定なし</li>
+		 * <li>true: 指定あり</li>
+		 * </ul>
-		 * @return	判定結果<ul>
-		 * 			false	: 指定なし<br>
-		 * 			true	: 指定あり</ul>
+		 * @return	判定結果
+		 * <ul>
+		 * <li>false: 指定なし</li>
+		 * <li>true: 指定あり</li>
+		 * </ul>
-		 * @return	判定結果<ul>
-		 * 			false	: 指定なし<br>
-		 * 			true	: 指定あり</ul>
+		 * @return	判定結果
+		 * <ul>
+		 * <li>false: 指定なし</li>
+		 * <li>true: 指定あり</li>
+		 * </ul>
-	 * <p>更新履歴：
+	 * <p>更新履歴：</p>
-	 * </p>
-		 * @return	判定結果<ul>
-		 * 			false	: 異常<br>
-		 * 			true	: 正常</ul>
+		 * @return	判定結果
+		 * <ul>
+		 * <li>false: 異常</li>
+		 * <li>true: 正常</li>
+		 * </ul>
-	 * <p>更新履歴：
+	 * <p>更新履歴：</p>
-	 * </p>
+		//----------------------------------------------------------------------
+		/**
+		 * インスタンスを生成します。<br><br>
+		 *
+		 * <p>メソッド名称： コンストラクタ</p>
+		 */
+		//----------------------------------------------------------------------
+		private HardLinkGroup() {
+		}
+
```


### `src/jp/co/enterogawa/nfs/program/NfsV3Program.java`

| 対象メソッド・メンバー | 種別 | 修正前行 | 修正後行 |
|---|---|---|---|
| `NfsV3Program` | クラスJavadoc（メソッドなし） | 53 | 53 |
| `NfsV3Program` | クラスJavadoc（メソッドなし） | 58 | 57 |
| `NfsV3Program.PROC_NULL` | フィールド（メソッドなし） | 144 | 144 |
| `NfsV3Program.PROC_GETATTR` | フィールド（メソッドなし） | 145 | 146 |
| `NfsV3Program.PROC_SETATTR` | フィールド（メソッドなし） | 146 | 148 |
| `NfsV3Program.PROC_LOOKUP` | フィールド（メソッドなし） | 147 | 150 |
| `NfsV3Program.PROC_ACCESS` | フィールド（メソッドなし） | 148 | 152 |
| `NfsV3Program.PROC_READLINK` | フィールド（メソッドなし） | 149 | 154 |
| `NfsV3Program.PROC_READ` | フィールド（メソッドなし） | 150 | 156 |
| `NfsV3Program.PROC_WRITE` | フィールド（メソッドなし） | 151 | 158 |
| `NfsV3Program.PROC_CREATE` | フィールド（メソッドなし） | 152 | 160 |
| `NfsV3Program.PROC_MKDIR` | フィールド（メソッドなし） | 153 | 162 |
| `NfsV3Program.PROC_SYMLINK` | フィールド（メソッドなし） | 154 | 164 |
| `NfsV3Program.PROC_MKNOD` | フィールド（メソッドなし） | 155 | 166 |
| `NfsV3Program.PROC_REMOVE` | フィールド（メソッドなし） | 156 | 168 |
| `NfsV3Program.PROC_RMDIR` | フィールド（メソッドなし） | 157 | 170 |
| `NfsV3Program.PROC_RENAME` | フィールド（メソッドなし） | 158 | 172 |
| `NfsV3Program.PROC_LINK` | フィールド（メソッドなし） | 159 | 174 |
| `NfsV3Program.PROC_READDIR` | フィールド（メソッドなし） | 160 | 176 |
| `NfsV3Program.PROC_READDIRPLUS` | フィールド（メソッドなし） | 161 | 178 |
| `NfsV3Program.PROC_FSSTAT` | フィールド（メソッドなし） | 162 | 180 |
| `NfsV3Program.PROC_FSINFO` | フィールド（メソッドなし） | 163 | 182 |
| `NfsV3Program.PROC_PATHCONF` | フィールド（メソッドなし） | 164 | 184 |
| `NfsV3Program.PROC_COMMIT` | フィールド（メソッドなし） | 165 | 186 |
| `NfsV3Program.resolveAttributeUid(Path)` | メソッド / コンストラクタ | 1837 | 1859 |
| `NfsV3Program.resolveAttributeGid(Path)` | メソッド / コンストラクタ | 1868 | 1891 |
| `NfsV3Program.isClientAllowed(Path)` | メソッド / コンストラクタ | 2158 | 2181 |
| `NfsV3Program.isWritable(Path)` | メソッド / コンストラクタ | 2175 | 2200 |
| `NfsV3Program.isValidLookupName(String)` | メソッド / コンストラクタ | 2191 | 2218 |
| `NfsV3Program.isReadOnlyPath(Path)` | メソッド / コンストラクタ | 2222 | 2251 |
| `PreOpAttributes` | クラスJavadoc（メソッドなし） | 2564 | 2595 |
| `PreOpAttributes` | クラスJavadoc（メソッドなし） | 2569 | 2599 |
| `PreOpAttributes.exists()` | メソッド / コンストラクタ | 2642 | 2672 |
| `SetAttributes` | クラスJavadoc（メソッドなし） | 2699 | 2731 |
| `SetAttributes` | クラスJavadoc（メソッドなし） | 2704 | 2735 |
| `SetAttributes.hasMode()` | メソッド / コンストラクタ | 2758 | 2789 |
| `SetAttributes.hasUid()` | メソッド / コンストラクタ | 2773 | 2806 |
| `SetAttributes.hasGid()` | メソッド / コンストラクタ | 2788 | 2823 |
| `SetAttributes.hasSize()` | メソッド / コンストラクタ | 2803 | 2840 |
| `ResolvedPath` | クラスJavadoc（メソッドなし） | 2944 | 2983 |
| `ResolvedPath` | クラスJavadoc（メソッドなし） | 2949 | 2987 |
| `ResolvedPath.isOk()` | メソッド / コンストラクタ | 3019 | 3057 |
| `DirectoryEntry` | クラスJavadoc（メソッドなし） | 3076 | 3116 |
| `DirectoryEntry` | クラスJavadoc（メソッドなし） | 3081 | 3120 |
| `HardLinkGroup.HardLinkGroup()` | メソッド / コンストラクタ | 3182 | 3222 |

```diff
- * <p>更新履歴：
+ * <p>更新履歴：</p>
- * </p>
+	/** NULL手続き */
+	/** GETATTR手続き */
+	/** SETATTR手続き */
+	/** LOOKUP手続き */
+	/** ACCESS手続き */
+	/** READLINK手続き */
+	/** READ手続き */
+	/** WRITE手続き */
+	/** CREATE手続き */
+	/** MKDIR手続き */
+	/** SYMLINK手続き */
+	/** MKNOD手続き */
+	/** REMOVE手続き */
+	/** RMDIR手続き */
+	/** RENAME手続き */
+	/** LINK手続き */
+	/** READDIR手続き */
+	/** READDIRPLUS手続き */
+	/** FSSTAT手続き */
+	/** FSINFO手続き */
+	/** PATHCONF手続き */
+	/** COMMIT手続き */
+	 * @param path	属性を取得する対象パス
+	 * @param path	属性を取得する対象パス
-	 * @return	判定結果<ul>
-	 * 			false	: 拒否<br>
-	 * 			true	: 許可</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 拒否</li>
+	 * <li>true: 許可</li>
+	 * </ul>
-	 * @return	判定結果<ul>
-	 * 			false	: 書込不可<br>
-	 * 			true	: 書込可</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 書込不可</li>
+	 * <li>true: 書込可</li>
+	 * </ul>
-	 * @return	判定結果<ul>
-	 * 			false	: 不正<br>
-	 * 			true	: 正常</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 不正</li>
+	 * <li>true: 正常</li>
+	 * </ul>
-	 * @return	判定結果<ul>
-	 * 			false	: 書込可能<br>
-	 * 			true	: 読込専用</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 書込可能</li>
+	 * <li>true: 読込専用</li>
+	 * </ul>
-	 * <p>更新履歴：
+	 * <p>更新履歴：</p>
-	 * </p>
-		 * @return	判定結果<ul>
-		 * 			false	: 不存在<br>
-		 * 			true	: 存在</ul>
+		 * @return	判定結果
+		 * <ul>
+		 * <li>false: 不存在</li>
+		 * <li>true: 存在</li>
+		 * </ul>
-	 * <p>更新履歴：
+	 * <p>更新履歴：</p>
-	 * </p>
-		 * @return	判定結果<ul>
-		 * 			false	: 指定なし<br>
-		 * 			true	: 指定あり</ul>
+		 * @return	判定結果
+		 * <ul>
+		 * <li>false: 指定なし</li>
+		 * <li>true: 指定あり</li>
+		 * </ul>
-		 * @return	判定結果<ul>
-		 * 			false	: 指定なし<br>
-		 * 			true	: 指定あり</ul>
+		 * @return	判定結果
+		 * <ul>
+		 * <li>false: 指定なし</li>
+		 * <li>true: 指定あり</li>
+		 * </ul>
-		 * @return	判定結果<ul>
-		 * 			false	: 指定なし<br>
-		 * 			true	: 指定あり</ul>
+		 * @return	判定結果
+		 * <ul>
+		 * <li>false: 指定なし</li>
+		 * <li>true: 指定あり</li>
+		 * </ul>
-		 * @return	判定結果<ul>
-		 * 			false	: 指定なし<br>
-		 * 			true	: 指定あり</ul>
+		 * @return	判定結果
+		 * <ul>
+		 * <li>false: 指定なし</li>
+		 * <li>true: 指定あり</li>
+		 * </ul>
-	 * <p>更新履歴：
+	 * <p>更新履歴：</p>
-	 * </p>
-		 * @return	判定結果<ul>
-		 * 			false	: 異常<br>
-		 * 			true	: 正常</ul>
+		 * @return	判定結果
+		 * <ul>
+		 * <li>false: 異常</li>
+		 * <li>true: 正常</li>
+		 * </ul>
-	 * <p>更新履歴：
+	 * <p>更新履歴：</p>
-	 * </p>
+		//----------------------------------------------------------------------
+		/**
+		 * インスタンスを生成します。<br><br>
+		 *
+		 * <p>メソッド名称： コンストラクタ</p>
+		 */
+		//----------------------------------------------------------------------
+		private HardLinkGroup() {
+		}
+
```


### `src/jp/co/enterogawa/nfs/program/PortmapV2Program.java`

| 対象メソッド・メンバー | 種別 | 修正前行 | 修正後行 |
|---|---|---|---|
| `PortmapV2Program` | クラスJavadoc（メソッドなし） | 19 | 19 |
| `PortmapV2Program` | クラスJavadoc（メソッドなし） | 24 | 23 |

```diff
- * <p>更新履歴：
+ * <p>更新履歴：</p>
- * </p>
```


### `src/jp/co/enterogawa/nfs/rpc/RpcCall.java`

| 対象メソッド・メンバー | 種別 | 修正前行 | 修正後行 |
|---|---|---|---|
| `RpcCall` | クラスJavadoc（メソッドなし） | 13 | 13 |
| `RpcCall` | クラスJavadoc（メソッドなし） | 18 | 17 |

```diff
- * <p>更新履歴：
+ * <p>更新履歴：</p>
- * </p>
```


### `src/jp/co/enterogawa/nfs/rpc/RpcCallHandler.java`

| 対象メソッド・メンバー | 種別 | 修正前行 | 修正後行 |
|---|---|---|---|
| `RpcCallHandler` | クラスJavadoc（メソッドなし） | 16 | 16 |
| `RpcCallHandler` | クラスJavadoc（メソッドなし） | 21 | 20 |
| `RpcCallHandler.shouldLogRequest(RpcCall, int, Integer)` | メソッド / コンストラクタ | 138 | 137 |

```diff
- * <p>更新履歴：
+ * <p>更新履歴：</p>
- * </p>
-	 * @return	判定結果<ul>
-	 * 			false	: 抑制<br>
-	 * 			true	: 出力</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 抑制</li>
+	 * <li>true: 出力</li>
+	 * </ul>
```


### `src/jp/co/enterogawa/nfs/rpc/RpcConstants.java`

| 対象メソッド・メンバー | 種別 | 修正前行 | 修正後行 |
|---|---|---|---|
| `RpcConstants` | クラスJavadoc（メソッドなし） | 11 | 11 |
| `RpcConstants` | クラスJavadoc（メソッドなし） | 16 | 15 |

```diff
- * <p>更新履歴：
+ * <p>更新履歴：</p>
- * </p>
```


### `src/jp/co/enterogawa/nfs/rpc/RpcCredential.java`

| 対象メソッド・メンバー | 種別 | 修正前行 | 修正後行 |
|---|---|---|---|
| `RpcCredential` | クラスJavadoc（メソッドなし） | 13 | 13 |
| `RpcCredential` | クラスJavadoc（メソッドなし） | 18 | 17 |
| `RpcCredential.isAuthSys()` | メソッド / コンストラクタ | 92 | 91 |
| `AuthSysFields` | クラスJavadoc（メソッドなし） | 177 | 178 |
| `AuthSysFields` | クラスJavadoc（メソッドなし） | 182 | 182 |

```diff
- * <p>更新履歴：
+ * <p>更新履歴：</p>
- * </p>
-	 * @return	判定結果<ul>
-	 * 			false	: その他<br>
-	 * 			true	: AUTH_SYS</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: その他</li>
+	 * <li>true: AUTH_SYS</li>
+	 * </ul>
-	 * <p>更新履歴：
+	 * <p>更新履歴：</p>
-	 * </p>
```


### `src/jp/co/enterogawa/nfs/rpc/RpcProgram.java`

| 対象メソッド・メンバー | 種別 | 修正前行 | 修正後行 |
|---|---|---|---|
| `RpcProgram` | クラスJavadoc（メソッドなし） | 15 | 15 |
| `RpcProgram` | クラスJavadoc（メソッドなし） | 20 | 19 |
| `RpcProgram.supportsVersion(int)` | メソッド / コンストラクタ | 56 | 55 |

```diff
- * <p>更新履歴：
+ * <p>更新履歴：</p>
- * </p>
-	 * @return	判定結果<ul>
-	 * 			false	: 未対応<br>
-	 * 			true	: 対応</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 未対応</li>
+	 * <li>true: 対応</li>
+	 * </ul>
```


### `src/jp/co/enterogawa/nfs/rpc/RpcProgramRouter.java`

| 対象メソッド・メンバー | 種別 | 修正前行 | 修正後行 |
|---|---|---|---|
| `RpcProgramRouter` | クラスJavadoc（メソッドなし） | 17 | 17 |
| `RpcProgramRouter` | クラスJavadoc（メソッドなし） | 22 | 21 |
| `RpcProgramRouter.supportsVersion(int)` | メソッド / コンストラクタ | 104 | 103 |

```diff
- * <p>更新履歴：
+ * <p>更新履歴：</p>
- * </p>
-	 * @return	判定結果<ul>
-	 * 			false	: 未対応<br>
-	 * 			true	: 対応</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 未対応</li>
+	 * <li>true: 対応</li>
+	 * </ul>
```


### `src/jp/co/enterogawa/nfs/rpc/RpcReplyWriter.java`

| 対象メソッド・メンバー | 種別 | 修正前行 | 修正後行 |
|---|---|---|---|
| `RpcReplyWriter` | クラスJavadoc（メソッドなし） | 13 | 13 |
| `RpcReplyWriter` | クラスJavadoc（メソッドなし） | 18 | 17 |

```diff
- * <p>更新履歴：
+ * <p>更新履歴：</p>
- * </p>
```


### `src/jp/co/enterogawa/nfs/rpc/RpcRequestContext.java`

| 対象メソッド・メンバー | 種別 | 修正前行 | 修正後行 |
|---|---|---|---|
| `RpcRequestContext` | クラスJavadoc（メソッドなし） | 11 | 11 |
| `RpcRequestContext` | クラスJavadoc（メソッドなし） | 16 | 15 |
| `RpcRequestContext.hasAuthSysIdentity()` | メソッド / コンストラクタ | 269 | 268 |

```diff
- * <p>更新履歴：
+ * <p>更新履歴：</p>
- * </p>
-	 * @return	判定結果<ul>
-	 * 			false	: なし<br>
-	 * 			true	: あり</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: なし</li>
+	 * <li>true: あり</li>
+	 * </ul>
```


### `src/jp/co/enterogawa/nfs/rpc/RpcServer.java`

| 対象メソッド・メンバー | 種別 | 修正前行 | 修正後行 |
|---|---|---|---|
| `RpcServer` | クラスJavadoc（メソッドなし） | 13 | 13 |
| `RpcServer` | クラスJavadoc（メソッドなし） | 18 | 17 |

```diff
- * <p>更新履歴：
+ * <p>更新履歴：</p>
- * </p>
```


### `src/jp/co/enterogawa/nfs/rpc/TcpRpcServer.java`

| 対象メソッド・メンバー | 種別 | 修正前行 | 修正後行 |
|---|---|---|---|
| `TcpRpcServer` | クラスJavadoc（メソッドなし） | 27 | 27 |
| `TcpRpcServer` | クラスJavadoc（メソッドなし） | 32 | 31 |

```diff
- * <p>更新履歴：
+ * <p>更新履歴：</p>
- * </p>
```


### `src/jp/co/enterogawa/nfs/rpc/UdpRpcServer.java`

| 対象メソッド・メンバー | 種別 | 修正前行 | 修正後行 |
|---|---|---|---|
| `UdpRpcServer` | クラスJavadoc（メソッドなし） | 22 | 22 |
| `UdpRpcServer` | クラスJavadoc（メソッドなし） | 27 | 26 |

```diff
- * <p>更新履歴：
+ * <p>更新履歴：</p>
- * </p>
```


### `src/jp/co/enterogawa/nfs/server/NfsServer.java`

| 対象メソッド・メンバー | 種別 | 修正前行 | 修正後行 |
|---|---|---|---|
| `NfsServer` | クラスJavadoc（メソッドなし） | 32 | 32 |
| `NfsServer` | クラスJavadoc（メソッドなし） | 37 | 36 |

```diff
- * <p>更新履歴：
+ * <p>更新履歴：</p>
- * </p>
```


### `src/jp/co/enterogawa/nfs/util/ServerLog.java`

| 対象メソッド・メンバー | 種別 | 修正前行 | 修正後行 |
|---|---|---|---|
| `ServerLog` | クラスJavadoc（メソッドなし） | 20 | 20 |
| `ServerLog` | クラスJavadoc（メソッドなし） | 25 | 24 |
| `ServerLog.isRequestLogEnabled()` | メソッド / コンストラクタ | 99 | 98 |
| `ServerLog.shouldLogOperation(String, int)` | メソッド / コンストラクタ | 116 | 117 |
| `ServerLog.isDebugEnabled()` | メソッド / コンストラクタ | 141 | 144 |
| `ServerLog.isVerboseMutationLogEnabled()` | メソッド / コンストラクタ | 156 | 161 |
| `ServerLog.isConsoleEnabled()` | メソッド / コンストラクタ | 171 | 178 |

```diff
- * <p>更新履歴：
+ * <p>更新履歴：</p>
- * </p>
-	 * @return	判定結果<ul>
-	 * 			false	: 無効<br>
-	 * 			true	: 有効</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 無効</li>
+	 * <li>true: 有効</li>
+	 * </ul>
-	 * @return	判定結果<ul>
-	 * 			false	: 抑制<br>
-	 * 			true	: 出力</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 抑制</li>
+	 * <li>true: 出力</li>
+	 * </ul>
-	 * @return	判定結果<ul>
-	 * 			false	: 無効<br>
-	 * 			true	: 有効</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 無効</li>
+	 * <li>true: 有効</li>
+	 * </ul>
-	 * @return	判定結果<ul>
-	 * 			false	: 無効<br>
-	 * 			true	: 有効</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 無効</li>
+	 * <li>true: 有効</li>
+	 * </ul>
-	 * @return	判定結果<ul>
-	 * 			false	: 無効<br>
-	 * 			true	: 有効</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 無効</li>
+	 * <li>true: 有効</li>
+	 * </ul>
```


### `src/jp/co/enterogawa/nfs/xdr/XdrReader.java`

| 対象メソッド・メンバー | 種別 | 修正前行 | 修正後行 |
|---|---|---|---|
| `XdrReader` | クラスJavadoc（メソッドなし） | 15 | 15 |
| `XdrReader` | クラスJavadoc（メソッドなし） | 20 | 19 |
| `XdrReader.hasRemaining()` | メソッド / コンストラクタ | 282 | 281 |

```diff
- * <p>更新履歴：
+ * <p>更新履歴：</p>
- * </p>
-	 * @return	判定結果<ul>
-	 * 			false	: 残データなし<br>
-	 * 			true	: 残データあり</ul>
+	 * @return	判定結果
+	 * <ul>
+	 * <li>false: 残データなし</li>
+	 * <li>true: 残データあり</li>
+	 * </ul>
```


### `src/jp/co/enterogawa/nfs/xdr/XdrWriter.java`

| 対象メソッド・メンバー | 種別 | 修正前行 | 修正後行 |
|---|---|---|---|
| `XdrWriter` | クラスJavadoc（メソッドなし） | 15 | 15 |
| `XdrWriter` | クラスJavadoc（メソッドなし） | 20 | 19 |
| `XdrWriter.XdrWriter()` | メソッド / コンストラクタ | 30 | 30 |

```diff
- * <p>更新履歴：
+ * <p>更新履歴：</p>
- * </p>
+	//--------------------------------------------------------------------------
+	/**
+	 * インスタンスを生成します。<br><br>
+	 *
+	 * <p>メソッド名称： コンストラクタ</p>
+	 */
+	//--------------------------------------------------------------------------
+	public XdrWriter() {
+	}
+
```


### `test/jp/co/enterogawa/nfs/AllTests.java`

| 対象メソッド・メンバー | 種別 | 修正前行 | 修正後行 |
|---|---|---|---|
| `AllTests.AllTests()` | メソッド / コンストラクタ | 48 | 49 |
| `AllTests.testNfsV2LargeReadDir()` | メソッド / コンストラクタ | 836 | 847 |
| `AllTests.testNfsV2LargeReadDir()` | メソッド / コンストラクタ | 844 | 856 |
| `AllTests.assertBrokenSymbolicLinkV3(NfsV3Program, FileHandle, Path)` | メソッド / コンストラクタ | 1553 | 1566 |
| `AllTests.assertBrokenSymbolicLinkV2(NfsV2Program, FileHandle, Path)` | メソッド / コンストラクタ | 2896 | 2910 |

```diff
+	//--------------------------------------------------------------------------
+	/**
+	 * インスタンスを生成します。<br><br>
+	 *
+	 * <p>メソッド名称： コンストラクタ</p>
+	 */
+	//--------------------------------------------------------------------------
+	public AllTests() {
+	}
+
+			// 複数ページの READDIR 応答を検証するために大量のファイルを用意する
+			// EOF になるまで上限ページ数の範囲でディレクトリを読み込む
+		// Windows 側で symlink を作成できない場合は検証を省略する
+		// Windows 側で symlink を作成できない場合は検証を省略する
```


### `test/jp/co/enterogawa/nfs/LocalRpcBenchmark.java`

| 対象メソッド・メンバー | 種別 | 修正前行 | 修正後行 |
|---|---|---|---|
| `LocalRpcBenchmark` | クラスJavadoc（メソッドなし） | 29 | 29 |
| `LocalRpcBenchmark` | クラスJavadoc（メソッドなし） | 31 | 31 |
| `LocalRpcBenchmark.PROGRAM_NFS` | フィールド（メソッドなし） | 39 | 42 |
| `LocalRpcBenchmark.NFS_VERSION` | フィールド（メソッドなし） | 41 | 45 |
| `LocalRpcBenchmark.XID` | フィールド（メソッドなし） | 43 | 48 |
| `LocalRpcBenchmark.TIMESTAMP_FORMAT` | フィールド（メソッドなし） | 45 | 51 |
| `LocalRpcBenchmark.options` | フィールド（メソッドなし） | 48 | 55 |
| `LocalRpcBenchmark.program` | フィールド（メソッドなし） | 50 | 58 |
| `LocalRpcBenchmark.handleTable` | フィールド（メソッドなし） | 52 | 61 |
| `LocalRpcBenchmark.root` | フィールド（メソッドなし） | 54 | 64 |
| `LocalRpcBenchmark.results` | フィールド（メソッドなし） | 56 | 67 |
| `LocalRpcBenchmark.run()` | メソッド / コンストラクタ | 116 | 128 |
| `LocalRpcBenchmark.run()` | メソッド / コンストラクタ | 120 | 133 |
| `LocalRpcBenchmark.run()` | メソッド / コンストラクタ | 125 | 139 |
| `LocalRpcBenchmark.runOneLoop(int)` | メソッド / コンストラクタ | 149 | 164 |
| `LocalRpcBenchmark.runOneLoop(int)` | メソッド / コンストラクタ | 159 | 175 |
| `LocalRpcBenchmark.runOneLoop(int)` | メソッド / コンストラクタ | 166 | 183 |
| `LocalRpcBenchmark.runOneLoop(int)` | メソッド / コンストラクタ | 172 | 190 |
| `LocalRpcBenchmark.runOneLoop(int)` | メソッド / コンストラクタ | 180 | 199 |
| `LocalRpcBenchmark.runOneLoop(int)` | メソッド / コンストラクタ | 183 | 203 |
| `LocalRpcBenchmark.runOneLoop(int)` | メソッド / コンストラクタ | 185 | 206 |
| `LocalRpcBenchmark.createDirectories(FileHandle, int)` | メソッド / コンストラクタ | 207 | 229 |
| `LocalRpcBenchmark.createDirectories(FileHandle, int)` | メソッド / コンストラクタ | 210 | 233 |
| `LocalRpcBenchmark.addAncestors(List<DirectoryNode>, DirectoryNode)` | メソッド / コンストラクタ | 239 | 263 |
| `LocalRpcBenchmark.addAncestors(List<DirectoryNode>, DirectoryNode)` | メソッド / コンストラクタ | 240 | 265 |
| `LocalRpcBenchmark.createData(int)` | メソッド / コンストラクタ | 259 | 285 |
| `LocalRpcBenchmark.readDirectory(FileHandle)` | メソッド / コンストラクタ | 381 | 408 |
| `LocalRpcBenchmark.call(int, XdrWriter)` | メソッド / コンストラクタ | 465 | 493 |
| `LocalRpcBenchmark.expectStatus(String, XdrReader, int)` | メソッド / コンストラクタ | 542 | 571 |
| `LocalRpcBenchmark.writeReports()` | メソッド / コンストラクタ | 588 | 618 |
| `LocalRpcBenchmark.deleteDirectory(Path)` | メソッド / コンストラクタ | 685 | 716 |
| `LocalRpcBenchmark.deleteDirectory(Path)` | メソッド / コンストラクタ | 692 | 724 |
| `Options` | クラスJavadoc（メソッドなし） | 701 | 733 |
| `Options.files` | フィールド（メソッドなし） | 704 | 739 |
| `Options.directories` | フィールド（メソッドなし） | 706 | 742 |
| `Options.depth` | フィールド（メソッドなし） | 708 | 745 |
| `Options.minSize` | フィールド（メソッドなし） | 710 | 748 |
| `Options.maxSize` | フィールド（メソッドなし） | 712 | 751 |
| `Options.loops` | フィールド（メソッドなし） | 714 | 754 |
| `Options.longRun` | フィールド（メソッドなし） | 716 | 757 |
| `Options.durationSeconds` | フィールド（メソッドなし） | 718 | 760 |
| `Options.writeSync` | フィールド（メソッドなし） | 720 | 763 |
| `Options.writeCacheEnabled` | フィールド（メソッドなし） | 722 | 766 |
| `Options.writeCacheMaxOpenFiles` | フィールド（メソッドなし） | 724 | 769 |
| `Options.writeCacheIdleMillis` | フィールド（メソッドなし） | 726 | 772 |
| `Options.udpWorkers` | フィールド（メソッドなし） | 728 | 775 |
| `Options.udpQueueSize` | フィールド（メソッドなし） | 730 | 778 |
| `Options.tcpTimeoutMillis` | フィールド（メソッドなし） | 732 | 781 |
| `Options.outputDirectory` | フィールド（メソッドなし） | 734 | 784 |
| `Options.Options()` | メソッド / コンストラクタ | 736 | 787 |
| `Options.parse(String[])` | メソッド / コンストラクタ | 740 | 811 |
| `Options.parse(String[])` | メソッド / コンストラクタ | 743 | 815 |
| `Options.parse(String[])` | メソッド / コンストラクタ | 748 | 821 |
| `Options.parse(String[])` | メソッド / コンストラクタ | 768 | 842 |
| `Options.parse(String[])` | メソッド / コンストラクタ | 772 | 847 |
| `Options.intValue(Map<String, String>, String, int)` | メソッド / コンストラクタ | 779 | 855 |
| `Options.booleanValue(Map<String, String>, String, boolean)` | メソッド / コンストラクタ | 784 | 872 |
| `DirectoryNode` | クラスJavadoc（メソッドなし） | 793 | 896 |
| `DirectoryNode.parent` | フィールド（メソッドなし） | 796 | 902 |
| `DirectoryNode.name` | フィールド（メソッドなし） | 798 | 905 |
| `DirectoryNode.handle` | フィールド（メソッドなし） | 800 | 908 |
| `DirectoryNode.DirectoryNode(DirectoryNode, String, FileHandle)` | メソッド / コンストラクタ | 802 | 911 |
| `FileEntry` | クラスJavadoc（メソッドなし） | 812 | 931 |
| `FileEntry.directory` | フィールド（メソッドなし） | 815 | 937 |
| `FileEntry.name` | フィールド（メソッドなし） | 817 | 940 |
| `FileEntry.FileEntry(DirectoryNode, String)` | メソッド / コンストラクタ | 819 | 943 |
| `Result` | クラスJavadoc（メソッドなし） | 828 | 961 |
| `Result.name` | フィールド（メソッドなし） | 831 | 967 |
| `Result.operations` | フィールド（メソッドなし） | 833 | 970 |
| `Result.elapsedNanos` | フィールド（メソッドなし） | 835 | 973 |
| `Result.usedMemoryBytes` | フィールド（メソッドなし） | 837 | 976 |
| `Result.Result(String, int, long, long)` | メソッド / コンストラクタ | 839 | 979 |

```diff
- * Local RPC benchmark runner.
+ * ローカルRPCベンチマーククラスです。<br><br>
- * <p>This runner calls NFSv2 procedures in-process. It intentionally does not
- * start the Windows service or bind network ports.</p>
+ * <p>クラス名称： ローカルRPCベンチマーク</p>
+ *
+ * <p>NFSv2手続きをプロセス内で呼び出して測定します。
+ * Windowsサービスの起動やネットワークポートへのバインドは行いません。</p>
+	/** NFSプログラム番号 */
+	/** NFSバージョン */
+	/** ローカルRPC要求のトランザクションID */
+	/** レポートファイル名の日時書式 */
+	/** 測定オプション */
+	/** 測定対象のNFSv2プログラム */
+	/** 測定用ファイルハンドル表 */
+	/** 測定用の共有ルート */
+	/** 操作ごとの測定結果 */
+				// 回数指定の測定で繰り返し回数に達した場合
+				// 長時間測定の終了時刻に達した場合
+			// NFS プログラムの初期化が完了している場合
+		// 指定した数のファイルを作成し、データを書き込む
+		// 作成した全ファイルの LOOKUP を測定する
+		// 作成した全ディレクトリの READDIR を測定する
+		// 作成した全ファイルの RENAME を測定する
+		// 作成した全ファイルを削除する
+		// 子ディレクトリから親ディレクトリの順に削除する
+			// 測定用のルート以外のディレクトリである場合
+		// 指定した数のディレクトリツリーを作成する
+			// 指定した深さまで子ディレクトリを作成する
+		// 祖先ディレクトリを親から子の順に測定対象へ追加する
+			// 測定対象にまだ登録されていないディレクトリの場合
+		// ファイル番号と位置から再現可能な書き込みデータを生成する
+			// エントリがなく EOF でもない不正な READDIR 応答の場合
+		// RPC 要求が受け付けられなかった場合
+		// NFS ステータスが期待値と異なる場合
+		// 各測定結果を Markdown と CSV の行に変換する
+		// 削除対象のパスが存在しない場合
+			// 子から親の順に測定用ファイルとディレクトリを削除する
-	 * Options.
+	 * 測定オプションクラスです。<br><br>
+	 *
+	 * <p>クラス名称： 測定オプション</p>
+		/** 各測定ループで作成するファイル数 */
+		/** ディレクトリツリー数 */
+		/** 各ディレクトリツリーの深さ */
+		/** 測定用ファイルの最小サイズ */
+		/** 測定用ファイルの最大サイズ */
+		/** 回数指定の測定における繰り返し回数 */
+		/** 終了時刻まで測定を繰り返すか */
+		/** 長時間測定の継続秒数 */
+		/** WRITE応答前の同期有無 */
+		/** 書込ファイルキャッシュの有効化 */
+		/** 書込キャッシュの最大オープンファイル数 */
+		/** 書込キャッシュのアイドル保持時間 */
+		/** 測定用設定に保存するUDPワーカー数 */
+		/** 測定用設定に保存するUDPキューサイズ */
+		/** 測定用設定に保存するTCP読込タイムアウト */
+		/** レポートの出力先ディレクトリ */
+		//----------------------------------------------------------------------
+		/**
+		 * インスタンスを生成します。<br><br>
+		 *
+		 * <p>メソッド名称： コンストラクタ</p>
+		 */
+		//----------------------------------------------------------------------
+		private Options() {
+		}
+
+		//----------------------------------------------------------------------
+		/**
+		 * コマンドライン引数から測定設定を読み込みます。<br><br>
+		 *
+		 * <p>メソッド名称： 測定設定読込</p>
+		 *
+		 * @param args	コマンドライン引数
+		 * @return	読み込んだ測定設定
+		 */
+		//----------------------------------------------------------------------
+			// コマンドライン引数から測定オプションを読み取る
+				// 長時間測定モードが指定された場合
+				// 値を伴うオプション名と後続の値が指定された場合
+			// 結果の出力先が指定された場合
+			// 最大ファイルサイズが最小ファイルサイズより小さい場合
+		//----------------------------------------------------------------------
+		/**
+		 * 整数の測定オプションを取得します。<br><br>
+		 *
+		 * <p>メソッド名称： 整数オプション取得</p>
+		 *
+		 * @param values	オプション名と値の対応表
+		 * @param key	取得するオプション名
+		 * @param defaultValue	値が指定されていない場合の既定値
+		 * @return	オプション値または既定値
+		 */
+		//----------------------------------------------------------------------
+		//----------------------------------------------------------------------
+		/**
+		 * booleanの測定オプションを取得します。<br><br>
+		 *
+		 * <p>メソッド名称： booleanオプション取得</p>
+		 *
+		 * @param values	オプション名と値の対応表
+		 * @param key	取得するオプション名
+		 * @param defaultValue	値が指定されていない場合の既定値
+		 * @return	オプション値または既定値
+		 * <ul>
+		 * <li>false: 無効</li>
+		 * <li>true: 有効</li>
+		 * </ul>
+		 */
+		//----------------------------------------------------------------------
-	 * Directory node.
+	 * 測定用ディレクトリクラスです。<br><br>
+	 *
+	 * <p>クラス名称： 測定用ディレクトリ</p>
+		/** 親ディレクトリ。測定用ルートの場合は null */
+		/** ディレクトリ名 */
+		/** ディレクトリのファイルハンドル */
+		//----------------------------------------------------------------------
+		/**
+		 * インスタンスを生成します。<br><br>
+		 *
+		 * <p>メソッド名称： コンストラクタ</p>
+		 *
+		 * @param parent	親ディレクトリ。測定用ルートの場合は null
+		 * @param name	ディレクトリ名
+		 * @param handle	ディレクトリのファイルハンドル
+		 */
+		//----------------------------------------------------------------------
-	 * File entry.
+	 * 測定用ファイルクラスです。<br><br>
+	 *
+	 * <p>クラス名称： 測定用ファイル</p>
+		/** ファイルを保持するディレクトリ */
+		/** 現在のファイル名 */
+		//----------------------------------------------------------------------
+		/**
+		 * インスタンスを生成します。<br><br>
+		 *
+		 * <p>メソッド名称： コンストラクタ</p>
+		 *
+		 * @param directory	ファイルを保持するディレクトリ
+		 * @param name	ファイル名
+		 */
+		//----------------------------------------------------------------------
-	 * Result row.
+	 * 操作別測定結果クラスです。<br><br>
+	 *
+	 * <p>クラス名称： 操作別測定結果</p>
+		/** 測定した操作の名前 */
+		/** 操作回数 */
+		/** 経過時間のナノ秒値 */
+		/** 測定時の使用メモリバイト数 */
+		//----------------------------------------------------------------------
+		/**
+		 * インスタンスを生成します。<br><br>
+		 *
+		 * <p>メソッド名称： コンストラクタ</p>
+		 *
+		 * @param name	測定した操作の名前
+		 * @param operations	操作回数
+		 * @param elapsedNanos	経過時間のナノ秒値
+		 * @param usedMemoryBytes	測定時の使用メモリバイト数
+		 */
+		//----------------------------------------------------------------------
```


### `test/jp/co/enterogawa/nfs/ServiceSmokeTest.java`

| 対象メソッド・メンバー | 種別 | 修正前行 | 修正後行 |
|---|---|---|---|
| `ServiceSmokeTest.ServiceSmokeTest()` | メソッド / コンストラクタ | 33 | 34 |
| `ServiceSmokeTest.main(String[])` | メソッド / コンストラクタ | 74 | 85 |
| `ServiceSmokeTest.main(String[])` | メソッド / コンストラクタ | 75 | 87 |
| `ServiceSmokeTest.main(String[])` | メソッド / コンストラクタ | 81 | 94 |
| `ServiceSmokeTest.main(String[])` | メソッド / コンストラクタ | 86 | 100 |
| `ServiceSmokeTest.main(String[])` | メソッド / コンストラクタ | 91 | 106 |
| `ServiceSmokeTest.prepareHandlePersistence(Path)` | メソッド / コンストラクタ | 145 | 161 |
| `ServiceSmokeTest.verifyFileIntegrity(Path)` | メソッド / コンストラクタ | 215 | 232 |

```diff
+	//--------------------------------------------------------------------------
+	/**
+	 * インスタンスを生成します。<br><br>
+	 *
+	 * <p>メソッド名称： コンストラクタ</p>
+	 */
+	//--------------------------------------------------------------------------
+	public ServiceSmokeTest() {
+	}
+
+		// 確認対象を指定する引数がある場合
+			// TCP 通信の確認が指定された場合
+			// 再起動後のファイルハンドル確認の準備が指定された場合
+			// 再起動後のファイルハンドル確認が指定された場合
+			// ファイル整合性の確認が指定された場合
+		// 状態ファイルに親ディレクトリがある場合
+		// RENAME 後も移動元の一時ファイルが残っている場合
```
