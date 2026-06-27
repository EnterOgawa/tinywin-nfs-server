@echo off
powershell.exe -NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -Command "Start-Process -FilePath '%~dp0TinyWinNfsManager.exe' -WorkingDirectory '%~dp0' -Verb RunAs"
