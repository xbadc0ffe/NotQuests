#!/usr/bin/env bash
#
# Focused server-side E2E sweep for the BetonQuest 3.x integration.
#
# Boots a real Paper server with BetonQuest 3.0.0 installed from paper/libs, registers a tiny
# BetonQuest package that uses every restored NotQuests hook, then drives the NotQuests admin
# commands that create BetonQuest-backed actions, rewards, objectives, and variables.
set -euo pipefail

E2E="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$E2E/.." && pwd)"
cd "$REPO"

LOG="${E2E_BETONQUEST_LOG:-/tmp/nq-betonquest-server.log}"
STRIPPED_LOG="${E2E_BETONQUEST_STRIPPED_LOG:-/tmp/nq-betonquest-server.clean.log}"
FIFO="${E2E_BETONQUEST_FIFO:-/tmp/nq-betonquest.fifo}"
RUN="$REPO/plugin/run"
BETONQUEST_JAR="$REPO/paper/libs/BetonQuest-3.0.0.jar"
BOOT_TIMEOUT_STEPS="${E2E_BOOT_STEPS:-240}" # x2s = 8 min max for first-time Paper setup

cleanup() { kill "${HOLDER:-}" "${GPID:-}" 2>/dev/null || true; rm -f "$FIFO"; }
trap cleanup EXIT

if [ ! -f "$BETONQUEST_JAR" ]; then
  echo "::error:: missing vendored BetonQuest test jar: $BETONQUEST_JAR"
  exit 1
fi

# Exit code reserved for "the harness itself broke", as distinct from 1 = "an assertion about
# NotQuests failed". Keeping those apart is the whole point of the assertion block below.
HARNESS_ERROR=2

# Every assertion in this file is a grep scan. Check the binary exists BEFORE the ~8 minute
# server boot: a missing tool should cost a second, not a CI slot — and it must never reach
# the assertion block, where a 127 could be mistaken for a failing assertion.
if ! command -v grep >/dev/null 2>&1; then
  echo "::error:: harness error: grep is not available; no assertion in this sweep can be evaluated."
  exit "$HARNESS_ERROR"
fi

rm -f "$LOG" "$STRIPPED_LOG" "$FIFO"
mkfifo "$FIFO"
mkdir -p "$RUN/plugins/BetonQuest/QuestPackages/nqtest"
echo "eula=true" > "$RUN/eula.txt"
cp "$BETONQUEST_JAR" "$RUN/plugins/BetonQuest-3.0.0.jar"

# Keep BetonQuest's async updater out of deterministic CI logs.
(
  cd "$RUN/plugins/BetonQuest"
  jar xf "$BETONQUEST_JAR" config.yml
)
perl -0pi -e 's/(updater:\s*\n\s*enabled:\s*)true/${1}false/' "$RUN/plugins/BetonQuest/config.yml"

cat > "$RUN/plugins/BetonQuest/QuestPackages/nqtest/package.yml" <<'YAML'
actions:
  testAction: "notify BetonQuest action ran"
  notquestsAction: "nq_action SendMessage Hello from BetonQuest"
  notquestsTriggerObjective: "nq_triggerobjective SomeTrigger"
  notquestsStartQuest: "nq_startquest BQQuest -force -silent -notriggers"
  notquestsFailQuest: "nq_failquest BQQuest"
  notquestsAbortQuest: "nq_abortquest BQQuest"
  notquestsQuestPoints: "nq_questpoints add 1 -silent"
conditions:
  testCondition: "permission notquests.e2e.betonquest"
  notquestsCondition: "nq_condition Boolean Flying equals true"
objectives:
  testObjective: "delay 1"
YAML

rm -rf "$RUN/world" "$RUN/world_nether" "$RUN/world_the_end" "$RUN/plugins/NotQuests" 2>/dev/null
cat > "$RUN/server.properties" <<'PROPS'
online-mode=false
generate-structures=false
spawn-protection=0
spawn-npcs=false
spawn-animals=false
spawn-monsters=false
view-distance=3
simulation-distance=3
max-players=5
allow-nether=false
allow-end=false
PROPS

echo "BETONQUEST_DRIVER_START"
tail -f /dev/null > "$FIFO" &
HOLDER=$!
./gradlew :plugin:runServer --console=plain --no-daemon < "$FIFO" > "$LOG" 2>&1 &
GPID=$!

state=TIMEOUT
for i in $(seq 1 "$BOOT_TIMEOUT_STEPS"); do
  if grep -q 'Done (' "$LOG" 2>/dev/null; then state=READY; break; fi
  if grep -qiE 'Error occurred while enabling|BUILD FAILED|Could not resolve dependencies' "$LOG" 2>/dev/null; then state=ENABLE_FAIL; break; fi
  if ! kill -0 "$GPID" 2>/dev/null; then state=GRADLE_DIED; break; fi
  sleep 2
