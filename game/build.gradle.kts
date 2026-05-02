repositories {
    maven("https://repo.infernalsuite.com/repository/maven-snapshots/")
    maven("https://repo.dmulloy2.net/repository/public/")
    maven("https://repo.lunarclient.dev")
}

dependencies {
    api(project(":shared"))

    api(project(":versioned:generics"))
    api(project(":versioned:modern"))

    compileOnly(project(":services:application:shared"))
    compileOnly(project(":parties"))

    compileOnly("net.dmulloy2:ProtocolLib:5.4.0")
    compileOnly("com.lunarclient:apollo-api:1.2.5")

//    val commonsVersion = property("commonsVersion")
//    compileOnly("gg.scala.commons:modern-access:$commonsVersion")
    compileOnly("dev.cubxity.plugins:unifiedmetrics-api:0.3.6")
    compileOnly("com.lunarclient:apollo-extra-adventure4:1.2.5")
}
