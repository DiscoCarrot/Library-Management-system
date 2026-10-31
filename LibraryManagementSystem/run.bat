@echo off
REM Library Management System - Windows Run Script
cd /d "%~dp0"
if not exist "bin" mkdir bin
if not exist "data" mkdir data
if not exist "bin\Main.class" (
    echo Compiled classes not found. Compiling first...
    javac -cp "lib/sqlite-jdbc.jar" -d bin src/*.java
)
echo Starting Library Management System...
java -cp "bin;lib/sqlite-jdbc.jar" Main
pause
