#!/bin/bash
set -e

JDK_DIR="jdk-22"
JDK_URL="https://download.java.net/openjdk/jdk22/ri/openjdk-22+36_linux-x64_bin.tar.gz"

# 1. Check if JDK already exists
if [ -d "$JDK_DIR" ]; then
    echo "JDK already present in $JDK_DIR"
    exit 0
fi

# 2. Download JDK
echo "Downloading JDK..."
curl -L -o jdk.tar.gz "$JDK_URL"

# 3. Extract
echo "Extracting..."
tar -xzf jdk.tar.gz
rm jdk.tar.gz

echo "JDK installed locally in $JDK_DIR"
