# OSS 公開整備の修正

## 対象と方針

OSS 公開状態の確認記録 `work/analysis/20261011_085327_oss_review/report.md` に基づいて、公開する書式見本、ビルド・配布、ライセンス、外部からの参加と保守を整備しました。
NFS 本体の `src` は変更していません。既存の作業フォルダ移行に関する AGENTS.md の変更と記録を維持しています。

## 主な変更対象

行番号は修正後のファイルを基準とします。

| ファイル | メソッド・対象 | 行番号 | 変更 |
|---|---|---:|---|
| `sample/AplMain.java` | `AplMain` / `formatName` | 30 / 43 | 業務実装を含まない書式見本へ置換。Javadoc の不正な HTML 構造と既定コンストラクタ警告も解消 |
| `test/jp/co/enterogawa/nfs/AllTests.java` | `main` / `run` | 76 / 96 | Unit、Transport、All の選択。既定はサーバーを起動しない19件 |
| `scripts/build-environment.ps1` | `Get-TinyWinNfsBuildEnvironment` | 29 | Java 21 と SWT の共通解決、指定引数・環境変数の利用 |
| `scripts/build-environment.ps1` | `Assert-TinyWinNfsWorkspacePath` | 7 | 出力先とリンクを確認して作業フォルダ外への書き込みを防止 |
| `scripts/build-environment.ps1` | `Get-TinyWinNfsPinnedArtifact` | 104 | 固定ハッシュ検証と一時取得後の配置。不一致のファイルを採用しない |
| `scripts/prepare-dependencies.ps1` | スクリプト本体 | 1 | SWT と任意指定の WinSW の取得 |
| `scripts/compile.ps1` / `scripts/test.ps1` | スクリプト本体 | 1 | 共通設定とテスト種別を利用 |
| `scripts/prepare-release-sources.ps1` | スクリプト本体 | 1 | 配布 JDK と対応ソースの版照合、完全な対応ソース ZIP の生成 |
| `scripts/package-manager.ps1` | スクリプト本体 | 1 | 依存・ソース取得、ライセンスとビルド情報の同梱、WinSW 初回取得の不具合修正 |
| `scripts/package-installer.ps1` | スクリプト本体 | 1 | JDK・SWT・ISCC の指定、コンパイラー終了コード確認、対応ソースの案内 |
| `scripts/download-winsw.ps1` | スクリプト本体 | 1 | 期待 SHA-256 との照合 |
| `scripts/benchmark-local-rpc.ps1` / `scripts/smoke-service.ps1` | スクリプト本体 | 1 | 旧固定 JDK パスを共通設定へ変更 |
| `scripts/start-wsl-test-server.ps1` / `scripts/test-windows-nfs-client.ps1` | スクリプト本体 | 1 | 旧固定 JDK パスを共通設定へ変更 |
| `scripts/test-windows-nfs-client-matrix.ps1` | ファイルの文字コード | 1 | UTF-8 BOM の付与のみ。処理内容の変更なし |
| `.classpath` / `.project` / `.gitignore` | プロジェクト定義 | 1 | 相対 SWT 参照、JavaSE-21、製品説明、取得 jar の除外 |
| `dependencies/components.json` | コンポーネント定義 | 1 | 版、取得元、ハッシュ、対応ソース、内包ライブラリを一覧化 |
| `installer/tinywin-nfs-server.iss` | `InitializeSetup` / 配布ファイル定義 | 79 / 43 | .NET Framework 4.8 以降の前提確認、ライセンス・コンポーネント・ビルド情報を同梱 |
| `service/winsw/nfs-server.xml` | 開発用 executable | 5 | システム環境変数 `TINYWIN_NFS_JDK` を参照。配布用 XML の runtime 参照は維持 |
| `.github/workflows/ci.yml` | Unit job | 1 | Windows / Java 21、固定 Actions コミット、読込権限のみ。署名・公開なし |
| `.github/ISSUE_TEMPLATE/bug_report.yml` / `feature_request.yml` / `config.yml` | 報告テンプレート | 1 | 日本語の不具合・提案・セキュリティ案内 |
| `.github/pull_request_template.md` | PR テンプレート | 1 | 確認結果と互換性への影響を記録 |

文書変更の対象は `README.md`、`CHANGELOG.md`、`CONTRIBUTING.md`、`SECURITY.md`、`THIRD_PARTY_NOTICES.md`、`licenses/README.md`、`docs/support_scope.md`、`docs/security_model.md`、`docs/windows_service.md`、`docs/installer.md`、`docs/source_distribution.md`、`docs/dependency_maintenance.md`、`docs/maintainer_guide.md` です。メソッドは該当せず、各ファイルの第1行以降を対象とします。
上流のライセンス本文と NOTICE は `licenses` に原文のまま保存します。

