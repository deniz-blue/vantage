#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

# Load env from .env.local (KEYSTORE_PATH, KEYSTORE_PASSWORD, KEY_ALIAS).
if [ -f .env.local ]; then
  set -a
  # shellcheck disable=SC1091
  source .env.local
  set +a
fi

: "${KEYSTORE_PATH:?Set KEYSTORE_PATH in .env.local}"
: "${KEYSTORE_PASSWORD:?Set KEYSTORE_PASSWORD in .env.local}"
: "${KEY_ALIAS:?Set KEY_ALIAS in .env.local}"

if [ ! -f "$KEYSTORE_PATH" ]; then
  echo "❌ Keystore not found at $KEYSTORE_PATH"
  exit 1
fi

echo "📦 Installing JS dependencies..."
pnpm install --frozen-lockfile

echo "🔨 Generating Android project (Expo CNG)..."
npx expo prebuild --platform android --clean --no-install

echo "🧩 Patching generated build.gradle with release signing config..."
node scripts/patch-gradle.mjs

echo "🔐 Injecting keystore..."
cp "$KEYSTORE_PATH" android/app/cos-release.keystore
cat > android/keystore.properties <<EOF
storeFile=cos-release.keystore
storePassword=${KEYSTORE_PASSWORD}
keyAlias=${KEY_ALIAS}
keyPassword=${KEYSTORE_PASSWORD}
EOF

echo "🚀 Building signed release..."
# Point Gradle at the Android SDK.
if [ -z "${ANDROID_HOME:-}" ] && [ -z "${ANDROID_SDK_ROOT:-}" ]; then
  if [ -d "$HOME/Android/Sdk" ]; then
    export ANDROID_HOME="$HOME/Android/Sdk"
    export ANDROID_SDK_ROOT="$ANDROID_HOME"
  fi
fi
: "${ANDROID_HOME:=\$ANDROID_SDK_ROOT}"
if [ -n "$ANDROID_HOME" ]; then
  echo "sdk.dir=$ANDROID_HOME" > android/local.properties
else
  echo "⚠️  ANDROID_HOME not set, android/local.properties was not created" >&2
fi

cd android
./gradlew bundleRelease assembleRelease \
  -x lint \
  -x test \
  --no-daemon

echo ""
echo "=== ✅ Build complete ==="
echo "AAB: $(pwd)/app/build/outputs/bundle/release/app-release.aab"
echo "APK: $(pwd)/app/build/outputs/apk/release/app-release.apk"
