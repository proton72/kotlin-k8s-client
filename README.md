# Kotlin Kubernetes Client

A lightweight, type-safe Kubernetes client library for Kotlin using Ktor HTTP client. This library provides a simple and intuitive API for interacting with Kubernetes clusters.

## Features

- **Type-safe**: Strongly typed Kubernetes resource models
- **Lightweight**: Built on Ktor HTTP client with minimal dependencies
- **Coroutine-based**: Fully asynchronous using Kotlin coroutines
- **Reactive Streaming**: Watch resources and stream logs using Kotlin Flow
- **In-cluster support**: Automatic service account authentication when running in Kubernetes
- **Manual configuration**: Support for custom API server and token configuration
- **Comprehensive API coverage**: Support for Pods, Services, and Deployments
- **Label Management**: Add, update, remove, and search resources by labels
- **Watch API**: Real-time monitoring of resource changes (ADDED, MODIFIED, DELETED events)
- **Pod Logs**: Stream pod logs with follow, tail, timestamps, and filtering options
- **SSL/TLS support**: Proper certificate validation with custom CA support

## Installation

### GitHub Packages

This library is published to GitHub Packages. You need to authenticate to use it.

#### Gradle (Kotlin DSL)

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/proton72/kotlin-k8s-client")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.token") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("io.github.proton72:kotlin-k8s-client:1.0.0")
}
```

#### Gradle (Groovy)

```groovy
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/proton72/kotlin-k8s-client")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.token") ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation 'io.github.proton72:kotlin-k8s-client:1.0.0'
}
```

#### Maven

Add to your `settings.xml`:

```xml
<servers>
  <server>
    <id>github</id>
    <username>YOUR_GITHUB_USERNAME</username>
    <password>YOUR_GITHUB_TOKEN</password>
  </server>
</servers>
```

Add to your `pom.xml`:

```xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/proton72/kotlin-k8s-client</url>
  </repository>
</repositories>

<dependency>
    <groupId>io.github.proton72</groupId>
    <artifactId>kotlin-k8s-client</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### Authentication

To authenticate, create a GitHub Personal Access Token with `read:packages` scope and set it as:
- Environment variables: `GITHUB_ACTOR` and `GITHUB_TOKEN`
- Gradle properties in `~/.gradle/gradle.properties`:
  ```properties
  gpr.user=YOUR_GITHUB_USERNAME
  gpr.token=YOUR_GITHUB_TOKEN
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

The `updatePod` method now uses Strategic Merge Patch to safely update mutable pod fields (labels, annotations, container images).

```kotlin
val pod = client.getPod("my-pod", "default")
val updatedPod = pod.copy(
    metadata = pod.metadata.copy(
        labels = mapOf("app" to "updated", "env" to "production")
    )
)
// This now safely updates only labels without touching immutable fields
client.updatePod(updatedPod, "default")
```

**Note:** For clarity when updating only labels/annotations, prefer using `patchPodMetadata` or `addPodLabels`.

#### Update Pod Labels

Use `patchPodMetadata` to update pod labels and annotations. This is the recommended way:

```kotlin
// Replace all labels on a pod
val patchedPod = client.patchPodMetadata(
    name = "my-pod",
    labels = mapOf("app" to "myapp", "env" to "production"),
    namespace = "default"
)

// Add or update specific labels (merges with existing labels)
val podWithNewLabels = client.addPodLabels(
    name = "my-pod",
    labels = mapOf("team" to "platform", "version" to "1.0"),
    namespace = "default"
)

// Remove specific labels
val podWithoutLabels = client.removePodLabels(
    name = "my-pod",
    labelKeys = listOf("old-label", "deprecated-label"),
    namespace = "default"
)

// Update annotations
val podWithAnnotations = client.patchPodMetadata(
    name = "my-pod",
    annotations = mapOf("description" to "Updated pod"),
    namespace = "default"
)
```

#### Search Pods by Label

```kotlin
// Find all pods with a specific label
val webPods = client.listPods("default", labelSelector = "app=web")

// Multiple label selectors (AND condition)
val prodWebPods = client.listPods("default", labelSelector = "app=web,env=production")

// Search across all namespaces
val allWebPods = client.listAllPods(labelSelector = "app=web")

// Label selectors support various operators
val newPods = client.listPods("default", labelSelector = "version!=1.0")
val podsByTier = client.listPods("default", labelSelector = "tier in (frontend,backend)")
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

