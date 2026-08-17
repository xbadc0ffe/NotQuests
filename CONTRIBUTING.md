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
plugin/build/libs/notquests-6.3.0-26.2.jar
```

The name is `notquests-<project.version>-<minecraftTargetVersion>.jar`
(`plugin/build.gradle.kts:96`). Changing `minecraftTargetVersion`
(`plugin/build.gradle.kts:67`) changes both the jar name and the `runServer`
target below.

## Running a test server

```bash
./gradlew :plugin:runServer
```

This starts a Paper 26.2 test server with the plugin loaded.

## Project structure

- `common/` - Shared code across platforms
- `paper/` - Paper-specific implementation (commands, events, GUIs, integrations)
- `plugin/` - Final plugin assembly (shading, plugin.yml / paper-plugin.yml generation)
