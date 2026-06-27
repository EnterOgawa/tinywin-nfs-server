# v1.5.0 Operational Stability Update

## 対象Issue
- #21 Harden write-path consistency for overwrite, truncate, rename, and COMMIT
- #22 Improve attribute and cache consistency after cross-client edits
- #23 Add long-running service stability verification
- #24 Improve RPC error handling and operational logs
- #25 Harden multi-export configuration validation and persistence
- #26 Verify installer upgrade, service replacement, and settings preservation
- #27 Update README for the v1.5.0 release

## 変更方針
- `NfsV2Program.java` / `NfsV3Program.java`
  - WRITE/SETATTR/RENAME/REMOVE/MKDIR/RMDIRなどの変更系処理で、サーバー側実ファイルの同期、属性反映、ログ粒度を強化する。
  - GETATTR/LOOKUP/READDIRで返す属性がクロスクライアント編集後も最新化されることをテストで確認する。
- `NfsServerConfig.java`
  - 複数export設定で、公開名、パス、入れ子、存在確認、ディレクトリ確認、読込/書込可否の検証を強化する。
- `TinyWinNfsSwtManager.java`
  - 保存前に設定値を検証し、失敗時に既存設定を破壊しない。
- `AllTests.java` / `ServiceSmokeTest.java`
  - 上書き、truncate、rename上書き、属性読み取り、設定異常系の回帰テストを追加する。
- `scripts` / `docs` / `README.md`
  - 長時間稼働検証、Windows Client for NFS検証、インストーラー更新検証、v1.5.0リリース前確認を文書化する。

## 変更対象
- `src/jp/co/enterogawa/nfs/config/NfsServerConfig.java`
- `src/jp/co/enterogawa/nfs/program/NfsV2Program.java`
- `src/jp/co/enterogawa/nfs/program/NfsV3Program.java`
- `src/jp/co/enterogawa/nfs/manager/TinyWinNfsSwtManager.java`
- `test/jp/co/enterogawa/nfs/AllTests.java`
- `test/jp/co/enterogawa/nfs/ServiceSmokeTest.java`
- `scripts/*.ps1`
- `docs/*.md`
- `README.md`
- `CHANGELOG.md`
