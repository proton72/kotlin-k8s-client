package io.github.proton72.k8s.exception

/**
 * Base exception for Kubernetes client errors.
 */
open class KubernetesException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * Exception thrown when Kubernetes API returns an error response.
 */
class KubernetesApiException(
    val statusCode: Int,
    message: String,
    cause: Throwable? = null
) : KubernetesException("Kubernetes API error (status: $statusCode): $message", cause)

/**
 * Exception thrown when authentication fails.
 */
class KubernetesAuthenticationException(
    message: String,
    cause: Throwable? = null
) : KubernetesException("Authentication failed: $message", cause)

/**
 * Exception thrown when a resource is not found.
 */
class KubernetesNotFoundException(
    resourceType: String,
    resourceName: String,
    namespace: String? = null
) : KubernetesException(
    "Resource not found: $resourceType/$resourceName" +
    (namespace?.let { " in namespace $it" } ?: "")
)

/**
 * Exception thrown when configuration is invalid.
 */
class KubernetesConfigException(
    message: String,
    cause: Throwable? = null
) : KubernetesException("Configuration error: $message", cause)
