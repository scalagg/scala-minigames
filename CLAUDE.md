# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

See `AGENTS.md` and `README.md` for deeper architectural narrative — this file focuses on what's not obvious from a directory listing.

## Build & Tooling

- **Toolchain**: Kotlin 2.1.0 (JVM target 21) + kapt, Gradle Kotlin DSL, Shadow 9.0.0-beta6. The Foojay resolver auto-fetches a JDK 21 even if the host has a different JDK (CI runs on JDK 17 — the toolchain still pulls 21 for compilation).
- **Submodules**: `minigames/{bedwars,skywars,miniwalls}*`, `microgames/{events,bridging}*`, `sbb`, and `persistentgames/housing-lobby` are git submodules. Run `git submodule update --init --recursive` after cloning or the Gradle build will fail to find these subprojects.
- **Common Gradle tasks**:
  - `./gradlew build` — builds every subproject. The root `build.gradle.kts` rewires `build` to depend on `shadowJar` and `publishMavenJavaPublicationToScalaRepository`, so a plain `build` will try to publish.
  - `./gradlew :module:build` — single module (e.g. `:shared`, `:game`, `:services:application`).
  - `./gradlew :module:shadowJar` — produces `Duels-<module>.jar` (fat JAR). All subprojects relocate `org.joda.time` → `gg.tropic.practice.datetime` in the shadow output.
  - `./gradlew test` — JUnit Platform is configured but **there are currently no `src/test` directories in-tree**. Adding tests is fine; just don't expect existing ones to run.
  - `./gradlew :services:application:run` — `services:application` applies the `application` plugin with main class `gg.tropic.practice.application.ApplicationServerKt`. It takes CLI args (`--dev`, `--redishost`, etc.) parsed via `kotlin-argparser`.
- **Publishing**: Targets ArchMC's Artifactory. Required Gradle properties: `artifactory_user`, `artifactory_password`, `artifactory_contextUrl`. The repo (`gradle-release` vs `gradle-dev`) is selected by the `currentBranch` variable in the root build (defaults to `master`/release). CI passes credentials via `-P` flags — see `Jenkinsfile`.
- **Common version**: `gg.scala.commons` plugins are pinned via `commonsVersion` in `gradle.properties` (currently `4.1.0`).

## Module Topology

Three layers, plus version-shim modules:

1. **Services** (`services/*`) — backend daemons or libraries packaged into them.
   - `services:application` is the only `application`-plugin module; it's the singleton source-of-truth. Aggregates queue/replications/game-manager/hosted-world-gateway plus per-game `*-shared` modules. Talks to MongoDB (kmongo) for persistence and Redis (lettuce + `gg.scala.aware`) for everything else.
   - `services:application:api` — exposed as `:services:application:shared` in `settings.gradle.kts` (note the path/name mismatch — this is intentional, the project coordinate is `:services:application:shared` even though the directory is `api/`). This is the public DTO/RPC surface that game/lobby plugins compile against.
   - `services:queue`, `services:replications`, `services:metadata`, `services:hosted-world-gateway`, `services:games:game-manager` — each is a library subproject the application aggregates. They're also depended on (compileOnly) by game/lobby plugins.
2. **Plugins** (`game/`, `lobby/`, `main-lobby/`, `parties/`, `agent/`, `devtools/`, `spigot-integration/`) — Spigot/Paper plugins that ship as shadow JARs. `game/` is the per-game-server plugin and `api`s the three `versioned/*` modules so it can dispatch to legacy or modern Spigot internals.
3. **Game implementations** (`minigames/*`, `microgames/*`, `persistentgames/*`) — each typically splits into `*-shared` (logic/DTOs), `*-lobby` (lobby-side UI/queue hooks), and the bare module name (game-server-side). The application bundles every `*-shared` module so it can route across all of them.

### Cross-cutting

- **`shared/`** — the largest single module. Contains commands, queue communications, replication RPC, profile, metadata wiring, etc. Almost everything depends on it. Look here first when an abstraction spans multiple games.
- **`versioned/{generics,legacy,modern}`** — version-shim layer for Spigot internals. `generics` defines the abstraction; `legacy`/`modern` are the implementations. `game/` `api`s all three; the runtime picks the right one.

## Cross-service Communication Pattern

Every service-to-service or service-to-plugin call goes through Redis-backed RPC:

```kotlin
private val gateway = DPSRedisService("replications")
val generationService = gateway.createAwareRPCService<GenerationRequest, GenerationResult>(
    serviceName = "generate",
    timeoutSeconds = 5L,
)
```

Caller side: `service.call(request)`. Server side: register a handler on the same gateway/serviceName. Examples: `shared/.../replication/ReplicationRPC.kt`, `services/application/...`. Don't add HTTP/REST endpoints — the convention is Redis RPC + `DPSRedisService` namespacing.

## Annotation-driven Wiring

- **Commands**: ACF-style via ScalaCommand. `@AutoRegister @CommandAlias("…") object Foo : ScalaCommand()` — kapt picks them up at compile time. See `shared/src/main/kotlin/gg/tropic/practice/commands/` for examples.
- **Services**: `@Service` / `@Configure` from `gg.scala.flavor.service` — apply on `object`s; the framework calls `@Configure`-annotated methods at startup.
- Both rely on kapt; if you add a new annotation processor or a module forgets to apply `kotlin-kapt`, registrations silently disappear.

## Configuration & Coding Notes

- Configuration is mostly code-driven (constants, `Globals.kt`, `Environment.kt`). Avoid introducing external config files unless there's an existing pattern to follow in the same module.
- All Kotlin compile tasks enable `-Xcontext-receivers` (still an experimental flag) — context receivers are used in places, so don't be surprised by them.
- `-parameters` is on for both Java and Kotlin (`javaParameters = true`) because reflection-driven frameworks (ACF, kmongo) rely on parameter names.
- Subprojects use `compileOnly` for Spigot/Paper/ProtocolLib/etc. — those are provided by the runtime, not bundled. Matching `implementation` would double-shade them into the JAR.
