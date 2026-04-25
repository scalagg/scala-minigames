pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

rootProject.name = "scala-minigames"
include(
    "shared", "game", "lobby",
    "main-lobby", "devtools", "sbb",
    "parties", "agent",

    "microgames:events",
    "microgames:events-shared",
    "microgames:events-lobby",

//    "persistentgames:housing",
//    "persistentgames:housing-shared",
//    "persistentgames:housing-lobby",

    "versioned:generics",
    "versioned:legacy",
    "versioned:modern",

    "microgames:bridging",
    "microgames:bridging-shared",

    "services:application",
    "services:replications",
    "services:queue",
    "services:metadata",
    "services:hosted-world-gateway",

    "services:application:shared",
    "services:games:game-manager",

    "minigames:skywars",
    "minigames:skywars-lobby",
    "minigames:skywars-shared",

    "minigames:bedwars",
    "minigames:bedwars-lobby",
    "minigames:bedwars-shared",

//    "minigames:miniwalls",
//    "minigames:miniwalls-lobby",
//    "minigames:miniwalls-shared",

    "spigot-integration"
)

include("persistentgames:prisons")
include("persistentgames:prisons-shared")