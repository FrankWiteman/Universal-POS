@echo off
REM ============================================================
REM  UniversalPOS — JWT Token Helper (Windows)
REM
REM  Usage:
REM    scripts\login.bat
REM    scripts\login.bat myemail@x.com MyPassword my-store
REM
REM  After running, %TOKEN% is set for this terminal session.
REM  Use it like:
REM    curl -H "Authorization: Bearer %TOKEN%" http://localhost:8080/api/customers/search?q=Jane
REM
REM  Requires: curl (built into Windows 10+)
REM            jq   (download from https://jqlang.github.io/jq/download/)
REM ============================================================

SET EMAIL=%1
SET PASSWORD=%2
SET TENANT=%3

IF "%EMAIL%"=="" SET EMAIL=admin@universalpos.local
IF "%PASSWORD%"=="" SET PASSWORD=ChangeMe123!
IF "%TENANT%"=="" SET TENANT=demo-store

echo Logging in as %EMAIL% @ %TENANT%...

REM Make the login request and save response
curl -s -X POST "http://localhost:8080/api/auth/login" ^
  -H "Content-Type: application/json" ^
  -d "{\"email\":\"%EMAIL%\",\"password\":\"%PASSWORD%\",\"tenantSlug\":\"%TENANT%\"}" ^
  -o "%TEMP%\pos_login.json"

REM Check if jq is available
where jq >nul 2>&1
IF %ERRORLEVEL% EQU 0 (
    FOR /F "delims=" %%i IN ('jq -r ".data.token" "%TEMP%\pos_login.json"') DO SET TOKEN=%%i
    FOR /F "delims=" %%i IN ('jq -r ".data.employeeName" "%TEMP%\pos_login.json"') DO SET EMPLOYEE=%%i
    FOR /F "delims=" %%i IN ('jq -r ".data.role" "%TEMP%\pos_login.json"') DO SET ROLE=%%i
    echo.
    echo Login successful: %EMPLOYEE% (%ROLE%)
    echo Token saved to %%TOKEN%%
    echo.
    echo Usage example:
    echo   curl -H "Authorization: Bearer %%TOKEN%%" http://localhost:8080/api/customers/search?q=Jane
) ELSE (
    echo.
    echo jq not found - showing raw response:
    type "%TEMP%\pos_login.json"
    echo.
    echo Install jq from https://jqlang.github.io/jq/download/ for automatic token extraction.
    echo Then copy the token value from "data"."token" above and run:
    echo   SET TOKEN=paste-your-token-here
)
