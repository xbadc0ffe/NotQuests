#!/usr/bin/env python3
"""
Analyze a NotQuests E2E server-sweep run.

Two guarantees, both fail the build (exit 1) when violated:

  1. COVERAGE — every objective/action/condition/trigger type registered in the source
     (minus documented integration-gated and variable-based types) has at least one test command
     in commands.txt. A newly added type that ships without a test command fails CI automatically.

  2. CORRECTNESS — no command may produce a parser/handler error, exception or stack trace, except
     commands explicitly annotated as tolerated (# PLAYER-ONLY / # NEEDS-ECONOMY / # UNSURE /
     # OBJECTIVE-SCOPED), which only run for completeness.

Attribution does NOT rely on echo markers (the server defers `say` output, so markers don't
interleave with command output). Instead, Brigadier prints every parse failure with the offending
command via a "...<command><--[HERE]" line, which we match back to commands.txt. Exceptions / stack
traces / custom "Cannot parse" errors have no such echo and are always treated as failures.

Usage: analyze.py <server.log>
"""
import os
import re
import sys
import pathlib
import json

E2E = pathlib.Path(__file__).resolve().parent
SRC = E2E.parent / "paper" / "src" / "main" / "java"
CMDS_FILE = E2E / "commands.txt"
GENERATED = E2E.parent / "plugin" / "run" / "plugins" / "NotQuests" / "generated"
# E2E_METADATA / E2E_COMMAND_SCHEMA let the harness be pointed at a downloaded CI artifact
# instead of a local run, so the gate can be verified against the exact bytes CI produced.
COMMAND_METADATA = pathlib.Path(os.environ.get("E2E_METADATA", GENERATED / "metadata.json"))
COMMAND_SCHEMA = pathlib.Path(os.environ.get("E2E_COMMAND_SCHEMA", GENERATED / "commands.json"))

REGISTRY_KINDS = ("objectives", "actions", "conditions", "triggers", "variables")
# The four kinds whose types are created through a `/nqa ... add <Type>` command and therefore
# can be exercised by commands.txt. `variables` is excluded from the coverage assertion — see
# the VARIABLE COVERAGE note in main().
COVERED_KINDS = ("objectives", "actions", "conditions", "triggers")
# Builder entry point per kind: `objectives.objective("KillMobs")`, `actions.action("Beam")`, …
# The receiver name is pinned deliberately; matching any `*.trigger(` also catches
# `trigger.trigger(activeQuest)` in triggers/ActiveTrigger.java, which is not a registration.
CATALOG_ENTRY = {"objectives": "objective", "actions": "action",
                 "conditions": "condition", "triggers": "trigger"}
# Minimum registered count per kind. Floors, not equalities: an exact count turns every new
# type into a CI failure, which is the friction that gets assertions deleted. Values are one
# safe step below the counts confirmed identical in CI and locally (33/26/8/8/66).
REGISTRY_FLOORS = {"objectives": 30, "actions": 24, "conditions": 8, "triggers": 8, "variables": 60}
# Tolerated edge errors are a ceiling, not an equality, for the same reason: removing a
# tolerated command is an improvement and must not fail the build.
TOLERATED_CEILING = 6

# Types that only register when their host plugin is installed, so an integration-free sweep
# can never exercise them. This list is load-bearing, not decorative: assertion (b) fails if a
# gated type is missing from it, and assertion (c) fails if an entry no longer exists in the
# source. Grouped by host so an entry's reason is visible without grepping.
EXCLUDE_INTEGRATION = {
    # EliteMobs
    "KillEliteMobs",
    # Citizens (escort routing)
    "EscortNPC",
    # Jobs Reborn
    "JobsRebornReachJobLevel",
    # Slimefun
    "SlimefunResearch",
    # Towny
    "TownyNationReachTownCount", "TownyReachResidentCount",
    "TownyNationName", "TownyNationTownCount", "TownyTownResidentCount", "TownyTownPlotCount",
    # PlaceholderAPI
    "PlaceholderAPINumber", "PlaceholderAPIString",
    # Floodgate
    "FloodgateIsFloodgatePlayer",
    # BetonQuest 3
    "BetonQuestObjectiveStateChange", "BetonQuestFireEvent", "BetonQuestFireInlineEvent",
    "BetonQuestCondition",
}
EXCLUDE_VARIABLE = {"Number", "String", "Boolean", "List", "ItemStackList"}
TOLERANT_TAGS = ("PLAYER-ONLY", "NEEDS-ECONOMY", "UNSURE", "OBJECTIVE-SCOPED")

