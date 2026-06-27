# NFSv2 WRITECACHE update

## Summary
- Implement NFSv2 procedure 7, `NFSPROC_WRITECACHE`, as a compatibility no-op.
- Add a unit test that verifies the procedure returns RPC success with an empty response payload.

## Changed files
- `src/jp/co/enterogawa/nfs/program/NfsV2Program.java:67`: add `PROC_WRITECACHE`.
- `src/jp/co/enterogawa/nfs/program/NfsV2Program.java:192`: dispatch `PROC_WRITECACHE` as successful no-op.
- `test/jp/co/enterogawa/nfs/AllTests.java:298`: add WRITECACHE coverage to the NFSv2 procedure test.
- `test/jp/co/enterogawa/nfs/AllTests.java:417`: add `assertWriteCache`.

## Validation
- `.\scripts\test.ps1`
  - Result: `TEST PASSED: 8 tests`

## GitHub
- Completes issue #1 in milestone `v1.1.0`.
