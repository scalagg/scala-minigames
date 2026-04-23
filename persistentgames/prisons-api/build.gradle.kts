plugins {
    kotlin("jvm")
}

group = "gg.scala.minigames"
version = "1.7.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}