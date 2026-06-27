# 第三者コンポーネント通知

TinyWinNFS Server は、バイナリ配布物に以下の第三者コンポーネントを含める、または含める可能性があります。
これらのコンポーネントには、それぞれのライセンスが適用されます。

## Eclipse SWT

- コンポーネント: Windows x86_64 向け Standard Widget Toolkit
- 同梱ファイル: `app/org.eclipse.swt.win32.win32.x86_64.jar`
- ライセンス: Eclipse Public License 2.0
- ライセンス URL: https://www.eclipse.org/legal/epl-2.0/

## WinSW

- コンポーネント: Windows Service Wrapper
- 同梱ファイル: `service/winsw/nfs-server.exe`
- ライセンス: MIT License
- プロジェクト URL: https://github.com/winsw/winsw

## Java Runtime

- コンポーネント: `jpackage` で生成した Java runtime image
- 同梱ディレクトリ: `runtime`
- ライセンス: OpenJDK は GPL v2 with the Classpath Exception でライセンスされています。
- ライセンス URL: https://openjdk.org/legal/gplv2+ce.html

同梱 runtime 自体の法的通知は `runtime/legal` 配下に含まれます。
