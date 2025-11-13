package io.github.proton72.k8s.model

import kotlinx.serialization.Serializable

/**
 * ObjectMeta is metadata that all persisted resources must have.
 */
@Serializable
data class ObjectMeta(
    val name: String? = null,
    val namespace: String? = null,
    val uid: String? = null,
    val resourceVersion: String? = null,
    val creationTimestamp: String? = null,
    val deletionTimestamp: String? = null,
    val labels: Map<String, String>? = null,
    val annotations: Map<String, String>? = null,
    val generateName: String? = null,
    val finalizers: List<String>? = null
)

/**
 * Status is a return value for calls that don't return other objects.
 */
@Serializable
data class Status(
    val kind: String = "Status",
    val apiVersion: String = "v1",
    val status: String? = null,
    val message: String? = null,
    val reason: String? = null,
    val code: Int? = null,
    val details: StatusDetails? = null
)

@Serializable
data class StatusDetails(
    val name: String? = null,
    val kind: String? = null,
    val causes: List<StatusCause>? = null
)

@Serializable
data class StatusCause(
    val reason: String? = null,
    val message: String? = null,
    val field: String? = null
)

/**
 * ListMeta describes metadata that synthetic resources must have.
 */
@Serializable
data class ListMeta(
    val resourceVersion: String? = null,
    val continue_: String? = null,
    val remainingItemCount: Long? = null
)

/**
 * LabelSelector is a label query over a set of resources.
 */
@Serializable
data class LabelSelector(
    val matchLabels: Map<String, String>? = null,
    val matchExpressions: List<LabelSelectorRequirement>? = null
)

@Serializable
data class LabelSelectorRequirement(
    val key: String,
    val operator: String,
    val values: List<String>? = null
)

/**
 * DeleteOptions may be provided when deleting an API object.
 */
@Serializable
data class DeleteOptions(
    val apiVersion: String = "v1",
    val kind: String = "DeleteOptions",
    val gracePeriodSeconds: Int? = null,
    val propagationPolicy: String? = null,
    val dryRun: List<String>? = null
)
