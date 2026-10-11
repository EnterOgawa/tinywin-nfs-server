# OSS 公開状態の確認

## 確認対象

- 確認日: 2026-10-11
- 公開リポジトリ: https://github.com/EnterOgawa/tinywin-nfs-server
- 公開 main: `4e111143077bddcdc0284a74aa90e5bb5a4bb075`
- 最新公開リリース: `v2.1.0`
- ローカル作業場所: `D:\develop\nfs`
- 確認方法: ソース、設定、文書、既存配布物の読み取りと GitHub API の照会

ソースや公開設定を変更せず、アプリケーション、サーバー、サービス、テスト、インストーラーは実行していません。署名状態とハッシュの照会は実施しました。

## 優先対応

| 優先度 | 項目 | 確認結果 |
|---|---|---|
| 高・要確認 | 参考サンプルの公開許諾 | 別製品の実コードと会社名義の著作権表示が公開されています。 |
| 高 | 第三者ライセンスの配布 | WinSW の MIT ライセンス本文を取得・同梱する処理がありません。SWT と runtime の対応ソース提供方法も整理が必要です。 |
| 高 | 第三者によるビルド | JDK と SWT のパス、および Eclipse の JRE 名が開発者の環境に固定されています。 |
| 中 | クリーンな取得後のパッケージ作成 | WinSW がない場合、同梱していないダウンロードスクリプトを呼び出します。 |
| 中 | 脆弱性の報告窓口 | `SECURITY.md` がなく、GitHub の非公開脆弱性報告も無効です。 |
| 中 | 自動検証とテスト分類 | CI がなく、「単体テスト」に実サーバー起動を伴う通信テストが含まれています。 |
| 中 | 同梱依存関係の管理 | 配布物の runtime は `21.0.8`。コンポーネント一覧、更新方針、取得時の期待ハッシュ照合が未整備です。 |
| 中 | 外部からの参加手順 | `CONTRIBUTING.md`、issue テンプレート、PR テンプレートがありません。 |
| 低 | 対応環境と公開情報 | 対応 Windows のバージョン・CPU・検証環境、保守方針、リポジトリ説明と topics が不足しています。 |

## 1. 参考サンプルの公開許諾

根拠: `sample/AplMain.java:1`、`sample/AplMain.java:102`。

このファイルは公開リポジトリに含まれており、`jp.co.daifuku.jcsim2` の実コードと `EnterSystem CO.,LTD.` の著作権表示を含みます。約 100 KB の別製品コードであり、コメント書式だけの見本ではありません。

公開許諾や Apache-2.0 の適用権限があるかは、リポジトリの情報だけでは判断できません。無断公開と断定するものではありませんが、公開可能な範囲を確認する必要があります。

対応候補は、許諾と適用ライセンスを明確にすること、または業務実装を含まない最小限の書式サンプルへ置き換えることです。公開範囲に問題が判明した場合、現在のファイルだけでなく過去のコミット・タグに残る内容の扱いも検討対象です。

