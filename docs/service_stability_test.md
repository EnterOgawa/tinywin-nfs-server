# Service stability test

This check is for operational hardening after the NFSv3 and TCP transport milestones.

Run it only on a machine where TinyWinNFS Server is installed as a Windows service and the configured export is safe for test file creation.

## Short run

```powershell
.\scripts\test-service-stability.ps1 -DurationMinutes 10 -IntervalSeconds 10
```

The script repeatedly runs:

```powershell
.\scripts\smoke-service.ps1 -VerifyFileIntegrity
```

The integrity smoke test verifies create, overwrite, truncate, append, rename overwrite, server-side disk content, and cleanup through the service RPC path.

## Restart run

```powershell
.\scripts\test-service-stability.ps1 -DurationMinutes 60 -IntervalSeconds 15 -RestartEveryIterations 10
```

This adds regular service restarts between integrity passes. Use this before release when validating that UDP/TCP ports are released and rebound cleanly.

## Pass criteria

- Every iteration prints `PASS: service file integrity`.
- The final line is `SERVICE STABILITY TEST PASSED`.
- `scripts\status-service.ps1` reports `TinyWinNfsServer` running between iterations.
- The configured export folder does not contain leftover `service-integrity-*.txt` files after the run.

## Failure handling

If the script fails, keep the service log and the export folder intact until the failed operation is identified. The relevant log entries should include the client address, RPC operation, status, and path context.
