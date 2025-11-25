package io.github.proton72.k8s.model

import kotlinx.serialization.Serializable

/**
 * ResourceQuota sets aggregate quota restrictions enforced per namespace
 */
@Serializable
data class ResourceQuota(
    val apiVersion: String = "v1",
    val kind: String = "ResourceQuota",
    val metadata: ObjectMeta,
    val spec: ResourceQuotaSpec? = null,
    val status: ResourceQuotaStatus? = null
)

/**
 * ResourceQuotaSpec defines the desired hard limits to enforce for Quota
 */
@Serializable
data class ResourceQuotaSpec(
    val hard: Map<String, String>? = null,
    val scopes: List<String>? = null,
    val scopeSelector: ScopeSelector? = null
)

/**
 * ScopeSelector represents the AND of the selectors represented by the scoped-resource selector requirements
 */
@Serializable
data class ScopeSelector(
    val matchExpressions: List<ScopedResourceSelectorRequirement>? = null
)

/**
 * A scoped-resource selector requirement is a selector that contains values, a scope name, and an operator
 * that relates the scope name and values
 */
@Serializable
data class ScopedResourceSelectorRequirement(
    val scopeName: String,
    val operator: String,
    val values: List<String>? = null
)

/**
 * ResourceQuotaStatus defines the enforced hard limits and observed use
 */
@Serializable
data class ResourceQuotaStatus(
    val hard: Map<String, String>? = null,
    val used: Map<String, String>? = null
)

/**
 * ResourceQuotaList is a list of ResourceQuota items
 */
@Serializable
data class ResourceQuotaList(
    val apiVersion: String = "v1",
    val kind: String = "ResourceQuotaList",
    val metadata: ListMeta,
    val items: List<ResourceQuota>
)
