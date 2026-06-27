# TinyWinNFS Server v1.1.0 release verification hardening proposal

## 目的

v1.1.0 リリース前に、今回実装した NFSv2 互換性機能を RFC 1094 の構造と照合し、テスト不足および属性報告の弱い箇所を補強する。

## 変更対象

- `src/jp/co/enterogawa/nfs/program/NfsV2Program.java`
  - `readLinkCount` 1705 行付近
  - `countFileLinksInExport` 1736 行付近
  - `isSameFileIdentity` 1780 行付近
- `test/jp/co/enterogawa/nfs/AllTests.java`
  - `testNfsV2FilenameCharset` 329 行付近
  - `assertSetAttrTime` 632 行付近
  - `assertReadDir` 849 行付近
  - `assertInvalidLookup` 965 行付近
  - `assertHardLink` 1024 行付近
  - `writeSattrTimes` 1350 行付近

## 代表差分

### hard link nlink fallback

```diff
- return 1L ;
+ return Math.max( 1L, countFileLinksInExport( path)) ;
```

### filename charset verification

```diff
+ runTest( "NFSv2 filename charset", this::testNfsV2FilenameCharset) ;
+ writeDiropargs( createArguments, rootHandle, name, charset) ;
+ ReadDirPage page = readDirPage( program, rootHandle, 0, 8192, charset) ;
```

### SETATTR time verification

```diff
+ assertSetAttrTime( program, fileHandle) ;
+ writeSattrTimes( arguments, atimeSeconds, mtimeSeconds) ;
+ assertEquals( "setattr mtime seconds", mtimeSeconds, (int)reader.readUnsignedInt()) ;
```

### READDIR cookie verification

```diff
+ assertEquals( "readdir paged no duplicate", pagedNames.size(), new HashSet<String>( pagedNames).size()) ;
+ ReadDirPage invalidCookiePage = readDirPage( program, rootHandle, 0x7fffffff, 8192) ;
```

## 確認結果

- `.\scripts\compile.ps1`
  - 成功
- `.\scripts\test.ps1`
  - `TEST PASSED: 9 tests`
- `.\scripts\package-installer.ps1`
  - `Installer created: C:\develop\nfs\dist\installer\TinyWinNfsSetup.exe`
- `dist\installer\TinyWinNfsSetup.exe`
  - `ProductVersion=1.1.0`
- `git diff --check`
  - エラーなし

## 備考

- WSL mount、Windows サービス起動、QNX 実機 mount は環境依存のため、この検証では実行していない。
- `SYMLINK` 成功系は Windows の権限や開発者モードに依存するため、今回の自動テストでは長さ検証とエラーマッピングを対象にしている。