done
echo "BOOT_STATE=$state after ~$((i*2))s"

if [ "$state" != "READY" ]; then
  echo "::error:: BetonQuest E2E server never reached READY ($state). Last 80 log lines:"
  tail -80 "$LOG"
  exit 1
fi

{
  echo "qa create BQQuest"
  echo "qa actions add BQFire BetonQuestFireEvent nqtest testAction"
  echo "qa actions add BQInline BetonQuestFireInlineEvent notify Inline from NotQuests"
  echo "qa edit BQQuest rewards add BetonQuestFireEvent nqtest testAction"
  echo "qa edit BQQuest rewards add BetonQuestFireInlineEvent notify Inline reward from NotQuests"
  echo "qa edit BQQuest objectives add BetonQuestObjectiveStateChange nqtest testObjective COMPLETED"
  echo "qa variables check BetonQuestCondition nqtest testCondition"
  echo "qa edit BQQuest objectives list"
  echo "qa actions"
  echo "save-all flush"
} > "$FIFO"

for i in $(seq 1 90); do
  grep -q 'Saved the game' "$LOG" 2>/dev/null && break
  sleep 1
done

echo "stop" > "$FIFO" 2>/dev/null || true
for i in $(seq 1 45); do
  kill -0 "$GPID" 2>/dev/null || break
  sleep 1
done

# Collect THIS run's command schema and runtime metadata, under names distinct from the base
# sweep's copies (staged by run-sweep.sh) — the two exports are the comparison, so neither may
# overwrite the other. Two things fall out of diffing them:
#
#  * whether `integrationOnly` means anything. Every entry in a no-integration export carries
#    integrationOnly: false, because gated types are never registered rather than
#    registered-and-flagged. Only an export taken with the host plugin PRESENT can say whether
#    the field carries information — which decides whether it can be trusted in the exclusion
#    logic of a future coverage gate.
#  * whether BetonQuestFireEvent, BetonQuestFireInlineEvent and BetonQuestObjectiveStateChange
#    actually registered — a second read on "did BetonQuest enable", independent of every
#    assertion below.
#
# If BetonQuest failed to enable, this export may be byte-identical to the base sweep's. That
# identity is itself the signal, not a collection failure.
#
# Evidence collection, never an assertion: a missing file prints a note and the sweep continues.
for generated in commands metadata; do
  generated_src="$RUN/plugins/NotQuests/generated/$generated.json"
  generated_dest="/tmp/nq-betonquest-$generated.json"
  if [ -f "$generated_src" ]; then
    cp "$generated_src" "$generated_dest" || echo "note: could not copy $generated_src to $generated_dest"
  else
    echo "note: $generated_src was not written; no BetonQuest-run $generated export to collect"
  fi
done

perl -pe 's/\e\[[0-9;]*m//g' "$LOG" > "$STRIPPED_LOG"

# ---------------------------------------------------------------------------------------
# Assertions.
#
# These used to run through ripgrep, which is NOT installed on GitHub-hosted ubuntu-24.04
# runners. The bug was not "the tool is missing" — it was that a missing tool was
# indistinguishable from a failing assertion. The old `if ! <scan> -q ...` form puts the scan
# in a condition context, where 127 (command not found) and 1 (pattern not found) both take
# the failure branch, so run 32271726659 printed
#     ::error:: NotQuests did not enable BetonQuest support.
# and then swallowed its own diagnostic dump with `|| true` — an accusation against the
# plugin with zero supporting evidence. The same conflation ran the other way at the final
# error scan, where a 127 landed on the "no errors found" branch and would have produced a
# false PASS.
#
# So: capture grep's exit code explicitly everywhere and branch on all three outcomes.
#     0  -> matched
#     1  -> did not match
#     >1 -> grep itself failed (missing file, unreadable file, bad pattern) => harness error
#
# COLLECT-ALL. A failing assertion (rc=1) is recorded and execution CONTINUES; the script
# exits 1 after a summary of every assertion. A harness error (rc>1) still aborts on the spot,
# because verdicts produced by an unreliable harness are not evidence.
#
# Why collect-all is safe here: every line these assertions read was written by the server run
# and ANSI-stripped at :130, before the first assertion evaluates. No assertion can affect what
# a later one sees, so stopping early discards information that is already on disk for free.
# Expect cascades — if BetonQuest never enabled, 2-9 all fail — and read them as signal: a full
# cascade means "BetonQuest absent", whereas 1-3 passing with a single later failure means
# "BetonQuest enabled but one nq_* hook did not register". A first-failure abort cannot tell
# those apart.
# ---------------------------------------------------------------------------------------

