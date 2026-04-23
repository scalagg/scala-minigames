package gg.tropic.practice.metrics.collectors

import dev.cubxity.plugins.metrics.api.metric.collector.Collector
import dev.cubxity.plugins.metrics.api.metric.data.GaugeMetric
import gg.tropic.practice.map.MapReplicationService

object ReplicationCountCollector : Collector
{
    override fun collect() = listOf(
        GaugeMetric(
            "practice_replication_count", mapOf(),
            MapReplicationService.readReplications().size
        ),
    )
}
