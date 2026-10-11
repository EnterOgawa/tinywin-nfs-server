# 第三者コンポーネント通知

TinyWinNFS Server は、バイナリ配布物に以下の第三者コンポーネントを含めます。
これらのコンポーネントには、それぞれのライセンスが適用されます。
使用版と取得元・SHA-256 は `dependencies/components.json`、実際のビルド情報は配布物の `build-information.json` を参照してください。

## Eclipse SWT

- コンポーネント: Windows x86_64 向け Standard Widget Toolkit
- 同梱ファイル: `app/org.eclipse.swt.win32.win32.x86_64.jar`
- ライセンス: Eclipse Public License 2.0
- ライセンス URL: https://www.eclipse.org/legal/epl-2.0/
- 同梱ライセンス本文: `licenses/EPL-2.0.txt`
- jar 内の `about.html` / `about_files` に含まれる第三者通知も保持します。

## WinSW

- コンポーネント: Windows Service Wrapper
- 使用形式: WinSW 2.12.0 の `NET461` 版。OS 側の .NET Framework 4.8 以降を使用し、.NET 6 runtime は再配布しません。
- 同梱ファイル: `service/winsw/nfs-server.exe`
- ライセンス: MIT License
- プロジェクト URL: https://github.com/winsw/winsw
- 同梱ライセンス本文と著作権・許諾通知: `licenses/WinSW-MIT.txt`
- WinSW 内包の log4net 2.0.12 は Apache-2.0、YamlDotNet 8.1.2 は MIT です。`licenses/log4net-Apache-2.0.txt`、`licenses/log4net-NOTICE.txt`、`licenses/YamlDotNet-MIT.txt` に通知を保持します。

## Java Runtime

- コンポーネント: `jpackage` で生成した Java runtime image
- 同梱ディレクトリ: `runtime`
- ライセンス: OpenJDK は GPL v2 with the Classpath Exception でライセンスされています。
- ライセンス URL: https://openjdk.org/legal/gplv2+ce.html

同梱 runtime 自体の法的通知は `runtime/legal` 配下に含まれます。
GPLv2 と Classpath Exception の本文は `licenses/OpenJDK-GPL-2.0-with-classpath-exception.txt` にも同梱します。

## 対応ソースの取得

インストーラーと同じ配布場所から `TinyWinNfsSources.zip` を取得してください。SWT の Java/native ソース、WinSW ソース、runtime の完全な OpenJDK ソースとビルドスクリプト、版・取得元・ハッシュを含みます。
本製品の GitHub 配布では同じリリースの asset として提供します。app image を直接配布する場合は `sources` フォルダに同じ内容を含めます。
詳細は [ソース配布手順](docs/source_distribution.md) を参照してください。ライセンス通知は対応ソースの提供に置き換わるものではありません。
