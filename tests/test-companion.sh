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
expect_refusal race

export RVS_FAKE_MODE=valid
"$RVS_TEST_REPO/tools/rpi-voice-setup" prepare-enrollment \
    --serial fixture --yes >/dev/null
grep -qx 'kill -9 4242' "$RVS_FAKE_LOG"

export RVS_FAKE_MODE=doctor-mismatch
doctor_output=$("$RVS_TEST_REPO/tools/rpi-voice-setup" doctor --serial fixture)
grep -q 'Modèle: Raspberry Pi 4' <<<"$doctor_output"
grep -q 'APEX connu mais incompatible avec ce modèle' <<<"$doctor_output"
grep -q 'État policy: Pi 4 corrigée et reconnue' <<<"$doctor_output"

export RVS_FAKE_MODE=finish-valid
finish_output=$("$RVS_TEST_REPO/tools/rpi-voice-setup" finish-enrollment --serial fixture)
grep -q 'HOTWORD active sur le microphone USB' <<<"$finish_output"

printf 'OK: garde-fous prepare-enrollment validés\n'