ASSERTION_FAILED=0
DIAGNOSTICS_DUMPED=0
SUMMARY=""

summary_add() {   # summary_add <PASS|FAIL|note> <label>
  SUMMARY+="$(printf '   %-4s  %s' "$1" "$2")"$'\n'
}

# Context for a failed assertion. Never silenced: a diagnostic scan that cannot run has to
# say so, otherwise we are back to accusations without evidence.
dump_diagnostics() {
  local rc=0
  grep -nE -- 'BetonQuest|BQQuest|Incorrect argument|Unknown or incomplete|Exception|ERROR' "$STRIPPED_LOG" || rc=$?
  case "$rc" in
    0) ;;
    1) echo "   diagnostics: no BetonQuest/BQQuest/error lines matched in $STRIPPED_LOG" ;;
    *) echo "::error:: harness error: diagnostic scan failed (grep exit $rc) on $STRIPPED_LOG" ;;
  esac
}

# One dump is enough. During a cascade, repeating it per failure would bury the summary that
# makes the cascade legible.
dump_diagnostics_once() {
  if [ "$DIAGNOSTICS_DUMPED" -ne 0 ]; then
    echo "   diagnostics: see the dump above (same log, unchanged since the first failure)"
    return 0
  fi
  DIAGNOSTICS_DUMPED=1
  dump_diagnostics
}

# NOTE: returns 0 on BOTH the pass and the fail path, deliberately. The verdict lives in
# ASSERTION_FAILED. Returning non-zero would trip `set -e` at the call site and re-introduce
# the fail-fast behaviour this function exists to avoid.
assert_log() {
  local label="$1"
  local pattern="$2"
  local message="$3"
  local rc=0
  grep -qE -- "$pattern" "$STRIPPED_LOG" || rc=$?
  case "$rc" in
    0)
      summary_add PASS "$label"
      return 0
      ;;
    1)
      ASSERTION_FAILED=1
      summary_add FAIL "$label"
      echo "::error:: $message"
      echo "   assertion pattern: $pattern"
      echo "   searched         : $STRIPPED_LOG"
      dump_diagnostics_once
      return 0
      ;;
    *)
      echo "::error:: harness error: grep exited $rc evaluating an assertion; this sweep proved nothing."
      echo "   assertion pattern: $pattern"
      echo "   searched         : $STRIPPED_LOG"
      echo "   would have reported: $message"
      exit "$HARNESS_ERROR"
      ;;
  esac
}

# 1. The integration enabled at all.
#    managers/integrations/Integration.java emits two DIFFERENT lines depending on load order:
#      :146 -> "BetonQuest found. Enabled BetonQuest support!"
#      :144 -> "BetonQuest found. Enabled BetonQuest support (late)!"
#    Accept both. A late enable is a functioning integration, not a defect, and failing on it
#    would blame the plugin for Paper's plugin ordering.
assert_log '1  integration enabled (on-time or late)' \
  'BetonQuest found\. Enabled BetonQuest support( \(late\))?!' \
  'NotQuests did not enable BetonQuest support (neither the on-time nor the late path fired).'

# ...then record WHICH path fired. plugin/build.gradle.kts:284-286 declares
# `register("BetonQuest") { load = BEFORE }` intending the on-time path; this line is the only
# evidence of what Paper actually does with it.
# Probe for BOTH variants rather than treating "not late" as "on-time" — otherwise a run where
# the integration never enabled at all would still report a load order, which is nonsense.
enable_late_rc=0
grep -qE -- 'BetonQuest found\. Enabled BetonQuest support \(late\)!' "$STRIPPED_LOG" || enable_late_rc=$?
enable_ontime_rc=0
grep -qE -- 'BetonQuest found\. Enabled BetonQuest support!' "$STRIPPED_LOG" || enable_ontime_rc=$?
if [ "$enable_late_rc" -eq 0 ]; then
  echo "BETONQUEST_ENABLE_PATH=late     # Integration.java:144 - BetonQuest enabled after NotQuests"
  summary_add note '1b enable path: LATE (Integration.java:144)'
elif [ "$enable_ontime_rc" -eq 0 ]; then
  echo "BETONQUEST_ENABLE_PATH=on-time  # Integration.java:146 - BetonQuest enabled before NotQuests"
  summary_add note '1b enable path: on-time (Integration.java:146)'
elif [ "$enable_late_rc" -eq 1 ] && [ "$enable_ontime_rc" -eq 1 ]; then
  echo "BETONQUEST_ENABLE_PATH=none     # neither Integration.java:144 nor :146 fired"
  summary_add note '1b enable path: NONE - integration never enabled'
