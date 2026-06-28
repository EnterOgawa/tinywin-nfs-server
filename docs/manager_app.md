# 管理ツール

`TinyWinNfsManager` は、インストールと設定を行うための SWT 製ダブルクリック起動ツールです。

## アプリイメージのビルド

```powershell
.\scripts\package-manager.ps1
```

出力先:

```text
dist\TinyWinNfsManager
```

## インストーラーのビルド

```powershell
.\scripts\package-installer.ps1
```

出力先:

```text
dist\installer\TinyWinNfsSetup.exe
```

## 起動

通常の状態表示と設定編集:

```text
dist\TinyWinNfsManager\TinyWinNfsManager.exe
```

サービスのインストール、アンインストール、開始、停止、ファイアウォール変更、権限が必要なスモークテスト:

```text
dist\TinyWinNfsManager\TinyWinNfsManager-Admin.cmd
```

インストール後のスタートメニューとデスクトップショートカットは、`TinyWinNfsManager.exe` を直接参照します。
インストーラーは、インストール済み管理ツール実行ファイルを Windows の `RUNASADMIN` 互換設定へ登録します。これにより、GUI はコマンドプロンプトなしで UAC 経由により開きます。

## 配布レイアウト

```text
TinyWinNfsManager/
  TinyWinNfsManager.exe
  TinyWinNfsManager-Admin.cmd
  runtime/
  app/tinywin-nfs-server.jar
  app/org.eclipse.swt.win32.win32.x86_64.jar
  assets/tinywin-nfs-server.png
  service/winsw/nfs-server.exe
  service/winsw/nfs-server.xml
  defaults/conf/nfs-server.properties
  export/
  scripts/
  docs/
```

インストール後の可変データは、アプリケーションディレクトリの外へ保存します。

```text
C:\ProgramData\EnterOgawa\TinyWinNFS Server\
  conf/nfs-server.properties
  export/
  logs/nfs-server.log
```

サービス実行ファイルは WinSW のままです。管理ツールは、ユーザーがコマンドライン操作を意識しなくてよいように隠蔽します。

`runtime` は管理ツールと WinSW サービスプロセスの両方で使用します。
SWT jar は管理ツールのみで使用します。

`TinyWinNfsServer`、`OgawaNfsServer`、旧 `QnxNfsServer` のいずれかがすでにインストールされている場合、管理ツールはインストール済みサービスのパスをアプリケーションルートとして扱い、`ProgramData` をデータルートとして扱います。
これにより、Windows サービスが別の `nfs-server.properties` を読んでいる状態で、コピーされたアプリイメージ側の設定を誤って編集することを防ぎます。

## 共有タブ

共有タブでは、サーバー側の共有フォルダを管理します。

各共有フォルダには以下を設定します。

- export 名。例: `/export`
- Windows フォルダパス
- 書込可否
- 許可クライアント。完全一致の IPv4 アドレスをカンマ区切りで指定

複数の共有フォルダは `追加`、`反映`、`削除` で管理します。
選択中の共有フォルダを使って、クライアント側の mount コマンドを生成します。

```text
mount_nfs <server-host>:<server-mount-name> <client-mount-point>
```

サーバー側 mount 名は NFS export 名です。例: `/export`、`/work`
クライアント側 mount point は NFS クライアント上のローカルディレクトリです。例: `/mnt`

## オプションタブ

オプションタブでは、詳細設定を管理します。

- 表示言語
- コマンド生成に使うサーバーホストとクライアント側 mount point
- UDP/TCP の両方で使う Portmap、NFS、MOUNT ポート
- UID/GID、ファイル mode、ディレクトリ mode、ブロックサイズ、read size、同期書込、書込キャッシュ、ファイル名文字コード
- NFSv3 `FSINFO` / `PATHCONF` で返す write size、ディレクトリ推奨サイズ、最大ファイルサイズ、時刻精度、PATHCONF 上限値

`permission.identity=auto` が既定です。管理ツールには詳細設定の予備値として UID/GID を残しています。
自動モードでは、サーバーは現在の NFS クライアントが送信した AUTH_SYS UID/GID をファイル属性に返します。
クライアントに関係なく設定済み UID/GID を必ず返したい場合のみ、`permission.identity=fixed` を使用します。

`filename.charset` は、NFSv2 のファイル名と symlink パス文字列のデコード/エンコード方法を制御します。
既定値は `UTF-8` です。古いクライアントが必要とする場合のみ、`windows-31j` などの Java Charset 名を指定します。

コピー性能を優先するため、`write.sync=false` が既定です。すべての WRITE 応答で物理ストレージ同期を待つ必要がある場合は `write.sync=true` にします。