HERE = re.compile(r"<--\[HERE\]\s*$")
TS = re.compile(r"^\[\d\d:\d\d:\d\d INFO\]:\s*")
# echo-less hard errors (crashes / custom parser failures) — never expected, always fail.
# Stack-frame lines ("at com.example...") are skipped separately; flagging the headline is enough.
#
# `\bException\b` used to carry this and caught almost nothing. \b needs a word/non-word
# transition, and in "ClassCastException" the character before "Exception" is "t" — a word
# character — so no boundary exists there. Every qualified exception name passed straight
# through: ClassCastException, IllegalArgumentException, IllegalStateException,
# NoClassDefFoundError. Only a bare "Exception" or the explicitly listed NullPointerException
# was ever flagged. The 11 ClassCastExceptions seen on the bench server in
# registry/ObjectiveType.handleEvent would not have failed this sweep.
#
# `[A-Za-z]*(?:Exception|Error)\b` matches the whole qualified name instead, and the trailing
# \b is what keeps plurals like "Suppressed 4 Exceptions" out. Paper prints "Could not pass
# event ... to NotQuests" as the headline for a throw inside an event handler, with the
# exception type on the following line and "Caused by:" for the chain, so those are matched
# too — an event-handler crash otherwise shows nothing else this pattern would see.
#
# Blast radius measured against the known-good logs from CI run 32313184030
# (nq-e2e-server.log, 1082 lines; nq-betonquest-server.clean.log, 443 lines): 0 false
# positives on either, so no allowlist is carried — each entry would be a permanent hole.
# Case is deliberate: lowercase "error" occurs in ordinary Paper output and is not matched.
CRASH = re.compile(
    r"Cannot parse|Cannot invoke|Could not pass event|"
    r"[A-Za-z]*(?:Exception|Error)\b(?!s\b)|"
    r"Caused by:|Unhandled exception")

# NQCommandManager warns and CONTINUES when a command fails to register or an export fails
# (commands/framework/NQCommandManager.java:197, :202, :207). Nothing else notices: the
# commands that did register behave normally, metadata.json is still written, and the schema
# gate still passes — so the sweep could go green on a half-registered command tree. Verified
# against the known-good logs from CI run 32313184030: 17 WARN lines, none matching (they are
# all JVM sun.misc.Unsafe / restricted-method / offline-mode notices).
REGISTRATION_WARNING = re.compile(r"Failed to register native command|Failed to export")


def runtime_types(metadata):
    """Types actually registered by the running plugin, from its own metadata export.

    This replaces a source regex that matched `registerObjective("Name"` — an API the
    upstream merge deleted. Every surviving registerX call takes a definition object, never
    a string literal, so the old scanner returned four empty sets and the coverage gate was
    a permanent no-op that printed `objective=0` on every run for months. Reading the
    plugin's own export cannot drift from the registration API that way.
    """
    registry = metadata.get("registry", {})
    return {kind: {entry.get("id") for entry in registry.get(kind, []) if entry.get("id")}
            for kind in REGISTRY_KINDS}


