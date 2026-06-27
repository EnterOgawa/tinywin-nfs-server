# v1.2.0 Windows Client for NFS integration

## 変更対象

- `scripts/test-windows-nfs-client.ps1`
- `conf/nfs-server-windows-client-test.properties`
- `docs/windows_nfs_client_test.md`
- `docs/release_checklist_v1_2_0.md`
- `scripts/package-manager.ps1`
- `installer/tinywin-nfs-server.iss`
- `README.md`
- `CHANGELOG.md`

## 内容

Windows 標準の Client for NFS を v1.2.0 の主要なローカル結合テストとして扱う。

主な確認範囲は以下の通り。

- `\\127.0.0.1\export` の native Windows NFS mount
- mount 経由の create / read / update / rename / delete
- mount 経由の directory create / delete
- `lang=shift-jis` と `filename.charset=Shift_JIS` による日本語ファイル名
- テスト後の mount / サーバープロセス / 一時ディレクトリ cleanup

## 代表差分

```diff
+ .\scripts\test-windows-nfs-client.ps1
+ .\conf\nfs-server-windows-client-test.properties
+ .\docs\windows_nfs_client_test.md
+ .\docs\release_checklist_v1_2_0.md
```

```diff
- $appVersion = "1.1.0"
+ $appVersion = "1.2.0"
```

```diff
- #define AppVersion "1.1.0"
+ #define AppVersion "1.2.0"
```
