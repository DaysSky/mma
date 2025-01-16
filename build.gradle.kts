plugins {
    id("fabric-loom") version "1.6-SNAPSHOT"
    id("maven-publish")
}

val version: String by project
val group: String by project
val archives_base_name: String by project
val minecraft_version: String by project
val loader_version: String by project
val fabric_version: String by project
val minecraft_version_target: String by project

base {
    archivesName = archives_base_name
}

repositories {
    exclusiveContent {
        forRepository {
            maven("https://api.modrinth.com/maven")
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
    maven("https://maven.parchmentmc.org")
    maven("https://maven.siphalor.de/")
    maven("https://maven.shedaniel.me/")
    maven("https://maven.terraformersmc.com/releases/")
    maven("https://jitpack.io/")
    maven("https://cursemaven.com")
    mavenLocal()
    mavenCentral()
}

loom {
    accessWidenerPath = file("src/main/resources/fma.accesswidener")
}

dependencies {
    // To change the versions see the gradle.properties file
    minecraft("com.mojang:minecraft:${minecraft_version_target}")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-1.20.4:2024.04.14@zip")
    })

    modImplementation("net.fabricmc:fabric-loader:${loader_version}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${fabric_version}")
    modApi("de.siphalor:amecsapi-1.20:1.3.9+mc1.20-pre1")

    modApi("com.terraformersmc:modmenu:7.2.2")
    modApi("me.shedaniel.cloth:cloth-config-fabric:11.1.118") {
        exclude(group = "net.fabricmc.fabric-api")
    }

    // Optional dependency, take care to never classload
    modApi("maven.modrinth:unofficial-monumenta-mod:1.10-fabric,1.20.4")

    //modApi(files("libs/Njols-UI-framework-mc1_20_4-1.1.1.jar"))
    modApi("com.github.Ascynx:MCUIFramework:mc1.20.4-SNAPSHOT")

    modApi("com.github.Ascynx:MCConfigFramework:mc1.20.4-SNAPSHOT")

    modApi("maven.modrinth:xaeros-minimap-fair:FP24.2.0_Fabric_1.20.4-fabric,1.20.4")
}

tasks {
    processResources {
        inputs.property("version", version)
        inputs.property("minecraft_version", minecraft_version)
        inputs.property("loader_version", loader_version)
        filteringCharset = "UTF-8"

        filesMatching("fabric.mod.json") {
            expand(
                mapOf(
                    "version" to version,
                    "minecraft_version" to minecraft_version,
                    "loader_version" to loader_version
                )
            )
        }

        filesMatching("en_us.json") {
            filter { line -> line.replace(Regex("//.+"), "") }
        }
    }

    jar {
        from("LICENSE") {
            rename { "${it}_${archives_base_name}" }
        }
    }
}

val targetJavaVersion = 17

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
}

java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    }

    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = archives_base_name
            from(project.components["java"])
        }
    }

    repositories {
    }
}