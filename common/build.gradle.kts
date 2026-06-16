import org.gradle.api.JavaVersion.VERSION_25


group = "com.notquests"
version = rootProject.version

java {
    // Configure the java toolchain. This allows gradle to auto-provision JDK 21 on systems that only have JDK 11 installed for example.
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    sourceCompatibility = VERSION_25
    targetCompatibility = VERSION_25
}

repositories {
    mavenCentral()
    //mavenLocal()

}

dependencies {
    //implementation("net.kyori:adventure-api:4.11.0")
    implementation("org.spongepowered:configurate-gson:4.2.0")
}

/**
 * Configure NotQuests for shading
 */
val shadowPath = "com.notquests.shadow"

/*processResources {
    def props = [version: version]
    inputs.properties props
    filteringCharset 'UTF-8'
    filesMatching('plugin.yml') {
        expand props
    }
}*/


tasks {
    // Run reobfJar on build
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

    shadowJar {
        minimize()

        //relocate("net.kyori", "$shadowPath.kyori")
        relocate("org.spongepowered.configurate", "$shadowPath.configurate")

        dependencies {
            //include(dependency("net.kyori:"))
            include(dependency("org.spongepowered:"))


        }
        //archiveBaseName.set("notquests")
        archiveClassifier.set("")
    }
}

/*publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.notquests"
            artifactId = "NotQuests"
            version = "4.0.0-dev"

            from(components["java"])
        }
    }
}*/