## Java 変更の抜粋

`sample/AplMain.java` の公開範囲を変更します。旧実装の全文は再掲載せず、業務実装を含まない見本へ置き換えます。

```diff
-package jp.co.daifuku.jcsim2;
+package jp.co.enterogawa.nfs.sample;
+	public static String formatName( String name) {
+		// 名前が指定されていない場合
+		if( name == null) {
+			return "" ;
+		}
+
+		return name.trim() ;
+	}
```

`AllTests.main` はテスト種別を解釈します。

```diff
-		new AllTests().run() ;
+		String suite = args.length == 0 ? "unit" : args[0] ;
+		// テスト種別が不正な場合
+		if(( args.length > 1 || !List.of( "unit", "transport", "all").contains( suite))) {
+			throw new IllegalArgumentException( "Expected unit, transport, or all") ;
+		}
+
+		new AllTests().run( suite) ;
```

`AllTests.run` の通常確認では通信テストを起動しません。Transport 指定では通信テストのみ、All 指定では従来の20件を実行します。

```diff
-		runTest( "TCP RPC transport", this::testTcpRpcTransport) ;
+		// 通信テストを含めて実行する場合
+		if( "all".equals( suite)) {
+			runTest( "TCP RPC transport", this::testTcpRpcTransport) ;
+		}
```

## ビルドと配布

- ビルド用 JDK は引数、`TINYWIN_NFS_JDK`、`JAVA_HOME`、既存の指定 JDK、PATH の順で解決します。C ドライブのツールは移動・変更していません。
- Eclipse は JavaSE-21 と `lib` の SWT を使用します。取得する SWT は既存の Eclipse の jar と同じ SHA-256 です。
- 通常のビルドは Java 21 を対象とし、配布用 runtime は対応ソースが固定できる Temurin 21.0.11+10 Windows x64 に限定します。
- WinSW は同じ 2.12.0 の NET461 版へ切り替えます。旧 x64 版に内包されていたサポート終了済み .NET 6 を再配布せず、OS 側の .NET Framework 4.8 以降を使用します。
- セットアップは .NET Framework の前提を最初に確認し、未導入時に既存サービスを停止しません。
- WinSW 内包の log4net / YamlDotNet の通知・ソースも追加します。
- 対応ソース ZIP は Java/native の第三者ソース、runtime のビルドスクリプト、本製品のソースと再ビルド入力を含みます。実際の共有データ、可変データ、ログ、過去の業務サンプルを含めません。
- インストーラーと対応ソース ZIP は同じ配布場所へ併載します。app image では `sources` を維持します。
- PowerShell の日本語を含むファイルは、5.1 との互換性のため UTF-8 BOM 付きにしました。

## 確認結果

- 指定の Java 21.0.11+10 でコンパイル成功。
- PowerShell 7 と Windows PowerShell 5.1 の両方で Unit 19件が成功。TCP 通信テストは実行していません。
- ソース ZIP を新規フォルダへ展開し、依存取得から Unit 19件まで成功。Eclipse の個別インストール先に依存しないことを確認しました。
- PowerShell 5.1 と 7 の構文解析、JSON/XML 読み取り、actionlint による CI 確認が成功。
- 書式見本は `javadoc -Xdoclint:all -Werror` が警告なしで成功。
- Inno Setup の実際の `[Code]` を検証用設定へ取り込み、ISCC でコード構文のコンパイルが成功。生成した検証用 exe は実行していません。
- 指定外の出力先と、期待 SHA-256 に一致しない取得ファイルを拒否することを確認。
- 対応ソース ZIP の生成と、固定した7つのソースアーカイブのハッシュを確認。
- NFS 本体の `src` に差分がないことを確認。
- 既存の署名済みインストーラー SHA-256 は `CC6831FEC2B7B678037F04C3EB5D5C747B109D8C63514C0475C5A1C550FFA0CF` を維持。

## 公開・実環境確認の残事項

- GitHub の description、topics、非公開脆弱性報告は、明示的なリモート反映指示まで変更していません。設定案と反映手順を `docs/maintainer_guide.md` に記録しています。
- 過去の業務サンプルは Git の履歴・既存タグ・現在の公開 main に残ります。ローカルの置き換えだけでは過去の公開を取り消せません。許諾確認と、必要な場合の履歴変更は別途判断が必要です。
- 新しい管理ツール・正式インストーラー全体の再作成と実行、WinSW NET461 版でのサービス導入・停止・再起動、実クライアントの mount は行っていません。これらはユーザーの明示的な実行指示と動作確認を経て行います。
- CI は定義の検証までで、GitHub 上の実行は反映後に確認します。
- コミット、push、tag、release 操作と署名処理は行っていません。
