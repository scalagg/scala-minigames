repositories {
    mavenCentral()
    maven("https://repo.glaremasters.me/repository/concuncan/")
    maven("https://repo.dmulloy2.net/repository/public/")
}

dependencies {
    api(project(":shared"))
    api(project(":minigames:skywars-shared"))

    compileOnly(fileTree("worldedit"))
    compileOnly("com.grinderwolf:slimeworldmanager-plugin:2.2.1")
}
