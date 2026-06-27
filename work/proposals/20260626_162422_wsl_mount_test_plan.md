# WSL mount テスト手順の追加

## 変更概要
- WSL mount 確認を任意の結合テストとして `AGENTS.md` に追記。
- 高番ポートを使う WSL テスト用設定 `conf/nfs-server-wsl-test.properties` を追加。
- WSLテスト用サーバー起動スクリプト `scripts/start-wsl-test-server.ps1` を追加。
- WSL からの mount 手順 `docs/wsl_mount_test.md` を追加。

## 追加した WSL テスト用ポート
- portmap: UDP `11111`
- nfsd: UDP `12049`
- mountd: UDP `12048`

## この環境での確認結果
- `wsl.exe --status`、`wsl.exe -l -v` は WSL 未インストールのため失敗。
- WSL mount テストは未実施。
- `scripts/test.ps1` による単体テストは成功。

## 単体テスト結果
```text
PASS: XDR round trip
PASS: RPC call parse
PASS: Portmap GETPORT
PASS: Mount MNT
PASS: NFSv2 procedures
TEST PASSED: 5 tests
```

## 注意
- Linux NFSクライアントで mount が通っても、QNX 4.25 互換性の完全保証にはならない。
- ただし、NFSv2 / MOUNT v1 / UDP の実クライアント結合テストとしては有効。
