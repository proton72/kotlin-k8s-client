package io.github.proton72.k8s.model

import kotlinx.serialization.Serializable

/**
 * Service is a named abstraction of software service consisting of local port
 * that the proxy listens on, and the selector that determines which pods will
 * answer requests sent through the proxy.
 */
@Serializable
data class Service(
    val apiVersion: String = "v1",
    val kind: String = "Service",
    val metadata: ObjectMeta,
    val spec: ServiceSpec? = null,
    val status: ServiceStatus? = null
)

/**
 * ServiceSpec describes the attributes that a user creates on a service.
 */
@Serializable
data class ServiceSpec(
    val ports: List<ServicePort>? = null,
    val selector: Map<String, String>? = null,
    val clusterIP: String? = null,
    val type: String? = null,
    val externalIPs: List<String>? = null,
    val sessionAffinity: String? = null,
    val loadBalancerIP: String? = null,
    val loadBalancerSourceRanges: List<String>? = null,
    val externalName: String? = null,
    val externalTrafficPolicy: String? = null,
    val healthCheckNodePort: Int? = null
)

/**
 * ServicePort contains information on service's port.
 */
@Serializable
data class ServicePort(
    val name: String? = null,
    val protocol: String? = null,
    val port: Int,
    val targetPort: Int? = null,
    val nodePort: Int? = null
)

/**
 * ServiceStatus represents the current status of a service.
 */
@Serializable
data class ServiceStatus(
    val loadBalancer: LoadBalancerStatus? = null
)

@Serializable
data class LoadBalancerStatus(
    val ingress: List<LoadBalancerIngress>? = null
)

@Serializable
data class LoadBalancerIngress(
    val ip: String? = null,
    val hostname: String? = null
)

/**
 * ServiceList holds a list of services.
 */
@Serializable
data class ServiceList(
    val apiVersion: String = "v1",
    val kind: String = "ServiceList",
    val metadata: ListMeta,
    val items: List<Service>
)
