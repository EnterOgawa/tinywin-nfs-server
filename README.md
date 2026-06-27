# TinyWinNFS Server

Java 21 で実装した、Windows 向けのユーザー空間 NFS サーバーです。

v1.6.1 時点では、NFSv2 / NFSv3 の通常ファイル・ディレクトリ操作、MOUNT v1-v3、AUTH_SYS、UDP/TCP transport、複数 export の read-write 共有に対応しています。

QNX 4.25 は NFSv2/UDP、Windows Client for NFS は NFSv3/UDP と NFSv3/TCP で検証対象にしています。製品自体は QNX 専用ではありません。

NFSv3 MKNOD、NLM/file locking、NFSv4 は現在の対応範囲外です。

## 画面

![TinyWinNFS Server マネージャー](docs/images/tinywin-nfs-manager-ja.png)

## Eclipse

このフォルダを既存ワークスペースへ `Existing Projects into Workspace` で取り込みます。

Java は `C:\develop\tools\eclipse202503\eclipse\jdk-21.0.8+9` の Java 21 を使用します。

## コンパイル

以下を実行します。

```powershell
.\scripts\compile.ps1
```

## テスト

サーバー起動を伴わない単体テストは以下で実行します。

```powershell
.\scripts\test.ps1
```

Windows サービスとして起動済みの場合、ローカル UDP のスモークテストは以下で実行します。

```powershell
.\scripts\smoke-service.ps1
```

TCP のスモークテストは以下で実行します。

```powershell
.\scripts\smoke-service.ps1 -Transport TCP
```

Windows 標準の Client for NFS を利用した mount テストは主要な結合テストです。v1.4.0以降はMOUNT v3/NFSv3のRPC到達もログで確認します。

```powershell
.\scripts\test-windows-nfs-client.ps1
```

TCP transport を明示して確認する場合は以下を実行します。

```powershell
.\scripts\test-windows-nfs-client.ps1 -Transport TCP
```

手順は `docs/windows_nfs_client_test.md` を参照します。

v1.5.0以降は、サービス経由の上書き、truncate、rename上書き、サーバー側実ファイル内容の整合性を確認できます。

```powershell
.\scripts\smoke-service.ps1 -VerifyFileIntegrity
```

長時間稼働確認は以下で実行します。

```powershell
.\scripts\test-service-stability.ps1 -DurationMinutes 60 -IntervalSeconds 15 -RestartEveryIterations 10
```

手順は `docs/service_stability_test.md` を参照します。

## Windows サービス

サービス化手順は `docs/windows_service.md` を参照します。

## 管理ツール配布

ダブルクリック用の管理ツールは SWT で実装しています。SWT は Eclipse 2025-03 付属の `org.eclipse.swt.win32.win32.x86_64` jar を同梱します。
管理ツールUIは英語と日本語に対応し、`conf/nfs-server.properties` の `ui.language=auto|en|ja` で表示言語を選択できます。

管理ツールは以下で作成します。

```powershell
.\scripts\package-manager.ps1
```

生成先は `dist\TinyWinNfsManager` です。サービス操作を行う場合は `TinyWinNfsManager-Admin.cmd` を使用します。

## 設定

`conf/nfs-server.properties` で共有フォルダ、ポート、UID/GID、モードを設定します。
共有フォルダは管理ツールの Share タブから複数登録できます。
共有フォルダは存在するディレクトリである必要があります。書込可にした共有は、Windows上でも書込可能なフォルダを指定してください。
共有ごとの `allowed.clients` に IPv4 アドレスをカンマ区切りで指定すると、MOUNT/NFS 要求をその接続元だけに制限できます。空欄の場合は従来どおり全クライアントを許可します。
ファイル名の文字コードは `filename.charset=UTF-8` が既定です。クライアント環境に合わせる必要がある場合は Java の Charset 名で指定できます。
書込性能を優先するため、`write.sync=false` が既定です。各WRITE応答前に物理同期したい場合は `write.sync=true` に変更してください。
大量コピー時のopen/close負荷を抑えるため、`write.cache.enabled=true` が既定です。保持数は `write.cache.max.open`、アイドル保持時間は `write.cache.idle.millis` で調整できます。
`permission.identity=auto` が既定です。AUTH_SYSのUID/GIDを属性応答に反映するため、QNXやWindows Client for NFSなど複数クライアントの権限解釈に自動追従します。従来の固定UID/GID応答が必要な場合は `permission.identity=fixed` に変更してください。

## 対応範囲

- 対応済み: NFSv2/NFSv3 の通常ファイル・ディレクトリ操作, MOUNT v1-v3, AUTH_SYS, UDP/TCP, read-write, 複数 export
- 主な検証対象: QNX 4.25 は NFSv2/UDP, Windows Client for NFS は NFSv3/UDP と NFSv3/TCP
- 対応範囲外: NFSv3 MKNOD, NLM/file locking, NFSv4

## 注意

UDP/TCP 111 と 2049 を利用するため、実行時は管理者権限と Windows Firewall の許可が必要です。
このリポジトリでは環境依存を避けるため、サーバー起動は手動確認対象です。

## ライセンス

TinyWinNFS Server は Apache License 2.0 で提供します。
配布物に同梱する第三者コンポーネントのライセンスは `THIRD_PARTY_NOTICES.md` を参照してください。
