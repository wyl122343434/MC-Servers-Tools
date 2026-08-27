#!/bin/bash
# MC-Servers-Tools - Java Edition (Linux/Mac)

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Find Java
JAVA_EXE="java"
if [ -x "$SCRIPT_DIR/runtime/bin/java" ]; then
    JAVA_EXE="$SCRIPT_DIR/runtime/bin/java"
fi

echo "========================================"
echo "  MC-Servers-Tools - Java Edition"
echo "========================================"
echo ""

"$JAVA_EXE" -cp "$SCRIPT_DIR/lib/*:$SCRIPT_DIR/out" com.mcmanager.Main
