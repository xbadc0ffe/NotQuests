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
import re
import sys
import pathlib
import json

E2E = pathlib.Path(__file__).resolve().parent
SRC = E2E.parent / "paper" / "src" / "main" / "java"
CMDS_FILE = E2E / "commands.txt"
COMMAND_METADATA = E2E.parent / "plugin" / "run" / "plugins" / "NotQuests" / "generated" / "metadata.json"
COMMAND_SCHEMA = E2E.parent / "plugin" / "run" / "plugins" / "NotQuests" / "generated" / "commands.json"

EXCLUDE_INTEGRATION = {
    "EscortNPC", "JobsRebornReachJobLevel", "SlimefunResearch",
    "TownyNationReachTownCount", "TownyReachResidentCount", "TownyNationName",
    "UltimateClansClanLevel", "BetonQuestObjectiveStateChange", "BetonQuestFireEvent",
    "BetonQuestFireInlineEvent",
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


def registered_types():
    kinds = {"objective": "registerObjective", "action": "registerAction",
             "condition": "registerCondition", "trigger": "registerTrigger"}
    found = {k: set() for k in kinds}
    pat = {k: re.compile(fn + r'\("([A-Za-z0-9]+)"') for k, fn in kinds.items()}
    for path in SRC.rglob("*.java"):
        text = path.read_text(errors="replace")
        for k, rx in pat.items():
            found[k].update(m.group(1) for m in rx.finditer(text))
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
    reg = registered_types()
    blob = "\n".join(all_cmds)
    missing = []
    for kind, names in reg.items():
        for n in sorted(names):
            if n in EXCLUDE_INTEGRATION or n in EXCLUDE_VARIABLE:
                continue
            if not re.search(r"(?<![A-Za-z0-9])" + re.escape(n) + r"(?![A-Za-z0-9])", blob):
                missing.append(f"{kind} {n}")

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

    # ---------- report ----------
    print("=" * 72)
    print("NotQuests E2E sweep analysis")
    print("=" * 72)
    print(f"server reached READY : {ready}")
    print(f"commands in sweep    : {len(commands)}")
    print("registered types     : " + ", ".join(f"{k}={len(v)}" for k, v in reg.items()))
    print(f"tolerated edge errors: {len(tolerated)}")
    print()
    if missing:
        print(f"COVERAGE GAP — {len(missing)} registered type(s) with no test command:")
        for m in missing:
            print(f"   - {m}")
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

    ok = ready and not missing and not real_fails and not schema_errors
    print("RESULT:", "PASS" if ok else "FAIL")
    sys.exit(0 if ok else 1)


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
