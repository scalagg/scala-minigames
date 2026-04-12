package gg.tropic.practice.metrics

import dev.cubxity.plugins.metrics.api.metric.collector.CollectorCollection
import gg.scala.commons.metrics.Metrics
import gg.tropic.practice.metrics.collectors.GameDistributionCollector
import gg.tropic.practice.metrics.collectors.HostedWorldCollector
import gg.tropic.practice.metrics.collectors.HostedWorldDistributionCollector
import gg.tropic.practice.metrics.collectors.ReplicationCountCollector

@Metrics
object GameServerCollection : CollectorCollection
{
    override val collectors = listOf(
        ReplicationCountCollector,
        HostedWorldCollector,
        HostedWorldDistributionCollector,
        GameDistributionCollector
    )

    override val isAsync = true
}
