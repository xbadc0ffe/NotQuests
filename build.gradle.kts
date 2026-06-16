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
version = "6.3.0"


repositories {
}

dependencies {
    paperweight.paperDevBundle("26.1.2.build.69-stable")
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
    shadowJar {
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
