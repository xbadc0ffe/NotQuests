import org.gradle.api.JavaVersion.VERSION_25


plugins {
    id("io.papermc.paperweight.userdev")
}

group = "com.notquests"
version = rootProject.version

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    sourceCompatibility = VERSION_25
    targetCompatibility = VERSION_25
}

repositories {
    // NOTE: We deliberately do NOT add any maven repository for a *plugin* dependency.
    // Every plugin integration API is vendored locally in paper/libs/ (see the dependencies block),
    // so a relocated/deleted plugin repo can never break our build. Only repos for libraries we
    // actually shade into our jar (or the platform itself) are listed here.
    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/") {
        content {
            includeGroup("io.papermc.paper")
            includeGroup("net.kyori")
        }
    }

    // packetevents — shaded library
    maven("https://repo.codemc.io/repository/maven-releases/") {
        content {
            includeGroup("com.github.retrooper")
        }
    }

    // Mojang libraries (brigadier / authlib / datafixerupper transitives)
    maven("https://libraries.minecraft.net/") {
        content {
            includeGroup("com.mojang")
        }
    }

    // Crunch — shaded expression-evaluation library
    maven("https://redempt.dev") {
        content {
            includeGroup("com.github.Redempt")
        }
    }

    // InvUI — shaded GUI library
    maven("https://repo.xenondevs.xyz/releases")
    //mavenLocal()

}

paperweight {
    // Keep the Paper dev bundle (NMS / Mojang-mapped server) on the COMPILE classpath only,
    // so it is NOT on the test runtime classpath where it conflicts with MockBukkit's own
    // Bukkit implementation ("two service providers" / "Bukkit not initialized").
    // See https://docs.mockbukkit.org/docs/en/user_guide/advanced/paperweight
    addServerDependencyTo.set(configurations.named("compileOnly").map { setOf(it) })
}