### Watch Operations

Watch resources for real-time changes using Kotlin Flow.

#### Watch Pods

```kotlin
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

// Watch pods in a namespace
val watchJob = launch {
    client.watchPods(namespace = "default", labelSelector = "app=myapp")
        .catch { e -> println("Watch error: ${e.message}") }
        .collect { event ->
            println("Event: ${event.type} - Pod: ${event.`object`.metadata.name}")
            when (event.type) {
                "ADDED" -> println("New pod created")
                "MODIFIED" -> println("Pod updated: ${event.`object`.status?.phase}")
                "DELETED" -> println("Pod deleted")
            }
        }
}

// Watch all pods across all namespaces
client.watchAllPods(labelSelector = "app=myapp")
    .collect { event ->
        println("${event.type}: ${event.`object`.metadata.name} in ${event.`object`.metadata.namespace}")
    }

// Cancel watching
watchJob.cancel()
```

#### Watch Services

```kotlin
client.watchServices(namespace = "default")
    .collect { event ->
        println("Service ${event.type}: ${event.`object`.metadata.name}")
        println("Cluster IP: ${event.`object`.spec?.clusterIP}")
    }
```

#### Watch Deployments

```kotlin
client.watchDeployments(namespace = "default", labelSelector = "environment=production")
    .collect { event ->
        val deployment = event.`object`
        println("Deployment ${event.type}: ${deployment.metadata.name}")
        println("Replicas: ${deployment.status?.readyReplicas}/${deployment.spec?.replicas}")
    }
```

### Pod Logs

Get and stream pod logs with various options.

#### Get Recent Logs

```kotlin
import kotlinx.coroutines.flow.collect

// Get last 100 lines of logs
client.getPodLogs(
    name = "my-pod",
    namespace = "default",
    tailLines = 100,
    timestamps = true
).collect { line ->
    println(line)
}
```

#### Follow Logs in Real-Time

```kotlin
import kotlinx.coroutines.launch

// Stream logs continuously
val logJob = launch {
    client.getPodLogs(
        name = "my-pod",
        namespace = "default",
        follow = true,
        timestamps = true
    )
        .catch { e -> println("Error: ${e.message}") }
        .collect { line ->
            println(line)
        }
}

// Stop following logs when done
delay(60000) // Follow for 60 seconds
logJob.cancel()
```

#### Get Logs from Specific Container

```kotlin
// For multi-container pods
client.getPodLogs(
    name = "my-pod",
    namespace = "default",
    container = "app-container",
    tailLines = 50
).collect { line ->
    println(line)
}
```

#### Get Logs from Previous Container

```kotlin
// Useful when a container has crashed
client.getPodLogs(
    name = "my-pod",
    namespace = "default",
    previous = true,
    tailLines = 100
).collect { line ->
    println(line)
}
```

#### Filter Logs by Time

```kotlin
// Get logs from last 5 minutes (300 seconds)
client.getPodLogs(
    name = "my-pod",
    namespace = "default",
    sinceSeconds = 300,
    timestamps = true
).collect { line ->
    println(line)
}
```

#### Advanced Log Monitoring

```kotlin
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

// Filter and process logs
client.getPodLogs(
    name = "my-pod",
    namespace = "default",
    follow = true
)
    .filter { line -> line.contains("ERROR") || line.contains("WARN") }
    .map { line -> "⚠️ $line" }
    .collect { line ->
        println(line)
        // Send alert, log to file, etc.
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

#### Watch Operations
- `fun watchPods(namespace: String = defaultNamespace, labelSelector: String? = null, resourceVersion: String? = null): Flow<WatchEvent<Pod>>`
- `fun watchAllPods(labelSelector: String? = null, resourceVersion: String? = null): Flow<WatchEvent<Pod>>`
- `fun watchServices(namespace: String = defaultNamespace, labelSelector: String? = null, resourceVersion: String? = null): Flow<WatchEvent<Service>>`
- `fun watchDeployments(namespace: String = defaultNamespace, labelSelector: String? = null, resourceVersion: String? = null): Flow<WatchEvent<Deployment>>`

#### Log Operations
- `fun getPodLogs(name: String, namespace: String = defaultNamespace, container: String? = null, follow: Boolean = false, previous: Boolean = false, sinceSeconds: Int? = null, tailLines: Int? = null, timestamps: Boolean = false): Flow<String>`

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
