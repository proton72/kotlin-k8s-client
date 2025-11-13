package io.github.proton72.k8s.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

class DeploymentModelTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    @Test
    fun `test Deployment serialization and deserialization`() {
        val deployment = Deployment(
            metadata = ObjectMeta(
                name = "test-deployment",
                namespace = "default",
                labels = mapOf("app" to "web")
            ),
            spec = DeploymentSpec(
                replicas = 3,
                selector = LabelSelector(
                    matchLabels = mapOf("app" to "web")
                ),
                template = PodTemplateSpec(
                    metadata = ObjectMeta(
                        labels = mapOf("app" to "web")
                    ),
                    spec = PodSpec(
                        containers = listOf(
                            Container(
                                name = "nginx",
                                image = "nginx:1.21"
                            )
                        )
                    )
                )
            ),
            status = DeploymentStatus(
                replicas = 3,
                readyReplicas = 3,
                updatedReplicas = 3,
                availableReplicas = 3
            )
        )

        val serialized = json.encodeToString(deployment)
        val deserialized = json.decodeFromString<Deployment>(serialized)

        assertEquals(deployment.metadata.name, deserialized.metadata.name)
        assertEquals(3, deserialized.spec?.replicas)
        assertEquals(3, deserialized.status?.readyReplicas)
    }

    @Test
    fun `test Deployment with RollingUpdate strategy`() {
        val deployment = Deployment(
            metadata = ObjectMeta(name = "rolling-deployment"),
            spec = DeploymentSpec(
                replicas = 5,
                selector = LabelSelector(matchLabels = mapOf("app" to "api")),
                template = PodTemplateSpec(
                    metadata = ObjectMeta(labels = mapOf("app" to "api")),
                    spec = PodSpec(
                        containers = listOf(
                            Container(name = "api", image = "api:v1")
                        )
                    )
                ),
                strategy = DeploymentStrategy(
                    type = "RollingUpdate",
                    rollingUpdate = RollingUpdateDeployment(
                        maxUnavailable = "1",
                        maxSurge = "2"
                    )
                ),
                minReadySeconds = 10,
                revisionHistoryLimit = 5,
                progressDeadlineSeconds = 600
            )
        )

        val serialized = json.encodeToString(deployment)
        val deserialized = json.decodeFromString<Deployment>(serialized)

        assertEquals("RollingUpdate", deserialized.spec?.strategy?.type)
        assertEquals("1", deserialized.spec?.strategy?.rollingUpdate?.maxUnavailable)
        assertEquals("2", deserialized.spec?.strategy?.rollingUpdate?.maxSurge)
        assertEquals(10, deserialized.spec?.minReadySeconds)
        assertEquals(5, deserialized.spec?.revisionHistoryLimit)
        assertEquals(600, deserialized.spec?.progressDeadlineSeconds)
    }

    @Test
    fun `test Deployment with Recreate strategy`() {
        val deployment = Deployment(
            metadata = ObjectMeta(name = "recreate-deployment"),
            spec = DeploymentSpec(
                replicas = 1,
                selector = LabelSelector(matchLabels = mapOf("app" to "db")),
                template = PodTemplateSpec(
                    metadata = ObjectMeta(labels = mapOf("app" to "db")),
                    spec = PodSpec(
                        containers = listOf(
                            Container(name = "postgres", image = "postgres:14")
                        )
                    )
                ),
                strategy = DeploymentStrategy(type = "Recreate")
            )
        )

        val serialized = json.encodeToString(deployment)
        val deserialized = json.decodeFromString<Deployment>(serialized)

        assertEquals("Recreate", deserialized.spec?.strategy?.type)
        assertNull(deserialized.spec?.strategy?.rollingUpdate)
    }

    @Test
    fun `test DeploymentStatus with conditions`() {
        val status = DeploymentStatus(
            observedGeneration = 5,
            replicas = 3,
            updatedReplicas = 3,
            readyReplicas = 3,
            availableReplicas = 3,
            unavailableReplicas = 0,
            conditions = listOf(
                DeploymentCondition(
                    type = "Available",
                    status = "True",
                    lastUpdateTime = "2024-01-01T00:00:00Z",
                    lastTransitionTime = "2024-01-01T00:00:00Z",
                    reason = "MinimumReplicasAvailable",
                    message = "Deployment has minimum availability."
                ),
                DeploymentCondition(
                    type = "Progressing",
                    status = "True",
                    lastUpdateTime = "2024-01-01T00:00:00Z",
                    lastTransitionTime = "2024-01-01T00:00:00Z",
                    reason = "NewReplicaSetAvailable",
                    message = "ReplicaSet has successfully progressed."
                )
            )
        )

        val serialized = json.encodeToString(status)
        val deserialized = json.decodeFromString<DeploymentStatus>(serialized)

        assertEquals(5L, deserialized.observedGeneration)
        assertEquals(3, deserialized.replicas)
        assertEquals(2, deserialized.conditions?.size)
        assertEquals("Available", deserialized.conditions?.get(0)?.type)
        assertEquals("Progressing", deserialized.conditions?.get(1)?.type)
    }

    @Test
    fun `test LabelSelector with matchLabels`() {
        val selector = LabelSelector(
            matchLabels = mapOf(
                "app" to "web",
                "tier" to "frontend",
                "environment" to "production"
            )
        )

        val serialized = json.encodeToString(selector)
        val deserialized = json.decodeFromString<LabelSelector>(serialized)

        assertEquals(3, deserialized.matchLabels?.size)
        assertEquals("web", deserialized.matchLabels?.get("app"))
        assertEquals("frontend", deserialized.matchLabels?.get("tier"))
        assertEquals("production", deserialized.matchLabels?.get("environment"))
    }

    @Test
    fun `test LabelSelector with matchExpressions`() {
        val selector = LabelSelector(
            matchExpressions = listOf(
                LabelSelectorRequirement(
                    key = "environment",
                    operator = "In",
                    values = listOf("prod", "staging")
                ),
                LabelSelectorRequirement(
                    key = "tier",
                    operator = "NotIn",
                    values = listOf("cache")
                ),
                LabelSelectorRequirement(
                    key = "app",
                    operator = "Exists"
                )
            )
        )

        val serialized = json.encodeToString(selector)
        val deserialized = json.decodeFromString<LabelSelector>(serialized)

        assertEquals(3, deserialized.matchExpressions?.size)
        assertEquals("environment", deserialized.matchExpressions?.get(0)?.key)
        assertEquals("In", deserialized.matchExpressions?.get(0)?.operator)
        assertEquals(2, deserialized.matchExpressions?.get(0)?.values?.size)
        assertEquals("NotIn", deserialized.matchExpressions?.get(1)?.operator)
        assertEquals("Exists", deserialized.matchExpressions?.get(2)?.operator)
    }

    @Test
    fun `test PodTemplateSpec with complete pod spec`() {
        val template = PodTemplateSpec(
            metadata = ObjectMeta(
                labels = mapOf("app" to "web", "version" to "v2"),
                annotations = mapOf("prometheus.io/scrape" to "true")
            ),
            spec = PodSpec(
                containers = listOf(
                    Container(
                        name = "web",
                        image = "nginx:1.21",
                        ports = listOf(
                            ContainerPort(containerPort = 80, name = "http")
                        ),
                        env = listOf(
                            EnvVar(name = "ENV", value = "production")
                        ),
                        resources = ResourceRequirements(
                            requests = mapOf("memory" to "128Mi", "cpu" to "100m"),
                            limits = mapOf("memory" to "256Mi", "cpu" to "200m")
                        )
                    )
                ),
                restartPolicy = "Always",
                serviceAccountName = "web-sa"
            )
        )

        val serialized = json.encodeToString(template)
        val deserialized = json.decodeFromString<PodTemplateSpec>(serialized)

        assertEquals(2, deserialized.metadata?.labels?.size)
        assertEquals(1, deserialized.metadata?.annotations?.size)
        assertEquals(1, deserialized.spec?.containers?.size)
        assertEquals("nginx:1.21", deserialized.spec?.containers?.get(0)?.image)
        assertEquals("web-sa", deserialized.spec?.serviceAccountName)
    }

    @Test
    fun `test DeploymentList serialization and deserialization`() {
        val deploymentList = DeploymentList(
            metadata = ListMeta(resourceVersion = "12345"),
            items = listOf(
                Deployment(
                    metadata = ObjectMeta(name = "deployment-1"),
                    spec = DeploymentSpec(
                        replicas = 3,
                        selector = LabelSelector(matchLabels = mapOf("app" to "app1")),
                        template = PodTemplateSpec(
                            metadata = ObjectMeta(labels = mapOf("app" to "app1")),
                            spec = PodSpec(
                                containers = listOf(
                                    Container(name = "c1", image = "image1")
                                )
                            )
                        )
                    )
                ),
                Deployment(
                    metadata = ObjectMeta(name = "deployment-2"),
                    spec = DeploymentSpec(
                        replicas = 5,
                        selector = LabelSelector(matchLabels = mapOf("app" to "app2")),
                        template = PodTemplateSpec(
                            metadata = ObjectMeta(labels = mapOf("app" to "app2")),
                            spec = PodSpec(
                                containers = listOf(
                                    Container(name = "c2", image = "image2")
                                )
                            )
                        )
                    )
                )
            )
        )

        val serialized = json.encodeToString(deploymentList)
        val deserialized = json.decodeFromString<DeploymentList>(serialized)

        assertEquals(2, deserialized.items.size)
        assertEquals("deployment-1", deserialized.items[0].metadata.name)
        assertEquals("deployment-2", deserialized.items[1].metadata.name)
        assertEquals(3, deserialized.items[0].spec?.replicas)
        assertEquals(5, deserialized.items[1].spec?.replicas)
    }

    @Test
    fun `test Deployment with paused state`() {
        val deployment = Deployment(
            metadata = ObjectMeta(name = "paused-deployment"),
            spec = DeploymentSpec(
                replicas = 3,
                selector = LabelSelector(matchLabels = mapOf("app" to "test")),
                template = PodTemplateSpec(
                    metadata = ObjectMeta(labels = mapOf("app" to "test")),
                    spec = PodSpec(
                        containers = listOf(
                            Container(name = "test", image = "test:v1")
                        )
                    )
                ),
                paused = true
            )
        )

        val serialized = json.encodeToString(deployment)
        val deserialized = json.decodeFromString<Deployment>(serialized)

        assertTrue(deserialized.spec?.paused == true)
    }

    @Test
    fun `test RollingUpdateDeployment with percentage values`() {
        val rollingUpdate = RollingUpdateDeployment(
            maxUnavailable = "25%",
            maxSurge = "50%"
        )

        val serialized = json.encodeToString(rollingUpdate)
        val deserialized = json.decodeFromString<RollingUpdateDeployment>(serialized)

        assertEquals("25%", deserialized.maxUnavailable)
        assertEquals("50%", deserialized.maxSurge)
    }
}