dependencies {
    implementation(project(path = ":common", configuration = "shadow"))
    // FORK DIVERGENCE: upstream targets 26.1.2; this fork targets 26.2, which the production
    // server runs. Keep this in sync with the root project and :plugin.
    paperweight.paperDevBundle("26.2.build.62-beta")

    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")

    // --- Plugin integration APIs ---
    // ALL vendored locally in paper/libs/ ON PURPOSE: the build must never depend on an external
    // maven repository for a *plugin* (those repos are frequently relocated / deleted / broken).
    // If every one of those repos disappeared, NotQuests would still compile. These are compileOnly
    // because the real plugin provides the classes at runtime. To update one, drop the new jar in
    // paper/libs/ and bump the filename here.
    compileOnly(files("libs/Citizens-2.0.42-SNAPSHOT.jar"))
    compileOnly(files("libs/FancyNpcs-2.10.1.jar"))
    compileOnly(files("libs/PlaceholderAPI-2.12.2.jar"))
    compileOnly(files("libs/VaultAPI-1.7.1.jar"))
    compileOnly(files("libs/Mythic-Dist-5.12.1.jar"))
    compileOnly(files("libs/EliteMobs-10.4.0.jar"))
    compileOnly(files("libs/worldedit-core-7.4.3.jar"))
    compileOnly(files("libs/worldedit-bukkit-7.4.3.jar"))
    compileOnly(files("libs/Slimefun4-RC-37.jar"))
    compileOnly(files("libs/LuckPerms-api-5.5.jar"))
    compileOnly(files("libs/Towny-0.103.0.0.jar"))
    compileOnly(files("libs/Jobs-5.2.6.5.jar"))
    compileOnly(files("libs/floodgate-api-2.2.5-SNAPSHOT.jar"))
    compileOnly(files("libs/EcoMobs-11.7.0.jar"))
    compileOnly(files("libs/eco-7.6.3.jar"))
    compileOnly(files("libs/BetonQuest-3.0.0.jar"))
    // libreforge-loader provides com.willfp.libreforge.loader.configs.RegistrableCategory, which
    // EcoMobs' registry (EcoMobs.INSTANCE) extends; needed on the compile classpath. Vendored like
    // the other eco-ecosystem plugins.
    compileOnly(files("libs/libreforge-loader-5.6.0-all.jar"))


    // --- Shaded libraries (bundled into our jar; fine to resolve from maven) ---

    // Adventure.
    //
    // UPSTREAM'S RULE, DELIBERATELY OVERRIDDEN HERE — upstream pins this to 4.26.1 and says:
    //     "Do NOT move to 5.x — Paper provides 4.x at runtime, so a 5.x compile target would
    //      break against the server."
    // That reasoning is correct FOR UPSTREAM, because upstream targets Paper 26.1.2, which bundles
    // Adventure 4.26.1. This fork targets Paper 26.2, which bundles Adventure 5.2.0. Here the rule
    // inverts: pinning 4.x would be the mismatch. The constraint is not "never 5.x", it is "match
    // whatever Adventure the targeted Paper bundles" — so this moves in lockstep with
    // paperDevBundle above.
    //
    // Two removed 4.x APIs are already ported for this (both verified 1:1, no behaviour change):
    //   minimessage/AbstractColorChangingTag  Internals.toString(this)
    //                                           -> this.examine(StringExaminer.simpleEscaping())
    //   managers/LogManager                   GsonComponentSerializer.builder().downsampleColors()
    //                                           -> GsonComponentSerializer.colorDownsamplingGson()
    //
    // If this fork is ever moved back to 26.1.x, revert this to 4.26.1 AND revert those two call
    // sites. Do not change one without the other.
    implementation("net.kyori:adventure-api:5.2.0") {}

    // InvUI
    // FORK DIVERGENCE: upstream ships 2.1.1, which targets the pre-26.2 API. InvUI 2.x compiles
    // against a single MC version: 2.0.0-RC.1..2.1.x build their private DIRTY_MARKER ItemStack
    // from net.minecraft.world.item.Items.GREEN_CANDLE, which 26.2 removed in favour of
    // Items.DYED_CANDLE + ColorCollection.green(). On 26.2, 2.1.1 dies in
    // CustomContainerMenu.<clinit> with NoSuchFieldError. 2.2.0 targets 26.2.
    // WARNING: 2.2.0 is 26.2-only and crashes symmetrically on 26.1.x.
    implementation("xyz.xenondevs.invui:invui:2.2.0")

    implementation("com.github.retrooper:packetevents-spigot:2.12.2")


    implementation("commons-io:commons-io:2.22.0")


    implementation("com.github.Redempt:Crunch:2.0.3")



    implementation("com.zaxxer:HikariCP:7.0.2")


    // --- Testing (JUnit 6 + MockBukkit) ---
    testImplementation(platform("org.junit:junit-bom:6.1.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // MockBukkit for Paper 26.2 (in-JVM mock server; no real server needed).
    //
    // FORK DIVERGENCE: upstream pins mockbukkit-v26.1.2 / paper-api 26.1.2 to match its own target.
    // Both are version-locked artifacts, and both must track the platform this fork actually ships,
    // for a hard reason and not just tidiness: testImplementation extends implementation, so the
    // Adventure 5.2.0 declared above lands on the test classpath too. Against the 26.1.2 artifacts
    // (built for Adventure 4.26.1) Gradle's highest-version-wins resolution silently upgraded them
    // to 5.2.0, and 12 tests died on APIs that 5.x removed or sealed -- NoClassDefFoundError on
    // net.kyori.adventure.audience.MessageType, and IncompatibleClassChangeError because
    // org.bukkit.inventory.meta.BookMeta is not a permitted subclass of the now-sealed
    // net.kyori.adventure.inventory.Book.
    //
    // Keep these two in lockstep with paperDevBundle above. Pinning them back to 26.1.2 would make
    // the suite pass while validating a platform this fork does not ship.
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v26.2:4.116.1")
    // MockBukkit does NOT bundle the Bukkit API (it assumes the plugin already provides it).
    // Our paper-api comes from the paperweight dev bundle, which is compileOnly (off the test
    // classpath), so add the regular paper-api + JetBrains annotations for the test compile.
    testImplementation("io.papermc.paper:paper-api:26.2.build.62-beta")
    testImplementation("org.jetbrains:annotations:26.1.0")

    // Mockito (spies/mocks) — ready for future tests (e.g. failing-Connection DB tests)
    testImplementation("org.mockito:mockito-core:5.23.0")

    // SQLite JDBC driver for deterministic DB-integrity tests (matches the runtime driver)
    testImplementation("org.xerial:sqlite-jdbc:3.53.2.0")
}

/**
 * Configure NotQuests for shading
 */
val shadowPath = "com.notquests.paper.shadow"


tasks {

    shadowJar {
        // DO NOT minimize the jar, since cloud doesnt like it
        // Reference: https://discord.com/channels/766366162388123678/1170254709722984460/1242027222773006376

        relocate("de.themoep", "$shadowPath.de.themoep")

        relocate("org.apache.commons.io", "$shadowPath.commons.io")

        relocate("io.github.retrooper.packetevents", "$shadowPath.packetevents.bukkit")
        relocate("com.github.retrooper.packetevents", "$shadowPath.packetevents.api")

        relocate("net.kyori.adventure.text.serializer.bungeecord", "$shadowPath.kyori.bungeecord")

        // FORK DIVERGENCE: Paper 26.2 ships Adventure 5.x, which dropped the net.kyori.examination
        // dependency entirely — none of the 12 kyori jars Paper provides contain it. Both this
        // plugin (minimessage/AbstractColorChangingTag implements Examinable, SimpleGradientTag's
        // examinableProperties) and the shaded PacketEvents adventure serializers
        // (CharacterAndFormat and LegacyFormat extend/implement Examinable) still reference it, so
        // without shading it the plugin only enables when some *other* plugin on the server happens
        // to publish net/kyori/examination/Examinable into Paper's PluginClassLoaderGroup. On a
        // server where nothing else supplies it, onLoad dies with NoClassDefFoundError.
        //
        // Relocated rather than shaded bare: an unrelocated net.kyori.examination would be
        // published into that same shared group, where other plugins could bind to our copy — the
        // mirror image of the accident that was masking this bug.
        //
        // Upstream is on Adventure 4.x, where Paper still provides examination, so it needs neither
        // the include nor the relocate. Do NOT "fix" this by dropping `implements Examinable` from
        // AbstractColorChangingTag: that would conflict on every future upstream merge touching
        // that file, and would leave the four shaded PacketEvents classes still broken.
        relocate("net.kyori.examination", "$shadowPath.kyori.examination")

        relocate("xyz.xenondevs.invui", "$shadowPath.invui")

        relocate("redempt.crunch", "$shadowPath.crunch")

        relocate("com.fasterxml.jackson", "$shadowPath.jackson")

        relocate("org.apache.http", "$shadowPath.apache.http")

        relocate("com.zaxxer.hikari", "$shadowPath.hikari")

        //relocate("com.jeff_media.updatechecker", "$shadowPath.updatechecker")


        dependencies {
            include(dependency("commons-io:commons-io:.*"))
            include(dependency("xyz.xenondevs.invui:.*:.*"))

            include(dependency("me.lucko:.*:.*"))

            include(dependency("com.github.retrooper:.*:.*"))
            include(dependency("io.github.retrooper:.*:.*"))

            include(dependency("net.kyori:adventure-text-serializer-bungeecord:.*"))

            // FORK DIVERGENCE: see the net.kyori.examination relocate above. Both artifacts already
            // resolve transitively via com.github.retrooper:packetevents-spigot, so this adds no new
            // repository and no new version pin. examination-string carries StringExaminer, used by
            // AbstractColorChangingTag's toString; examination-api carries Examinable itself, which
            // the other five referencing classes need.
            include(dependency("net.kyori:examination-api:.*"))
            include(dependency("net.kyori:examination-string:.*"))

            include(dependency("com.github.Redempt:.*:.*"))

            include(dependency("com.fasterxml.jackson.dataformat:.*:.*"))
            include(dependency("com.fasterxml.jackson.core:.*:.*"))

            include(dependency("org.apache.httpcomponents:.*:.*"))

            include(dependency("com.zaxxer:.*:.*"))
        }

        // Strip plugin metadata from shaded libraries. PacketEvents ships its own plugin.yml (it can
        // run as a standalone plugin); if it survives into this shaded jar, the jar masquerades as
        // PacketEvents and Paper tries to load io.github.retrooper.packetevents.PacketEventsPlugin as
        // the main class. The real plugin.yml/paper-plugin.yml is generated by the :plugin module.
        exclude("plugin.yml")
        exclude("paper-plugin.yml")

        // Give the shaded jar a distinct classifier so it does NOT overwrite the thin `:paper:jar`
        // (both would otherwise be paper-<version>.jar). When the thin jar wins that race, the
        // consuming :plugin module bundles un-relocated paper classes WITHOUT the shaded libraries
        // (e.g. packetevents), and the plugin crashes at enable with NoClassDefFoundError. The
        // shadowRuntimeElements configuration that :plugin depends on tracks this task's output by
        // task, not filename, so it still resolves to this (now collision-free) shaded jar.
        archiveClassifier.set("all")

    }

    test {
        useJUnitPlatform()
        // Quiet Mockito's self-attaching agent on JDK 25+ and allow MockBukkit's reflection.
        jvmArgs("-XX:+EnableDynamicAgentLoading", "--add-opens", "java.base/java.lang=ALL-UNNAMED")
        testLogging {
            events("passed", "skipped", "failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }

    compileJava {
        mustRunAfter(":common:jar")

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