def source_types():
    """Every type name registered anywhere in the Java source, by kind.

    Two idioms are in use and BOTH must be resolved:
        objectives.objective("BreakBlocks")            — 25 of 40 objectives, all actions,
                                                          conditions and triggers
        public static final String TYPE = "KillMobs";  — the other 15 objectives
        objectives.objective(TYPE)
    A literal-only scanner finds 25 of 40 and looks healthy doing it, which is a worse
    failure than finding zero. Variables use a third entry point, registerVariable("Name", …),
    and must be included or EXCLUDE_INTEGRATION's variable entries (TownyNationName) look
    stale when they are not.
    """
    found = {kind: set() for kind in REGISTRY_KINDS}
    unresolved = []
    for path in SRC.rglob("*.java"):
        text = path.read_text(errors="replace")
        constants = dict(re.findall(
            r'static\s+final\s+String\s+([A-Za-z_]\w*)\s*=\s*"([^"]+)"', text))

        def take(kind, arg, where):
            if arg.startswith('"'):
                found[kind].add(arg.strip('"'))
            elif arg in constants:
                found[kind].add(constants[arg])
            else:
                unresolved.append(f"{path.name}: {where}({arg})")

        for kind, method in CATALOG_ENTRY.items():
            for arg in re.findall(r'\b' + kind + r'\.' + method + r'\(\s*([^)\s,]+)\s*\)', text):
                take(kind, arg, f"{kind}.{method}")
        for arg in re.findall(r'\bregisterVariable\(\s*([^,\s]+)\s*,', text):
            take("variables", arg, "registerVariable")

    if unresolved:
        sys.exit(
            "COVERAGE SCANNER ERROR — type name(s) could not be resolved to a string literal:\n"
            + "\n".join("   - " + u for u in unresolved)
            + "\n\nThe scanner refuses to guess. Either a name is now built dynamically, which\n"
              "breaks this gate's premise, or a new registration idiom was introduced and\n"
              "source_types() needs teaching. Do NOT 'fix' this by skipping the name — silently\n"
              "dropping unresolvable names is exactly how a scanner under-counts and still\n"
              "reports success.")
    return found


def load_commands():
    out = []
    for raw in CMDS_FILE.read_text().splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        tolerant = any(t in line for t in TOLERANT_TAGS)
        cmd = re.sub(r"\s+#.*$", "", line).strip()
        if cmd.startswith("/"):
            cmd = cmd[1:]
        out.append((cmd, tolerant))
    return out


def echo_command(line):
    s = TS.sub("", line).rstrip()
    s = HERE.sub("", s)
    return s.lstrip(".").strip()


