package io.github.proton72.k8s.model

import kotlinx.serialization.Serializable

/**
 * Deployment enables declarative updates for Pods and ReplicaSets.
 */
@Serializable
data class Deployment(
    val apiVersion: String = "apps/v1",
    val kind: String = "Deployment",
    val metadata: ObjectMeta,
    val spec: DeploymentSpec? = null,
    val status: DeploymentStatus? = null
)

/**
 * DeploymentSpec is the specification of the desired behavior of the Deployment.
 */
@Serializable
data class DeploymentSpec(
    val replicas: Int? = null,
    val selector: LabelSelector,
    val template: PodTemplateSpec,
    val strategy: DeploymentStrategy? = null,
    val minReadySeconds: Int? = null,
    val revisionHistoryLimit: Int? = null,
    val paused: Boolean? = null,
    val progressDeadlineSeconds: Int? = null
)

/**
 * PodTemplateSpec describes the data a pod should have when created from a template.
 */
@Serializable
data class PodTemplateSpec(
    val metadata: ObjectMeta? = null,
    val spec: PodSpec? = null
)

/**
 * DeploymentStrategy describes how to replace existing pods with new ones.
 */
@Serializable
data class DeploymentStrategy(
    val type: String? = null,
    val rollingUpdate: RollingUpdateDeployment? = null
)

@Serializable
data class RollingUpdateDeployment(
    val maxUnavailable: String? = null,
    val maxSurge: String? = null
)

/**
 * DeploymentStatus is the most recently observed status of the Deployment.
 */
@Serializable
data class DeploymentStatus(
    val observedGeneration: Long? = null,
    val replicas: Int? = null,
    val updatedReplicas: Int? = null,
    val readyReplicas: Int? = null,
    val availableReplicas: Int? = null,
    val unavailableReplicas: Int? = null,
    val conditions: List<DeploymentCondition>? = null
)

@Serializable
data class DeploymentCondition(
    val type: String,
    val status: String,
    val lastUpdateTime: String? = null,
    val lastTransitionTime: String? = null,
    val reason: String? = null,
    val message: String? = null
)

/**
 * DeploymentList is a list of Deployments.
 */
@Serializable
data class DeploymentList(
    val apiVersion: String = "apps/v1",
    val kind: String = "DeploymentList",
    val metadata: ListMeta,
    val items: List<Deployment>
)
