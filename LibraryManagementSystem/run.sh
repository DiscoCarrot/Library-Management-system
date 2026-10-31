#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
mkdir -p bin data
if [ ! -f "bin/Main.class" ]; then
    echo "Compiled classes not found. Compiling first..."
    javac -cp lib/sqlite-jdbc.jar -d bin src/*.java
fi
echo "Launching Library Management System..."
java -cp bin:lib/sqlite-jdbc.jar Main "$@"
