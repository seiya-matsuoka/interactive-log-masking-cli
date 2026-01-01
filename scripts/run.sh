#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")/.."

./gradlew -q run
