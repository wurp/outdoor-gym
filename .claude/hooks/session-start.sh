#!/usr/bin/env bash
# Runs when a Claude Code session starts. In a cloud session, it tries to
# install the Android SDK so Claude can check builds before pushing.
#
# This only works if the session's network settings allow dl.google.com.
# If they don't, it says so and exits: GitHub Actions still builds every push.
# Prints one line, which Claude reads. Never fails the session.

set -o pipefail  # no -u: an unset variable must never abort the session

# Only cloud sessions. On a developer's own computer, do nothing.
[ "${CLAUDE_CODE_REMOTE:-}" = "true" ] || exit 0

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(pwd)}"
SDK_DIR="${VIBE_SDK_DIR:-/opt/android-sdk}"  # override only for testing the script
CMDLINE_TOOLS_VERSION="13114758"
PACKAGES=("platform-tools" "platforms;android-36" "build-tools;35.0.0")

unavailable() {
  echo "In-session build: unavailable ($1). Don't try to build here; push and let GitHub Actions build."
  exit 0
}

available() {
  echo "sdk.dir=$SDK_DIR" > "$PROJECT_DIR/local.properties"
  if [ -n "${CLAUDE_ENV_FILE:-}" ]; then
    echo "export ANDROID_HOME=$SDK_DIR" >> "$CLAUDE_ENV_FILE"
  fi
  echo "In-session build: available. Check changes with 'gradle assembleDebug --console=plain' (not ./gradlew) before pushing."
  exit 0
}

# Already installed (for example, a resumed session).
if [ -d "$SDK_DIR/platforms/android-36" ] && [ -d "$SDK_DIR/build-tools/35.0.0" ]; then
  available
fi

command -v gradle >/dev/null 2>&1 || unavailable "Gradle isn't installed in this environment"

code=$(curl -sS -o /dev/null -w '%{http_code}' --max-time 10 \
  https://dl.google.com/android/repository/repository2-3.xml 2>/dev/null || echo "000")
[ "$code" = "200" ] || unavailable "dl.google.com is blocked by this environment's network settings"

# Java tools ignore HTTPS_PROXY. Pass the cloud proxy to sdkmanager explicitly.
PROXY_ARGS=()
proxy="${HTTPS_PROXY:-${https_proxy:-}}"
if [ -n "$proxy" ]; then
  hostport="${proxy#*://}"; hostport="${hostport#*@}"; hostport="${hostport%%/*}"
  PROXY_ARGS=(--proxy=http "--proxy_host=${hostport%%:*}" "--proxy_port=${hostport##*:}")
fi

mkdir -p "$SDK_DIR" 2>/dev/null || unavailable "can't write to $SDK_DIR"

tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
curl -fsSL --max-time 120 -o "$tmp/tools.zip" \
  "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip" \
  || unavailable "couldn't download the Android command-line tools"
unzip -q "$tmp/tools.zip" -d "$tmp" || unavailable "couldn't unpack the Android command-line tools"
rm -rf "$SDK_DIR/cmdline-tools/latest"
mkdir -p "$SDK_DIR/cmdline-tools"
mv "$tmp/cmdline-tools" "$SDK_DIR/cmdline-tools/latest"

sdkmanager="$SDK_DIR/cmdline-tools/latest/bin/sdkmanager"
# 'yes' exits with SIGPIPE (141) when sdkmanager stops reading; don't let that count as failure.
(yes || true) | "$sdkmanager" ${PROXY_ARGS[@]+"${PROXY_ARGS[@]}"} --sdk_root="$SDK_DIR" --licenses >/dev/null 2>&1
"$sdkmanager" ${PROXY_ARGS[@]+"${PROXY_ARGS[@]}"} --sdk_root="$SDK_DIR" "${PACKAGES[@]}" >/dev/null 2>&1 \
  || unavailable "the Android SDK install failed"

available