def main():
    log_path = sys.argv[1] if len(sys.argv) > 1 else "/tmp/nq-e2e-server.log"
    log = pathlib.Path(log_path).read_text(errors="replace").splitlines()
    commands = load_commands()
    tol_by_cmd = {c: t for c, t in commands}
    all_cmds = [c for c, _ in commands]

    # ---------- coverage ----------
    # Three assertions over two independently-derived sets. RUNTIME is what the plugin says it
    # registered; SOURCE is what the tree declares. Their difference is the integration-gated
    # set, and every member of it must be excluded BY HAND — `integrationOnly` in the export is
    # false on all 286 entries across an integration-free run and a run with the host plugin
    # present, so it cannot carry the exclusion (measured, CI run 32313184030).
    reg = {kind: set() for kind in REGISTRY_KINDS}
    coverage = []
    uncovered_variables = []
    metadata = read_metadata()
    if metadata is None:
        coverage.append(f"metadata was not written ({COMMAND_METADATA}); the coverage gate could not run")
    else:
        reg = runtime_types(metadata)
        src = source_types()
        blob = "\n".join(all_cmds)

        def covered(name):
            return re.search(r"(?<![A-Za-z0-9])" + re.escape(name) + r"(?![A-Za-z0-9])", blob)

        # (a) everything that registered in an integration-free run must have a test command
        for kind in COVERED_KINDS:
            for n in sorted(reg[kind]):
                if n in EXCLUDE_VARIABLE:
                    continue
                if not covered(n):
                    coverage.append(f"(a) {kind[:-1]} {n} registered at runtime but has no command in commands.txt")

        # (b) everything in source but NOT registered is integration-gated and must be listed
        gated = set()
        for kind in REGISTRY_KINDS:
            gated |= src[kind] - reg[kind]
        for n in sorted(gated - EXCLUDE_INTEGRATION):
            coverage.append(f"(b) {n} is integration-gated (declared in source, never registered) "
                            f"but is not listed in EXCLUDE_INTEGRATION")

        # (c) the exclusion list must not rot in the other direction
        all_source = set().union(*src.values())
        for n in sorted(EXCLUDE_INTEGRATION - all_source):
            coverage.append(f"(c) EXCLUDE_INTEGRATION lists {n}, which no longer exists in the source")

        # (d) floors — a catastrophic registration failure must be loud, not a silent zero
        for kind in REGISTRY_KINDS:
            count = len(reg[kind])
            if count == 0:
                coverage.append(f"(d) {kind} registry is EMPTY — nothing registered at runtime")
            elif count < REGISTRY_FLOORS[kind]:
                coverage.append(f"(d) {kind} = {count}, below the floor of {REGISTRY_FLOORS[kind]}")

        # VARIABLE COVERAGE: reported, not asserted. 50 of 66 registered variables have no
        # `qa variables check` line, so making this fail would ship a red gate on day one and
        # the assertion would simply be deleted. Printed every run so the number cannot be
        # forgotten; promote it to a hard assertion once commands.txt covers them.
        uncovered_variables = sorted(n for n in reg["variables"] if not covered(n))

    # ---------- correctness ----------
    ready = any("Done (" in l for l in log)
    real_fails, tolerated = [], []
    for line in log:
        if HERE.search(line):                       # Brigadier parse failure (has the command echo)
            # The echo shows the input UP TO the failure cursor (then truncated from the left with
            # "..."), so the visible tail is a SUBSTRING of the command — not necessarily a suffix
            # (mid-command failures cut before the end). Match by containment; with several matches
            # stay conservative: only tolerate if every matching command is tolerated.
            tail = echo_command(line)
            matches = [c for c in all_cmds if tail and tail in c]
            if matches:
                tol = all(tol_by_cmd[c] for c in matches)
                rec = (matches[0], line.strip())
                (tolerated if tol else real_fails).append(rec)
            else:
                real_fails.append((f"<unmatched echo: {tail}>", line.strip()))
        elif CRASH.search(line) and not line.strip().startswith("at "):
            # echo-less crash / custom parser error: never expected, can't be attributed -> fail
            real_fails.append(("<crash / unattributed>", line.strip()))

    registration_failures = [l.strip() for l in log if REGISTRATION_WARNING.search(l)]

    # ---------- report ----------
    print("=" * 72)
    print("NotQuests E2E sweep analysis")
    print("=" * 72)
    print(f"server reached READY : {ready}")
    print(f"commands in sweep    : {len(commands)}")
    print("registered types     : " + ", ".join(f"{k}={len(reg[k])}" for k in REGISTRY_KINDS))
    print(f"tolerated edge errors: {len(tolerated)} (ceiling {TOLERATED_CEILING})")
    # Always printed, pass or fail. A gate that only speaks up when it fails gives no evidence
    # it ran at all — which is how `objective=0` went unnoticed on every run for months.
    labels = {"(a)": "runtime types have a test command",
              "(b)": "gated types are excluded by hand",
              "(c)": "exclusions still exist in source",
              "(d)": "registry counts meet their floors"}
    print("coverage gate        :")
    for tag, label in labels.items():
        failed = sum(1 for c in coverage if c.startswith(tag))
        print(f"   {tag} {label:38} {'ok' if not failed else f'FAIL ({failed})'}")
    print()
    if uncovered_variables:
        print(f"VARIABLE COVERAGE NOTE — {len(uncovered_variables)} of {len(reg['variables'])} "
              f"registered variables have no test command (reported, not asserted):")
        print("   " + ", ".join(uncovered_variables))
        print()
    if coverage:
        print(f"COVERAGE GATE — {len(coverage)} failure(s):")
        for c in coverage:
            print(f"   - {c}")
        print()
    if real_fails:
        print(f"FAILURES — {len(real_fails)} unexpected error(s):")
        for cmd, line in real_fails:
            print(f"   {cmd}")
            print(f"       -> {line}")
        print()
    if tolerated:
        print("Tolerated (ran for completeness, errors expected):")
        for cmd, _ in tolerated:
            print(f"   {cmd}")
        print()

    schema_errors = []
    try:
        schema = load_metadata_or_command_schema()
        exported = schema.get("commands", [])
        syntaxes = {entry.get("syntax") for entry in exported}
        if not exported:
            schema_errors.append("schema contains no commands")
        if "/nqa debug exportCommandSchema" not in syntaxes:
            schema_errors.append("schema is missing /nqa debug exportCommandSchema")
        if "/nqa debug exportMetadata" not in syntaxes:
            schema_errors.append("schema is missing /nqa debug exportMetadata")
        if COMMAND_METADATA.exists():
            metadata = json.loads(COMMAND_METADATA.read_text())
            registry = metadata.get("registry", {})
            for key in ("objectives", "actions", "conditions", "triggers", "variables"):
                if not registry.get(key):
                    schema_errors.append(f"metadata registry has no {key}")
        else:
            schema_errors.append(f"metadata file was not written: {COMMAND_METADATA}")
        for entry in exported:
            for segment in entry.get("segments", []):
                if weak_description(segment.get("description"), segment.get("name"), segment.get("token")):
                    schema_errors.append(
                        "weak command-segment description in "
                        + str(entry.get("syntax"))
                        + ": "
                        + str(segment.get("token"))
                        + " -> "
                        + repr(segment.get("description")))
            for flag in entry.get("flags", []):
                if weak_description(flag.get("description"), flag.get("name"), flag.get("token")):
                    schema_errors.append(
                        "weak flag description in "
                        + str(entry.get("syntax"))
                        + ": --"
                        + str(flag.get("name"))
                        + " -> "
                        + repr(flag.get("description")))
    except FileNotFoundError:
        schema_errors.append(f"schema file was not written: {COMMAND_METADATA}")
    except json.JSONDecodeError as exc:
        schema_errors.append(f"schema JSON is invalid: {exc}")
    if schema_errors:
        print("COMMAND SCHEMA ERROR:")
        for error in schema_errors:
            print(f"   - {error}")
        print()

    if registration_failures:
        print(f"COMMAND REGISTRATION — {len(registration_failures)} command(s) or export(s) "
              f"failed to register; the command tree is incomplete:")
        for line in registration_failures:
            print(f"   - {line}")
        print()

    if len(tolerated) > TOLERATED_CEILING:
        print(f"TOLERATED CEILING EXCEEDED — {len(tolerated)} tolerated edge errors, "
              f"ceiling is {TOLERATED_CEILING}. A new command started failing and was absorbed "
              f"by an existing tolerance tag.")
        print()

    ok = (ready and not coverage and not real_fails and not schema_errors
          and not registration_failures and len(tolerated) <= TOLERATED_CEILING)
    print("RESULT:", "PASS" if ok else "FAIL")
    sys.exit(0 if ok else 1)


