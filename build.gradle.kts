import org.gradle.api.JavaVersion.VERSION_25

plugins {
    `java-library`
    `maven-publish`
    id("com.gradleup.shadow") version "9.6.1"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.23"
    // run-paper is applied by :paper, the real server-ready plugin artifact.
    id("xyz.jpenilla.run-paper") version "3.1.0" apply false
}

subprojects {
    plugins.apply("java-library")
    plugins.apply("maven-publish")
    plugins.apply("com.gradleup.shadow")

    repositories {
        mavenCentral()
        maven("https://redempt.dev") {
            content {
                includeGroup("com.github.Redempt")
            }
        }
        maven("https://jitpack.io") {
            content {
                includeGroup("com.github.Redempt")
            }
        }
    }
}

group = "com.notquests"
// FORK DIVERGENCE: the fork versions as <mc major>.<mc minor>.<fork build counter>.
// The first two fields track the supported Minecraft version; the third is a globally
// monotonic build counter that never resets and never decrements, so it keeps climbing
// across Minecraft versions (26.2.8 -> 26.3.9, not 26.3.1). Upstream's 7.x line does
// not correspond to these numbers.
version = "26.3.7"

// Derived from the version above - never edit this by hand. The guard fails the build
// at configuration time if the version stops matching the fork scheme.
val minecraftTargetVersion = project.version.toString().split(".").let { fields ->
    require(fields.size >= 3) {
        "Fork version must be <mc major>.<mc minor>.<build counter>, got: ${project.version}"
    }
    "${fields[0]}.${fields[1]}"
}
extra["minecraftTargetVersion"] = minecraftTargetVersion

repositories {
}

dependencies {
    paperweight.paperDevBundle("26.3.build.35-alpha")
}

java {
    // Gradle can provision the Java 25 toolchain when it is not installed locally.
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
    val collectFinalJars = register<Sync>("collectFinalJars") {
        group = "build"
        description = "Collects final NotQuests platform jars into build/final-jars."

        dependsOn(":paper:shadowJar", ":neoforge:jar")

        into(layout.buildDirectory.dir("final-jars"))

        // FORK DIVERGENCE: the fork version already carries the Minecraft version in its
        // first two fields, so the deployable Paper jar is plain notquests-<version>.jar
        // (the name admins deploy). The NeoForge jar keeps a -neoforge suffix to stay
        // unambiguous next to it.
        from(project(":paper").tasks.named("shadowJar").map { it.outputs.files.singleFile }) {
            rename { "notquests-${project.version}.jar" }
        }
        from(project(":neoforge").tasks.named("jar").map { it.outputs.files.singleFile }) {
            rename { "notquests-${project.version}-neoforge.jar" }
        }
    }

    build {
        dependsOn(collectFinalJars)
    }

    jar {
        enabled = false
    }

    shadowJar {
        enabled = false
        archiveClassifier.set("")
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
