#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0

set -Eeuo pipefail

project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
version=${VERSION:-0.1.0-test1}
dist_dir=${DIST_DIR:-"$project_dir/dist"}
work_dir=$(mktemp -d "${TMPDIR:-/tmp}/rpi-voice-setup-release.XXXXXX")
trap 'rm -rf "$work_dir"' EXIT

sha256_file() {
    if command -v sha256sum >/dev/null 2>&1; then
        sha256sum "$1" | awk '{print $1}'
    else
        shasum -a 256 "$1" | awk '{print $1}'
    fi
}

cd "$project_dir"
tests/test-companion.sh
./gradlew :app:assembleDebug :app:lintDebug --no-daemon

apk_source="$project_dir/app/build/outputs/apk/debug/app-debug.apk"
[[ -s "$apk_source" ]] || { printf 'APK absente\n' >&2; exit 1; }

rm -rf "$dist_dir"
mkdir -p "$dist_dir"
apk_name="raspberry-voice-setup-v$version.apk"
companion_name="rpi-voice-setup-adb-v$version"
bundle_name="rpi-android-voice-setup-v$version.zip"
cp "$apk_source" "$dist_dir/$apk_name"
cp "$project_dir/tools/rpi-voice-setup" "$dist_dir/$companion_name"
chmod 0755 "$dist_dir/$companion_name"

stage="$work_dir/bundle"
mkdir -p "$stage"
cp "$dist_dir/$apk_name" "$dist_dir/$companion_name" "$stage/"
cp "$project_dir/docs/LISEZ-MOI-RELEASE-FR.txt" "$stage/LISEZ-MOI-FR.txt"
cp "$project_dir/LICENSE" "$project_dir/NOTICE" "$stage/"
(
    cd "$stage"
    for file in "$apk_name" "$companion_name" LISEZ-MOI-FR.txt LICENSE NOTICE; do
        printf '%s  %s\n' "$(sha256_file "$file")" "$file"
    done > PACKAGE-SHA256SUMS
    find . -exec touch -t 202608200000 {} +
    zip -X -q -r "$dist_dir/$bundle_name" .
)
unzip -tq "$dist_dir/$bundle_name" >/dev/null

(
    cd "$dist_dir"
    for file in "$apk_name" "$companion_name" "$bundle_name"; do
        printf '%s  %s\n' "$(sha256_file "$file")" "$file"
    done > SHA256SUMS
)

printf 'Release construite et verifiee dans %s\n' "$dist_dir"
cat "$dist_dir/SHA256SUMS"
