#!/usr/bin/env bash
# Builds Pirouette. Works on Termux, Ubuntu/Debian, macOS and Linux.
# Needs Java 21 and an internet connection. The first build downloads
# Minecraft and Fabric, so give it a few minutes.
set -euo pipefail
cd "$(dirname "$0")"

echo "==> Checking Java"
if ! command -v java >/dev/null 2>&1; then
  echo "Java 21 is not installed."
  echo "  Termux : pkg install openjdk-21"
  echo "  Ubuntu : sudo apt install openjdk-21-jdk"
  exit 1
fi
JAVA_MAJOR="$(java -version 2>&1 | head -n 1 | sed -E 's/[^0-9]*([0-9]+).*/\1/')"
if [ "${JAVA_MAJOR:-0}" -lt 21 ]; then
  echo "Java ${JAVA_MAJOR} found, but Java 21 or newer is required."
  exit 1
fi

echo "==> Checking Gradle wrapper"
if [ ! -f gradlew ] || [ ! -f gradle/wrapper/gradle-wrapper.jar ]; then
  FOUND=""
  for dir in .. ../* "$HOME"/*; do
    if [ -f "$dir/gradlew" ] && [ -f "$dir/gradle/wrapper/gradle-wrapper.jar" ]; then
      FOUND="$dir"
      break
    fi
  done

  if [ -n "$FOUND" ]; then
    echo "    borrowing the wrapper from $FOUND"
    mkdir -p gradle/wrapper
    cp "$FOUND/gradlew" ./gradlew
    cp "$FOUND/gradle/wrapper/gradle-wrapper.jar" gradle/wrapper/gradle-wrapper.jar
    if [ -f "$FOUND/gradlew.bat" ]; then cp "$FOUND/gradlew.bat" ./gradlew.bat; fi
  elif command -v gradle >/dev/null 2>&1; then
    echo "    generating the wrapper with the installed Gradle"
    TMP="$(mktemp -d)"
    ( cd "$TMP" && gradle wrapper --gradle-version 8.12 >/dev/null )
    mkdir -p gradle/wrapper
    cp "$TMP/gradlew" ./gradlew
    cp "$TMP/gradlew.bat" ./gradlew.bat
    cp "$TMP/gradle/wrapper/gradle-wrapper.jar" gradle/wrapper/gradle-wrapper.jar
    rm -rf "$TMP"
  else
    cat <<'MSG'
No Gradle wrapper found. Do ONE of these, then run this script again:
  - copy "gradlew", "gradlew.bat" and "gradle/wrapper/gradle-wrapper.jar"
    from any of your other Fabric mods into this folder
  - or install Gradle (Termux: pkg install gradle)
MSG
    exit 1
  fi
fi
chmod +x ./gradlew

echo "==> Building (the first run downloads Minecraft and Fabric)"
./gradlew build "$@"

echo
echo "==> Done. Your mod jar:"
ls -1 build/libs/*.jar | grep -v -- '-sources' || true
