# CI の Temurin バージョン表記修正

## 原因

コミット `01d3f41` の [GitHub Actions](https://github.com/EnterOgawa/tinywin-nfs-server/actions/runs/38110100719) が、Java の取得段階で失敗した。

`actions/setup-java` が取得した利用可能版は `21.0.11+10.0.LTS` と表記されており、設定した `21.0.11+10` に一致しなかった。JDK の更新版・ビルド番号は変更せず、同じ Java 21.0.11 build 10 の登録名を指定する。

## 変更箇所

- ファイル: `.github/workflows/ci.yml`
- メソッド: なし（workflow の `Java 21 を設定` ステップ）
- 行番号: 31

```diff
-          java-version: '21.0.11+10'
+          java-version: '21.0.11+10.0.LTS'
```

## 確認方針

- workflow の構文と差分を確認する。
- 修正コミットを `main` に push し、Java の取得・依存検証・サーバーを起動しない Unit テストが完了することを確認する。
- ローカル指定 JDK、配布 runtime の設定、製品ソースは変更しない。
- tag、Release、インストーラー署名、配布 asset の公開は行わない。
