@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "Start-Process -FilePath '%~dp0TinyWinNfsManager.exe' -Verb RunAs"
