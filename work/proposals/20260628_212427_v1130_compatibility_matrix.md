# v1.13.0 互換性検証強化 提案書

## 変更対象

- `scripts/test-windows-nfs-client-matrix.ps1`
  - Windows Client for NFS の UDP/TCP を連続実行するマトリクススクリプトを追加。
- `scripts/test-windows-nfs-client.ps1`
  - 失敗時レポートに復旧案を追加。
- `src/jp/co/enterogawa/nfs/program/NfsV3Program.java`
  - `handleRmdir` で存在しない対象は `NOENT`、通常ファイルは `NOTDIR` を返すよう修正。
- `test/jp/co/enterogawa/nfs/AllTests.java`
  - `testNfsStatusAndAttributes` を追加。
  - `testCrossClientEditRegression` を追加。
- `docs/release_checklist_v1_13_0.md`
  - v1.13.0 向けの確認項目を追加。

## 主な差分

```diff
+ scripts/test-windows-nfs-client-matrix.ps1
+ docs/release_checklist_v1_13_0.md
+ runTest( "NFS status and attributes", this::testNfsStatusAndAttributes) ;
+ runTest( "Cross client edit regression", this::testCrossClientEditRegression) ;
+ response.writeInt( NfsStatus.NOTDIR) ;
```

## 確認結果

- `git diff --check`: 成功。
- PowerShell scriptblock 構文確認: 成功。
- `.\scripts\test.ps1`: `TEST PASSED: 20 tests`。
- `.\scripts\package-manager.ps1`: 成功。
- `.\scripts\package-installer.ps1`: 成功。
- `TinyWinNfsSetup.exe` ProductVersion: `1.13.0`。
- `TinyWinNfsSetup.exe` 署名状態: `NotSigned`。
