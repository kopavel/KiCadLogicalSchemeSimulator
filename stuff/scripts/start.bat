@echo off
start "netSim" cmd /u /c "C:\WINDOWS\system32\chcp.com 65001 && call %~dp0run.bat %*"
