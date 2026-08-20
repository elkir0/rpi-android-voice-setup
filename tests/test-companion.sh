#!/usr/bin/env bash
set -Eeuo pipefail

RVS_TEST_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
RVS_TEST_REPO=$(CDPATH= cd -- "$RVS_TEST_DIR/.." && pwd)
RVS_TEST_TMP=$(mktemp -d)
trap 'rm -rf "$RVS_TEST_TMP"' EXIT

export RVS_ADB_BIN="$RVS_TEST_DIR/fake-adb"
export RVS_FAKE_LOG="$RVS_TEST_TMP/adb.log"
: > "$RVS_FAKE_LOG"

expect_refusal() {
    local mode=$1
    export RVS_FAKE_MODE=$mode
    if "$RVS_TEST_REPO/tools/rpi-voice-setup" prepare-enrollment \
            --serial fixture --yes >/dev/null 2>&1; then
        printf 'ECHEC: le mode %s aurait dû être refusé\n' "$mode" >&2
        exit 1
    fi
    [[ ! -s "$RVS_FAKE_LOG" ]] || {
        printf 'ECHEC: un kill a été envoyé en mode %s\n' "$mode" >&2
        exit 1
    }
}

expect_refusal none
expect_refusal wrong-user
expect_refusal multiple

export RVS_FAKE_MODE=valid
"$RVS_TEST_REPO/tools/rpi-voice-setup" prepare-enrollment \
    --serial fixture --yes >/dev/null
grep -qx 'kill -9 4242' "$RVS_FAKE_LOG"

printf 'OK: garde-fous prepare-enrollment validés\n'

