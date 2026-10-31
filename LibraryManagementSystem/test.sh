#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
mkdir -p bin data
echo "Compiling test suite..."
javac -cp lib/sqlite-jdbc.jar -d bin src/*.java
echo "Running automated technical and business verification tests..."
java -Djava.awt.headless=true -cp bin:lib/sqlite-jdbc.jar SystemTest
