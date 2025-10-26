plugins {
    id("java")
    id("dev.architectury.loom") version("1.10-SNAPSHOT")
    id("architectury-plugin") version("3.4-SNAPSHOT")
    kotlin("jvm") version "2.2.0"
    id("com.gradleup.shadow") version "9.0.0-beta4"
    id("maven-publish")
}

group = "com.github.kuramastone"
version = "1.2.0"

architectury {
    platformSetupLoomIde()
    fabric()
}

loom {
    accessWidenerPath = file("src/main/resources/fightorflight.accesswidener")
    silentMojangMappingsLicense()

    mixin {
        defaultRefmapName.set("mixins.${project.name}.refmap.json")
    }
}

val fabricApiVersion: String by project
val blossomLibVersion: String by project
val blossomPvpVersion: String by project

repositories {
    mavenLocal()
    mavenCentral()
    maven(url = "https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/")
    maven("https://maven.impactdev.net/repository/development/")
    maven(url = "https://maven.fabricmc.net/")
    maven(url = "https://maven.enginehub.org/repo/")
    maven("https://maven.nucleoid.xyz/") { name = "Nucleoid" }
    maven ("https://oss.sonatype.org/content/repositories/snapshots/")
    maven ("https://repo.kyori.net/repository/maven-public/")

    maven("https://maven.blazing-coop.net/releases")
    maven("https://repo.kyori.net/snapshots")
    maven("https://maven.codedsakura.dev/releases")
}

val shade by configurations.creating {
    isTransitive = false // Prevents unnecessary transitive dependencies
}

// Extend implementation to include shaded dependencies
configurations.implementation.get().extendsFrom(shade)

dependencies {
    minecraft("net.minecraft:minecraft:1.21.1")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:0.17.2")

    modImplementation("net.fabricmc.fabric-api:fabric-api:${fabricApiVersion}")
    setOf(
        "fabric-api-base",
        "fabric-command-api-v2",
        "fabric-lifecycle-events-v1",
        "fabric-networking-api-v1",
        "fabric-events-interaction-v0"
    ).forEach {
        // Add each module as a dependency
        modImplementation(fabricApi.module(it, fabricApiVersion))
    }
    modImplementation(include("me.lucko:fabric-permissions-api:0.3.1")!!)

    modImplementation("net.fabricmc:fabric-language-kotlin:1.12.3+kotlin.2.0.21")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")

    shade("net.kyori:adventure-text-minimessage:4.18.0")
    shade("net.kyori:adventure-api:4.18.0")
    shade("net.kyori:examination-api:1.3.0")
    shade("net.kyori:adventure-key:4.18.0")
    shade("net.kyori:adventure-text-serializer-plain:4.18.0")
    shade("net.kyori:adventure-text-serializer-gson:4.18.0")

    modImplementation("com.cobblemon:fabric:1.7.0+1.21.1-SNAPSHOT")
    // command library
    //includeAndImplement("io.github.revxrsal:lamp.common:4.0.0-rc.2")
    //includeAndImplement("io.github.revxrsal:lamp.fabric:4.0.0-rc.2")
    shade("dev.dejvokep:boosted-yaml:1.3.6")

    compileOnly("net.luckperms:api:5.4")
    compileOnly("dev.codedsakura.blossom:blossom-lib:${blossomLibVersion}") {
        exclude("xyz.nucleoid")
    }
    compileOnly("dev.codedsakura.blossom:blossom-pvp:${blossomPvpVersion}") {
        exclude("xyz.nucleoid")
    }
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.remapJar {
    dependsOn(tasks.shadowJar) // Ensure shadowJar runs first
    inputFile.set(tasks.shadowJar.get().archiveFile) // Use the shadowed JAR for remapping
    archiveFileName.set("${project.name}-${project.version}.jar") // Rename output
//    destinationDirectory = file("run/mods")
}

tasks.shadowJar {
    archiveClassifier.set("shaded") // Optional: Adds a classifier for the shadowed jar
    configurations = listOf(shade)

    relocate("net.kyori", "com.github.kuramastone.fightOrFlight.shade.net.kyori")
    relocate("dev.dejvokep", "com.github.kuramastone.fightOrFlight.shade.dev.dejvokep")
    relocate("org.intellij", "com.github.kuramastone.fightOrFlight.shade.org.intellij")
    relocate("org.jetbrains", "com.github.kuramastone.fightOrFlight.shade.org.jetbrains")
}

tasks.processResources {
    filesMatching("fabric.mod.json") {
        expand(project.properties)
    }
}

tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

tasks.named("publishToMavenLocal") {
    dependsOn(tasks.remapJar)
}

// configure the maven publication
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = project.group.toString() // ✅ required
            artifactId = "fightorflight"
            version = project.version.toString() // ✅ required
            artifact(tasks.remapJar.get()) {
                builtBy(tasks.remapJar)
            }

            // Optionally publish sources
            artifact(tasks.named("sourcesJar").get())
        }
    }

    // See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
    repositories {
        mavenLocal()
    }
}
