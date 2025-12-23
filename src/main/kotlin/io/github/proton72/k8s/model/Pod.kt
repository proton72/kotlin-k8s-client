package io.github.proton72.k8s.model

import kotlinx.serialization.Serializable

/**
 * Pod is a collection of containers that can run on a host.
 */
@Serializable
data class Pod(
    val apiVersion: String = "v1",
    val kind: String = "Pod",
    val metadata: ObjectMeta,
    val spec: PodSpec? = null,
    val status: PodStatus? = null
)

/**
 * PodSpec is a description of a pod.
 */
@Serializable
data class PodSpec(
    val containers: List<Container>,
    val restartPolicy: String? = null,
    val nodeSelector: Map<String, String>? = null,
    val serviceAccountName: String? = null,
    val volumes: List<Volume>? = null,
    val initContainers: List<Container>? = null,
    val hostname: String? = null,
    val subdomain: String? = null,
    val affinity: Affinity? = null,
    val tolerations: List<Toleration>? = null,
    val imagePullSecrets: List<LocalObjectReference>? = null,
    val priorityClassName: String? = null,
    val securityContext: PodSecurityContext? = null
)

/**
 * Container represents a single container in a pod.
 */
@Serializable
data class Container(
    val name: String,
    val image: String,
    val command: List<String>? = null,
    val args: List<String>? = null,
    val env: List<EnvVar>? = null,
    val ports: List<ContainerPort>? = null,
    val volumeMounts: List<VolumeMount>? = null,
    val resources: ResourceRequirements? = null,
    val imagePullPolicy: String? = null,
    val livenessProbe: Probe? = null,
    val readinessProbe: Probe? = null
)

@Serializable
data class EnvVar(
    val name: String,
    val value: String? = null,
    val valueFrom: EnvVarSource? = null
)

@Serializable
data class EnvVarSource(
    val fieldRef: ObjectFieldSelector? = null,
    val configMapKeyRef: ConfigMapKeySelector? = null,
    val secretKeyRef: SecretKeySelector? = null
)

@Serializable
data class ObjectFieldSelector(
    val fieldPath: String,
    val apiVersion: String? = null
)

@Serializable
data class ConfigMapKeySelector(
    val name: String,
    val key: String,
    val optional: Boolean? = null
)

@Serializable
data class SecretKeySelector(
    val name: String,
    val key: String,
    val optional: Boolean? = null
)

@Serializable
data class ContainerPort(
    val containerPort: Int,
    val name: String? = null,
    val protocol: String? = null,
    val hostPort: Int? = null
)

@Serializable
data class VolumeMount(
    val name: String,
    val mountPath: String,
    val readOnly: Boolean? = null,
    val subPath: String? = null
)

@Serializable
data class ResourceRequirements(
    val limits: Map<String, String>? = null,
    val requests: Map<String, String>? = null
)

@Serializable
data class Probe(
    val httpGet: HTTPGetAction? = null,
    val exec: ExecAction? = null,
    val tcpSocket: TCPSocketAction? = null,
    val initialDelaySeconds: Int? = null,
    val timeoutSeconds: Int? = null,
    val periodSeconds: Int? = null,
    val successThreshold: Int? = null,
    val failureThreshold: Int? = null
)

@Serializable
data class HTTPGetAction(
    val path: String? = null,
    val port: Int,
    val host: String? = null,
    val scheme: String? = null
)

@Serializable
data class ExecAction(
    val command: List<String>
)

@Serializable
data class TCPSocketAction(
    val port: Int,
    val host: String? = null
)

@Serializable
data class Volume(
    val name: String,
    val emptyDir: EmptyDirVolumeSource? = null,
    val configMap: ConfigMapVolumeSource? = null,
    val secret: SecretVolumeSource? = null,
    val persistentVolumeClaim: PersistentVolumeClaimVolumeSource? = null
)

@Serializable
data class EmptyDirVolumeSource(
    val medium: String? = null,
    val sizeLimit: String? = null
)

@Serializable
data class ConfigMapVolumeSource(
    val name: String,
    val items: List<KeyToPath>? = null,
    val defaultMode: Int? = null
)

@Serializable
data class SecretVolumeSource(
    val secretName: String,
    val items: List<KeyToPath>? = null,
    val defaultMode: Int? = null
)

