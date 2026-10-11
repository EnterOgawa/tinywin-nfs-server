# README のアイコン・文面整理

## 目的と対象

- 対象ファイル: `README.md`
- 対象メソッド: なし。ドキュメントのみの変更。
- 参考: [EnterOgawa/p-model の README](https://github.com/EnterOgawa/p-model)
- 既存の OSS 整備に関する未コミット変更を保持し、今回の変更は README の表示・案内に限定する。
- 実装、設定値、コマンド、インストーラー、署名済み配布物は変更しない。

## 変更箇所

行番号は今回の変更後の `README.md` を基準とする。以下は変更箇所の抜粋。

### 冒頭: 1、3、5、7、9、11 行

既存の製品アイコンを見出しに表示し、使い道、Java の追加導入が不要であること、ダウンロード先を分かりやすく案内する。

```diff
-# TinyWinNFS Server
+# <img src="assets/tinywin-nfs-server.png" width="40" height="40" alt="TinyWinNFS アイコン"> TinyWinNFS Server
-TinyWinNFS Server は、Java 21 で実装した Windows 向けのユーザー空間 NFS サーバーです。
+**Windows のフォルダを、NFS で共有。**
+TinyWinNFS Server は、Windows のフォルダを NFS クライアントから読み書きできるようにするサーバーです。共有したいフォルダを管理ツールで選び、Windows サービスとして常駐させられます。
-v2.0.0 は正式版です。インストーラーには Java runtime、管理ツール、Windows サービス、既定設定、運用ドキュメントを同梱しています。
+Java 21 で実装し、v2.0.0 から正式版として提供しています。インストーラーには Java runtime と管理ツールを同梱しているので、使い始めるために Java を別途インストールする必要はありません。
+📥 [最新版をダウンロード](https://github.com/EnterOgawa/tinywin-nfs-server/releases/latest) · 📖 [導入・更新ガイド](docs/install_upgrade_uninstall.md) · 💬 [不具合・ご提案](https://github.com/EnterOgawa/tinywin-nfs-server/issues)
```

### 使い方・画面・対応範囲: 13、29、39、42、48、60 行

見出しに用途に合ったアイコンを付与する。画面は共有手順の直後へ移動する。信頼できる LAN と許可クライアントの注意を残し、実装済みの mode / UID / GID を対応表に明記する。

```diff
-## 入手とインストール
+## 📥 入手とインストール
-## 基本的な使い方
+## 🚀 まずはフォルダを共有してみましょう
+> [!IMPORTANT]
+> 信頼できる LAN 内での利用を想定しています。許可クライアントが空欄の場合は全クライアントを許可するため、実運用では必要な IP アドレスを登録し、ファイアウォールの公開範囲も絞ってください。[セキュリティとアクセス制限](docs/security_model.md)
-## 画面
+## 🖥️ 管理ツールの画面
-![TinyWinNFS Server マネージャー](docs/images/tinywin-nfs-manager-ja.png)
+![TinyWinNFS Server マネージャー: 共有フォルダと許可クライアントの設定画面](docs/images/tinywin-nfs-manager-ja.png)
-## 対応範囲
+## ✨ できること
+| NFS 属性 | mode / UID / GID の変更・保持。Windows ACL や Windows 所有者への完全変換ではありません |
```

### 保存場所・開発・運用・設定: 71、89、172、188、202 行

技術資料は残し、読み手の目的が分かる見出しと導入文にする。設定表とコマンド例は維持する。

```diff
-## インストール後の配置
+## 📁 ファイルの保存場所
-## 開発者向けクイックスタート
+## 🛠️ 開発に参加するには
-## Windows サービス
+## 🔄 Windows サービスとして使う
-## 管理ツール
+## 🌐 管理ツールと言語設定
-## 設定ファイル
+## ⚙️ 設定を詳しく見る
```

### 接続・テスト・制限・ライセンス・報告: 285、301、316、340、358、364 行

接続例のホスト名や IP アドレスを置き換える箇所を補足する。ドキュメントのリンク名を用途が伝わる表示に変更する。既知の制限、非公開の脆弱性報告、ベストエフォートの保守方針は維持する。

```diff
-## マウント例
+## 🔌 クライアントから接続する
-| QNX 4.25 | [docs/qnx425_mount.md](docs/qnx425_mount.md) |
+| QNX 4.25 | [QNX のマウント手順](docs/qnx425_mount.md) |
-## テスト
+## 🧪 動作を確かめる
-## 互換性と制限事項
+## ⚠️ 使う前に知っておきたいこと
-## ライセンス
+## 📄 ライセンス
-## 報告と保守
+## 💬 不具合の報告・改善のご提案
-不具合や提案は [GitHub Issues](https://github.com/EnterOgawa/tinywin-nfs-server/issues) で受け付けます。脆弱性の詳細は公開せず [SECURITY.md](SECURITY.md) の手順に従ってください。
+「この環境でつながらない」「こんな設定があると便利」といった気づきは、[GitHub Issues](https://github.com/EnterOgawa/tinywin-nfs-server/issues) へお寄せください。報告用のテンプレートも用意しています。
+脆弱性の詳細は公開 Issue に書かず、[SECURITY.md](SECURITY.md) の手順でご連絡ください。
```

## 確認結果

| 確認 | 結果 |
|---|---|
| PowerShell の `ConvertFrom-Markdown` による HTML 変換 | 成功 |
| Markdown / HTML のローカル参照先 | 21 件、欠落なし |
| 見出し・表・画像 | H1: 1、H2: 14、表: 12、画像: 2 |
| コマンド・設定例のコードブロック | 13 個すべて今回の変更前の内容と一致 |
| 共有・ネットワーク・権限/属性・性能の設定表 | 4 表すべて `HEAD` と一致 |
| `git diff --check -- README.md` | 問題なし |
| README の旧見出しへの既存ドキュメント内リンク | `work` を除く Markdown で該当なし |
| NFS サーバー・サービス・mount 確認 | 未実施。ドキュメントのみの変更のため起動しない |
| commit / push / Release 作成 | 未実施 |

GitHub 上の表示は push 後に確認する。今回の確認では公開操作を行っていない。
