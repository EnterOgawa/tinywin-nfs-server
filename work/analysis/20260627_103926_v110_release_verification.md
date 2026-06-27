# TinyWinNFS Server v1.1.0 release verification

## 確認基準

- NFSv2 の手続き番号、`SETATTR` の未指定フィールド `-1`、`fattr` / `sattr` の構造、`filename<255>`、`path<1024>`、`READDIR` cookie、`STATFS` は RFC 1094 を基準に確認した。
- 参照: https://datatracker.ietf.org/doc/html/rfc1094

## 実装確認

- `SETATTR`
  - `NfsV2Program.applySetAttributes` 1290 行付近で size / mode / atime / mtime を処理する。
  - `AllTests.assertSetAttrMode` と `AllTests.assertSetAttrTime` で mode と時刻属性を確認した。
- `READDIR`
  - `NfsV2Program.findReadDirStartIndex` 1012 行付近で cookie の継続位置を決定する。
  - `AllTests.assertReadDir` でページング、重複なし、不正 cookie の EOF を確認した。
- `fattr` / `STATFS`
  - `NfsV2Program.writeAttributes` 1631 行付近で mode / nlink / uid / gid / size / blocks / fsid / fileid / time を返す。
  - `NfsV2Program.handleStatFs` 1441 行付近で `FileStore` 由来の容量を返す。
  - Windows で `unix:nlink` が取れない場合に `countFileLinksInExport` で hard link 数を補正する。
- `LINK`
  - `NfsV2Program.handleLink` 734 行付近で同一 export、既存有無、ディレクトリ除外、Windows hard link 作成を確認する。
  - `AllTests.assertHardLink` で同一実体、相互反映、`nlink >= 2` を確認した。
- `SYMLINK`
  - `NfsV2Program.handleSymlink` 803 行付近で writable、`MAXPATHLEN`、既存有無、Windows 例外マッピングを確認する。
  - 自動テストでは `MAXPATHLEN` 超過を確認した。
- filename charset
  - `NfsServerConfig` 89 行付近で `filename.charset` を読み込む。
  - `NfsV2Program` は NFS name/path の読書きに `filenameCharset` を使用する。
  - `AllTests.testNfsV2FilenameCharset` で Shift_JIS の CREATE / LOOKUP / READDIR を確認した。

## 実行した確認

- `.\scripts\compile.ps1`
  - 成功
- `.\scripts\test.ps1`
  - `TEST PASSED: 9 tests`
- `.\scripts\package-installer.ps1`
  - `Installer created: C:\develop\nfs\dist\installer\TinyWinNfsSetup.exe`
- `Get-Item .\dist\installer\TinyWinNfsSetup.exe`
  - `ProductVersion=1.1.0`
- `git diff --check`
  - エラーなし

## 残リスク

- WSL mount、Windows サービス起動、QNX 実機 mount は今回未実施。
- `SYMLINK` 成功系は Windows 権限に依存するため、インストール先環境では管理者権限または開発者モードの有無で結果が変わる。
- `nlink` フォールバックは export 内を走査するため、非常に大きい共有フォルダでは GETATTR の負荷が増える。
