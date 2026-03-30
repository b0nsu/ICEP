#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR_PATH="$ROOT_DIR/out/study-room-cli.jar"

if [[ ! -f "$JAR_PATH" ]]; then
  "$ROOT_DIR/build.sh"
fi

java -jar "$JAR_PATH"