大量コピー時の繰り返し open/close 負荷を減らすため、`write.cache.enabled=true` が既定です。開いたファイルハンドル数をより厳しく制限する必要がある場合のみ、`write.cache.max.open` と `write.cache.idle.millis` を調整します。

## 言語

管理ツールは英語と日本語の UI リソースに対応しています。

言語設定は `C:\ProgramData\EnterOgawa\TinyWinNFS Server\conf\nfs-server.properties` に保存されます。

```text
ui.language=auto
```

対応値:

- `auto`: Windows/JVM の locale を使用
- `en`: 英語
- `ja`: 日本語

言語を変更した後は、設定を保存して管理ツールを開き直します。

## export フォルダ

クライアント側の mount コマンドでは、Windows フォルダパスを直接指定しません。
サーバー側 export 名を mount します。

```text
mount_nfs <server-host>:/export /mnt
```

共有フォルダを変更した後は、`保存して再起動` を使用するか、`サービス` タブからサービスを再起動します。
サービスは起動時にのみ `C:\ProgramData\EnterOgawa\TinyWinNFS Server\conf\nfs-server.properties` を読み込みます。

## クライアント制限

すべてのクライアントを許可する場合は、`許可クライアント` を空欄にします。

特定ホストだけに export を制限する場合は、完全一致の IPv4 アドレスをカンマ区切りで入力します。

```text
192.168.1.30,127.0.0.1
```

この設定は export ごとに保存されます。

```properties
exports.1.allowed.clients=192.168.1.30,127.0.0.1
```

拒否された MOUNT/NFS 要求は、可能な範囲でクライアントアドレス、XID、status、対象 path とともにログへ記録されます。

## サービス表示

サービス タブには以下を表示します。

- サービス ID と旧 ID
- アプリケーションルート
- データルート
- 有効な設定ファイル
- 有効なログファイル
- 登録済みサービス実行ファイルパス
- データルート、設定ファイル、既定 export、ログフォルダの存在と書込可否
- Portmap、NFS、MOUNT の UDP/TCP ポート状態
- Windows Client for NFS の `NfsClnt` 状態
- 設定バックアップフォルダ

`保存して再起動` は、保存成功と再起動成功を分けてログ表示します。
サービスのインストール、開始、停止、再起動、アンインストール、ファイアウォール操作には管理者権限が必要です。

`ログを開く` は TinyWinNFS サーバーログの場所を開きます。
`WinSWログ` はサービスラッパーのログフォルダを開きます。
`診断出力` は、調査に必要な設定、ログ、状態情報をZIPにまとめます。

## 診断パッケージ

`サービス` タブの `診断出力` を実行すると、以下にZIPファイルを作成します。

```text
C:\ProgramData\EnterOgawa\TinyWinNFS Server\diagnostics
```

ファイル名は以下の形式です。

```text
tinywin-nfs-diagnostics-YYYYMMDD-HHMMSS.zip
```

含まれる情報:

- `summary.txt`: サービス状態、Java/OS/ユーザー情報、データルート、設定ファイル、ポート状態、Windows Client for NFS 状態、共有定義概要、診断概要
- `diagnostics/report.txt`: 設定診断、export診断、大文字小文字衝突、ファイル数、ディレクトリ数、総サイズ
- `conf/nfs-server.properties`: 現在の設定ファイル
- `conf/backups`: 設定バックアップ
- `logs/nfs-server.log`: TinyWinNFS サーバーログ
- `winsw-logs`: WinSW の `.log` / `.out` / `.err`

export 配下の共有ファイル本体は含めません。
問題調査時は、このZIPと発生時刻、クライアント種別、mount コマンドを合わせて確認します。

### export診断

診断では、各exportについて以下を確認します。

- exportパスの存在、ディレクトリ種別、読込可否、書込可否
- ファイル数、ディレクトリ数、総バイト数
- 大文字小文字を無視した相対パス衝突

通常のWindowsフォルダでは、`File.h` と `file.h` のような名前を同じディレクトリに共存させられません。
QNXなど大文字小文字を区別するクライアントからコピーする場合、診断結果に衝突が出たパスは事前に名前を整理してください。

## 設定バックアップ

管理ツールで設定を保存する場合、既存の `nfs-server.properties` があれば保存前にバックアップします。

```text
C:\ProgramData\EnterOgawa\TinyWinNFS Server\conf\backups
```

バックアップ名は以下の形式です。

```text
nfs-server-YYYYMMDD-HHMMSS-SSS.properties
```

保持世代は最大 10 件です。
旧 `Program Files` 配下の設定を `ProgramData` へ移行する場合も、移行元設定を `legacy-nfs-server-...properties` としてバックアップします。
