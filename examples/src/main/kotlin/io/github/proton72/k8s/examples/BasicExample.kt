package io.github.proton72.k8s.examples

import io.github.proton72.k8s.client.KubernetesClient
import io.github.proton72.k8s.exception.KubernetesException
import io.github.proton72.k8s.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.take

/**
 * Basic example demonstrating common Kubernetes operations.
 */
fun main() = runBlocking {
    val client = KubernetesClient()

    try {
        // List all pods
        println("=== Listing Pods ===")
        val pods = client.listPods()
        pods.items.forEach { pod ->
            println("Pod: ${pod.metadata.name}, Status: ${pod.status?.phase}, IP: ${pod.status?.podIP}")
        }

        // Create a simple nginx pod
        println("\n=== Creating Pod ===")
        val newPod = Pod(
            metadata = ObjectMeta(
                name = "nginx-example",
                labels = mapOf("app" to "nginx", "example" to "true")
            ),
            spec = PodSpec(
                containers = listOf(
                    Container(
                        name = "nginx",
                        image = "nginx:1.21",
                        ports = listOf(
                            ContainerPort(containerPort = 80)
                        ),
                        resources = ResourceRequirements(
                            requests = mapOf(
                                "memory" to "64Mi",
                                "cpu" to "100m"
                            ),
                            limits = mapOf(
                                "memory" to "128Mi",
                                "cpu" to "200m"
                            )
                        )
                    )
                ),
                restartPolicy = "Always"
            )
        )

        val createdPod = client.createPod(newPod)
        println("Created pod: ${createdPod.metadata.name}")

        // Get pod details
        println("\n=== Getting Pod Details ===")
        val pod = client.getPod("nginx-example")
        println("Pod: ${pod.metadata.name}")
        println("Namespace: ${pod.metadata.namespace}")
        println("UID: ${pod.metadata.uid}")
        println("Phase: ${pod.status?.phase}")

        // List services
        println("\n=== Listing Services ===")
        val services = client.listServices()
        services.items.forEach { service ->
            println("Service: ${service.metadata.name}, Type: ${service.spec?.type}, ClusterIP: ${service.spec?.clusterIP}")
        }

        // Create a service for the nginx pod
        println("\n=== Creating Service ===")
        val newService = Service(
            metadata = ObjectMeta(
                name = "nginx-example-service"
            ),
            spec = ServiceSpec(
                selector = mapOf("app" to "nginx", "example" to "true"),
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

        val createdService = client.createService(newService)
        println("Created service: ${createdService.metadata.name}, ClusterIP: ${createdService.spec?.clusterIP}")

        // List deployments
        println("\n=== Listing Deployments ===")
        val deployments = client.listDeployments()
        deployments.items.forEach { deployment ->
            println("Deployment: ${deployment.metadata.name}, Replicas: ${deployment.spec?.replicas}, Ready: ${deployment.status?.readyReplicas}")
        }

        // Create a deployment
        println("\n=== Creating Deployment ===")
        val newDeployment = Deployment(
            metadata = ObjectMeta(
                name = "nginx-example-deployment"
            ),
            spec = DeploymentSpec(
                replicas = 2,
                selector = LabelSelector(
                    matchLabels = mapOf("app" to "nginx-deployment", "example" to "true")
                ),
                template = PodTemplateSpec(
                    metadata = ObjectMeta(
                        labels = mapOf("app" to "nginx-deployment", "example" to "true")
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
                ),
                strategy = DeploymentStrategy(
                    type = "RollingUpdate",
                    rollingUpdate = RollingUpdateDeployment(
                        maxUnavailable = "1",
                        maxSurge = "1"
                    )
                )
            )
        )

        val createdDeployment = client.createDeployment(newDeployment)
        println("Created deployment: ${createdDeployment.metadata.name}")

        // Scale the deployment
        println("\n=== Scaling Deployment ===")
        val scaledDeployment = client.scaleDeployment("nginx-example-deployment", replicas = 3)
        println("Scaled deployment to ${scaledDeployment.spec?.replicas} replicas")

        // List resource quotas
        println("\n=== Listing ResourceQuotas ===")
        val resourceQuotas = client.listResourceQuotas()
        resourceQuotas.items.forEach { quota ->
            println("ResourceQuota: ${quota.metadata.name}")
            quota.spec?.hard?.forEach { (key, value) ->
                val used = quota.status?.used?.get(key) ?: "0"
                println("  $key: $used / $value")
            }
        }

        // Create a resource quota
        println("\n=== Creating ResourceQuota ===")
        val newResourceQuota = ResourceQuota(
            metadata = ObjectMeta(
                name = "example-quota",
                labels = mapOf("example" to "true")
            ),
            spec = ResourceQuotaSpec(
                hard = mapOf(
                    "requests.cpu" to "10",
                    "requests.memory" to "20Gi",
                    "limits.cpu" to "20",
                    "limits.memory" to "40Gi",
                    "pods" to "10",
                    "persistentvolumeclaims" to "5"
                ),
                scopes = listOf("NotTerminating")
            )
        )

        val createdQuota = client.createResourceQuota(newResourceQuota)
        println("Created resource quota: ${createdQuota.metadata.name}")
        createdQuota.spec?.hard?.forEach { (key, value) ->
            println("  $key: $value")
        }

        // Get resource quota details
        println("\n=== Getting ResourceQuota Details ===")
        val quota = client.getResourceQuota("example-quota")
        println("ResourceQuota: ${quota.metadata.name}")
        println("Namespace: ${quota.metadata.namespace}")
        println("Hard limits:")
        quota.spec?.hard?.forEach { (key, value) ->
            println("  $key: $value")
        }
        if (quota.status?.used?.isNotEmpty() == true) {
            println("Used resources:")
            quota.status?.used?.forEach { (key, value) ->
                println("  $key: $value")
            }
        }

        // Watch pods for changes (demonstrate for 10 seconds)
        println("\n=== Watching Pods ===")
        val watchJob = launch {
            client.watchPods(labelSelector = "example=true")
                .catch { e ->
                    println("Watch error: ${e.message}")
                }
                .collect { event ->
                    println("Watch Event: ${event.type} - Pod: ${event.`object`.metadata.name}, Phase: ${event.`object`.status?.phase}")
                }
        }

        // Let the watch run for a few seconds
        delay(10000)
        watchJob.cancel()
        println("Stopped watching pods")

        // Get pod logs (get last 10 lines)
        println("\n=== Getting Pod Logs ===")
        try {
            launch {
                client.getPodLogs(
                    name = "nginx-example",
                    tailLines = 10,
                    timestamps = true
                )
                    .catch { e ->
                        println("Error getting logs: ${e.message}")
                    }
                    .take(10) // Take only first 10 lines
                    .collect { line ->
                        println("Log: $line")
                    }
            }.join()
        } catch (e: Exception) {
            println("Note: Pod logs may not be available yet if the pod is still starting")
        }

        // Example: Follow pod logs in real-time (commented out to avoid long-running example)
        /*
        println("\n=== Following Pod Logs ===")
        val logJob = launch {
            client.getPodLogs(
                name = "nginx-example",
                follow = true,
                timestamps = true
            )
                .catch { e ->
                    println("Error following logs: ${e.message}")
                }
                .collect { line ->
                    println("Log: $line")
                }
        }

        // Follow logs for 30 seconds
        delay(30000)
        logJob.cancel()
        println("Stopped following logs")
        */

        // Clean up (uncomment if you want to delete resources after testing)
        /*
        println("\n=== Cleaning Up ===")
        client.deleteDeployment("nginx-example-deployment")
        println("Deleted deployment")

        client.deleteService("nginx-example-service")
        println("Deleted service")

        client.deleteResourceQuota("example-quota")
        println("Deleted resource quota")

        client.deletePod("nginx-example", gracePeriodSeconds = 0)
        println("Deleted pod")
        */

    } catch (e: KubernetesException) {
        println("Error: ${e.message}")
        e.printStackTrace()
    } finally {
        client.close()
    }
}
