# 開発への参加

不具合報告、機能提案、ドキュメント修正、PR を受け付けます。議論では相手を尊重し、再現条件と技術的な理由を示してください。
issue、PR、コメントは原則として日本語で記載します。変更の採用や応答期限は保証しません。

## 開発環境

| 項目 | 要件 |
|---|---|
| OS・CPU | Windows x64 |
| JDK | Java 21。`TINYWIN_NFS_JDK`、`JAVA_HOME`、または `-JdkPath` で指定 |
| Eclipse | JavaSE-21 を扱える通常の Java プロジェクト |
| SWT | `dependencies/components.json` に固定した Windows x86_64 用 jar |
| PowerShell | Windows PowerShell 5.1 または PowerShell 7 |
| Inno Setup | インストーラーを作成する場合のみ。現行 `.iss` の構文を扱える Inno Setup 6 |

Inno Setup の場所は `TINYWIN_NFS_ISCC` または `package-installer.ps1 -IsccPath` で指定できます。標準の Program Files 配下も自動検出します。

リポジトリを取得し、依存ファイルを用意します。

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-21'
.\scripts\prepare-dependencies.ps1
.\scripts\compile.ps1
.\scripts\test.ps1 -Suite Unit
```

取得先と SHA-256 は `dependencies/components.json` で管理します。SWT を既に保有している場合は `TINYWIN_NFS_SWT_JAR` または `-SwtJarPath` で指定できますが、固定版のハッシュと一致する必要があります。
Eclipse では依存取得後に `Existing Projects into Workspace` で取り込み、JavaSE-21 に JDK を割り当ててください。

## テストの分類

| コマンド | 内容 | 前提 |
|---|---|---|
| `scripts/test.ps1` または `-Suite Unit` | サーバーを起動しない19件の回帰確認 | 一時ファイルを `work/tmp` に作成 |
| `scripts/test.ps1 -Suite Transport` | TCP 通信の1件の確認 | 高番ポートで NFS サーバーを起動 |
| `scripts/test.ps1 -Suite All` | 上記20件 | 通信テストの前提が必要 |
| サービス・Windows NFS・QNX・WSL の確認 | 実環境の結合確認 | 手動実施。権限・接続先・共有データを事前確認 |

CI は Windows 上で `Unit` だけを実行します。通信テスト、サービス操作、mount、長時間負荷確認は自動で追加しないでください。
このプロジェクトでエージェントに作業を依頼する場合、サーバー・サービス起動と mount は明示的な実行指示がある場合のみ実施します。

## コードと文書

- 本体は `src`、テストは `test` に置き、Eclipse RCP / PDE / OSGi 定義を追加しません。
- `sample/AplMain.java` を Javadoc とコメント書式の見本にしてください。この見本は業務コードを含みません。
- `if` の条件説明、`for` の処理意図、閉じかっこの後の空行は、以下のコーディング規約に従ってください。
- 不具合修正は再現条件と必要な回帰確認を示し、NFS エラー、属性、ファイル内容、export 境界への影響を確認してください。
- `docs` は日本語で記載し、設定キー・API・ログなど正確さのために必要な用語は英語を維持します。
- 日本語を含む PowerShell ファイルは Windows PowerShell 5.1 でも読める UTF-8 BOM 付きで保存します。Java は UTF-8 で保存します。
- 修正提案は `work/proposals/YYYYMMDD_HHMMSS_説明.md`、解析結果は `work/analysis` の個別フォルダに保存します。これらはローカルの作業記録として扱い、Git や PR へファイルを追加しないでください。
- 公開許諾のない業務コード、機密情報、個人の作業パス、生成バイナリを追加しないでください。

### コーディング規約

1. `if` 文には、条件の意味が分かるコメントを必ず付けてください。
2. `for` 文の前には、処理意図が分かるコメントを必ず付けてください。
3. `if` や `for` の閉じかっこの後には、1 行の空白行を入れてから次のソースを記述してください。
4. `if` と `else` の間には空白行は不要です。
5. `if` の直後が `//else` コメントの場合も、空白行は不要です。
6. `if` と `else` の間にコメントがある場合も、空白行は不要です。
7. `try` と `catch` の間には、コメントがある場合を含めて空白行は不要です。
8. `try` の閉じかっこの後には空白行は不要です。
9. `switch` 文では、次の `case` の上にある `break` の直後に1 行の空白行を入れてください。

## 公開するファイル

GitHub には、ビルド・テスト・配布に必要なソース、設定、スクリプト、画像、CI と、README、ライセンス、利用・開発ガイドを公開します。

`work/` 配下の作業記録・解析結果・修正提案・一時ファイルと、ローカル作業用の `AGENTS.md` は公開対象外です。`.gitignore` で除外しているため、強制的に Git へ追加しないでください。必要な変更目的や確認結果は PR 本文へ記載してください。
テストやビルドで使う `work/` 配下の出力先は、各スクリプトが必要に応じて作成します。リポジトリに作業フォルダの内容を含める必要はありません。

## 提案と PR

不具合報告では OS、CPU、製品・クライアントの版、NFS の版、UDP/TCP、再現手順、期待結果と実際の結果を記載してください。
ログや診断 ZIP の内容を確認し、ユーザー名、社内アドレス、実パス、ファイル内容を取り除いてください。脆弱性の詳細は公開 issue に記載せず [SECURITY.md](SECURITY.md) に従ってください。

PR では変更目的、関連 issue、確認結果、未実施事項、互換性への影響を記載してください。
貢献者は提出内容を公開・ライセンスできる権限を持つ必要があります。別途明示した合意がない限り、本プロジェクトに取り込むために提出する貢献は Apache-2.0 の条件で扱います。第三者のコードやライセンス表示は保持してください。

## 配布物の変更

依存関係の変更では、版、取得元、期待ハッシュ、ライセンスと対応ソースをまとめて更新します。
署名はメンテナーが手動で実施します。CI に署名や自動リリースを追加しないでください。公開手順は [メンテナー向け手順](docs/maintainer_guide.md) を参照してください。
