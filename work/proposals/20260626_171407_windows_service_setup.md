# Windows サービス化

## 変更概要
- WinSW v2.12.0 の `WinSW-x64.exe` を取得し、`service/winsw/nfs-server.exe` として配置。
- `service/winsw/nfs-server.xml` に `workingdirectory` と `stoptimeout` を追加。
- サービス操作用スクリプトを追加。
  - `scripts/download-winsw.ps1`
  - `scripts/install-service.ps1`
  - `scripts/uninstall-service.ps1`
  - `scripts/start-service.ps1`
  - `scripts/stop-service.ps1`
  - `scripts/status-service.ps1`
  - `scripts/add-firewall-rules.ps1`
  - `scripts/smoke-service.ps1`
- Windows サービス手順書 `docs/windows_service.md` を追加。
- ローカル UDP RPC スモークテスト `test/jp/co/enterogawa/nfs/ServiceSmokeTest.java` を追加。
- WinSW exe とログを Git 管理対象外にするため `.gitignore` を更新。

## WinSW
- Release: `v2.12.0`
- Asset: `WinSW-x64.exe`
- 配置先: `service/winsw/nfs-server.exe`
- SHA256: `05B82D46AD331CC16BDC00DE5C6332C1EF818DF8CEEFCD49C726553209B3A0DA`

## サービス登録結果
- Service ID: `QnxNfsServer`
- Display Name: `QNX NFS Server`
- 状態: `Running`
- Firewall inbound allow rule を追加済み。
  - UDP `111`
  - UDP `2049`
  - UDP `20048`

## 待受確認
```text
UDP 111    LISTENING by java.exe
UDP 2049   LISTENING by java.exe
UDP 20048  LISTENING by java.exe
```

## テスト結果
単体テスト:

```text
PASS: XDR round trip
PASS: RPC call parse
PASS: Portmap GETPORT
PASS: Mount MNT
PASS: NFSv2 procedures
TEST PASSED: 5 tests
```

サービススモークテスト:

```text
PASS: service portmap GETPORT
PASS: service mount MNT
PASS: service nfs GETATTR
SERVICE SMOKE TEST PASSED
```

## 未実施
- QNX 4.25 からの `mount_nfs windows-host:/export /mnt` 確認。
- WSL/Linux からの mount 確認。
