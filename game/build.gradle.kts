repositories {
    maven("https://repo.infernalsuite.com/repository/maven-snapshots/")
    maven("https://repo.dmulloy2.net/repository/public/")
    maven("https://repo.lunarclient.dev")
}

dependencies {
    api(project(":shared"))

    api(project(":versioned:generics"))
    api(project(":versioned:legacy"))
    api(project(":versioned:modern"))

    compileOnly(project(":services:application:api"))
    compileOnly(project(":parties"))
    compileOnly(fileTree("spigot"))

    val commonsVersion = property("commonsVersion")
    compileOnly("gg.scala.commons:modern-access:$commonsVersion")

    compileOnly("dev.cubxity.plugins:unifiedmetrics-api:0.3.6")
}