else
  echo "BETONQUEST_ENABLE_PATH=unknown  # probe failed (grep exits: late=$enable_late_rc on-time=$enable_ontime_rc)"
  summary_add note '1b enable path: UNKNOWN (probe failed)'
fi

# 2. The conversation interceptor registered.
assert_log '2  conversation interceptor registered' \
  'Registered BetonQuest interceptor: notquests' \
  'NotQuests did not register the BetonQuest conversation interceptor.'

# 3. BetonQuest parsed the nqtest fixture. Its summary line is built from the format string
#    `There are [%s] loaded from %s packages.`
#    (org.betonquest.betonquest.kernel.DefaultProcessorDataLoader). Only the format-string
#    literals are asserted here; the ORDER of the elements inside [...] and their
#    singular/plural rendering are BetonQuest's business and must not fail this sweep. Each
#    count is checked independently so a failure names which one was wrong instead of failing
#    opaquely on one long ordering-coupled regex.
assert_log '3a package count == 1' \
  'loaded from 1 packages\.' \
  'BetonQuest did not load exactly 1 quest package (expected only the nqtest fixture).'
assert_log '3b   ...containing 7 Actions' \
  'There are \[(7 Actions?|.*, 7 Actions?)' \
  'BetonQuest did not load 7 actions from nqtest (6 of them are the restored nq_* hooks).'
assert_log '3c   ...containing 2 Conditions' \
  'There are \[(2 Conditions?|.*, 2 Conditions?)' \
  'BetonQuest did not load 2 conditions from nqtest (one is the restored nq_condition hook).'
assert_log '3d   ...containing 1 Objective' \
  'There are \[(1 Objectives?|.*, 1 Objectives?)' \
  'BetonQuest did not load 1 objective from nqtest.'

# 4-9. NotQuests' own command output.
assert_log '4  BQFire saved action created' \
  'BetonQuestFireEvent Action with the name BQFire has been created successfully!' \
  'BetonQuestFireEvent saved-action command failed.'
assert_log '5  BQInline saved action created' \
  'BetonQuestFireInlineEvent Action with the name BQInline has been created successfully!' \
  'BetonQuestFireInlineEvent saved-action command failed.'
assert_log '6  BQFire reward added to quest' \
  'BetonQuestFireEvent Reward successfully added to Quest BQQuest!' \
  'BetonQuestFireEvent reward command failed.'
assert_log '7  BQInline reward added to quest' \
  'BetonQuestFireInlineEvent Reward successfully added to Quest BQQuest!' \
  'BetonQuestFireInlineEvent reward command failed.'
assert_log '8  BQ objective added to quest' \
  'BetonQuestObjectiveStateChange Objective successfully added to Quest BQQuest!' \
  'BetonQuest objective command failed.'
assert_log '9  BQCondition variable evaluated' \
  'BetonQuestCondition variable \(BOOLEAN\) result for player unknown: false' \
  'BetonQuestCondition variable check command failed.'

# Final negative scan. Independent evidence, so it runs regardless of how the assertions above
# went. The polarity here is the inverse of assert_log, and it used to hide the same 127 bug:
# the old `if <scan> -n ...` form treated ANY non-zero exit as "no errors", so a missing binary
# fell straight through to RESULT: PASS. Only exit 1 means clean.
error_scan_rc=0
grep -nE -- 'Incorrect argument|Unknown or incomplete|Exception|ERROR|Could not pass event|NoClassDefFoundError|zip file closed' "$STRIPPED_LOG" || error_scan_rc=$?
case "$error_scan_rc" in
  0)
    ASSERTION_FAILED=1
    summary_add FAIL '-  no parser/runtime errors in log'
    echo "::error:: BetonQuest E2E sweep produced parser/runtime errors (matching lines above)."
    ;;
  1)
    summary_add PASS '-  no parser/runtime errors in log'
    ;;
  *)
    echo "::error:: harness error: final error scan failed (grep exit $error_scan_rc) on $STRIPPED_LOG"
    echo "   the sweep cannot claim PASS without having run this scan."
    exit "$HARNESS_ERROR"
    ;;
esac

echo
echo "=============== BetonQuest E2E assertion summary ==============="
printf '%s' "$SUMMARY"
echo "==============================================================="
echo "   A full FAIL cascade from 1 downward means BetonQuest never enabled."
echo "   1-3 passing with an isolated later FAIL means BetonQuest enabled but"
echo "   a specific NotQuests hook or command did not work."
echo

if [ "$ASSERTION_FAILED" -ne 0 ]; then
  echo "RESULT: FAIL"
  exit 1
fi

echo "RESULT: PASS"
