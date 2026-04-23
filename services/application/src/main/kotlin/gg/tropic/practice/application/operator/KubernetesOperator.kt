package gg.tropic.practice.application.operator

import gg.scala.commons.agnostic.sync.server.ServerContainer
import gg.scala.commons.agnostic.sync.server.impl.GameServer
import gg.scala.commons.agnostic.sync.server.state.ServerState
import gg.tropic.practice.games.manager.GameManager
import gg.tropic.practice.persistence.RedisShared
import gg.tropic.practice.replications.manager.ReplicationManager
import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level
import java.util.logging.Logger

/**
 * @author Limo
 * @date 2/15/2026
 */
object KubernetesOperator : GameServerProvisioner
{
    private val logger = Logger.getLogger("KubernetesOperator")

    private lateinit var client: KubernetesClient
    private val initialized = AtomicBoolean(false)

    private const val NAMESPACE = "scala-network"
    private const val MANAGED_BY_LABEL = "scala-operator"
    private const val DRAIN_METADATA_KEY = "operator:draining"

    private val executor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "k8s-operator").apply { isDaemon = true }
    }

    private val pendingPods = ConcurrentHashMap<String, Long>()

    private val drainingServers = ConcurrentHashMap<String, Long>()

    private const val POD_BOOT_TIMEOUT_MS = 120_000L

    private const val DRAIN_TIMEOUT_MS = 600_000L

    private const val MAX_GAME_SERVER_PODS = 10
    private const val MIN_IDLE_SERVERS = 1

    private const val SCALE_COOLDOWN_MS = 10_000L
    private var lastScaleUpTime = 0L

    private data class DeploymentConfig(
        val name: String,
        val maxReplicas: Int
    )

    private val deploymentTypes = mapOf(
        "proxy" to DeploymentConfig("proxy", maxReplicas = 2),
        "mainlobby" to DeploymentConfig("main-lobby", maxReplicas = 4),
        "bwlobby" to DeploymentConfig("bedwars-lobby", maxReplicas = 4),
        "application" to DeploymentConfig("application", maxReplicas = 1)
    )

    private val templates = mapOf(
        "bedwars" to GameServerTemplate(
            gameType = "bedwars",
            idPrefix = "bwg",
            image = "localhost:30500/scala/spigot-base:latest",
            templatePath = "/opt/glacial/templates/gameserver/bedwars",
            serverGroups = "bedwars-game,minigame,mipgame"
        )
    )

    fun load()
    {
        try
        {
            client = KubernetesClientBuilder().build()
            initialized.set(true)
            logger.info("Kubernetes client initialized successfully in namespace $NAMESPACE")

            GameServerProvisioner.instance = this

            executor.scheduleAtFixedRate(
                ::reconcile, 15L, 5L, TimeUnit.SECONDS
            )

            logger.info("Reconciliation loop started (every 5s)")
        }
        catch (exception: Exception)
        {
            logger.log(Level.WARNING, "Failed to initialize Kubernetes client — operator disabled", exception)
        }
    }

    override fun isAvailable(): Boolean = initialized.get()

    override fun provision(gameType: String): String? = createGameServerPod(gameType)

    fun createGameServerPod(gameType: String): String?
    {
        if (!isAvailable())
        {
            logger.warning("Operator not available, cannot create Pod")
            return null
        }

        val template = templates[gameType]
        if (template == null)
        {
            logger.warning("No template found for game type: $gameType")
            return null
        }

        val now = System.currentTimeMillis()
        if (now - lastScaleUpTime < SCALE_COOLDOWN_MS)
        {
            logger.info("Scale cooldown active, skipping Pod creation")
            return null
        }

        val managedPodCount = getManagedPodCount()
        if (managedPodCount >= MAX_GAME_SERVER_PODS)
        {
            logger.warning("Maximum Pod count reached ($MAX_GAME_SERVER_PODS), cannot create more")
            return null
        }

        val serverId = generateServerId(template)

        try
        {
            val pod = buildGameServerPod(serverId, template)
            client.pods()
                .inNamespace(NAMESPACE)
                .resource(pod)
                .create()

            pendingPods[serverId] = now
            lastScaleUpTime = now
            logger.info("Created game server Pod: $serverId (type=$gameType)")
            return serverId
        }
        catch (exception: Exception)
        {
            logger.log(Level.SEVERE, "Failed to create Pod: $serverId", exception)
            return null
        }
    }

    fun drainGameServerPod(serverId: String)
    {
        if (drainingServers.containsKey(serverId))
        {
            return
        }

        drainingServers[serverId] = System.currentTimeMillis()

        try
        {
            RedisShared.keyValueCache.sync().hset(
                "operator:drain-flags",
                serverId,
                System.currentTimeMillis().toString()
            )
            logger.info("Marked server $serverId for draining")
        }
        catch (exception: Exception)
        {
            logger.log(Level.WARNING, "Failed to set drain flag for $serverId", exception)
        }
    }

    override fun isDraining(serverId: String): Boolean
    {
        return drainingServers.containsKey(serverId)
            || RedisShared.keyValueCache.sync()
                .hexists("operator:drain-flags", serverId)
    }

    fun deleteGameServerPod(serverId: String)
    {
        if (!isAvailable()) return

        try
        {
            client.pods()
                .inNamespace(NAMESPACE)
                .withName(serverId)
                .delete()

            pendingPods.remove(serverId)
            drainingServers.remove(serverId)

            RedisShared.keyValueCache.sync().hdel("operator:drain-flags", serverId)

            logger.info("Deleted game server Pod: $serverId")
        }
        catch (exception: Exception)
        {
            logger.log(Level.WARNING, "Failed to delete Pod: $serverId", exception)
        }
    }

    fun pendingPodCount(): Int = pendingPods.size

    private fun reconcile()
    {
        if (!isAvailable()) return

        try
        {
            reconcilePendingPods()
            reconcileDrainingServers()
            reconcileFailedPods()
            reconcileIdleServers()
            reconcileMinimumServers()
        }
        catch (exception: Exception)
        {
            logger.log(Level.WARNING, "Reconciliation error", exception)
        }
    }

    private fun reconcilePendingPods()
    {
        val now = System.currentTimeMillis()
        val iterator = pendingPods.entries.iterator()

        while (iterator.hasNext())
        {
            val (serverId, createdAt) = iterator.next()
            val server = ServerContainer.getServer(serverId)

            if (server != null && server.state == ServerState.Loaded)
            {
                logger.info("Pending Pod $serverId is now registered and loaded")
                iterator.remove()
                continue
            }

            if (now - createdAt > POD_BOOT_TIMEOUT_MS)
            {
                logger.warning("Pending Pod $serverId timed out after ${POD_BOOT_TIMEOUT_MS / 1000}s, deleting")
                deleteGameServerPod(serverId)
                iterator.remove()
            }
        }
    }

    private fun reconcileDrainingServers()
    {
        val now = System.currentTimeMillis()
        val iterator = drainingServers.entries.iterator()

        while (iterator.hasNext())
        {
            val (serverId, drainStarted) = iterator.next()

            val serverStatus = ReplicationManager.allServerStatuses()[serverId]
            val activeGameCount = serverStatus?.status?.replications?.values
                ?.flatten()
                ?.count { it.inUse }
                ?: 0

            val server = ServerContainer.getServer(serverId)

            if (activeGameCount == 0 || server == null)
            {
                logger.info("Draining server $serverId has no active games, deleting Pod")
                deleteGameServerPod(serverId)
                iterator.remove()
                continue
            }

            if (now - drainStarted > DRAIN_TIMEOUT_MS)
            {
                logger.warning("Drain timeout for $serverId after ${DRAIN_TIMEOUT_MS / 1000}s, force-deleting")
                deleteGameServerPod(serverId)
                iterator.remove()
            }
        }
    }

    private fun reconcileFailedPods()
    {
        val pods = client.pods()
            .inNamespace(NAMESPACE)
            .withLabel("managed-by", MANAGED_BY_LABEL)
            .list()
            .items

        for (pod in pods)
        {
            val phase = pod.status?.phase ?: continue
            val podName = pod.metadata?.name ?: continue

            if (phase == "Failed" || phase == "Unknown")
            {
                logger.warning("Cleaning up failed Pod: $podName (phase=$phase)")
                deleteGameServerPod(podName)
                continue
            }

            val containerStatuses = pod.status?.containerStatuses ?: continue
            val isCrashLooping = containerStatuses.any { cs ->
                cs.state?.waiting?.reason == "CrashLoopBackOff"
            }

            if (isCrashLooping)
            {
                logger.warning("Cleaning up crash-looping Pod: $podName")
                deleteGameServerPod(podName)
            }
        }
    }

    private fun reconcileIdleServers()
    {
        val instances = GameManager.instances()
        val serverStatuses = ReplicationManager.allServerStatuses()

        val activeServers = ServerContainer
            .allServers()
            .values
            .filter { it.state == ServerState.Loaded }
            .filter { isManagedPod(it.id) }

        val idleServers = activeServers.filter { server ->
            val status = serverStatuses[server.id]
            val gameCount = status?.status?.replications?.values
                ?.flatten()
                ?.count { it.inUse }
                ?: 0

            val gameServer = server as? GameServer
            val playerCount = gameServer?.getPlayersCount() ?: 0
            gameCount == 0 && playerCount == 0 && !isDraining(server.id)
        }

        val serversToRemove = idleServers.size - MIN_IDLE_SERVERS
        if (serversToRemove > 0)
        {
            idleServers
                .sortedBy { it.initialHeartbeat }
                .take(serversToRemove)
                .forEach { server ->
                    logger.info("Draining idle server: ${server.id}")
                    drainGameServerPod(server.id)
                }
        }
    }

    private fun reconcileMinimumServers()
    {
        for ((gameType, template) in templates)
        {
            val podPrefix = "${template.idPrefix}-"

            val activeCount = ServerContainer
                .allServers()
                .values
                .filter { it.state == ServerState.Loaded }
                .filter { it.id.startsWith(podPrefix) }
                .filter { !isDraining(it.id) }
                .size

            val pendingCount = pendingPods.keys.count { it.startsWith(podPrefix) }
            val totalCount = activeCount + pendingCount

            if (totalCount < MIN_IDLE_SERVERS)
            {
                val needed = MIN_IDLE_SERVERS - totalCount
                logger.info("Game type $gameType has $totalCount servers (active=$activeCount, pending=$pendingCount), need $needed more")

                for (i in 0 until needed)
                {
                    createGameServerPod(gameType)
                }
            }
        }
    }

    private fun isManagedPod(serverId: String): Boolean
    {
        return templates.values.any { serverId.startsWith("${it.idPrefix}-") }
    }

    private fun getManagedPodCount(): Int
    {
        return try
        {
            client.pods()
                .inNamespace(NAMESPACE)
                .withLabel("managed-by", MANAGED_BY_LABEL)
                .list()
                .items
                .size
        }
        catch (exception: Exception)
        {
            logger.log(Level.WARNING, "Failed to count managed Pods", exception)
            0
        }
    }

    private fun buildGameServerPod(serverId: String, template: GameServerTemplate): Pod
    {
        return PodBuilder()
            .withNewMetadata()
                .withName(serverId)
                .withNamespace(NAMESPACE)
                .addToLabels("app", "gameserver")
                .addToLabels("game", template.gameType)
                .addToLabels("tier", "game")
                .addToLabels("managed-by", MANAGED_BY_LABEL)
            .endMetadata()
            .withNewSpec()
                .addNewContainer()
                    .withName("gameserver")
                    .withImage(template.image)
                    .addNewPort()
                        .withContainerPort(25565)
                        .withName("minecraft")
                    .endPort()
                    .withNewResources()
                        .addToRequests("memory", Quantity(template.memoryRequest))
                        .addToRequests("cpu", Quantity(template.cpuRequest))
                        .addToLimits("memory", Quantity(template.memoryLimit))
                        .addToLimits("cpu", Quantity(template.cpuLimit))
                    .endResources()
                    .addNewEnvFrom()
                        .withNewConfigMapRef()
                            .withName("scala-config")
                        .endConfigMapRef()
                    .endEnvFrom()
                    .addNewEnv()
                        .withName("POD_IP")
                        .withNewValueFrom()
                            .withNewFieldRef()
                                .withFieldPath("status.podIP")
                            .endFieldRef()
                        .endValueFrom()
                    .endEnv()
                    .addNewEnv()
                        .withName("SERVER_ID_PREFIX")
                        .withValue(template.idPrefix)
                    .endEnv()
                    .addNewEnv()
                        .withName("SERVER_GROUPS")
                        .withValue(template.serverGroups)
                    .endEnv()
                    .addNewEnv()
                        .withName("SERVER_MEMORY")
                        .withValue(template.serverMemory)
                    .endEnv()
                    .addNewEnv()
                        .withName("REDIS_PASSWORD")
                        .withNewValueFrom()
                            .withNewSecretKeyRef()
                                .withName("scala-secrets")
                                .withKey("REDIS_PASSWORD")
                                .withOptional(true)
                            .endSecretKeyRef()
                        .endValueFrom()
                    .endEnv()
                    .addNewVolumeMount()
                        .withName("template")
                        .withMountPath("/server")
                    .endVolumeMount()
                    .addNewVolumeMount()
                        .withName("maps")
                        .withMountPath("/server/maps")
                        .withReadOnly(true)
                    .endVolumeMount()
                    .withNewReadinessProbe()
                        .withNewTcpSocket()
                            .withPort(IntOrString(25565))
                        .endTcpSocket()
                        .withInitialDelaySeconds(30)
                        .withPeriodSeconds(10)
                    .endReadinessProbe()
                    .withNewLivenessProbe()
                        .withNewTcpSocket()
                            .withPort(IntOrString(25565))
                        .endTcpSocket()
                        .withInitialDelaySeconds(60)
                        .withPeriodSeconds(15)
                    .endLivenessProbe()
                .endContainer()
                .addNewVolume()
                    .withName("template")
                    .withNewHostPath()
                        .withPath(template.templatePath)
                        .withType("Directory")
                    .endHostPath()
                .endVolume()
                .addNewVolume()
                    .withName("maps")
                    .withNewPersistentVolumeClaim()
                        .withClaimName("maps-pvc")
                    .endPersistentVolumeClaim()
                .endVolume()
                .withRestartPolicy("Never")
            .endSpec()
            .build()
    }

    private fun generateServerId(template: GameServerTemplate): String
    {
        val prefix = template.idPrefix
        val podPrefix = "$prefix-"

        val podIds = try
        {
            client.pods()
                .inNamespace(NAMESPACE)
                .withLabel("managed-by", MANAGED_BY_LABEL)
                .list()
                .items
                .mapNotNull { pod ->
                    val name = pod.metadata?.name ?: return@mapNotNull null
                    if (!name.startsWith(podPrefix)) return@mapNotNull null
                    name.removePrefix(podPrefix).toIntOrNull()
                }
                .toSet()
        }
        catch (exception: Exception)
        {
            emptySet()
        }

        val syncIds = ServerContainer
            .allServers()
            .values
            .mapNotNull { server ->
                if (!server.id.startsWith(podPrefix)) return@mapNotNull null
                server.id.removePrefix(podPrefix).toIntOrNull()
            }
            .toSet()

        val pendingIds = pendingPods.keys
            .filter { it.startsWith(podPrefix) }
            .mapNotNull { it.removePrefix(podPrefix).toIntOrNull() }
            .toSet()

        val allUsedIds = podIds + syncIds + pendingIds

        var nextId = 1
        while (nextId in allUsedIds)
        {
            nextId++
        }

        return "$podPrefix$nextId"
    }

    fun getDeploymentTypes(): Set<String> = deploymentTypes.keys

    fun getGameTypes(): Set<String> = templates.keys

    fun scaleDeployment(type: String, replicas: Int): String
    {
        if (!isAvailable()) return "Operator not available"

        val config = deploymentTypes[type.lowercase()]
            ?: return "Unknown deployment type: $type. Valid types: ${deploymentTypes.keys.joinToString(", ")}"

        if (replicas > config.maxReplicas)
        {
            return "Cannot scale $type beyond ${config.maxReplicas} replicas (requested $replicas)"
        }

        if (replicas < 0)
        {
            return "Replica count cannot be negative"
        }

        return try
        {
            val deployment = client.apps().deployments()
                .inNamespace(NAMESPACE)
                .withName(config.name)
                .get()

            if (deployment == null) return "Deployment '${config.name}' not found"

            val currentReplicas = deployment.spec?.replicas ?: 0

            client.apps().deployments()
                .inNamespace(NAMESPACE)
                .withName(config.name)
                .scale(replicas)

            logger.info("Scaled deployment ${config.name} from $currentReplicas to $replicas replicas")
            "Scaled $type (${config.name}) from $currentReplicas to $replicas replicas"
        }
        catch (exception: Exception)
        {
            logger.log(Level.WARNING, "Failed to scale deployment ${config.name}", exception)
            "Failed to scale $type: ${exception.message}"
        }
    }

    fun restartDeployment(type: String): String
    {
        if (!isAvailable()) return "Operator not available"

        val config = deploymentTypes[type.lowercase()]
            ?: return "Unknown deployment type: $type. Valid types: ${deploymentTypes.keys.joinToString(", ")}"

        return try
        {
            val deployment = client.apps().deployments()
                .inNamespace(NAMESPACE)
                .withName(config.name)
                .get()

            if (deployment == null) return "Deployment '${config.name}' not found"

            val annotations = deployment.spec?.template?.metadata?.annotations?.toMutableMap()
                ?: mutableMapOf()
            annotations["kubectl.kubernetes.io/restartedAt"] = java.time.Instant.now().toString()
            deployment.spec.template.metadata.annotations = annotations

            client.apps().deployments()
                .inNamespace(NAMESPACE)
                .withName(config.name)
                .replace(deployment)

            logger.info("Restarted deployment ${config.name} via rolling restart")
            "Initiated rolling restart for $type (${config.name})"
        }
        catch (exception: Exception)
        {
            logger.log(Level.WARNING, "Failed to restart deployment ${config.name}", exception)
            "Failed to restart $type: ${exception.message}"
        }
    }

    fun listDeployments(): List<DeploymentInfo>
    {
        if (!isAvailable()) return emptyList()

        return try
        {
            client.apps().deployments()
                .inNamespace(NAMESPACE)
                .list()
                .items
                .map { deployment ->
                    DeploymentInfo(
                        name = deployment.metadata.name,
                        replicas = deployment.spec?.replicas ?: 0,
                        readyReplicas = deployment.status?.readyReplicas ?: 0,
                        availableReplicas = deployment.status?.availableReplicas ?: 0
                    )
                }
        }
        catch (exception: Exception)
        {
            logger.log(Level.WARNING, "Failed to list deployments", exception)
            emptyList()
        }
    }

    fun scaleGameServers(gameType: String, desiredCount: Int): String
    {
        if (!isAvailable()) return "Operator not available"

        val template = templates[gameType.lowercase()]
            ?: return "Unknown game type: $gameType. Valid types: ${templates.keys.joinToString(", ")}"

        if (desiredCount > MAX_GAME_SERVER_PODS)
        {
            return "Cannot scale $gameType beyond $MAX_GAME_SERVER_PODS servers (requested $desiredCount)"
        }

        if (desiredCount < 0)
        {
            return "Server count cannot be negative"
        }

        val podPrefix = "${template.idPrefix}-"

        val currentServers = ServerContainer
            .allServers()
            .values
            .filter { it.id.startsWith(podPrefix) }
            .filter { !isDraining(it.id) }

        val currentCount = currentServers.size + pendingPods.keys.count { it.startsWith(podPrefix) }

        return when
        {
            desiredCount > currentCount ->
            {
                val toCreate = desiredCount - currentCount
                val created = mutableListOf<String>()
                repeat(toCreate) {

                    lastScaleUpTime = 0L
                    val serverId = createGameServerPod(gameType.lowercase())
                    if (serverId != null) created.add(serverId)
                }
                "Scaling $gameType up: created ${created.size} server(s) [${created.joinToString(", ")}]. Total target: $desiredCount"
            }
            desiredCount < currentCount ->
            {
                val toRemove = currentCount - desiredCount
                val drained = currentServers
                    .sortedByDescending { it.initialHeartbeat }
                    .take(toRemove)
                    .map { server ->
                        drainGameServerPod(server.id)
                        server.id
                    }
                "Scaling $gameType down: draining ${drained.size} server(s) [${drained.joinToString(", ")}]. Total target: $desiredCount"
            }
            else -> "$gameType already at $desiredCount server(s)"
        }
    }

    data class DeploymentInfo(
        val name: String,
        val replicas: Int,
        val readyReplicas: Int,
        val availableReplicas: Int
    )

    data class GameServerTemplate(
        val gameType: String,
        val idPrefix: String,
        val image: String,
        val templatePath: String,
        val serverGroups: String,
        val memoryRequest: String = "512Mi",
        val memoryLimit: String = "1536Mi",
        val cpuRequest: String = "250m",
        val cpuLimit: String = "1000m",
        val serverMemory: String = "1024M"
    )
}
