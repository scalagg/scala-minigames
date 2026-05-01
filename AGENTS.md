# AGENTS.md

## Overview
This monorepo implements ArchMC's minigame infrastructure, supporting a distributed, microservice-oriented architecture for Minecraft game servers. The codebase is organized by service boundaries and game types, with a focus on scalability, cross-server orchestration, and real-time data flows.

## Architecture & Structure
- **services/**: Core microservices (e.g., application, queue, replications, metadata, hosted-world-gateway) orchestrate minigame lifecycle, player routing, and world management. Each service is a Kotlin JVM project with its own build file.
- **minigames/**, **microgames/**, **persistentgames/**: Game implementations, often split into shared, lobby, and main modules. Shared modules provide logic and data models reused across services and plugins.
- **shared/**, **versioned/**: Common utilities, cross-version support, and shared logic for plugins and services.
- **game/**: The main plugin powering game servers, integrating with shared/versioned modules and service APIs.
- **lobby/**, **main-lobby/**: Plugins for lobby servers, handling player entry, queueing, and UI.
- **parties/**: Cross-server party management and sync.

## Data Flow & Communication
- **Redis-based RPC**: All cross-service and cross-server communication uses Redis RPC (see `ReplicationRPC`, `DPSRedisService`). Services register handlers and call each other via Redis channels.
- **Singleton microservice**: The `services/application` module acts as the single source of truth for game/lobby state and data.
- **Player Routing**: Players are dynamically routed to servers based on queue state, server version, and world requirements.
- **Grafana**: Performance and metrics are monitored via Grafana dashboards (see images in README).

## Developer Workflows
- **Build**: Use `./gradlew build` (or `gradlew.bat build` on Windows) from the project root. All modules are built and tested.
- **Shadow JARs**: Each plugin/service produces a shadow JAR (fat JAR) via the Shadow plugin. Artifacts are named like `Duels-<module>.jar`.
- **Test**: JUnit is used for testing (`useJUnitPlatform()` in Gradle). Run `./gradlew test` for all tests.
- **Publish**: Artifacts are published to a custom Artifactory repository (see `configureScalaRepository` in root build file).
- **Java Version**: JVM toolchain is set to Java 21 across all modules.

## Project Conventions & Patterns
- **Command System**: All commands use the ACF/ScalaCommand system with annotations (`@CommandAlias`, `@Subcommand`, `@AutoRegister`). See `shared/src/main/kotlin/gg/tropic/practice/commands/` for examples.
- **Service Registration**: Services are registered via `@Service` and `@Configure` annotations (see `gg.scala.flavor.service`).
- **Cross-version Support**: Versioned logic is isolated in `versioned/` modules and injected as needed.
- **Game World Management**: World instantiation and replication are handled via dedicated services and Redis RPC (see `ReplicationGeneratorService`, `HostedWorldGatewayManager`).
- **Configuration**: Most configuration is code-driven; minimal use of external config files.

## Integration Points
- **External Dependencies**: Integrates with Spigot, Paper, ProtocolLib, FastAsyncWorldEdit, SlimeWorldManager, and custom Scala/ArchMC libraries.
- **Metrics/Monitoring**: Unified metrics via `unifiedmetrics-api` and Grafana.
- **Data Stores**: Redis (primary), MongoDB (for persistent data, see `ApplicationServer.kt`).

## Key Files & Directories
- `README.md`: High-level architecture and rationale
- `build.gradle.kts` (root): Build, test, and publish configuration
- `services/`, `minigames/`, `shared/`, `versioned/`: Main code organization
- `shared/src/main/kotlin/gg/tropic/practice/replication/ReplicationRPC.kt`: Redis RPC pattern
- `shared/src/main/kotlin/gg/tropic/practice/commands/`: Command conventions
- `services/application/src/main/kotlin/gg/tropic/practice/application/ApplicationServer.kt`: Service entrypoint and configuration

## Example Patterns
- Registering a command:
  ```kotlin
  @AutoRegister
  @CommandAlias("statistic")
  object StatisticCommand : ScalaCommand() { ... }
  ```
- Defining a Redis RPC service:
  ```kotlin
  val generationService = gateway.createAwareRPCService<GenerationRequest, GenerationResult>(...)
  ```
- Service registration:
  ```kotlin
  @Service
  object ReplicationGeneratorService { ... }
  ```

