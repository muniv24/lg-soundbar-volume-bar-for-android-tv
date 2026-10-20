#!/bin/sh
# Builds android/out/volumebar.apk with the plain Android SDK tools (no Gradle).
# Needs Java 17 and the Android SDK with platform 34 and build-tools 34:
#   macOS:  brew install openjdk@17 && brew install --cask android-commandlinetools
#           sdkmanager "platforms;android-34" "build-tools;34.0.0"
set -e
cd "$(dirname "$0")"

# Find Java 17 and the SDK if they aren't set.
if [ -z "$JAVA_HOME" ]; then
  for j in /opt/homebrew/opt/openjdk@17 /usr/local/opt/openjdk@17 /usr/lib/jvm/java-17-openjdk-amd64; do
    if [ -d "$j" ]; then JAVA_HOME=$j; break; fi
  done
fi
if [ -n "$JAVA_HOME" ]; then export PATH="$JAVA_HOME/bin:$PATH"; fi
SDK=${ANDROID_HOME:-$ANDROID_SDK_ROOT}
if [ -z "$SDK" ]; then
  for d in /opt/homebrew/share/android-commandlinetools "$HOME/Library/Android/sdk" "$HOME/Android/Sdk"; do
    if [ -d "$d" ]; then SDK=$d; break; fi
  done
fi
TOOLS=$SDK/build-tools/34.0.0
ANDROID_JAR=$SDK/platforms/android-34/android.jar
if [ ! -f "$ANDROID_JAR" ] || [ ! -d "$TOOLS" ]; then
  echo "Android SDK not found. Set ANDROID_HOME and install platforms;android-34 and build-tools;34.0.0." >&2
  exit 1
fi

rm -rf out
mkdir -p out/gen out/classes out/dex

# 1. Resources (layout, strings, icons) -> base APK, plus R.java so the code can refer to them.
"$TOOLS/aapt2" compile --dir res -o out/res.zip
"$TOOLS/aapt2" link out/res.zip -o out/base.apk -I "$ANDROID_JAR" \
  --manifest AndroidManifest.xml --java out/gen

# 2. Java -> classes -> dex, added to the APK.
find src out/gen -name '*.java' > out/sources.txt
javac -nowarn -Xlint:-options -encoding UTF-8 -source 1.8 -target 1.8 \
  -bootclasspath "$ANDROID_JAR:$TOOLS/core-lambda-stubs.jar" -d out/classes @out/sources.txt
find out/classes -name '*.class' > out/classes.txt
"$TOOLS/d8" --min-api 26 --release --lib "$ANDROID_JAR" --output out/dex @out/classes.txt
cp out/base.apk out/unsigned.apk
(cd out/dex && zip -q ../unsigned.apk classes.dex)
"$TOOLS/zipalign" -f 4 out/unsigned.apk out/aligned.apk

# 3. Sign. The first build makes key.jks: keep it, updates must be signed with the same key.
KEY=${KEYSTORE:-key.jks}
PASS=${KEYSTORE_PASS:-volumebar}
if [ ! -f "$KEY" ]; then
  keytool -genkeypair -keystore "$KEY" -storepass "$PASS" -keypass "$PASS" -alias volumebar \
    -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Volume Bar" >/dev/null 2>&1
fi
"$TOOLS/apksigner" sign --ks "$KEY" --ks-pass "pass:$PASS" --out out/volumebar.apk out/aligned.apk
echo "Built android/out/volumebar.apk"
