package gg.tropic.practice.application.api.defaults.defaults.kit

import gg.tropic.practice.kit.feature.FeatureFlag

data class ImmutableKit(
    val id: String,
    val displayName: String,
    val features: Map<FeatureFlag, MutableMap<String, String>> = mutableMapOf()
)
{
    fun features(flag: FeatureFlag) = features.containsKey(flag)
    fun featureConfig(flag: FeatureFlag, key: String) =
        features[flag]?.get(key) ?: flag.schema[key]!!
}
