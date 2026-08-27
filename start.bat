@echo off
title MC-Servers-Tools

set "APP_DIR=%~dp0"
set "JAVA_EXE="

REM 1. Try bundled JRE first
if exist "%APP_DIR%runtime\bin\java.exe" (
    set "JAVA_EXE=%APP_DIR%runtime\bin\java.exe"
    goto :found
)

REM 2. Try JAVA_HOME
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
        goto :found
    )
)

REM 3. Try system PATH
where java >nul 2>&1
if %errorlevel%==0 (
    set "JAVA_EXE=java"
    goto :found
)

REM 4. Try common install locations
if exist "C:\Program Files\Java\jdk-17\bin\java.exe" set "JAVA_EXE=C:\Program Files\Java\jdk-17\bin\java.exe" & goto :found
if exist "C:\Program Files\Java\jre-17\bin\java.exe" set "JAVA_EXE=C:\Program Files\Java\jre-17\bin\java.exe" & goto :found

:notfound
echo.
echo ========================================
echo   Error: Java Runtime not found
echo ========================================
echo.
echo This program requires Java 17 or later.
echo.
echo Solutions:
echo   1. Download the version with bundled JRE (recommended)
echo   2. Install Java 17 from https://adoptium.net/
echo   3. Set JAVA_HOME environment variable
echo.
pause
exit /b 1

:found
echo ========================================
echo   MC-Servers-Tools - Java Edition
echo ========================================
echo.
echo Using Java: %JAVA_EXE%
echo.

"%JAVA_EXE%" -cp "%APP_DIR%lib\*;%APP_DIR%out" com.mcmanager.Main

if errorlevel 1 (
    echo.
    echo Program exited with code: %errorlevel%
    echo.
    pause
)
