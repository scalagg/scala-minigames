package gg.tropic.practice.commands.hostedworlds.list

import gg.tropic.practice.ugc.HostedWorldInstanceReference

enum class HostedWorldInstanceSort(val sort: List<HostedWorldInstanceReference>.() -> List<HostedWorldInstanceReference>)
{
    Type({
        sortedBy {
            it.type
        }
    });

    fun previous(): HostedWorldInstanceSort
    {
        return HostedWorldInstanceSort.entries
            .getOrNull(ordinal - 1)
            ?: HostedWorldInstanceSort.entries.last()
    }

    fun next(): HostedWorldInstanceSort
    {
        return HostedWorldInstanceSort.entries
            .getOrNull(ordinal + 1)
            ?: HostedWorldInstanceSort.entries.first()
    }
}
