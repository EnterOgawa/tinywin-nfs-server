# v1.14.0 正式版候補ドキュメント整理 提案書

## 変更対象

- `docs/support_scope.md`
  - 2.0.0 正式版で保証する範囲、制限付き機能、対象外機能を追加。
- `docs/configuration_compatibility.md`
  - `ProgramData` 配置、設定キー互換性、アップグレード時に維持するデータを追加。
- `docs/security_model.md`
  - export 境界、allowed clients、AUTH_SYS、Windows 権限の扱いを追加。
- `docs/install_upgrade_uninstall.md`
  - 新規インストール、上書き更新、アンインストール、サービス再登録の確認手順を追加。
- `test/jp/co/enterogawa/nfs/AllTests.java`
  - `testConfigValidation` に重複 export 名、重複 export パス、ネスト export パスの拒否確認を追加。
- `README.md`
  - v1.14.0 状態と正式版候補文書への導線を追加。
- `docs/release_checklist_v1_14_0.md`
  - v1.14.0 リリース確認項目を追加。

## 主な差分

```diff
+ docs/support_scope.md
+ docs/configuration_compatibility.md
+ docs/security_model.md
+ docs/install_upgrade_uninstall.md
+ docs/release_checklist_v1_14_0.md
+ assertThrows( "duplicate export name", () -> NfsServerConfig.load( duplicateNameConfig)) ;
+ assertThrows( "duplicate export path", () -> NfsServerConfig.load( duplicatePathConfig)) ;
+ assertThrows( "nested export path", () -> NfsServerConfig.load( nestedPathConfig)) ;
```

## 確認結果

- `git diff --check`: 成功。
- `.\scripts\test.ps1`: `TEST PASSED: 20 tests`。
- `.\scripts\package-manager.ps1`: 成功。
- `.\scripts\package-installer.ps1`: 成功。
- `TinyWinNfsSetup.exe` ProductVersion: `1.14.0`。
- `TinyWinNfsSetup.exe` 署名状態: `NotSigned`。
- 新規 docs 5 件が配布物に含まれることを確認。
