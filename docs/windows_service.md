# Windows サービス

サービスには WinSW v2.12.0 を使用します。

## ファイル

- WinSW 実行ファイル: `service/winsw/nfs-server.exe`
- WinSW 設定: `service/winsw/nfs-server.xml`
- サービス ID: `TinyWinNfsServer`
- 旧サービス ID: `OgawaNfsServer`, `QnxNfsServer`
- パッケージ同梱 Java runtime: `runtime/bin/java.exe`
- データルート: `C:\ProgramData\EnterOgawa\TinyWinNFS Server`

パッケージ同梱の管理ツールとサービスは同じ Java runtime を共有します。

## インストール

PowerShell を管理者として実行します。

```powershell
.\scripts\download-winsw.ps1
.\scripts\install-service.ps1
.\scripts\add-firewall-rules.ps1
```

Inno Setup インストーラーでも、インストールタスクとしてサービス登録とファイアウォール設定を実行できます。

## 開始

```powershell
.\scripts\start-service.ps1
.\scripts\status-service.ps1
```

## 停止

```powershell
.\scripts\stop-service.ps1
```

## アンインストール

```powershell
.\scripts\uninstall-service.ps1
```

## ポート

既定設定では以下を使用します。

- portmap: UDP/TCP `111`
- nfsd: UDP/TCP `2049`
- mountd: UDP/TCP `20048`

## ログ

WinSW は `service/winsw` 配下にサービスログを書き込みます。

TinyWinNFS は要求診断を以下に書き込みます。

```text
C:\ProgramData\EnterOgawa\TinyWinNFS Server\logs\nfs-server.log
```

既定では、トラブルシュートに有用な運用ログを残しつつ、RPC 要求トレースや成功した変更操作など大量に出る成功ログは抑制します。これにより、大量コピーや一括削除時の同期 I/O を抑えます。

大容量コピーで Windows ファイルキャッシュを利用できるように、WRITE 応答は既定で `write.sync=false` を使用します。各 WRITE の応答前に物理同期が必須の場合のみ、`C:\ProgramData\EnterOgawa\TinyWinNFS Server\conf\nfs-server.properties` で `write.sync=true` に変更します。

書込ファイルキャッシュも既定で有効です。直近に書き込んだファイルを短時間開いたままにし、繰り返し open/close する負荷を避けます。

```text
write.cache.enabled=true
write.cache.max.open=64
write.cache.idle.millis=3000
```

通常のファイル操作をキャッシュが妨げないように、サーバーは `SETATTR`、`REMOVE`、`RMDIR`、`RENAME`、サービス停止の前にキャッシュ中のファイルを閉じます。

UDP RPC 要求はワーカープールで処理し、受信ループと要求処理を分離します。既定のワーカー数は CPU 数に基づき、最大 8 です。変更する場合は以下の Java システムプロパティを指定します。

```text
-Dtinywin.nfs.udp.workers=4
```

Windows では、NFS 属性 `nlink` にプラットフォーム値を利用できる場合はそれを使用します。Windows がネイティブのリンク数を公開できない場合、通常ファイルには軽量な数値を返し、NFS 経由で作成したハードリンクはメモリ上で追跡します。これにより、QNX の大量コピー時に属性応答ごとに export ツリー全体を走査することを避けます。

RPC 要求レベルのログを含める場合は、以下の Java システムプロパティを指定します。

```text
-Dtinywin.nfs.requestLog=true
```

すべての debug ログと大量出力の操作ログを含める場合は、以下の Java システムプロパティを指定します。

```text
-Dtinywin.nfs.debug=true
```

ファイルハンドル永続化情報は、実行中は既定で 30 秒単位で保存し、サービス停止時に flush します。ハンドル変更ごとに即時保存したい場合は、間隔を `0` にします。

```text
-Dtinywin.nfs.handleStoreSaveIntervalMillis=0
```

## 権限 ID

既定の権限 ID mode は以下です。

```text
permission.identity=auto
```

自動モードでは、TinyWinNFS は現在のクライアントが送信した AUTH_SYS UID/GID を NFS ファイル属性に返します。
これにより、クライアント IP 別プロファイルを持たずに、QNX、Windows Client for NFS、その他 AUTH_SYS クライアントがそれぞれの ID 解釈を利用できます。

属性に常に設定済みの `uid` と `gid` を返す必要がある場合のみ、以下の値を指定します。

```text
permission.identity=fixed
```

## スモークテスト

サービス起動後に以下を実行します。

```powershell
.\scripts\smoke-service.ps1
```

TCP 通信方式を確認する場合は以下を実行します。

```powershell
.\scripts\smoke-service.ps1 -Transport TCP
```

期待結果:

```text
PASS: service portmap GETPORT
PASS: service mount MNT
PASS: service nfs GETATTR
PASS: service nfs CREATE/WRITE/SETATTR/READ/RENAME/REMOVE
SERVICE SMOKE TEST PASSED
```

サービス再起動後もファイルハンドルが利用できることを確認する場合は以下を実行します。

```powershell
.\scripts\smoke-service.ps1 -RestartHandlePersistence
```

NFS 書込で export された Windows ファイル内容が壊れないことを確認する場合は以下を実行します。

```powershell
.\scripts\smoke-service.ps1 -VerifyFileIntegrity
```

ディレクトリ階層と複数サイズのファイルを含む任意の整合性確認を行う場合は以下を実行します。

```powershell
.\scripts\smoke-service.ps1 -VerifyLargeTreeIntegrity
```

この確認では、複数階層、空ファイル、小さいファイル、大きめのファイル、28件の通常ファイル、rename、削除、最後の残骸確認を行います。大量コピーや削除後にサーバー側のファイル内容やディレクトリ状態が崩れていないかを確認する用途です。
