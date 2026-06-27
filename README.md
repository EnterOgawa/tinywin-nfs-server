# TinyWinNFS Server

Java 21 で実装する、Windows 向けのユーザー空間 NFS サーバーです。

現在は NFSv2 / MOUNT v1-v2 / AUTH_SYS / UDP / 複数 export の read-write MVP です。QNX 4.25 は重要な検証対象ですが、製品自体は QNX 専用ではありません。

完成目標は NFSv2 / NFSv3 / MOUNT v1-v3 / AUTH_SYS / UDP / TCP / 複数 export の read-write 対応です。

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

## 注意

UDP 111 と 2049 を利用するため、実行時は管理者権限と Windows Firewall の許可が必要です。
このリポジトリでは環境依存を避けるため、サーバー起動は手動確認対象です。
