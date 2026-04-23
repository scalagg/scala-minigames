package gg.tropic.practice.metrics.collectors

import dev.cubxity.plugins.metrics.api.metric.collector.Collector
import dev.cubxity.plugins.metrics.api.metric.data.GaugeMetric
import gg.tropic.practice.games.GameService
import gg.tropic.practice.ugc.HostedWorldInstanceService

object HostedWorldCollector : Collector
{
    override fun collect() = listOf(
        GaugeMetric(
            "hosted_worlds_active",
            mapOf(),
            HostedWorldInstanceService.worldInstances().size
        ),
        *HostedWorldInstanceService.worldInstances()
            .groupBy { it.providerType }
            .map { entry ->
                GaugeMetric(
                    "hosted_worlds_active_provider",
                    mapOf("type" to entry.key.name),
                    entry.value.size
                )
            }
            .toTypedArray()
    )
}
