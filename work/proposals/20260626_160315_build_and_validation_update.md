# ビルド補助と設定検証の追加

## 変更概要
- `NfsServerConfig` にポート番号、export名、ブロックサイズ、読込サイズの検証を追加。
- `NfsServer` に公開ディレクトリ存在確認を追加。
- 指定 JDK を使うコンパイルスクリプト `scripts/compile.ps1` を追加。
- QNX 4.25 からの mount 確認用メモ `docs/qnx425_mount.md` を追加。

## 変更対象
- `src/jp/co/enterogawa/nfs/config/NfsServerConfig.java`
- `src/jp/co/enterogawa/nfs/server/NfsServer.java`
- `scripts/compile.ps1`
- `docs/qnx425_mount.md`

## 確認結果
- `C:\develop\tools\eclipse202503\eclipse\jdk-21.0.8+9\bin\javac.exe` でコンパイル成功。
- `scripts/compile.ps1` の実行に成功。
- NFSサーバー起動、WinSWサービス起動、QNX 4.25 からの mount は未実施。
