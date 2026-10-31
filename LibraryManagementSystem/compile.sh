#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
mkdir -p bin data
echo "Compiling Java sources..."
javac -cp lib/sqlite-jdbc.jar -d bin src/*.java
echo "Compilation successful! Class files are ready in bin/"
