import org.gradle.api.JavaVersion.VERSION_25

plugins {

    id("io.papermc.paperweight.userdev")
    id("xyz.jpenilla.run-paper")
    id("de.eldoria.plugin-yml.bukkit") version "0.9.0"
    id("de.eldoria.plugin-yml.paper") version "0.9.0"
}


group = "com.notquests"
version = rootProject.version

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    sourceCompatibility = VERSION_25
    targetCompatibility = VERSION_25
}

repositories {
    // This module only pulls the Paper platform + PaperLib; no plugin dependencies are declared
    // here, so (like the paper module) it lists no plugin maven repos.
    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/"){
        content {
            includeGroup("io.papermc.paper")
            includeGroup("net.kyori")
            includeGroup("io.papermc")
        }
    }

    // Mojang libraries (brigadier / authlib / datafixerupper transitives of the dev bundle)
    maven("https://libraries.minecraft.net/"){
        content {
            includeGroup("com.mojang")
        }
    }

    //mavenLocal()

}

dependencies {
    // FORK DIVERGENCE: upstream targets 26.1.2; this fork targets 26.2, which the production
    // server runs. Keep this in sync with the root project and :paper.
    paperweight.paperDevBundle("26.2.build.62-beta")

    implementation(project(path= ":common", configuration= "shadowRuntimeElements"))
    implementation(project(path= ":paper", configuration= "shadowRuntimeElements"))

    //implementation(project(":spigot"))
    //implementation(project(":paper"))

    //compileOnly("io.papermc.paper:paper-api:1.18.1-R0.1-SNAPSHOT")

    implementation("io.papermc:paperlib:1.0.8")
}

/**
 * Configure NotQuests for shading
 */
val shadowPath = "com.notquests"
// FORK DIVERGENCE: upstream builds for 26.1.2; this fork builds for 26.2.
//
// The fork version scheme is <mc major>.<mc minor>.<fork build counter>, so the Minecraft
// version this plugin targets IS the first two fields of the project version. Derive it here
// instead of declaring it a second time: that is what keeps the jar name and the two
// apiVersion declarations (bukkit + paper blocks) from silently drifting apart.
//
// Single source of truth is `version` in the root build.gradle.kts. Bump that and the jar
// name, the runServer target and both api-version fields all follow.
val minecraftTargetVersion = project.version.toString().split(".").let { fields ->
    require(fields.size >= 3) {
        "Project version '${project.version}' does not match the required " +
            "<mc major>.<mc minor>.<fork build counter> scheme."
    }
    "${fields[0]}.${fields[1]}"
}

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
    build {
        dependsOn(shadowJar)
    }
    // Don't emit the thin (un-shaded) plugin jar into build/libs: it has a valid plugin.yml but
    // none of the shaded code, so loading it would crash at enable. The shadowJar is the only
    // server-ready artifact.
    jar {
        enabled = false
    }
    shadowJar {
        // DO NOT minimize the jar, since cloud doesnt like it
        // Reference: https://discord.com/channels/766366162388123678/1170254709722984460/1242027222773006376

        // The :plugin module produces the real, server-ready jar. The project version already
        // carries the Minecraft target in its first two fields, so no separate suffix is needed.
        archiveFileName.set("notquests-${project.version}.jar")
        archiveClassifier.set("")

        relocate("io.papermc.lib", "$shadowPath.paperlib")
    }


    compileJava {
        dependsOn(":common:jar", ":paper:jar", ":paper:build")

        options.encoding = Charsets.UTF_8.name()
        options.release.set(25)
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion(minecraftTargetVersion)
    }

    register<Copy>("copyToServer") {
        val path = System.getenv("PLUGIN_DIR")
        if (path.isNullOrEmpty()) {
            println("No environment variable PLUGIN_DIR set")
            return@register
        }
        from(reobfJar)
        destinationDir = File(path)
    }
}




