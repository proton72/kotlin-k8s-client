package io.github.proton72.k8s.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

class CommonModelTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    @Test
    fun `test ObjectMeta serialization and deserialization`() {
        val metadata = ObjectMeta(
            name = "test-resource",
            namespace = "production",
            uid = "abc-123-def",
            resourceVersion = "12345",
            creationTimestamp = "2024-01-01T00:00:00Z",
            labels = mapOf("app" to "myapp", "version" to "v1"),
            annotations = mapOf("description" to "Test resource")
        )

        val serialized = json.encodeToString(metadata)
        val deserialized = json.decodeFromString<ObjectMeta>(serialized)

        assertEquals(metadata.name, deserialized.name)
        assertEquals(metadata.namespace, deserialized.namespace)
        assertEquals(metadata.uid, deserialized.uid)
        assertEquals(2, deserialized.labels?.size)
        assertEquals(1, deserialized.annotations?.size)
    }

    @Test
    fun `test ObjectMeta with generateName`() {
        val metadata = ObjectMeta(
            generateName = "pod-",
            namespace = "default"
        )

        val serialized = json.encodeToString(metadata)
        val deserialized = json.decodeFromString<ObjectMeta>(serialized)

        assertEquals("pod-", deserialized.generateName)
        assertNull(deserialized.name)
    }

    @Test
    fun `test ObjectMeta with finalizers`() {
        val metadata = ObjectMeta(
            name = "resource-with-finalizers",
            finalizers = listOf(
                "kubernetes.io/pvc-protection",
                "custom.io/cleanup"
            )
        )

        val serialized = json.encodeToString(metadata)
        val deserialized = json.decodeFromString<ObjectMeta>(serialized)

        assertEquals(2, deserialized.finalizers?.size)
        assertEquals("kubernetes.io/pvc-protection", deserialized.finalizers?.get(0))
        assertEquals("custom.io/cleanup", deserialized.finalizers?.get(1))
    }

    @Test
    fun `test Status serialization and deserialization`() {
        val status = Status(
            status = "Success",
            message = "Operation completed successfully",
            reason = "OK",
            code = 200,
            details = StatusDetails(
                name = "test-pod",
                kind = "Pod",
                causes = listOf(
                    StatusCause(
                        reason = "FieldValueInvalid",
                        message = "Invalid value",
                        field = "spec.containers[0].image"
                    )
                )
            )
        )

        val serialized = json.encodeToString(status)
        val deserialized = json.decodeFromString<Status>(serialized)

        assertEquals("Success", deserialized.status)
        assertEquals(200, deserialized.code)
        assertNotNull(deserialized.details)
        assertEquals("test-pod", deserialized.details?.name)
        assertEquals(1, deserialized.details?.causes?.size)
    }

    @Test
    fun `test Status with failure`() {
        val status = Status(
            status = "Failure",
            message = "Resource not found",
            reason = "NotFound",
            code = 404
        )

        val serialized = json.encodeToString(status)
        val deserialized = json.decodeFromString<Status>(serialized)

        assertEquals("Failure", deserialized.status)
        assertEquals("NotFound", deserialized.reason)
        assertEquals(404, deserialized.code)
    }

    @Test
    fun `test ListMeta serialization and deserialization`() {
        val listMeta = ListMeta(
            resourceVersion = "12345",
            continue_ = "next-token",
            remainingItemCount = 100
        )

        val serialized = json.encodeToString(listMeta)
        val deserialized = json.decodeFromString<ListMeta>(serialized)

        assertEquals("12345", deserialized.resourceVersion)
        assertEquals("next-token", deserialized.continue_)
        assertEquals(100L, deserialized.remainingItemCount)
    }

    @Test
    fun `test LabelSelector with matchLabels only`() {
        val selector = LabelSelector(
            matchLabels = mapOf("app" to "web", "tier" to "frontend")
        )

        val serialized = json.encodeToString(selector)
        val deserialized = json.decodeFromString<LabelSelector>(serialized)

        assertEquals(2, deserialized.matchLabels?.size)
        assertEquals("web", deserialized.matchLabels?.get("app"))
        assertNull(deserialized.matchExpressions)
    }

    @Test
    fun `test LabelSelector with matchExpressions only`() {
        val selector = LabelSelector(
            matchExpressions = listOf(
                LabelSelectorRequirement(
                    key = "environment",
                    operator = "In",
                    values = listOf("prod", "staging")
                )
            )
        )

        val serialized = json.encodeToString(selector)
        val deserialized = json.decodeFromString<LabelSelector>(serialized)

        assertNull(deserialized.matchLabels)
        assertEquals(1, deserialized.matchExpressions?.size)
        assertEquals("environment", deserialized.matchExpressions?.get(0)?.key)
    }

    @Test
    fun `test LabelSelectorRequirement with In operator`() {
        val requirement = LabelSelectorRequirement(
            key = "tier",
            operator = "In",
            values = listOf("frontend", "backend")
        )

        val serialized = json.encodeToString(requirement)
        val deserialized = json.decodeFromString<LabelSelectorRequirement>(serialized)

        assertEquals("tier", deserialized.key)
        assertEquals("In", deserialized.operator)
        assertEquals(2, deserialized.values?.size)
    }

    @Test
    fun `test LabelSelectorRequirement with Exists operator`() {
        val requirement = LabelSelectorRequirement(
            key = "app",
            operator = "Exists"
        )

        val serialized = json.encodeToString(requirement)
        val deserialized = json.decodeFromString<LabelSelectorRequirement>(serialized)

        assertEquals("app", deserialized.key)
        assertEquals("Exists", deserialized.operator)
        assertNull(deserialized.values)
    }

    @Test
    fun `test DeleteOptions serialization and deserialization`() {
        val deleteOptions = DeleteOptions(
            gracePeriodSeconds = 30,
            propagationPolicy = "Foreground",
            dryRun = listOf("All")
        )

        val serialized = json.encodeToString(deleteOptions)
        val deserialized = json.decodeFromString<DeleteOptions>(serialized)

        assertEquals(30, deserialized.gracePeriodSeconds)
        assertEquals("Foreground", deserialized.propagationPolicy)
        assertEquals(1, deserialized.dryRun?.size)
        assertEquals("All", deserialized.dryRun?.get(0))
    }

    @Test
    fun `test DeleteOptions with default values`() {
        val deleteOptions = DeleteOptions()

        val serialized = json.encodeToString(deleteOptions)
        val deserialized = json.decodeFromString<DeleteOptions>(serialized)

        assertEquals("v1", deserialized.apiVersion)
        assertEquals("DeleteOptions", deserialized.kind)
        assertNull(deserialized.gracePeriodSeconds)
        assertNull(deserialized.propagationPolicy)
    }

    @Test
    fun `test StatusDetails with multiple causes`() {
        val details = StatusDetails(
            name = "invalid-pod",
            kind = "Pod",
            causes = listOf(
                StatusCause(
                    reason = "FieldValueRequired",
                    message = "Required value: must specify image",
                    field = "spec.containers[0].image"
                ),
                StatusCause(
                    reason = "FieldValueInvalid",
                    message = "Invalid value: must be a valid port number",
                    field = "spec.containers[0].ports[0].containerPort"
                )
            )
        )

        val serialized = json.encodeToString(details)
        val deserialized = json.decodeFromString<StatusDetails>(serialized)

        assertEquals("invalid-pod", deserialized.name)
        assertEquals("Pod", deserialized.kind)
        assertEquals(2, deserialized.causes?.size)
        assertEquals("FieldValueRequired", deserialized.causes?.get(0)?.reason)
        assertEquals("FieldValueInvalid", deserialized.causes?.get(1)?.reason)
    }

    @Test
    fun `test empty ObjectMeta serialization`() {
        val metadata = ObjectMeta()

        val serialized = json.encodeToString(metadata)
        val deserialized = json.decodeFromString<ObjectMeta>(serialized)

        assertNull(deserialized.name)
        assertNull(deserialized.namespace)
        assertNull(deserialized.labels)
        assertNull(deserialized.annotations)
    }

    @Test
    fun `test ObjectMeta with deletion timestamp`() {
        val metadata = ObjectMeta(
            name = "deleting-resource",
            namespace = "default",
            deletionTimestamp = "2024-01-01T12:00:00Z",
            finalizers = listOf("cleanup")
        )

        val serialized = json.encodeToString(metadata)
        val deserialized = json.decodeFromString<ObjectMeta>(serialized)

        assertEquals("deleting-resource", deserialized.name)
        assertNotNull(deserialized.deletionTimestamp)
        assertEquals(1, deserialized.finalizers?.size)
    }
}
