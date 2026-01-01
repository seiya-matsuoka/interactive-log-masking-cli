@echo off
setlocal

cd  /d "%~dp0.."

call gradlew.bat -q run

endlocal
