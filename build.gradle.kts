import org.gradle.api.JavaVersion.VERSION_25

plugins {
    `java-library`
    `maven-publish`
    id("com.gradleup.shadow") version "9.4.2"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
    // run-paper is only applied to :plugin (the real, server-ready plugin). Declared here so the
    // subproject can apply it without repeating the version. Booting :paper/:common (intermediate
    // library jars with no plugin.yml) would just error, so they don't get a runServer task.
    id("xyz.jpenilla.run-paper") version "3.0.2" apply false
}

subprojects {
    plugins.apply("java-library")
    plugins.apply("maven-publish")
    plugins.apply("com.gradleup.shadow")
}

group = "com.notquests"
// FORK DIVERGENCE: the fork versions as <mc major>.<mc minor>.<fork build counter>.
// The first two fields track the supported Minecraft version; the third is a globally
// monotonic build counter that never resets and never decrements, so it keeps climbing
// across Minecraft versions (26.2.8 -> 26.3.9, not 26.3.1).
version = "26.2.1"


repositories {
}

dependencies {
    // FORK DIVERGENCE: upstream targets 26.1.2; this fork targets 26.2, which the production
    // server runs. Keep this in sync with :paper and :plugin — all three must name the same bundle.
    paperweight.paperDevBundle("26.2.build.62-beta")
}

java {
    // Configure the java toolchain. This allows gradle to auto-provision JDK 21 on systems that only have JDK 11 installed for example.
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    sourceCompatibility = VERSION_25
    targetCompatibility = VERSION_25
}

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

/**
 * Configure NotQuests for shading
 */
val path = "com.notquests"


tasks {
    // The root project has no sources of its own (`:compileJava NO-SOURCE`), so `jar` and
    // `shadowJar` both emit an empty ~333-byte archive containing nothing but a manifest.
    //
    // That was harmless while the deployable :plugin artifact was named
    // `notquests-<version>-<mc>.jar`, because the root's `notquests-<version>.jar` had a
    // different name. Once the version scheme change dropped the `-<mc>` suffix the two
    // collided: `build/libs/notquests-26.2.1.jar` (333 bytes, no classes, no plugin
    // descriptor) and `plugin/build/libs/notquests-26.2.1.jar` (7.9 MB, the real one).
    // Deploying the former silently gets you a plugin Paper cannot load.
    //
    // `archiveClassifier.set("")` on the root shadowJar is what put it on that exact name;
    // :plugin already sets `jar { enabled = false }` for the same reason. Disable both here
    // so :plugin is the only module that ever emits a `notquests-*.jar`.
    jar {
        enabled = false
    }
    shadowJar {
        enabled = false
    }

    //build {
    //    dependsOn(shadowJar)
    //}
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(25)
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }
}
