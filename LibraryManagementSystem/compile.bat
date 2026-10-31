@echo off
REM Library Management System - Windows Compile Script
cd /d "%~dp0"
if not exist "bin" mkdir bin
if not exist "data" mkdir data
echo Compiling Java source files...
javac -cp "lib/sqlite-jdbc.jar" -d bin src/*.java
if %ERRORLEVEL% equ 0 (
    echo Compilation successful! Class files located in bin\
) else (
    echo Compilation failed! Please ensure JDK 17+ is installed and configured in PATH.
)
pause
