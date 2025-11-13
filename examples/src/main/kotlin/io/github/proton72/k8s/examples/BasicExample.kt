package io.github.proton72.k8s.examples

import io.github.proton72.k8s.client.KubernetesClient
import io.github.proton72.k8s.exception.KubernetesException
import io.github.proton72.k8s.model.*
import kotlinx.coroutines.runBlocking

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

        // Clean up (uncomment if you want to delete resources after testing)
        /*
        println("\n=== Cleaning Up ===")
        client.deleteDeployment("nginx-example-deployment")
        println("Deleted deployment")

        client.deleteService("nginx-example-service")
        println("Deleted service")

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
