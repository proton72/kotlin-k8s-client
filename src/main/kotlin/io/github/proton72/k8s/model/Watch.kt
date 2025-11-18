package io.github.proton72.k8s.model

import kotlinx.serialization.Serializable

/**
 * Represents an event from the Kubernetes Watch API
 * @param T the type of Kubernetes resource being watched
 */
@Serializable
data class WatchEvent<T>(
    /**
     * Type of watch event: ADDED, MODIFIED, DELETED, ERROR, BOOKMARK
     */
    val type: String,
    /**
     * The Kubernetes resource object
     */
    val `object`: T
)

/**
 * Type of watch events
 */
enum class WatchEventType {
    ADDED,
    MODIFIED,
    DELETED,
    ERROR,
    BOOKMARK
}
