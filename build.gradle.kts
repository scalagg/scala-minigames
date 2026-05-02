import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    `maven-publish`
    kotlin("jvm") version "2.1.0"
    kotlin("kapt") version "2.1.0"
    id("org.ajoberstar.grgit") version "4.1.1"
    id("com.gradleup.shadow") version "9.0.0-beta6"
}

var currentBranch: String = "master"

allprojects {
    group = "gg.scala.minigames"
    version = "1.7.0"

    repositories {
        configureScalaRepository()
        configureScalaRepository(dev = true)
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
        maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }
        mavenCentral()
        maven {
            name = "lunarclient"
            url = uri("https://repo.lunarclient.dev")
        }
        maven {
            url = uri("https://repo.dmulloy2.net/repository/public/")
        }
        maven {
            name = "papermc"
            url = uri("https://repo.papermc.io/repository/maven-public/")
        }
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.kapt")
    apply(plugin = "com.gradleup.shadow")
    apply(plugin = "maven-publish")

    dependencies {
        compileOnly(kotlin("stdlib"))

        val commonsVersion = property("commonsVersion")
        kapt("gg.scala.commons:bukkit:$commonsVersion")
        compileOnly("gg.scala.commons:bukkit:$commonsVersion")

        compileOnly("gg.scala.store:spigot:1.0.0")
        compileOnly("gg.scala.staff:scala-scstaff-plugin:1.2.2")
        compileOnly("gg.scala.friends:scala-friends:2.1.3")
        compileOnly("gg.scala.basics.plugin:scala-basics-plugin:1.1.8")
        compileOnly("gg.scala.game.extensions:scala-core-game-extensions:1.5.5")

        // Dropped legacy 1.8 baseline (gg.scala.spigot:Spigot-API/Server:1.5.4) — modern Paper 1.21 only.
        compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
        compileOnly("it.unimi.dsi:fastutil:8.5.13")
        compileOnly("com.github.retrooper:packetevents-spigot:2.11.2")
        compileOnly("com.github.retrooper:packetevents-api:2.11.2")
        compileOnly("gg.scala.lemon:bukkit:2.1.5")
        compileOnly("gg.scala.cloudsync:spigot:1.0.4")
        compileOnly("dev.cubxity.plugins:unifiedmetrics-api:0.3.6")
        implementation("com.squareup.retrofit2:retrofit:2.9.0")
        implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    }

    kapt {
        keepJavacAnnotationProcessors = true
    }

    tasks.withType<JavaCompile> {
        options.compilerArgs.add("-parameters")
        options.fork()
        options.encoding = "UTF-8"
    }

    kotlin {
        jvmToolchain(jdkVersion = 21)
    }

    tasks {
        withType<ShadowJar> {
            relocate("org.joda.time", "gg.tropic.practice.datetime")

            archiveClassifier.set("")
            archiveFileName.set(
                "Duels-${project.name}.jar"
            )
        }

        withType<JavaCompile> {
            options.compilerArgs.add("-parameters")
            options.fork()
            options.encoding = "UTF-8"
        }

        withType<KotlinCompile> {
            compilerOptions {
                jvmTarget = JvmTarget.JVM_21
                javaParameters = true
                freeCompilerArgs.add("-Xcontext-receivers")
            }
        }

        getByName("build") {
            dependsOn(
                "shadowJar",
                "publishMavenJavaPublicationToScalaRepository"
            )
        }
    }

    publishing {
        repositories.configureScalaRepository(dev = currentBranch != "master")

        publications {
            register(
                name = "mavenJava",
                type = MavenPublication::class,
                configurationAction = shadow::component
            )
        }
    }
}

fun RepositoryHandler.configureScalaRepository(dev: Boolean = false)
{
    maven("${property("artifactory_contextUrl")}/gradle-${if (dev) "dev" else "release"}") {
        name = "scala"
        credentials {
            username = property("artifactory_user").toString()
            password = property("artifactory_password").toString()
        }
    }
}
