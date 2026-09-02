# Javaコーディング規約対応

## 目的
- `C:\Users\ogawa\Downloads\java_coding_standard.md` の規約へ、動作を変更しない範囲で既存ソースを合わせる。
- 対象は `src` 配下の Java ソースとし、`test` 配下およびユーザー変更中の `AGENTS.md` は対象外とする。

## 確認対象
- `src/jp/co/enterogawa/nfs/**/*.java`
- 代表箇所:
  - `src/jp/co/enterogawa/nfs/NfsServerMain.java:40` `main` メソッド
  - `src/jp/co/enterogawa/nfs/config/NfsExport.java:110` `isWritable` メソッド
  - `src/jp/co/enterogawa/nfs/config/NfsExport.java:139` `allowsClient` メソッド
  - `src/jp/co/enterogawa/nfs/export/NfsModeStore.java:97` `getMode` メソッド
  - `src/jp/co/enterogawa/nfs/export/NfsModeStore.java:206` `setOwner` メソッド
  - `src/jp/co/enterogawa/nfs/export/NfsModeStore.java:530` `moveValues` メソッド
  - `src/jp/co/enterogawa/nfs/program/NfsV2Program.java:273` `handle` メソッド
  - `src/jp/co/enterogawa/nfs/program/NfsV3Program.java:291` `handle` メソッド
  - `src/jp/co/enterogawa/nfs/rpc/UdpRpcServer.java:158` `receiveLoop` メソッド
  - `src/jp/co/enterogawa/nfs/rpc/TcpRpcServer.java:179` `acceptLoop` メソッド
  - `src/jp/co/enterogawa/nfs/io/WriteFileCache.java:240` `cleanerLoop` メソッド

## 変更内容
- クラスJavadocに `著作権` と `更新履歴` を補完。
- 引数付きメソッド、コンストラクタ、インターフェースメソッド宣言の開始丸かっこ内へ1スペースを付与。
- boolean戻り値のJavadocを、`false` / `true` の意味が分かる `<ul>` 形式へ変更。
- `@param` / `@return` / `@throws` の説明開始位置をタブへ統一。
- `if` / `for` / `while` / `switch` の直前に、条件成立時または反復・分岐の目的コメントを補完。
- 単一行の複合条件を外側丸かっこ付きの形式へ統一。

## 変更例

### クラスJavadoc
```diff
  * <p>クラス名称： NFS属性メタデータ管理</p>
  *
+ * <p>著作権： Copyright (c) 2026 EnterOgawa. All Rights Reserved.</p>
+ *
+ * <p>更新履歴：
+ * <pre>
+ *     VerNo.         author        update     comment
+ *     Ver.01.00.00   Shunji Ogawa  2026/09/02 新規作成
+ * </pre>
+ * </p>
+ *
  * @author Shunji Ogawa
  * @version 01.00.00
```

### メソッド宣言
```diff
-	public synchronized int getMode(Path path, int defaultMode) {
+	public synchronized int getMode( Path path, int defaultMode) {
```

### boolean戻り値Javadoc
```diff
-	 * @return true:書込可 false:読込専用
+	 * @return	判定結果<ul>
+	 * 			false	: 読込専用<br>
+	 * 			true	: 書込可</ul>
```

### 制御文コメント
```diff
+        // 停止要求を受けるまでUDPパケット受信を継続する
         while( running) {
```

### 複合条件
```diff
-		if( value == null || value.isBlank()) {
+		if(( value == null || value.isBlank())) {
```

## 静的確認
- `@version` を持つ `src` 配下ファイルについて、`著作権` と `更新履歴` の不足がないことを確認。
- 引数付き宣言の `(` 直後スペース不足が残っていないことを確認。
- `@return true:... false:...` 形式が残っていないことを確認。
- `@param` / `@return` / `@throws` の説明開始位置の空白揺れが残っていないことを確認。
- `if` / `for` / `while` / `switch` の直前コメント不足が残っていないことを確認。
- 単一行の `if` / `else if` / `while` 複合条件について、外側丸かっこ不足が残っていないことを確認。
- `git diff -w --ignore-blank-lines -- src` で、コメント追加・Javadoc整形・複合条件の外側括弧追加以外の差分を確認。
- `git diff --check -- src` で、空白エラーがないことを確認。
- `C:\develop\tools\eclipse202503\eclipse\jdk-21.0.11+10\bin\javac.exe --release 21` で `src` 配下のコンパイルが通ることを確認。出力先は `work/tmp/coding-standard-classes-20260902_144916`。

## 未実施
- AGENTS.md の「環境依存があるため、実行は行わない」に従い、アプリ起動、サービス起動、NFS mount、単体テスト実行は行っていない。
