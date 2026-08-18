# Contributing to NotQuests

## Prerequisites

The toolchain is pinned in `mise.toml`. With [mise](https://mise.jdx.dev/) installed, run:

```bash
mise install
```

This provisions everything the project needs:

- **Java 25 (Temurin)** — required by Paper 26.2 / Minecraft 26.2
- **Gradle 9.5.1** — matches `gradle/wrapper/gradle-wrapper.properties`

(Without mise: install a JDK 25 and Gradle 9.5.1 manually.)

## Setup

```bash
git clone https://github.com/AlessioGr/NotQuests.git
cd NotQuests
```

Point `JAVA_HOME` at the pinned Java so Gradle launches with it (the build's
Java 25 toolchain is otherwise auto-provisioned):

```bash
export JAVA_HOME="$(mise where java)"
```

## Building

The Gradle wrapper jar (`gradle/wrapper/gradle-wrapper.jar`) **is** committed, so
`./gradlew` works on a fresh clone with no extra setup:

```bash
./gradlew clean build
```

The final plugin jar is at:

```
plugin/build/libs/notquests-26.2.1.jar
```

The name is `notquests-<project.version>.jar`
(`plugin/build.gradle.kts:109`).

The fork versions as `<mc major>.<mc minor>.<fork build counter>`. The first
two fields track the supported Minecraft version; the third is a globally
monotonic build counter that never resets and never decrements, so it keeps
climbing across Minecraft versions (`26.2.8` → `26.3.9`, not `26.3.1`).

Because the Minecraft target is the first two fields of the version,
`minecraftTargetVersion` (`plugin/build.gradle.kts:74`) is *derived* from
`project.version` rather than declared separately, and it feeds the
`runServer` target and both `apiVersion` declarations. **`version` in the
root `build.gradle.kts` is the single source of truth** — bump that one value
and the jar name, the `runServer` target and both descriptors follow. A
version that does not have three dot-separated fields fails the build at
configuration time.

## Running a test server

```bash
./gradlew :plugin:runServer
```

This starts a Paper 26.2 test server with the plugin loaded.

## Project structure

- `common/` - Shared code across platforms
- `paper/` - Paper-specific implementation (commands, events, GUIs, integrations)
- `plugin/` - Final plugin assembly (shading, plugin.yml / paper-plugin.yml generation)
