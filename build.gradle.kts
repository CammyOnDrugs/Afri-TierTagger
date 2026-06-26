plugins {
    id("net.fabricmc.fabric-loom-remap") version "1.14-SNAPSHOT"
    id("io.freefair.lombok") version "9.1.0"
}

version = "${project.property("mod_version")}+mc${project.property("minecraft_version")}"
group = project.property("maven_group") as String

// Build variant: -Ptester=true produces the staff/tester jar (includes StaffCommands).
val tester = (project.findProperty("tester") as String?)?.toBoolean() ?: false

sourceSets {
    main {
        java {
            if (!tester) exclude("com/skalpha/tiertagger/StaffCommands.java")
        }
    }
}

repositories {
    maven {
        url = uri("https://maven.uku3lig.net/releases")
    }
    maven {
        url = uri("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    }
}

dependencies {
    // To change the versions see the gradle.properties file
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")

    modImplementation(fabricApi.module("fabric-command-api-v2", project.property("fabric_api_version") as String))

    modApi("net.uku3lig:ukulib:${project.property("ukulib_version")}")

    modRuntimeOnly("me.djtheredstoner:DevAuth-fabric:${project.property("devauth_version")}")
}

base {
    archivesName = (if (tester) project.property("archives_base_name") as String + "-Tester" else project.property("archives_base_name") as String)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.processResources {
    inputs.property("version", project.version)
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "mod_id" to if (tester) "afri-tierstagger-tester" else "afri-tierstagger",
            "mod_name" to if (tester) "Afri-TiersTagger Tester" else "Afri-TiersTagger",
            "mod_description" to if (tester) "This is STRICTLY for Testers, please don't use this mod if your not a Tester, your IP is shown to our logs, so remove it before you get IPBanned" else "Shows players' tiers from AfriTiers next to their names!"
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.base.archivesName.get()}" }
    }
}