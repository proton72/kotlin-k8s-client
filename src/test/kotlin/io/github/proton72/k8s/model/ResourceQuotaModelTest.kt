package io.github.proton72.k8s.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

class ResourceQuotaModelTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    @Test
    fun `test ResourceQuota serialization and deserialization`() {
        val resourceQuota = ResourceQuota(
            metadata = ObjectMeta(
                name = "compute-resources",
                namespace = "default",
                labels = mapOf("team" to "backend", "env" to "production")
            ),
            spec = ResourceQuotaSpec(
                hard = mapOf(
                    "cpu" to "10",
                    "memory" to "20Gi",
                    "pods" to "10",
                    "persistentvolumeclaims" to "5"
                ),
                scopes = listOf("NotTerminating")
            ),
            status = ResourceQuotaStatus(
                hard = mapOf(
                    "cpu" to "10",
                    "memory" to "20Gi",
                    "pods" to "10"
                ),
                used = mapOf(
                    "cpu" to "5",
                    "memory" to "10Gi",
                    "pods" to "5"
                )
            )
        )

        val serialized = json.encodeToString(resourceQuota)
        val deserialized = json.decodeFromString<ResourceQuota>(serialized)

        assertEquals(resourceQuota.metadata.name, deserialized.metadata.name)
        assertEquals(resourceQuota.metadata.namespace, deserialized.metadata.namespace)
        assertEquals(resourceQuota.spec?.hard?.get("cpu"), deserialized.spec?.hard?.get("cpu"))
        assertEquals(resourceQuota.spec?.hard?.get("memory"), deserialized.spec?.hard?.get("memory"))
        assertEquals(resourceQuota.status?.used?.get("cpu"), deserialized.status?.used?.get("cpu"))
    }

    @Test
    fun `test ResourceQuotaSpec with hard limits`() {
        val spec = ResourceQuotaSpec(
            hard = mapOf(
                "requests.cpu" to "1",
                "requests.memory" to "1Gi",
                "limits.cpu" to "2",
                "limits.memory" to "2Gi",
                "requests.nvidia.com/gpu" to "4"
            )
        )

        val serialized = json.encodeToString(spec)
        val deserialized = json.decodeFromString<ResourceQuotaSpec>(serialized)

        assertNotNull(deserialized.hard)
        assertEquals("1", deserialized.hard?.get("requests.cpu"))
        assertEquals("1Gi", deserialized.hard?.get("requests.memory"))
        assertEquals("2", deserialized.hard?.get("limits.cpu"))
        assertEquals("2Gi", deserialized.hard?.get("limits.memory"))
        assertEquals("4", deserialized.hard?.get("requests.nvidia.com/gpu"))
    }

    @Test
    fun `test ResourceQuotaSpec with scopes`() {
        val spec = ResourceQuotaSpec(
            hard = mapOf(
                "cpu" to "10",
                "memory" to "20Gi"
            ),
            scopes = listOf("Terminating", "NotBestEffort")
        )

        val serialized = json.encodeToString(spec)
        val deserialized = json.decodeFromString<ResourceQuotaSpec>(serialized)

        assertNotNull(deserialized.scopes)
        assertEquals(2, deserialized.scopes?.size)
        assertEquals("Terminating", deserialized.scopes?.get(0))
        assertEquals("NotBestEffort", deserialized.scopes?.get(1))
    }

    @Test
    fun `test ResourceQuotaSpec with scopeSelector`() {
        val spec = ResourceQuotaSpec(
            hard = mapOf(
                "count/pods" to "10"
            ),
            scopeSelector = ScopeSelector(
                matchExpressions = listOf(
                    ScopedResourceSelectorRequirement(
                        scopeName = "PriorityClass",
                        operator = "In",
                        values = listOf("high", "medium")
                    ),
                    ScopedResourceSelectorRequirement(
                        scopeName = "Terminating",
                        operator = "Exists"
                    )
                )
            )
        )

        val serialized = json.encodeToString(spec)
        val deserialized = json.decodeFromString<ResourceQuotaSpec>(serialized)

        assertNotNull(deserialized.scopeSelector)
        assertNotNull(deserialized.scopeSelector?.matchExpressions)
        assertEquals(2, deserialized.scopeSelector?.matchExpressions?.size)

        val firstExpr = deserialized.scopeSelector?.matchExpressions?.get(0)
        assertEquals("PriorityClass", firstExpr?.scopeName)
        assertEquals("In", firstExpr?.operator)
        assertEquals(2, firstExpr?.values?.size)
        assertEquals("high", firstExpr?.values?.get(0))

        val secondExpr = deserialized.scopeSelector?.matchExpressions?.get(1)
        assertEquals("Terminating", secondExpr?.scopeName)
        assertEquals("Exists", secondExpr?.operator)
    }

    @Test
    fun `test ResourceQuotaStatus with used resources`() {
        val status = ResourceQuotaStatus(
            hard = mapOf(
                "requests.cpu" to "10",
                "requests.memory" to "20Gi",
                "persistentvolumeclaims" to "10"
            ),
            used = mapOf(
                "requests.cpu" to "7",
                "requests.memory" to "15Gi",
                "persistentvolumeclaims" to "6"
            )
        )

        val serialized = json.encodeToString(status)
        val deserialized = json.decodeFromString<ResourceQuotaStatus>(serialized)

        assertNotNull(deserialized.hard)
        assertNotNull(deserialized.used)
        assertEquals("10", deserialized.hard?.get("requests.cpu"))
        assertEquals("7", deserialized.used?.get("requests.cpu"))
        assertEquals("20Gi", deserialized.hard?.get("requests.memory"))
        assertEquals("15Gi", deserialized.used?.get("requests.memory"))
    }

    @Test
    fun `test ResourceQuotaList serialization and deserialization`() {
        val resourceQuotaList = ResourceQuotaList(
            metadata = ListMeta(
                resourceVersion = "98765",
                remainingItemCount = 0
            ),
            items = listOf(
                ResourceQuota(
                    metadata = ObjectMeta(name = "quota-1", namespace = "default"),
                    spec = ResourceQuotaSpec(
                        hard = mapOf("cpu" to "5", "memory" to "10Gi")
                    )
                ),
                ResourceQuota(
                    metadata = ObjectMeta(name = "quota-2", namespace = "default"),
                    spec = ResourceQuotaSpec(
                        hard = mapOf("pods" to "10")
                    )
                )
            )
        )

        val serialized = json.encodeToString(resourceQuotaList)
        val deserialized = json.decodeFromString<ResourceQuotaList>(serialized)

        assertEquals(2, deserialized.items.size)
        assertEquals("quota-1", deserialized.items[0].metadata.name)
        assertEquals("quota-2", deserialized.items[1].metadata.name)
        assertEquals("98765", deserialized.metadata.resourceVersion)
        assertEquals("5", deserialized.items[0].spec?.hard?.get("cpu"))
        assertEquals("10", deserialized.items[1].spec?.hard?.get("pods"))
    }

    @Test
    fun `test ResourceQuota with object count quotas`() {
        val resourceQuota = ResourceQuota(
            metadata = ObjectMeta(
                name = "object-counts",
                namespace = "development"
            ),
            spec = ResourceQuotaSpec(
                hard = mapOf(
                    "count/configmaps" to "10",
                    "count/secrets" to "10",
                    "count/services" to "5",
                    "count/services.loadbalancers" to "2",
                    "count/replicationcontrollers" to "5"
                )
            )
        )

        val serialized = json.encodeToString(resourceQuota)
        val deserialized = json.decodeFromString<ResourceQuota>(serialized)

        assertNotNull(deserialized.spec?.hard)
        assertEquals("10", deserialized.spec?.hard?.get("count/configmaps"))
        assertEquals("10", deserialized.spec?.hard?.get("count/secrets"))
        assertEquals("5", deserialized.spec?.hard?.get("count/services"))
        assertEquals("2", deserialized.spec?.hard?.get("count/services.loadbalancers"))
    }

    @Test
    fun `test ResourceQuota with apiVersion and kind`() {
        val resourceQuota = ResourceQuota(
            metadata = ObjectMeta(name = "test-quota", namespace = "default"),
            spec = ResourceQuotaSpec(hard = mapOf("cpu" to "1"))
        )

        assertEquals("v1", resourceQuota.apiVersion)
        assertEquals("ResourceQuota", resourceQuota.kind)

        val serialized = json.encodeToString(resourceQuota)
        val deserialized = json.decodeFromString<ResourceQuota>(serialized)

        assertEquals("v1", deserialized.apiVersion)
        assertEquals("ResourceQuota", deserialized.kind)
    }

    @Test
    fun `test ResourceQuotaList with apiVersion and kind`() {
        val resourceQuotaList = ResourceQuotaList(
            metadata = ListMeta(),
            items = listOf()
        )

        assertEquals("v1", resourceQuotaList.apiVersion)
        assertEquals("ResourceQuotaList", resourceQuotaList.kind)

        val serialized = json.encodeToString(resourceQuotaList)
        val deserialized = json.decodeFromString<ResourceQuotaList>(serialized)

        assertEquals("v1", deserialized.apiVersion)
        assertEquals("ResourceQuotaList", deserialized.kind)
    }

    @Test
    fun `test ScopedResourceSelectorRequirement without values`() {
        val requirement = ScopedResourceSelectorRequirement(
            scopeName = "BestEffort",
            operator = "Exists"
        )

        val serialized = json.encodeToString(requirement)
        val deserialized = json.decodeFromString<ScopedResourceSelectorRequirement>(serialized)

        assertEquals("BestEffort", deserialized.scopeName)
        assertEquals("Exists", deserialized.operator)
        assertNull(deserialized.values)
    }

    @Test
    fun `test empty ResourceQuota`() {
        val resourceQuota = ResourceQuota(
            metadata = ObjectMeta(name = "empty-quota", namespace = "default")
        )

        val serialized = json.encodeToString(resourceQuota)
        val deserialized = json.decodeFromString<ResourceQuota>(serialized)

        assertEquals("empty-quota", deserialized.metadata.name)
        assertEquals("default", deserialized.metadata.namespace)
        assertNull(deserialized.spec)
        assertNull(deserialized.status)
    }
}
