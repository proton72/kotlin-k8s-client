# Kotlin Kubernetes Client

A lightweight, type-safe Kubernetes client library for Kotlin using Ktor HTTP client.

## Features

- Type-safe Kubernetes resource models with full coroutine support
- Watch resources and stream logs using Kotlin Flow
- In-cluster and manual configuration support
- Pod, Service, and Deployment operations
- Label management and real-time monitoring
- SSL/TLS support with custom CA

## Installation

Available on Maven Central:

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("io.github.proton72:kotlin-k8s-client:1.3.1")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'io.github.proton72:kotlin-k8s-client:1.3.1'
}
```

### Maven

```xml
<dependency>
    <groupId>io.github.proton72</groupId>
    <artifactId>kotlin-k8s-client</artifactId>
    <version>1.3.1</version>
</dependency>
```

## Quick Start

### In-Cluster

```kotlin
val client = KubernetesClient()
val pods = client.listPods()
```

### Manual Configuration

```kotlin
val config = KubernetesClientConfig(
    apiServer = "https://k8s.example.com:6443",
    token = "your-token",
    namespace = "default"
)
val client = KubernetesClient(config)
```

## Usage Examples

### Pods

```kotlin
// List and filter
val pods = client.listPods("default")
val filtered = client.listPods("default", labelSelector = "app=web")

// Get, create, delete
val pod = client.getPod("my-pod", "default")
client.createPod(pod, "default")
client.deletePod("my-pod", "default")

// Update labels
client.patchPodMetadata(name = "my-pod", labels = mapOf("env" to "prod"))
client.addPodLabels(name = "my-pod", labels = mapOf("version" to "1.0"))
client.removePodLabels(name = "my-pod", labelKeys = listOf("old-label"))
```

### Services

```kotlin
// List and filter
val services = client.listServices("default")
val filtered = client.listServices("default", labelSelector = "app=web")

// CRUD operations
val svc = client.getService("my-service", "default")
client.createService(service, "default")
client.updateService(updatedService, "default")
client.deleteService("my-service", "default")
```

### Deployments

```kotlin
// List and filter
val deployments = client.listDeployments("default")
val filtered = client.listDeployments("default", labelSelector = "app=web")

// CRUD and scale operations
val deploy = client.getDeployment("my-deployment", "default")
client.createDeployment(deployment, "default")
client.updateDeployment(updatedDeployment, "default")
client.scaleDeployment("my-deployment", replicas = 5)
client.deleteDeployment("my-deployment", "default")
```

### Watch Resources

```kotlin
// Watch pods for real-time changes
client.watchPods(namespace = "default", labelSelector = "app=web")
    .collect { event ->
        println("${event.type}: ${event.`object`.metadata.name}")
    }

// Watch services and deployments
client.watchServices("default").collect { event -> /* ... */ }
client.watchDeployments("default").collect { event -> /* ... */ }
```

### Pod Logs

```kotlin
// Get recent logs
client.getPodLogs(name = "my-pod", tailLines = 100, timestamps = true)
    .collect { println(it) }

// Follow logs in real-time
client.getPodLogs(name = "my-pod", follow = true)
    .collect { println(it) }

// Specific container or previous container
client.getPodLogs(name = "my-pod", container = "app")
client.getPodLogs(name = "my-pod", previous = true)
```

## Error Handling

```kotlin
try {
    client.getPod("pod-name", "default")
} catch (e: KubernetesNotFoundException) {
    // Resource not found
} catch (e: KubernetesApiException) {
    // API error with status code
}
```

## Requirements

- Kotlin 2.2.20+
- Java 17+

## License

Apache License 2.0 - See [LICENSE](https://github.com/proton72/kotlin-k8s-client/blob/main/LICENSE) for details.

## Contributing

Contributions welcome! Visit the [GitHub repository](https://github.com/proton72/kotlin-k8s-client) to report issues or submit pull requests.
