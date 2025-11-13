package io.github.proton72.k8s.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

class ServiceModelTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    @Test
    fun `test Service serialization and deserialization`() {
        val service = Service(
            metadata = ObjectMeta(
                name = "test-service",
                namespace = "default",
                labels = mapOf("app" to "web")
            ),
            spec = ServiceSpec(
                selector = mapOf("app" to "web"),
                ports = listOf(
                    ServicePort(
                        name = "http",
                        port = 80,
                        targetPort = 8080,
                        protocol = "TCP"
                    )
                ),
                type = "ClusterIP",
                clusterIP = "10.96.0.1"
            )
        )

        val serialized = json.encodeToString(service)
        val deserialized = json.decodeFromString<Service>(serialized)

        assertEquals(service.metadata.name, deserialized.metadata.name)
        assertEquals(service.spec?.type, deserialized.spec?.type)
        assertEquals(service.spec?.clusterIP, deserialized.spec?.clusterIP)
        assertEquals(1, deserialized.spec?.ports?.size)
    }

    @Test
    fun `test Service with ClusterIP type`() {
        val service = Service(
            metadata = ObjectMeta(name = "clusterip-service"),
            spec = ServiceSpec(
                type = "ClusterIP",
                selector = mapOf("app" to "backend"),
                ports = listOf(
                    ServicePort(port = 8080, targetPort = 8080)
                ),
                sessionAffinity = "ClientIP"
            )
        )

        val serialized = json.encodeToString(service)
        val deserialized = json.decodeFromString<Service>(serialized)

        assertEquals("ClusterIP", deserialized.spec?.type)
        assertEquals("ClientIP", deserialized.spec?.sessionAffinity)
    }

    @Test
    fun `test Service with NodePort type`() {
        val service = Service(
            metadata = ObjectMeta(name = "nodeport-service"),
            spec = ServiceSpec(
                type = "NodePort",
                selector = mapOf("app" to "web"),
                ports = listOf(
                    ServicePort(
                        port = 80,
                        targetPort = 8080,
                        nodePort = 30080,
                        protocol = "TCP"
                    )
                )
            )
        )

        val serialized = json.encodeToString(service)
        val deserialized = json.decodeFromString<Service>(serialized)

        assertEquals("NodePort", deserialized.spec?.type)
        assertEquals(30080, deserialized.spec?.ports?.get(0)?.nodePort)
    }

    @Test
    fun `test Service with LoadBalancer type`() {
        val service = Service(
            metadata = ObjectMeta(name = "lb-service"),
            spec = ServiceSpec(
                type = "LoadBalancer",
                selector = mapOf("app" to "web"),
                ports = listOf(
                    ServicePort(port = 443, targetPort = 8443)
                ),
                loadBalancerIP = "203.0.113.1",
                loadBalancerSourceRanges = listOf("0.0.0.0/0"),
                externalTrafficPolicy = "Local",
                healthCheckNodePort = 30000
            ),
            status = ServiceStatus(
                loadBalancer = LoadBalancerStatus(
                    ingress = listOf(
                        LoadBalancerIngress(
                            ip = "203.0.113.1",
                            hostname = "lb.example.com"
                        )
                    )
                )
            )
        )

        val serialized = json.encodeToString(service)
        val deserialized = json.decodeFromString<Service>(serialized)

        assertEquals("LoadBalancer", deserialized.spec?.type)
        assertEquals("203.0.113.1", deserialized.spec?.loadBalancerIP)
        assertEquals("Local", deserialized.spec?.externalTrafficPolicy)
        assertNotNull(deserialized.status?.loadBalancer)
        assertEquals(1, deserialized.status?.loadBalancer?.ingress?.size)
        assertEquals("203.0.113.1", deserialized.status?.loadBalancer?.ingress?.get(0)?.ip)
    }

    @Test
    fun `test Service with ExternalName type`() {
        val service = Service(
            metadata = ObjectMeta(name = "external-service"),
            spec = ServiceSpec(
                type = "ExternalName",
                externalName = "database.example.com"
            )
        )

        val serialized = json.encodeToString(service)
        val deserialized = json.decodeFromString<Service>(serialized)

        assertEquals("ExternalName", deserialized.spec?.type)
        assertEquals("database.example.com", deserialized.spec?.externalName)
    }

    @Test
    fun `test Service with multiple ports`() {
        val service = Service(
            metadata = ObjectMeta(name = "multi-port-service"),
            spec = ServiceSpec(
                selector = mapOf("app" to "web"),
                ports = listOf(
                    ServicePort(
                        name = "http",
                        port = 80,
                        targetPort = 8080,
                        protocol = "TCP"
                    ),
                    ServicePort(
                        name = "https",
                        port = 443,
                        targetPort = 8443,
                        protocol = "TCP"
                    ),
                    ServicePort(
                        name = "metrics",
                        port = 9090,
                        targetPort = 9090,
                        protocol = "TCP"
                    )
                ),
                type = "LoadBalancer"
            )
        )

        val serialized = json.encodeToString(service)
        val deserialized = json.decodeFromString<Service>(serialized)

        assertEquals(3, deserialized.spec?.ports?.size)
        assertEquals("http", deserialized.spec?.ports?.get(0)?.name)
        assertEquals("https", deserialized.spec?.ports?.get(1)?.name)
        assertEquals("metrics", deserialized.spec?.ports?.get(2)?.name)
    }

    @Test
    fun `test ServiceList serialization and deserialization`() {
        val serviceList = ServiceList(
            metadata = ListMeta(resourceVersion = "12345"),
            items = listOf(
                Service(
                    metadata = ObjectMeta(name = "service-1"),
                    spec = ServiceSpec(
                        selector = mapOf("app" to "app1"),
                        ports = listOf(ServicePort(port = 80, targetPort = 8080))
                    )
                ),
                Service(
                    metadata = ObjectMeta(name = "service-2"),
                    spec = ServiceSpec(
                        selector = mapOf("app" to "app2"),
                        ports = listOf(ServicePort(port = 443, targetPort = 8443))
                    )
                )
            )
        )

        val serialized = json.encodeToString(serviceList)
        val deserialized = json.decodeFromString<ServiceList>(serialized)

        assertEquals(2, deserialized.items.size)
        assertEquals("service-1", deserialized.items[0].metadata.name)
        assertEquals("service-2", deserialized.items[1].metadata.name)
        assertEquals("12345", deserialized.metadata.resourceVersion)
    }

    @Test
    fun `test ServicePort with all fields`() {
        val servicePort = ServicePort(
            name = "https",
            protocol = "TCP",
            port = 443,
            targetPort = 8443,
            nodePort = 30443
        )

        val serialized = json.encodeToString(servicePort)
        val deserialized = json.decodeFromString<ServicePort>(serialized)

        assertEquals("https", deserialized.name)
        assertEquals("TCP", deserialized.protocol)
        assertEquals(443, deserialized.port)
        assertEquals(8443, deserialized.targetPort)
        assertEquals(30443, deserialized.nodePort)
    }

    @Test
    fun `test Service with external IPs`() {
        val service = Service(
            metadata = ObjectMeta(name = "external-ip-service"),
            spec = ServiceSpec(
                selector = mapOf("app" to "web"),
                ports = listOf(ServicePort(port = 80, targetPort = 8080)),
                externalIPs = listOf("192.168.1.100", "192.168.1.101")
            )
        )

        val serialized = json.encodeToString(service)
        val deserialized = json.decodeFromString<Service>(serialized)

        assertEquals(2, deserialized.spec?.externalIPs?.size)
        assertEquals("192.168.1.100", deserialized.spec?.externalIPs?.get(0))
        assertEquals("192.168.1.101", deserialized.spec?.externalIPs?.get(1))
    }
}