bukkit {
    name = "NotQuests"
    version = rootProject.version.toString()
    main = "com.notquests.Main"
    // FORK DIVERGENCE: derived from the project version's first two fields (see
    // minecraftTargetVersion above) so this cannot drift from the jar name or the other
    // descriptor block. Must stay in step with paperDevBundle.
    apiVersion = minecraftTargetVersion
    authors = listOf("AlessioGr")
    description = "Flexible, open, GUI Quest Plugin for Minecraft"
    website = "https://www.notquests.com"
    softDepend = listOf(
        "ProtocolLib",
        "ProtocolSupport",
        "ViaVersion",
        "ViaBackwards",
        "ViaRewind",
        "Geyser-Spigot",
        "Citizens",
        "FancyNpcs",
        "Vault",
        "PlaceholderAPI",
        "MythicMobs",
        "EliteMobs",
        "WorldEdit",
        "Slimefun",
        "LuckPerms",
        "Towny",
        "Jobs",

        "EcoMobs",
        "eco",
        "Floodgate",
        "BetonQuest"
    )

    load = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.PluginLoadOrder.POSTWORLD

    permissions {
        register("notquests.admin"){
            default = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default.OP
            description = "Gives the player permission to everything in the plugin."
            childrenMap = mapOf(
                "notquests.admin.armorstandeditingitems" to true,
                "notquests.use" to true
            )
        }
        register("notquests.admin.armorstandeditingitems"){
            default = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default.OP
            description = "Gives the player permission to use quest editing items for armor stands."
        }
        register("notquests.use"){
            default = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default.TRUE
            description = "Gives the player permission to use the /notquests user command. They can not create new quests or other administrative tasks with just this permission."
        }
        register("notquests.user.profiles"){
            default = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default.OP
            description = "Gives the player permission to use the /notquests profiles command, and to create, delete and switch profiles."
        }
    }
}

paper {
    name = "NotQuests"
    version = rootProject.version.toString()
    main = "com.notquests.Main"
    // FORK DIVERGENCE: derived from the project version's first two fields (see
    // minecraftTargetVersion above) so this cannot drift from the jar name or the other
    // descriptor block. Must stay in step with paperDevBundle.
    apiVersion = minecraftTargetVersion
    authors = listOf("AlessioGr")
    description = "Flexible, open, GUI Quest Plugin for Minecraft"
    website = "https://www.notquests.com"

    serverDependencies {
        register("ProtocolLib") {
            required = false
        }
        register("ProtocolSupport") {
            required = false
        }
        register("ViaVersion") {
            required = false
        }
        register("ViaBackwards") {
            required = false
        }
        register("ViaRewind") {
            required = false
        }
        register("Geyser-Spigot") {
            required = false
        }
        register("Citizens") {
            required = false
        }
        register("FancyNpcs") {
            required = false
        }
        register("Vault") {
            required = false
        }
        register("PlaceholderAPI") {
            required = false
        }
        register("MythicMobs") {
            required = false
        }
        register("EliteMobs") {
            required = false
        }
        register("WorldEdit") {
            required = false
        }
        register("Slimefun") {
            required = false
        }
        register("LuckPerms") {
            required = false
        }
        register("Towny") {
            required = false
        }
        register("Jobs") {
            required = false
        }
        register("EcoMobs") {
            required = false
        }
        register("eco") {
            required = false
        }
        register("Floodgate") {
            required = false
        }
        register("BetonQuest") {
            required = false
            load = net.minecrell.pluginyml.paper.PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
    }

    // IMPORTANT: Paper prefers paper-plugin.yml over plugin.yml when both exist, so the permission
    // defaults MUST be declared here too. Without this, notquests.use (default true, which lets every
    // player run /notquests) is never registered, so non-OP players are denied the command.
    permissions {
        register("notquests.admin") {
            default = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default.OP
            description = "Gives the player permission to everything in the plugin."
            childrenMap = mapOf(
                "notquests.admin.armorstandeditingitems" to true,
                "notquests.use" to true
            )
        }
        register("notquests.admin.armorstandeditingitems") {
            default = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default.OP
            description = "Gives the player permission to use quest editing items for armor stands."
        }
        register("notquests.use") {
            default = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default.TRUE
            description = "Gives the player permission to use the /notquests user command. They can not create new quests or other administrative tasks with just this permission."
        }
        register("notquests.user.profiles") {
            default = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default.OP
            description = "Gives the player permission to use the /notquests profiles command, and to create, delete and switch profiles."
        }
    }

}
