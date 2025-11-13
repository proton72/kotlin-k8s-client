package io.github.proton72.k8s.exception

import kotlin.test.*

class KubernetesExceptionTest {

    @Test
    fun `test KubernetesException with message`() {
        val exception = KubernetesException("Test error message")

        assertEquals("Test error message", exception.message)
        assertNull(exception.cause)
    }

    @Test
    fun `test KubernetesException with message and cause`() {
        val cause = RuntimeException("Root cause")
        val exception = KubernetesException("Test error message", cause)

        assertEquals("Test error message", exception.message)
        assertNotNull(exception.cause)
        assertEquals("Root cause", exception.cause?.message)
    }

    @Test
    fun `test KubernetesApiException with status code`() {
        val exception = KubernetesApiException(
            statusCode = 500,
            message = "Internal server error"
        )

        assertEquals(500, exception.statusCode)
        assertTrue(exception.message?.contains("500") == true)
        assertTrue(exception.message?.contains("Internal server error") == true)
    }

    @Test
    fun `test KubernetesApiException with 400 status code`() {
        val exception = KubernetesApiException(
            statusCode = 400,
            message = "Bad request: invalid pod spec"
        )

        assertEquals(400, exception.statusCode)
        assertTrue(exception.message?.contains("400") == true)
        assertTrue(exception.message?.contains("Bad request") == true)
    }

    @Test
    fun `test KubernetesApiException with 404 status code`() {
        val exception = KubernetesApiException(
            statusCode = 404,
            message = "Resource not found"
        )

        assertEquals(404, exception.statusCode)
        assertTrue(exception.message?.contains("404") == true)
    }

    @Test
    fun `test KubernetesApiException with cause`() {
        val cause = IllegalArgumentException("Invalid argument")
        val exception = KubernetesApiException(
            statusCode = 422,
            message = "Unprocessable entity",
            cause = cause
        )

        assertEquals(422, exception.statusCode)
        assertNotNull(exception.cause)
        assertEquals("Invalid argument", exception.cause?.message)
    }

    @Test
    fun `test KubernetesAuthenticationException with message`() {
        val exception = KubernetesAuthenticationException("Invalid token")

        assertTrue(exception.message?.contains("Authentication failed") == true)
        assertTrue(exception.message?.contains("Invalid token") == true)
    }

    @Test
    fun `test KubernetesAuthenticationException with cause`() {
        val cause = SecurityException("Token expired")
        val exception = KubernetesAuthenticationException(
            message = "Token validation failed",
            cause = cause
        )

        assertTrue(exception.message?.contains("Authentication failed") == true)
        assertTrue(exception.message?.contains("Token validation failed") == true)
        assertNotNull(exception.cause)
        assertEquals("Token expired", exception.cause?.message)
    }

    @Test
    fun `test KubernetesNotFoundException for pod`() {
        val exception = KubernetesNotFoundException(
            resourceType = "Pod",
            resourceName = "my-pod",
            namespace = "default"
        )

        assertTrue(exception.message?.contains("Resource not found") == true)
        assertTrue(exception.message?.contains("Pod") == true)
        assertTrue(exception.message?.contains("my-pod") == true)
        assertTrue(exception.message?.contains("default") == true)
    }

    @Test
    fun `test KubernetesNotFoundException for service without namespace`() {
        val exception = KubernetesNotFoundException(
            resourceType = "Service",
            resourceName = "my-service"
        )

        assertTrue(exception.message?.contains("Resource not found") == true)
        assertTrue(exception.message?.contains("Service") == true)
        assertTrue(exception.message?.contains("my-service") == true)
        assertFalse(exception.message?.contains("namespace") == true)
    }

    @Test
    fun `test KubernetesNotFoundException for deployment`() {
        val exception = KubernetesNotFoundException(
            resourceType = "Deployment",
            resourceName = "my-deployment",
            namespace = "production"
        )

        assertTrue(exception.message?.contains("Deployment") == true)
        assertTrue(exception.message?.contains("my-deployment") == true)
        assertTrue(exception.message?.contains("production") == true)
    }

    @Test
    fun `test KubernetesConfigException with message`() {
        val exception = KubernetesConfigException("Invalid API server URL")

        assertTrue(exception.message?.contains("Configuration error") == true)
        assertTrue(exception.message?.contains("Invalid API server URL") == true)
    }

    @Test
    fun `test KubernetesConfigException with cause`() {
        val cause = IllegalStateException("Missing configuration file")
        val exception = KubernetesConfigException(
            message = "Failed to load configuration",
            cause = cause
        )

        assertTrue(exception.message?.contains("Configuration error") == true)
        assertTrue(exception.message?.contains("Failed to load configuration") == true)
        assertNotNull(exception.cause)
        assertEquals("Missing configuration file", exception.cause?.message)
    }

    @Test
    fun `test exception hierarchy - KubernetesApiException is KubernetesException`() {
        val exception = KubernetesApiException(500, "Error")

        assertTrue(exception is KubernetesException)
        assertTrue(exception is RuntimeException)
    }

    @Test
    fun `test exception hierarchy - KubernetesAuthenticationException is KubernetesException`() {
        val exception = KubernetesAuthenticationException("Auth failed")

        assertTrue(exception is KubernetesException)
        assertTrue(exception is RuntimeException)
    }

    @Test
    fun `test exception hierarchy - KubernetesNotFoundException is KubernetesException`() {
        val exception = KubernetesNotFoundException("Pod", "test-pod")

        assertTrue(exception is KubernetesException)
        assertTrue(exception is RuntimeException)
    }

    @Test
    fun `test exception hierarchy - KubernetesConfigException is KubernetesException`() {
        val exception = KubernetesConfigException("Config error")

        assertTrue(exception is KubernetesException)
        assertTrue(exception is RuntimeException)
    }

    @Test
    fun `test exception with nested causes`() {
        val rootCause = IllegalArgumentException("Invalid argument")
        val midCause = RuntimeException("Processing failed", rootCause)
        val exception = KubernetesException("Operation failed", midCause)

        assertNotNull(exception.cause)
        assertEquals("Processing failed", exception.cause?.message)
        assertNotNull(exception.cause?.cause)
        assertEquals("Invalid argument", exception.cause?.cause?.message)
    }

    @Test
    fun `test multiple exception types with different messages`() {
        val exceptions = listOf(
            KubernetesException("General error"),
            KubernetesApiException(401, "Unauthorized"),
            KubernetesAuthenticationException("Invalid credentials"),
            KubernetesNotFoundException("ConfigMap", "app-config", "default"),
            KubernetesConfigException("Missing CA certificate")
        )

        assertEquals(5, exceptions.size)
        exceptions.forEach { exception ->
            assertNotNull(exception.message)
            assertTrue(exception.message!!.isNotEmpty())
        }
    }

    @Test
    fun `test KubernetesApiException with common HTTP status codes`() {
        val testCases = mapOf(
            400 to "Bad Request",
            401 to "Unauthorized",
            403 to "Forbidden",
            404 to "Not Found",
            409 to "Conflict",
            422 to "Unprocessable Entity",
            500 to "Internal Server Error",
            503 to "Service Unavailable"
        )

        testCases.forEach { (statusCode, description) ->
            val exception = KubernetesApiException(statusCode, description)
            assertEquals(statusCode, exception.statusCode)
            assertTrue(exception.message?.contains(statusCode.toString()) == true)
        }
    }
}