公開ファイル: [sample/AplMain.java](https://github.com/EnterOgawa/tinywin-nfs-server/blob/main/sample/AplMain.java)

## 2. 第三者ライセンスの配布

根拠: `THIRD_PARTY_NOTICES.md:13`、`scripts/package-manager.ps1:88`、`scripts/package-manager.ps1:89`。

WinSW の通知はライセンス名とプロジェクト URL のみで、上流の著作権・許諾本文を含んでいません。既存 app image のルートには本製品の Apache-2.0 の `LICENSE` と `THIRD_PARTY_NOTICES.md` があり、WinSW フォルダには exe と XML のみが含まれています。現在のパッケージ手順も WinSW の `LICENSE.txt` を取得・同梱しません。

WinSW の MIT ライセンスは、著作権と許諾の通知をコピーに含める条件を定めています。使用バージョンの本文を同梱し、通知から参照できるようにすることを優先します。[WinSW v2.12.0 のライセンス](https://raw.githubusercontent.com/winsw/winsw/v2.12.0/LICENSE.txt)

SWT jar 内には `about.html` と `about_files` の第三者通知が保存されており、runtime にも `runtime/legal` の通知が保存されています。この点は確認済みです。一方、配布文書には対応する SWT ソースの具体的な取得手順や、同梱 runtime の配布元・対応ソースの提供方法がありません。jar 内の通知も考慮し、EPL と GPL の各配布条件を満たす提供方法を明確にする必要があります。[EPL-2.0 第3節](https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.txt)、[OpenJDK の GPLv2 と Classpath Exception](https://raw.githubusercontent.com/openjdk/jdk21u/master/LICENSE)

## 3. 第三者によるビルド

根拠: `scripts/compile.ps1:4`、`scripts/test.ps1:4`、`scripts/package-manager.ps1:4`、`.classpath:5`、`.classpath:6`、`README.md:74`。

JDK は `C:\develop\tools\eclipse202503\eclipse\jdk-21.0.8+9`、SWT は同じ Eclipse の plugins 配下に固定されています。Eclipse 定義の JRE も `jdk-21.0.8+9` という環境固有の登録名です。README のコマンドを実行するための依存取得と設定手順が不足しています。AGENTS.md の指定 JDK `21.0.11+10` とスクリプトの JDK 指定にも差があります。

公開用のビルド手順は、Java 21 の要件を保ったまま、JDK と SWT の場所を引数・環境変数などで指定可能にし、使用バージョンと取得元を文書化する必要があります。ローカルのツールを移動する必要はありません。Eclipse での通常 Java プロジェクト運用も維持できます。

## 4. クリーンな取得後のパッケージ作成

根拠: `scripts/package-manager.ps1:92`、`scripts/package-manager.ps1:124`、`scripts/package-manager.ps1:127`、`.gitignore:7`。

`service/winsw/nfs-server.exe` は Git 管理対象外です。WinSW がない場合、パッケージ先の `scripts/download-winsw.ps1` を実行しようとしますが、`$runtimeScripts` のコピー対象にそのスクリプトがありません。そのため、ツール設定を整えても、WinSW を別途用意していない新規取得環境ではこの分岐で失敗する構造です。

パッケージ作成前にリポジトリ側の取得処理を行うか、呼び出すスクリプトを実際に配置し、初回取得からの手順を確認する必要があります。今回の判断はソース確認によるもので、パッケージ処理は実行していません。

## 5. 脆弱性の報告窓口

根拠: 公開ルートのファイル一覧、GitHub `private-vulnerability-reporting` API。

`docs/security_model.md` は運用上の境界と制限を説明していますが、脆弱性の非公開報告手順は記載していません。`SECURITY.md` はなく、GitHub の非公開脆弱性報告は `enabled=false` です。

対応対象バージョンと報告方法を `SECURITY.md` に記載し、非公開報告を利用できる状態にすることを推奨します。[GitHub の非公開脆弱性報告](https://docs.github.com/en/code-security/how-tos/report-and-fix-vulnerabilities/configure-vulnerability-reporting/configure-for-a-repository)

GitHub の secret scanning と push protection は有効でした。これらが未整備という判断ではありません。

## 6. 自動検証とテスト分類

根拠: GitHub Actions API の workflow 数 `0`、`README.md:88`、`test/jp/co/enterogawa/nfs/AllTests.java:93`、`test/jp/co/enterogawa/nfs/AllTests.java:216`。

テストソースは存在しますが、公開リポジトリに CI がなく、PR や変更ごとに検証結果を共有する仕組みがありません。

また、`scripts/test.ps1` が実行する `AllTests` は `TCP RPC transport` を必ず実行し、このテスト内で NFS サーバーを起動します。高番ポート `11111` / `12049` / `12048` を使っていますが、サーバー起動を伴わない純粋な単体テストではありません。

サーバーを起動しないテストと通信テストを分離し、必要な前提条件を README と貢献手順に記載したうえで、Windows 上のビルドと単体テストを CI 化することを推奨します。QNX 実機・Windows NFS mount・署名・公開リリースは引き続き明示的な確認手順として扱えます。

## 7. 同梱依存関係の管理

根拠: `dist/TinyWinNfsManager/runtime/release`、`scripts/download-winsw.ps1:6`、`scripts/download-winsw.ps1:13`、`THIRD_PARTY_NOTICES.md`。

公開インストーラーとハッシュが一致するローカル配布物の runtime は `JAVA_VERSION="21.0.8"` です。SWT jar のバージョンは `3.129.0.v20250221-1734`、WinSW の取得指定は `v2.12.0` です。

通知文書にコンポーネントの完全なバージョン・取得元・対応ソースがまとまっておらず、依存関係を定期確認する方針もありません。WinSW の取得処理は取得後の SHA-256 を表示しますが、期待するハッシュとの一致を検証しません。

コンポーネント一覧または SBOM、依存関係の更新方針、取得時のハッシュ照合、更新後の互換性回帰手順を整備すると保守しやすくなります。今回、既知 CVE の網羅的な照合や、最新 runtime への更新可否の検証は実施していません。

## 8. 外部からの参加手順

根拠: 公開ルートのファイル一覧、GitHub community profile。

`CONTRIBUTING.md`、issue テンプレート、PR テンプレートがありません。AGENTS.md はローカル作業環境とエージェント向けの規則であり、外部開発者が必要とする依存取得・テスト・変更提案の説明が不足しています。

Java 21 と Eclipse の設定、コーディング規約、テスト分類、PR 作成手順、ライセンス上の貢献の扱いを日本語で案内することを推奨します。不具合報告では、OS・CPU、製品バージョン、NFS クライアント、UDP/TCP、再現手順、期待結果、機密情報を除いたログを収集できるテンプレートが有用です。

## 9. 対応環境と公開情報

根拠: `README.md:15`、`installer/tinywin-nfs-server.iss:26`、`docs/support_scope.md:5`、GitHub repository API。

README の OS 表記は `Windows Pro / Windows Server 系` のみです。使用する SWT は Windows x86_64 用で、インストーラーのアーキテクチャ指定は `x64compatible` ですが、どの Windows バージョン・CPU・ファイルシステムで検証したかが明確ではありません。未検証の環境は未検証として示すと判断しやすくなります。

機能のサポート範囲はありますが、対応する保守バージョン、問い合わせ窓口、ベストエフォートの保守かどうかは明記されていません。`正式版で保証する範囲` は、検証済み機能の意味と、保守・保証の意味を明確にすると誤解を避けられます。

GitHub のリポジトリ説明は空欄、topics は空です。目的の短い説明と NFS・Windows・Java などの topics を設定すると発見しやすくなります。行動規範、README の英語概要、ソースの SPDX 識別子は追加候補ですが、これらがないこと自体で OSS ではなくなるものではありません。

## 整備済みの事項

- 公開リポジトリで Apache-2.0 が認識されています。
- README、CHANGELOG、画面画像、設定表、導入手順があります。
- サポート範囲、既知制限、セキュリティ境界、更新・アンインストール手順があります。
- 公開リリースにインストーラーと SHA-256 が掲載されています。
- ローカルのインストーラー SHA-256 は公開 asset と一致しました: `CC6831FEC2B7B678037F04C3EB5D5C747B109D8C63514C0475C5A1C550FFA0CF`。
- 同じローカルインストーラーの Authenticode 状態は `Valid`、署名者は `ENTER SYSTEM, K.K.` でした。
- SWT の jar 内通知と runtime の `legal` ディレクトリは保持されています。
- GitHub secret scanning と push protection は有効です。

公開リリース: [v2.1.0](https://github.com/EnterOgawa/tinywin-nfs-server/releases/tag/v2.1.0)

## 確認の範囲

今回の確認は OSS の公開・配布・参加・保守に関する確認です。著作権者の許諾、全依存関係のライセンス適合、プロトコルの脆弱性、Git 全履歴の秘密情報、全 OS での動作を網羅的に検証したものではありません。許諾に関する指摘と、ソースから確定できる不具合・未整備事項は区別しています。

既存の AGENTS.md 変更と作業フォルダ移行記録を維持し、新たにこの解析記録のみ追加しました。issue 登録、公開設定の変更、コミット、push、release 操作は行っていません。
