# Kotlin Kubernetes Client

A lightweight, type-safe Kubernetes client library for Kotlin using Ktor HTTP client. This library provides a simple and intuitive API for interacting with Kubernetes clusters.

## Features

- **Type-safe**: Strongly typed Kubernetes resource models
- **Lightweight**: Built on Ktor HTTP client with minimal dependencies
- **Coroutine-based**: Fully asynchronous using Kotlin coroutines
- **In-cluster support**: Automatic service account authentication when running in Kubernetes
- **Manual configuration**: Support for custom API server and token configuration
- **Comprehensive API coverage**: Support for Pods, Services, and Deployments
- **SSL/TLS support**: Proper certificate validation with custom CA support

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("io.github.proton72:kotlin-k8s-client:1.0.0")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'io.github.proton72:kotlin-k8s-client:1.0.0'
}
```

### Maven

```xml
<dependency>
    <groupId>io.github.proton72</groupId>
    <artifactId>kotlin-k8s-client</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Usage

### In-Cluster Configuration

When running inside a Kubernetes pod, the client automatically uses the service account credentials:

```kotlin
import io.github.proton72.k8s.client.KubernetesClient
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val client = KubernetesClient()

    // List all pods in the current namespace
    val pods = client.listPods()
    pods.items.forEach { pod ->
        println("Pod: ${pod.metadata.name}, Status: ${pod.status?.phase}")
    }

    client.close()
}
```

### Manual Configuration

For testing or when running outside the cluster:

```kotlin
import io.github.proton72.k8s.client.KubernetesClient
import io.github.proton72.k8s.client.KubernetesClientConfig
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val config = KubernetesClientConfig(
        apiServer = "https://kubernetes.example.com:6443",
        token = "your-bearer-token",
        namespace = "default"
    )

    val client = KubernetesClient(config)

    // Your operations here

    client.close()
}
```

## Examples

### Pod Operations

#### Get a Pod

```kotlin
val pod = client.getPod("my-pod", "default")
println("Pod IP: ${pod.status?.podIP}")
```

#### List Pods

```kotlin
// List pods in a specific namespace
val pods = client.listPods("default")

// List pods with label selector
val filteredPods = client.listPods("default", labelSelector = "app=myapp")

// List all pods across all namespaces
val allPods = client.listAllPods()
```

#### Create a Pod

```kotlin
import io.github.proton72.k8s.model.*

val pod = Pod(
    metadata = ObjectMeta(
        name = "nginx-pod",
        labels = mapOf("app" to "nginx")
    ),
    spec = PodSpec(
        containers = listOf(
            Container(
                name = "nginx",
                image = "nginx:latest",
                ports = listOf(
                    ContainerPort(containerPort = 80)
                )
            )
        )
    )
)

val createdPod = client.createPod(pod, "default")
println("Created pod: ${createdPod.metadata.name}")
```

#### Delete a Pod

```kotlin
// Delete with default grace period (30s)
client.deletePod("my-pod", "default")

// Delete with custom grace period
client.deletePod("my-pod", "default", gracePeriodSeconds = 15)

// Delete with propagation policy
client.deletePod(
    "my-pod",
    "default",
    gracePeriodSeconds = 0,
    propagationPolicy = "Foreground"
)
```

#### Update a Pod

```kotlin
val pod = client.getPod("my-pod", "default")
val updatedPod = pod.copy(
    metadata = pod.metadata.copy(
        labels = mapOf("app" to "updated")
    )
)
client.updatePod(updatedPod, "default")
```

### Service Operations

#### Get a Service

```kotlin
val service = client.getService("my-service", "default")
println("Cluster IP: ${service.spec?.clusterIP}")
```

#### List Services

```kotlin
// List all services in a namespace
val services = client.listServices("default")

// List with label selector
val filteredServices = client.listServices("default", labelSelector = "app=myapp")
```

#### Create a Service

```kotlin
import io.github.proton72.k8s.model.*

val service = Service(
    metadata = ObjectMeta(
        name = "nginx-service"
    ),
    spec = ServiceSpec(
        selector = mapOf("app" to "nginx"),
        ports = listOf(
            ServicePort(
                port = 80,
                targetPort = 80,
                protocol = "TCP"
            )
        ),
        type = "ClusterIP"
    )
)

val createdService = client.createService(service, "default")
```

#### Delete a Service

```kotlin
client.deleteService("my-service", "default")
```

#### Update a Service

```kotlin
val service = client.getService("my-service", "default")
val updatedService = service.copy(
    spec = service.spec?.copy(
        type = "LoadBalancer"
    )
)
client.updateService(updatedService, "default")
```

### Deployment Operations

#### Get a Deployment

```kotlin
val deployment = client.getDeployment("my-deployment", "default")
println("Replicas: ${deployment.spec?.replicas}")
```

#### List Deployments

```kotlin
// List all deployments in a namespace
val deployments = client.listDeployments("default")

// List with label selector
val filteredDeployments = client.listDeployments("default", labelSelector = "app=myapp")
```

#### Create a Deployment

```kotlin
import io.github.proton72.k8s.model.*

val deployment = Deployment(
    metadata = ObjectMeta(
        name = "nginx-deployment"
    ),
    spec = DeploymentSpec(
        replicas = 3,
        selector = LabelSelector(
            matchLabels = mapOf("app" to "nginx")
        ),
        template = PodTemplateSpec(
            metadata = ObjectMeta(
                labels = mapOf("app" to "nginx")
            ),
            spec = PodSpec(
                containers = listOf(
                    Container(
                        name = "nginx",
                        image = "nginx:1.21",
                        ports = listOf(
                            ContainerPort(containerPort = 80)
                        )
                    )
                )
            )
        )
    )
)

val createdDeployment = client.createDeployment(deployment, "default")
```

