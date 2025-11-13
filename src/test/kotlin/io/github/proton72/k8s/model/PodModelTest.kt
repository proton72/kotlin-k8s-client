package io.github.proton72.k8s.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

class PodModelTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    @Test
    fun `test Pod serialization and deserialization`() {
        val pod = Pod(
            metadata = ObjectMeta(
                name = "test-pod",
                namespace = "default",
                labels = mapOf("app" to "test", "version" to "v1")
            ),
            spec = PodSpec(
                containers = listOf(
                    Container(
                        name = "nginx",
                        image = "nginx:1.21",
                        ports = listOf(ContainerPort(containerPort = 80))
                    )
                ),
                restartPolicy = "Always"
            ),
            status = PodStatus(
                phase = "Running",
                podIP = "10.0.0.1"
            )
        )

        val serialized = json.encodeToString(pod)
        val deserialized = json.decodeFromString<Pod>(serialized)

        assertEquals(pod.metadata.name, deserialized.metadata.name)
        assertEquals(pod.metadata.namespace, deserialized.metadata.namespace)
        assertEquals(pod.spec?.containers?.size, deserialized.spec?.containers?.size)
        assertEquals(pod.status?.phase, deserialized.status?.phase)
    }

    @Test
    fun `test Container with environment variables`() {
        val container = Container(
            name = "app",
            image = "myapp:1.0",
            env = listOf(
                EnvVar(name = "ENV", value = "production"),
                EnvVar(
                    name = "DB_PASSWORD",
                    valueFrom = EnvVarSource(
                        secretKeyRef = SecretKeySelector(
                            name = "db-secret",
                            key = "password",
                            optional = false
                        )
                    )
                ),
                EnvVar(
                    name = "POD_NAME",
                    valueFrom = EnvVarSource(
                        fieldRef = ObjectFieldSelector(
                            fieldPath = "metadata.name"
                        )
                    )
                )
            )
        )

        val serialized = json.encodeToString(container)
        val deserialized = json.decodeFromString<Container>(serialized)

        assertEquals(3, deserialized.env?.size)
        assertEquals("ENV", deserialized.env?.get(0)?.name)
        assertEquals("production", deserialized.env?.get(0)?.value)
        assertEquals("db-secret", deserialized.env?.get(1)?.valueFrom?.secretKeyRef?.name)
        assertEquals("metadata.name", deserialized.env?.get(2)?.valueFrom?.fieldRef?.fieldPath)
    }

    @Test
    fun `test Container with resource requirements`() {
        val container = Container(
            name = "app",
            image = "myapp:1.0",
            resources = ResourceRequirements(
                requests = mapOf(
                    "memory" to "128Mi",
                    "cpu" to "100m"
                ),
                limits = mapOf(
                    "memory" to "256Mi",
                    "cpu" to "200m"
                )
            )
        )

        val serialized = json.encodeToString(container)
        val deserialized = json.decodeFromString<Container>(serialized)

        assertNotNull(deserialized.resources)
        assertEquals("128Mi", deserialized.resources?.requests?.get("memory"))
        assertEquals("100m", deserialized.resources?.requests?.get("cpu"))
        assertEquals("256Mi", deserialized.resources?.limits?.get("memory"))
        assertEquals("200m", deserialized.resources?.limits?.get("cpu"))
    }

    @Test
    fun `test Container with probes`() {
        val container = Container(
            name = "app",
            image = "myapp:1.0",
            livenessProbe = Probe(
                httpGet = HTTPGetAction(
                    path = "/health",
                    port = 8080,
                    scheme = "HTTP"
                ),
                initialDelaySeconds = 30,
                periodSeconds = 10,
                timeoutSeconds = 5,
                successThreshold = 1,
                failureThreshold = 3
            ),
            readinessProbe = Probe(
                tcpSocket = TCPSocketAction(port = 8080),
                initialDelaySeconds = 5,
                periodSeconds = 10
            )
        )

        val serialized = json.encodeToString(container)
        val deserialized = json.decodeFromString<Container>(serialized)

        assertNotNull(deserialized.livenessProbe)
        assertEquals("/health", deserialized.livenessProbe?.httpGet?.path)
        assertEquals(8080, deserialized.livenessProbe?.httpGet?.port)
        assertEquals(30, deserialized.livenessProbe?.initialDelaySeconds)

        assertNotNull(deserialized.readinessProbe)
        assertEquals(8080, deserialized.readinessProbe?.tcpSocket?.port)
    }

    @Test
    fun `test PodSpec with volumes`() {
        val podSpec = PodSpec(
            containers = listOf(
                Container(
                    name = "app",
                    image = "myapp:1.0",
                    volumeMounts = listOf(
                        VolumeMount(
                            name = "config",
                            mountPath = "/etc/config",
                            readOnly = true
                        ),
                        VolumeMount(
                            name = "data",
                            mountPath = "/var/data"
                        )
                    )
                )
            ),
            volumes = listOf(
                Volume(
                    name = "config",
                    configMap = ConfigMapVolumeSource(
                        name = "app-config",
                        defaultMode = 420
                    )
                ),
                Volume(
                    name = "data",
                    emptyDir = EmptyDirVolumeSource(
                        medium = "Memory",
                        sizeLimit = "1Gi"
                    )
                )
            )
        )

        val serialized = json.encodeToString(podSpec)
        val deserialized = json.decodeFromString<PodSpec>(serialized)

        assertEquals(2, deserialized.volumes?.size)
        assertEquals("config", deserialized.volumes?.get(0)?.name)
        assertEquals("app-config", deserialized.volumes?.get(0)?.configMap?.name)
        assertEquals("data", deserialized.volumes?.get(1)?.name)
        assertEquals("Memory", deserialized.volumes?.get(1)?.emptyDir?.medium)
    }

    @Test
    fun `test PodList serialization and deserialization`() {
        val podList = PodList(
            metadata = ListMeta(
                resourceVersion = "12345",
                remainingItemCount = 0
            ),
            items = listOf(
                Pod(
                    metadata = ObjectMeta(name = "pod-1", namespace = "default"),
                    spec = PodSpec(containers = listOf(Container(name = "c1", image = "nginx")))
                ),
                Pod(
                    metadata = ObjectMeta(name = "pod-2", namespace = "default"),
                    spec = PodSpec(containers = listOf(Container(name = "c2", image = "redis")))
                )
            )
        )

        val serialized = json.encodeToString(podList)
        val deserialized = json.decodeFromString<PodList>(serialized)

        assertEquals(2, deserialized.items.size)
        assertEquals("pod-1", deserialized.items[0].metadata.name)
        assertEquals("pod-2", deserialized.items[1].metadata.name)
        assertEquals("12345", deserialized.metadata.resourceVersion)
    }

    @Test
    fun `test PodStatus with conditions`() {
        val podStatus = PodStatus(
            phase = "Running",
            podIP = "10.0.0.1",
            hostIP = "192.168.1.1",
            conditions = listOf(
                PodCondition(
                    type = "Ready",
                    status = "True",
                    lastTransitionTime = "2024-01-01T00:00:00Z",
                    reason = "ContainersReady",
                    message = "All containers are ready"
                ),
                PodCondition(
                    type = "Initialized",
                    status = "True",
                    lastTransitionTime = "2024-01-01T00:00:00Z"
                )
            ),
            containerStatuses = listOf(
                ContainerStatus(
                    name = "nginx",
                    ready = true,
                    restartCount = 0,
                    state = ContainerState(
                        running = ContainerStateRunning(
                            startedAt = "2024-01-01T00:00:00Z"
                        )
                    )
                )
            )
        )

        val serialized = json.encodeToString(podStatus)
        val deserialized = json.decodeFromString<PodStatus>(serialized)

        assertEquals("Running", deserialized.phase)
        assertEquals(2, deserialized.conditions?.size)
        assertEquals("Ready", deserialized.conditions?.get(0)?.type)
        assertEquals(1, deserialized.containerStatuses?.size)
        assertTrue(deserialized.containerStatuses?.get(0)?.ready == true)
    }

    @Test
    fun `test ObjectMeta with annotations and finalizers`() {
        val metadata = ObjectMeta(
            name = "test-resource",
            namespace = "production",
            labels = mapOf("app" to "myapp", "tier" to "backend"),
            annotations = mapOf(
                "description" to "Test resource",
                "managed-by" to "kubernetes"
            ),
            finalizers = listOf("kubernetes.io/pvc-protection"),
            resourceVersion = "12345",
            uid = "abc-def-123"
        )

        val serialized = json.encodeToString(metadata)
        val deserialized = json.decodeFromString<ObjectMeta>(serialized)

        assertEquals("test-resource", deserialized.name)
        assertEquals("production", deserialized.namespace)
        assertEquals(2, deserialized.labels?.size)
        assertEquals(2, deserialized.annotations?.size)
        assertEquals(1, deserialized.finalizers?.size)
        assertEquals("kubernetes.io/pvc-protection", deserialized.finalizers?.get(0))
    }

    @Test
    fun `test Toleration serialization`() {
        val toleration = Toleration(
            key = "node.kubernetes.io/not-ready",
            operator = "Exists",
            effect = "NoExecute",
            tolerationSeconds = 300
        )

        val serialized = json.encodeToString(toleration)
        val deserialized = json.decodeFromString<Toleration>(serialized)

        assertEquals("node.kubernetes.io/not-ready", deserialized.key)
        assertEquals("Exists", deserialized.operator)
        assertEquals("NoExecute", deserialized.effect)
        assertEquals(300L, deserialized.tolerationSeconds)
    }

    @Test
    fun `test NodeAffinity serialization`() {
        val affinity = Affinity(
            nodeAffinity = NodeAffinity(
                requiredDuringSchedulingIgnoredDuringExecution = NodeSelector(
                    nodeSelectorTerms = listOf(
                        NodeSelectorTerm(
                            matchExpressions = listOf(
                                NodeSelectorRequirement(
                                    key = "kubernetes.io/hostname",
                                    operator = "In",
                                    values = listOf("node-1", "node-2")
                                )
                            )
                        )
                    )
                )
            )
        )

        val serialized = json.encodeToString(affinity)
        val deserialized = json.decodeFromString<Affinity>(serialized)

        assertNotNull(deserialized.nodeAffinity)
        val terms = deserialized.nodeAffinity?.requiredDuringSchedulingIgnoredDuringExecution?.nodeSelectorTerms
        assertNotNull(terms)
        assertEquals(1, terms.size)
        assertEquals("kubernetes.io/hostname", terms[0].matchExpressions?.get(0)?.key)
    }
}
