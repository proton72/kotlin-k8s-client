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
    /**
     * Hard is the set of desired hard limits for each named resource
     * Example: mapOf("requests.cpu" to "4", "requests.memory" to "16Gi", "pods" to "10")
     */
    val hard: Map<String, String>? = null,

    /**
     * A collection of filters that must match each object tracked by a quota
     */
    val scopeSelector: ScopeSelector? = null,

    /**
     * A collection of filters like scopeSelector that must match each object tracked by a quota
     * but expressed using ScopedResourceSelectorRequirement
     */
    val scopes: List<String>? = null
)

/**
 * ResourceQuotaStatus defines the enforced hard limits and observed use
 */
@Serializable
data class ResourceQuotaStatus(
    /**
     * Hard is the set of enforced hard limits for each named resource
     */
    val hard: Map<String, String>? = null,

    /**
     * Used is the current observed total usage of the resource in the namespace
     */
    val used: Map<String, String>? = null
)

/**
 * A scope selector represents the AND of the selectors represented by the scoped-resource selector requirements
 */
@Serializable
data class ScopeSelector(
    /**
     * A list of scope selector requirements by scope of the resources
     */
    val matchExpressions: List<ScopedResourceSelectorRequirement>? = null
)

/**
 * A scoped-resource selector requirement is a selector that contains values, a scope name, and an operator
 */
@Serializable
data class ScopedResourceSelectorRequirement(
    /**
     * The name of the scope that the selector applies to
     */
    val scopeName: String,

    /**
     * Represents a scope's relationship to a set of values
     * Valid operators are In, NotIn, Exists, DoesNotExist
     */
    val operator: String,

    /**
     * An array of string values. If the operator is In or NotIn,
     * the values array must be non-empty. If the operator is Exists or DoesNotExist,
     * the values array must be empty
     */
    val values: List<String>? = null
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