#### Delete a Deployment

```kotlin
client.deleteDeployment("my-deployment", "default")
```

#### Scale a Deployment

```kotlin
// Scale to 5 replicas
client.scaleDeployment("my-deployment", replicas = 5, namespace = "default")
```

#### Update a Deployment

```kotlin
val deployment = client.getDeployment("my-deployment", "default")
val updatedDeployment = deployment.copy(
    spec = deployment.spec?.copy(
        replicas = 5
    )
)
client.updateDeployment(updatedDeployment, "default")
```

### Advanced Examples

#### Create a Complex Pod with Environment Variables and Volumes

```kotlin
val pod = Pod(
    metadata = ObjectMeta(
        name = "complex-pod",
        labels = mapOf("app" to "myapp", "tier" to "backend")
    ),
    spec = PodSpec(
        containers = listOf(
            Container(
                name = "app",
                image = "myapp:1.0",
                env = listOf(
                    EnvVar(name = "ENV", value = "production"),
                    EnvVar(
                        name = "DB_PASSWORD",
                        valueFrom = EnvVarSource(
                            secretKeyRef = SecretKeySelector(
                                name = "db-secret",
                                key = "password"
                            )
                        )
                    )
                ),
                ports = listOf(
                    ContainerPort(containerPort = 8080, name = "http")
                ),
                volumeMounts = listOf(
                    VolumeMount(
                        name = "config",
                        mountPath = "/etc/config"
                    )
                ),
                resources = ResourceRequirements(
                    requests = mapOf(
                        "memory" to "128Mi",
                        "cpu" to "100m"
                    ),
                    limits = mapOf(
                        "memory" to "256Mi",
                        "cpu" to "200m"
                    )
                ),
                livenessProbe = Probe(
                    httpGet = HTTPGetAction(
                        path = "/health",
                        port = 8080
                    ),
                    initialDelaySeconds = 30,
                    periodSeconds = 10
                )
            )
        ),
        volumes = listOf(
            Volume(
                name = "config",
                configMap = ConfigMapVolumeSource(
                    name = "app-config"
                )
            )
        )
    )
)

client.createPod(pod, "default")
```

#### Watch Pod Status

```kotlin
import kotlinx.coroutines.delay

suspend fun waitForPodReady(client: KubernetesClient, podName: String, namespace: String) {
    while (true) {
        val pod = client.getPod(podName, namespace)
        val phase = pod.status?.phase

        when (phase) {
            "Running" -> {
                println("Pod is running")
                break
            }
            "Failed", "Unknown" -> {
                println("Pod failed: ${pod.status?.message}")
                break
            }
            else -> {
                println("Pod status: $phase")
                delay(2000)
            }
        }
    }
}
```

### Error Handling

```kotlin
import io.github.proton72.k8s.exception.*

try {
    val pod = client.getPod("non-existent-pod", "default")
} catch (e: KubernetesNotFoundException) {
    println("Pod not found: ${e.message}")
} catch (e: KubernetesAuthenticationException) {
    println("Authentication failed: ${e.message}")
} catch (e: KubernetesApiException) {
    println("API error (${e.statusCode}): ${e.message}")
} catch (e: KubernetesException) {
    println("General error: ${e.message}")
}
```

## API Reference

### KubernetesClient

#### Pod Operations
- `suspend fun getPod(name: String, namespace: String = defaultNamespace): Pod`
- `suspend fun listPods(namespace: String = defaultNamespace, labelSelector: String? = null): PodList`
- `suspend fun listAllPods(labelSelector: String? = null): PodList`
- `suspend fun createPod(pod: Pod, namespace: String = defaultNamespace): Pod`
- `suspend fun deletePod(name: String, namespace: String = defaultNamespace, gracePeriodSeconds: Int = 30, propagationPolicy: String? = null): Status`
- `suspend fun updatePod(pod: Pod, namespace: String = defaultNamespace): Pod`

#### Service Operations
- `suspend fun getService(name: String, namespace: String = defaultNamespace): Service`
- `suspend fun listServices(namespace: String = defaultNamespace, labelSelector: String? = null): ServiceList`
- `suspend fun createService(service: Service, namespace: String = defaultNamespace): Service`
- `suspend fun deleteService(name: String, namespace: String = defaultNamespace): Status`
- `suspend fun updateService(service: Service, namespace: String = defaultNamespace): Service`

#### Deployment Operations
- `suspend fun getDeployment(name: String, namespace: String = defaultNamespace): Deployment`
- `suspend fun listDeployments(namespace: String = defaultNamespace, labelSelector: String? = null): DeploymentList`
- `suspend fun createDeployment(deployment: Deployment, namespace: String = defaultNamespace): Deployment`
- `suspend fun deleteDeployment(name: String, namespace: String = defaultNamespace, gracePeriodSeconds: Int = 30, propagationPolicy: String? = null): Status`
- `suspend fun updateDeployment(deployment: Deployment, namespace: String = defaultNamespace): Deployment`
- `suspend fun scaleDeployment(name: String, replicas: Int, namespace: String = defaultNamespace): Deployment`

## Requirements

- Kotlin 2.2.20 or higher
- Java 17 or higher
- Ktor 3.3.2
- Kotlinx Serialization 1.8.0
- Kotlinx Coroutines 1.10.1

## License

Apache License 2.0

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

For issues, questions, or contributions, please visit the [GitHub repository](https://github.com/proton72/kotlin-k8s-client).

## Acknowledgments

Based on the original code snippet for Kubernetes pod deletion, expanded to provide comprehensive Kubernetes API support.
