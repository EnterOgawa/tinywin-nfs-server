# 第三者コンポーネントの対応ソース配布

次の配布物から、インストーラーと同じ配布場所に `TinyWinNfsSources.zip` を併載します。利用者は runtime をインストールせず、この ZIP を取得して対応ソースを閲覧できます。
既存の公開リリースにこの ZIP が自動で追加されるわけではありません。メンテナーが公開内容を確認して併載します。

## 内容

| ディレクトリ | 内容 |
|---|---|
| `swt` | 固定版の Java ソース jar と JNI を含む SWT の完全なリポジトリソース |
| `winsw` | WinSW と内包する log4net・YamlDotNet の対応ソース |
| `runtime` | 使用する Temurin 版の OpenJDK 完全ソースと対応する Temurin ビルドスクリプト |
| `licenses` / `runtime-legal` | ライセンスと第三者通知 |
| `src` / `test` / `scripts` | 本製品のソース、テスト、runtime image を生成するスクリプト |
| `assets` / `conf` / `installer` / `launcher` / `service` | 再ビルドに必要な固定の入力。共有内の実データやログは含めません |
| `components.json` | 版・取得元・各ソースアーカイブの SHA-256 |
| `jdk-release.txt` | 使用 JDK の配布元・版・ソースコミット情報 |
| `SOURCES.md` | このソース配布手順。`README.md` は本製品の案内 |

SWT の jar と native ソースは同じリリースのビルドに対応します。runtime の配布では、Java 21 という主バージョンだけでなく、配布元・パッチ版・ビルド番号を一致させます。
現行の配布用 JDK は `dependencies/components.json` に指定する Windows x64 向け Eclipse Temurin 21.0.11+10 です。異なる JDK での通常ビルドは可能ですが、配布用 runtime に採用する場合は対応ソースと版情報も更新してください。

## 作成

```powershell
.\scripts\prepare-release-sources.ps1 -JdkPath 'C:\path\to\jdk-21.0.11+10'
```

取得ファイルは `work/tmp/dependencies` にキャッシュし、SHA-256 を検証します。初回は OpenJDK のソース約115 MBを含むダウンロードが発生します。
`scripts/package-manager.ps1` と `scripts/package-installer.ps1` もこの処理を呼び出します。JDK と対応ソースの組み合わせが不一致の場合、配布物の作成を停止します。

生成先は `dist/installer/TinyWinNfsSources.zip` です。app image の `sources` にも同じ内容を含めるため、app image を配布する場合はそのフォルダも維持してください。
インストーラーにはソース全体を埋め込まず、ライセンスと取得案内を同梱します。インストーラーを公開・ミラー配布する場合、対応ソース ZIP を同じ場所で同等に取得できる状態にしてください。

ZIP 内の archive は、その名前に応じて ZIP、jar、tar.gz として展開できます。Windows では `tar -xf` など、対応する展開ツールを使用してください。
本製品自身のソースは GitHub の対応リリースタグから取得できます。

## 再ビルド情報

runtime は指定の JDK の `jpackage --type app-image` で生成し、サービス用の `java.exe` を同じ JDK からコピーします。
実際の引数とリソース配置は ZIP 内の `scripts/package-manager.ps1`、元の OpenJDK ビルド手順は `runtime` のソースと Temurin ビルドスクリプトを参照してください。
`runtime/legal` や SWT jar 内の通知を削除しないでください。ライセンス本文の参照だけで対応ソースの提供を省略しないでください。
