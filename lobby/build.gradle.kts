repositories {
    maven("https://repo.dmulloy2.net/nexus/repository/public")
    maven("https://repo.extendedclip.com/releases/")
    maven("https://repo.codemc.org/repository/maven-public/") {
        name = "codemc"
    }
}

dependencies {
    api(project(":shared"))
    compileOnly(project(":parties"))
    compileOnly(fileTree("spigot"))
    api(project(":microgames:bridging-shared"))

    compileOnly("gg.scala.queue:spigot:1.0.1")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("net.skinsrestorer:skinsrestorer-shared:15.7.3")
}
