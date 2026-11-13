@echo off
setlocal
title Badminton Premium - Permanent Android Signing Setup
echo.
echo BADMINTON PREMIUM - ONE-TIME STABLE SIGNING
echo ---------------------------------------------------------------
echo Only run this package if it came from your own GitHub repository.
echo A private keystore will be generated on YOUR Windows computer.
echo Do not post the key or password to GitHub, email or any chat.
echo.
echo You need GitHub CLI and Java JDK / Android Studio installed.
echo.
pause
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0SETUP_BADMINTON_STABLE_SIGNING.ps1"
if errorlevel 1 (
  echo.
  echo Setup stopped before completion.
  echo No signing key should be replaced. Read the error above.
) else (
  echo.
  echo Setup finished. Follow the steps printed above.
)
echo.
pause
endlocal
