repositories {
    maven("https://repo.glaremasters.me/repository/concuncan/")
    maven("https://repo.dmulloy2.net/repository/public/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    api(project(":services:metadata"))
    compileOnly(project(":parties"))
    compileOnly(fileTree("spigot"))

    compileOnly(project(":versioned:generics"))
    compileOnly(project(":versioned:legacy"))
    compileOnly(project(":versioned:modern"))

//    compileOnly(fileTree("worldedit"))
    implementation(platform("com.intellectualsites.bom:bom-newest:1.56")) // Ref: https://github.com/IntellectualSites/bom
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit") { isTransitive = false }
    compileOnly("com.grinderwolf:slimeworldmanager-plugin:2.2.1")
    compileOnly("net.dmulloy2:ProtocolLib:5.4.0")

    api("joda-time:joda-time:2.12.5")
    api("xyz.xenondevs:particle:1.7.1")
    api("lol.arch.symphony:api:1.1.1")
}
