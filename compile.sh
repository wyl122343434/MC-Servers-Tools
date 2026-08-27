#!/bin/bash
# Compile script for MC-Servers-Tools Java Edition

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAVA_HOME="$SCRIPT_DIR/../javatools/jdk-17.0.12+7"
JAVAC="$JAVA_HOME/bin/javac"
JAVA="$JAVA_HOME/bin/java"

SRC_DIR="$SCRIPT_DIR/src"
OUT_DIR="$SCRIPT_DIR/out"
LIB_DIR="$SCRIPT_DIR/lib"

# Clean output
rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

# Find all Java files
find "$SRC_DIR" -name "*.java" > "$OUT_DIR/sources.txt"

# Compile
echo "Compiling..."
"$JAVAC" -encoding UTF-8 -cp "$LIB_DIR/jsch-0.1.55.jar" -d "$OUT_DIR" @"$OUT_DIR/sources.txt"

if [ $? -eq 0 ]; then
    echo "Compilation successful!"
    echo "Output: $OUT_DIR"
else
    echo "Compilation failed!"
    exit 1
fi