@Serializable
data class PersistentVolumeClaimVolumeSource(
    val claimName: String,
    val readOnly: Boolean? = null
)

@Serializable
data class KeyToPath(
    val key: String,
    val path: String,
    val mode: Int? = null
)

@Serializable
data class Affinity(
    val nodeAffinity: NodeAffinity? = null,
    val podAffinity: PodAffinity? = null,
    val podAntiAffinity: PodAntiAffinity? = null
)

@Serializable
data class NodeAffinity(
    val requiredDuringSchedulingIgnoredDuringExecution: NodeSelector? = null
)

@Serializable
data class PodAffinity(
    val requiredDuringSchedulingIgnoredDuringExecution: List<PodAffinityTerm>? = null
)

@Serializable
data class PodAntiAffinity(
    val requiredDuringSchedulingIgnoredDuringExecution: List<PodAffinityTerm>? = null
)

@Serializable
data class NodeSelector(
    val nodeSelectorTerms: List<NodeSelectorTerm>
)

@Serializable
data class NodeSelectorTerm(
    val matchExpressions: List<NodeSelectorRequirement>? = null
)

@Serializable
data class NodeSelectorRequirement(
    val key: String,
    val operator: String,
    val values: List<String>? = null
)

@Serializable
data class PodAffinityTerm(
    val labelSelector: LabelSelector? = null,
    val topologyKey: String
)

@Serializable
data class Toleration(
    val key: String? = null,
    val operator: String? = null,
    val value: String? = null,
    val effect: String? = null,
    val tolerationSeconds: Long? = null
)

@Serializable
data class LocalObjectReference(
    val name: String
)

/**
 * PodSecurityContext holds pod-level security attributes and common container settings.
 * Some fields are also present in Container.securityContext.
 * Field values from Container.securityContext take precedence over field values from PodSecurityContext.
 */
@Serializable
data class PodSecurityContext(
    val runAsUser: Long? = null,
    val runAsGroup: Long? = null,
    val runAsNonRoot: Boolean? = null,
    val fsGroup: Long? = null,
    val fsGroupChangePolicy: String? = null,
    val supplementalGroups: List<Long>? = null,
    val seLinuxOptions: SELinuxOptions? = null,
    val seccompProfile: SeccompProfile? = null
)

/**
 * SELinuxOptions are the labels to be applied to the container
 */
@Serializable
data class SELinuxOptions(
    val user: String? = null,
    val role: String? = null,
    val type: String? = null,
    val level: String? = null
)

/**
 * SeccompProfile defines a pod/container's seccomp profile settings.
 * Only one profile source may be set.
 */
@Serializable
data class SeccompProfile(
    val type: String,
    val localhostProfile: String? = null
)

/**
 * PodStatus represents information about the status of a pod.
 */
@Serializable
data class PodStatus(
    val phase: String? = null,
    val conditions: List<PodCondition>? = null,
    val message: String? = null,
    val reason: String? = null,
    val hostIP: String? = null,
    val podIP: String? = null,
    val startTime: String? = null,
    val containerStatuses: List<ContainerStatus>? = null
)

@Serializable
data class PodCondition(
    val type: String,
    val status: String,
    val lastProbeTime: String? = null,
    val lastTransitionTime: String? = null,
    val reason: String? = null,
    val message: String? = null
)

@Serializable
data class ContainerStatus(
    val name: String,
    val ready: Boolean,
    val restartCount: Int,
    val state: ContainerState? = null,
    val lastState: ContainerState? = null,
    val imageID: String? = null,
    val containerID: String? = null
)

@Serializable
data class ContainerState(
    val waiting: ContainerStateWaiting? = null,
    val running: ContainerStateRunning? = null,
    val terminated: ContainerStateTerminated? = null
)

@Serializable
data class ContainerStateWaiting(
    val reason: String? = null,
    val message: String? = null
)

@Serializable
data class ContainerStateRunning(
    val startedAt: String? = null
)

@Serializable
data class ContainerStateTerminated(
    val exitCode: Int,
    val signal: Int? = null,
    val reason: String? = null,
    val message: String? = null,
    val startedAt: String? = null,
    val finishedAt: String? = null
)

/**
 * PodList is a list of Pods.
 */
@Serializable
data class PodList(
    val apiVersion: String = "v1",
    val kind: String = "PodList",
    val metadata: ListMeta,
    val items: List<Pod>
)
