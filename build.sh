#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
OUT_DIR="$ROOT_DIR/out"
CLASS_DIR="$OUT_DIR/classes"
JAR_PATH="$OUT_DIR/study-room-cli.jar"

mkdir -p "$CLASS_DIR"
rm -f "$JAR_PATH"

javac -encoding UTF-8 -d "$CLASS_DIR" "$ROOT_DIR"/src/*.java
jar --create --file "$JAR_PATH" --main-class Main -C "$CLASS_DIR" .

echo "Build success: $JAR_PATH"
