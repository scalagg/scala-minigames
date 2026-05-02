repositories {
    maven("https://repo.infernalsuite.com/repository/maven-snapshots/")
    maven("https://repo.papermc.io/repository/maven-public/")
}

// Modern targets Paper 1.21+. The root `subprojects {}` injects the legacy
// gg.scala.spigot:Spigot-API/Server (1.8-era Bukkit) into every module — strip it
// here so paper-api owns the org.bukkit.* surface and modern Bukkit additions
// (addPassenger, setInvisible, setSilent, addScoreboardTag, etc.) resolve.
configurations.all {
    exclude(group = "gg.scala.spigot", module = "Spigot-API")
    exclude(group = "gg.scala.spigot", module = "Spigot-Server")
}

dependencies {
    compileOnly(project(":versioned:generics"))
    compileOnly("com.infernalsuite.asp:api:4.0.0-SNAPSHOT")
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
}
