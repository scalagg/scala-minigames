repositories {
    maven("https://repo.infernalsuite.com/repository/maven-snapshots/")
}

dependencies {
    compileOnly(project(":versioned:generics"))
    compileOnly("com.infernalsuite.asp:api:4.0.0-SNAPSHOT")
}