def read_metadata():
    """The runtime metadata bundle, or None if the plugin never wrote it."""
    if not COMMAND_METADATA.exists():
        return None
    try:
        return json.loads(COMMAND_METADATA.read_text())
    except json.JSONDecodeError:
        return None


def weak_description(description, name, token):
    text = str(description or "").strip()
    if not text or text == "Variable Name":
        return True
    weak_phrases = {
        "adds a new entry in this command branch.",
        "checks the selected value or condition.",
        "creates a new entry.",
        "deletes the selected entry.",
        "lists matching entries.",
        "opens edit commands for the selected entry.",
        "optional command flags for this command.",
        "removes all entries in this command branch.",
        "removes the selected entry or value.",
        "sets a new value.",
        "shows detailed information about the selected entry.",
        "shows or changes the category assigned to this entry.",
        "shows the current value.",
        "shows, sets, or removes user-facing description text.",
    }
    if text.lower() in weak_phrases:
        return True
    token_text = str(token or "").strip().replace("[", "").replace("]", "").replace("<", "").replace(">", "")
    candidates = {str(name or "").strip(), token_text}
    return text.lower() in {candidate.lower() for candidate in candidates if candidate}


def load_metadata_or_command_schema():
    if COMMAND_METADATA.exists():
        metadata = json.loads(COMMAND_METADATA.read_text())
        commands = metadata.get("commands", {})
        if isinstance(commands, dict):
            return commands
        return {"commands": commands}
    return json.loads(COMMAND_SCHEMA.read_text())


if __name__ == "__main__":
    main()
