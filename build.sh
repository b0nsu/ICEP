#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR_PATH="$ROOT_DIR/out/study-room-cli.jar"

"$ROOT_DIR/gradlew" clean jar

echo "Build success: $JAR_PATH"
