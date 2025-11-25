package io.github.proton72.k8s.client

import io.github.proton72.k8s.exception.*
import io.github.proton72.k8s.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

private const val SERVICE_ACCOUNT_TOKEN_PATH = "/var/run/secrets/kubernetes.io/serviceaccount/token"
private const val SERVICE_ACCOUNT_NAMESPACE_PATH = "/var/run/secrets/kubernetes.io/serviceaccount/namespace"
private const val SERVICE_ACCOUNT_CA_CERT_PATH = "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt"

/**
 * Kubernetes client configuration.
 */
data class KubernetesClientConfig(
    val apiServer: String? = null,
    val token: String? = null,
    val namespace: String? = null,
    val caCertPath: String? = null,
    val logger: Logger? = null
)

/**
 * Ktor-based Kubernetes client for interacting with the Kubernetes API.
 *
 * Supports both in-cluster configuration (using service account) and
 * manual configuration through [KubernetesClientConfig].
 *
 * @param config Optional configuration. If not provided, uses in-cluster service account.
 */
class KubernetesClient(
    config: KubernetesClientConfig = KubernetesClientConfig()
) {
    private val logger: Logger = config.logger ?: LoggerFactory.getLogger(KubernetesClient::class.java)

    private val kubernetesHost = System.getenv("KUBERNETES_SERVICE_HOST") ?: "kubernetes.default.svc"
    private val kubernetesPort = System.getenv("KUBERNETES_SERVICE_PORT") ?: "443"
    private val apiServer = config.apiServer ?: "https://$kubernetesHost:$kubernetesPort"

    private val token: String by lazy {
        config.token ?: try {
            File(SERVICE_ACCOUNT_TOKEN_PATH).readText().trim()
        } catch (e: Exception) {
            logger.warn("Failed to read service account token from $SERVICE_ACCOUNT_TOKEN_PATH: ${e.message}")
            throw KubernetesAuthenticationException("Cannot read service account token", e)
        }
    }

    private val defaultNamespace: String by lazy {
        config.namespace ?: try {
            File(SERVICE_ACCOUNT_NAMESPACE_PATH).readText().trim()
        } catch (e: Exception) {
            logger.warn("Failed to read namespace from $SERVICE_ACCOUNT_NAMESPACE_PATH, using 'default': ${e.message}")
            "default"
        }
    }

    private val httpClient: HttpClient by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                    isLenient = true
                })
            }

            engine {
                https {
                    trustManager = createTrustManager(config.caCertPath)
                }
            }
        }
    }

    private fun createTrustManager(customCaCertPath: String? = null): X509TrustManager {
        try {
            val caCertPath = customCaCertPath ?: SERVICE_ACCOUNT_CA_CERT_PATH
            val caCertFile = File(caCertPath)
            if (!caCertFile.exists()) {
                logger.warn("CA certificate not found at $caCertPath, using default trust manager")
                return createDefaultTrustManager()
            }

            // Load the CA certificate
            val certificateFactory = CertificateFactory.getInstance("X.509")
            val certificate = FileInputStream(caCertFile).use { fis ->
                certificateFactory.generateCertificate(fis) as X509Certificate
            }

            // Create a KeyStore containing the CA certificate
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            keyStore.load(null, null)
            keyStore.setCertificateEntry("kubernetes-ca", certificate)

            // Create TrustManager that trusts the CA
            val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            trustManagerFactory.init(keyStore)

            val trustManagers = trustManagerFactory.trustManagers
            return trustManagers.first { it is X509TrustManager } as X509TrustManager
        } catch (e: Exception) {
            logger.error("Failed to create trust manager from CA certificate: ${e.message}", e)
            return createDefaultTrustManager()
        }
    }

    private fun createDefaultTrustManager(): X509TrustManager {
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(null as KeyStore?)
        val trustManagers = trustManagerFactory.trustManagers
        return trustManagers.first { it is X509TrustManager } as X509TrustManager
    }

    private suspend inline fun <reified T> executeRequest(
        method: HttpMethod,
        url: String,
        body: Any? = null
    ): T {
        try {
            val response = httpClient.request(url) {
                this.method = method
                headers {
                    append("Authorization", "Bearer $token")
                }
                contentType(ContentType.Application.Json)

                body?.let {
                    setBody(it)
                }
            }

            return when (response.status.value) {
                in 200..299 -> response.body()
                404 -> throw KubernetesNotFoundException("Resource", "unknown")
                401, 403 -> throw KubernetesAuthenticationException("Unauthorized: ${response.status}")
                else -> {
                    val errorBody = try {
                        response.bodyAsText()
                    } catch (e: Exception) {
                        "Unable to read error body"
                    }
                    throw KubernetesApiException(response.status.value, errorBody)
                }
            }
        } catch (e: KubernetesException) {
            throw e
        } catch (e: Exception) {
            throw KubernetesException("Request failed: ${e.message}", e)
        }
    }

    // ==================== Pod Operations ====================

    /**
     * Gets a pod by name in the specified namespace.
     *
     * @param name Name of the pod
     * @param namespace Kubernetes namespace (defaults to the service account namespace)
     * @return The Pod object
     * @throws KubernetesNotFoundException if the pod is not found
     */
    suspend fun getPod(name: String, namespace: String = defaultNamespace): Pod {
        logger.debug("Getting pod: $name in namespace: $namespace")
        val url = "$apiServer/api/v1/namespaces/$namespace/pods/$name"
        return executeRequest(HttpMethod.Get, url)
    }

    /**
     * Lists all pods in the specified namespace.
     *
     * @param namespace Kubernetes namespace (defaults to the service account namespace)
     * @param labelSelector Label selector to filter pods (e.g., "app=myapp")
     * @return PodList containing all matching pods
     */
    suspend fun listPods(
        namespace: String = defaultNamespace,
        labelSelector: String? = null
    ): PodList {
        logger.debug("Listing pods in namespace: $namespace with selector: $labelSelector")
        var url = "$apiServer/api/v1/namespaces/$namespace/pods"
        labelSelector?.let {
            url += "?labelSelector=${it.encodeURLParameter()}"
        }
        return executeRequest(HttpMethod.Get, url)
    }

    /**
     * Lists all pods across all namespaces.
     *
     * @param labelSelector Label selector to filter pods (e.g., "app=myapp")
     * @return PodList containing all matching pods
     */
    suspend fun listAllPods(labelSelector: String? = null): PodList {
        logger.debug("Listing all pods with selector: $labelSelector")
        var url = "$apiServer/api/v1/pods"
        labelSelector?.let {
            url += "?labelSelector=${it.encodeURLParameter()}"
        }
        return executeRequest(HttpMethod.Get, url)
    }

    /**
     * Creates a new pod in the specified namespace.
     *
     * @param pod Pod object to create
     * @param namespace Kubernetes namespace (defaults to the service account namespace)
     * @return The created Pod object
     */
    suspend fun createPod(pod: Pod, namespace: String = defaultNamespace): Pod {
        logger.info("Creating pod: ${pod.metadata.name} in namespace: $namespace")
        val url = "$apiServer/api/v1/namespaces/$namespace/pods"
        return executeRequest(HttpMethod.Post, url, pod)
    }

    /**
     * Deletes a pod by name in the specified namespace.
     *
     * @param name Name of the pod to delete
     * @param namespace Kubernetes namespace (defaults to the service account namespace)
     * @param gracePeriodSeconds Grace period for pod termination (default: 30)
     * @param propagationPolicy Propagation policy (e.g., "Foreground", "Background")
     * @return Status object indicating the result
     */
    suspend fun deletePod(
        name: String,
        namespace: String = defaultNamespace,
        gracePeriodSeconds: Int = 30,
        propagationPolicy: String? = null
    ): Status {
        logger.info("Deleting pod: $name in namespace: $namespace with grace period: ${gracePeriodSeconds}s")
        var url = "$apiServer/api/v1/namespaces/$namespace/pods/$name"
        url += "?gracePeriodSeconds=$gracePeriodSeconds"
        propagationPolicy?.let {
            url += "&propagationPolicy=${it.encodeURLParameter()}"
        }
        return executeRequest(HttpMethod.Delete, url)
    }

    /**
     * Updates an existing pod.
     *
     * @param pod Pod object with updated fields
     * @param namespace Kubernetes namespace (defaults to the service account namespace)
     * @return The updated Pod object
     */
    suspend fun updatePod(pod: Pod, namespace: String = defaultNamespace): Pod {
        val name = pod.metadata.name ?: throw KubernetesException("Pod name is required")
        logger.info("Updating pod: $name in namespace: $namespace")
        val url = "$apiServer/api/v1/namespaces/$namespace/pods/$name"
        return executeRequest(HttpMethod.Put, url, pod)
    }

    // ==================== Service Operations ====================

    /**
     * Gets a service by name in the specified namespace.
     *
     * @param name Name of the service
     * @param namespace Kubernetes namespace (defaults to the service account namespace)
     * @return The Service object
     * @throws KubernetesNotFoundException if the service is not found
     */
    suspend fun getService(name: String, namespace: String = defaultNamespace): Service {
        logger.debug("Getting service: $name in namespace: $namespace")
        val url = "$apiServer/api/v1/namespaces/$namespace/services/$name"
        return executeRequest(HttpMethod.Get, url)
    }

    /**
     * Lists all services in the specified namespace.
     *
     * @param namespace Kubernetes namespace (defaults to the service account namespace)
     * @param labelSelector Label selector to filter services
     * @return ServiceList containing all matching services
     */
    suspend fun listServices(
        namespace: String = defaultNamespace,
        labelSelector: String? = null
    ): ServiceList {
        logger.debug("Listing services in namespace: $namespace with selector: $labelSelector")
        var url = "$apiServer/api/v1/namespaces/$namespace/services"
        labelSelector?.let {
            url += "?labelSelector=${it.encodeURLParameter()}"
        }
        return executeRequest(HttpMethod.Get, url)
    }

    /**
     * Creates a new service in the specified namespace.
     *
     * @param service Service object to create
     * @param namespace Kubernetes namespace (defaults to the service account namespace)
     * @return The created Service object
     */
    suspend fun createService(service: Service, namespace: String = defaultNamespace): Service {
        logger.info("Creating service: ${service.metadata.name} in namespace: $namespace")
        val url = "$apiServer/api/v1/namespaces/$namespace/services"
        return executeRequest(HttpMethod.Post, url, service)
    }

    /**
     * Deletes a service by name in the specified namespace.
     *
     * @param name Name of the service to delete
     * @param namespace Kubernetes namespace (defaults to the service account namespace)
     * @return Status object indicating the result
     */
    suspend fun deleteService(name: String, namespace: String = defaultNamespace): Status {
        logger.info("Deleting service: $name in namespace: $namespace")
        val url = "$apiServer/api/v1/namespaces/$namespace/services/$name"
        return executeRequest(HttpMethod.Delete, url)
    }

    /**
     * Updates an existing service.
     *
     * @param service Service object with updated fields
     * @param namespace Kubernetes namespace (defaults to the service account namespace)
     * @return The updated Service object
     */
    suspend fun updateService(service: Service, namespace: String = defaultNamespace): Service {
        val name = service.metadata.name ?: throw KubernetesException("Service name is required")
        logger.info("Updating service: $name in namespace: $namespace")
        val url = "$apiServer/api/v1/namespaces/$namespace/services/$name"
        return executeRequest(HttpMethod.Put, url, service)
    }

    // ==================== Deployment Operations ====================

    /**
     * Gets a deployment by name in the specified namespace.
     *
     * @param name Name of the deployment
     * @param namespace Kubernetes namespace (defaults to the service account namespace)
     * @return The Deployment object
     * @throws KubernetesNotFoundException if the deployment is not found
     */
    suspend fun getDeployment(name: String, namespace: String = defaultNamespace): Deployment {
        logger.debug("Getting deployment: $name in namespace: $namespace")
        val url = "$apiServer/apis/apps/v1/namespaces/$namespace/deployments/$name"
        return executeRequest(HttpMethod.Get, url)
    }

    /**
     * Lists all deployments in the specified namespace.
     *
     * @param namespace Kubernetes namespace (defaults to the service account namespace)
     * @param labelSelector Label selector to filter deployments
     * @return DeploymentList containing all matching deployments
     */
    suspend fun listDeployments(
        namespace: String = defaultNamespace,
        labelSelector: String? = null
    ): DeploymentList {
        logger.debug("Listing deployments in namespace: $namespace with selector: $labelSelector")
        var url = "$apiServer/apis/apps/v1/namespaces/$namespace/deployments"
        labelSelector?.let {
            url += "?labelSelector=${it.encodeURLParameter()}"
        }
        return executeRequest(HttpMethod.Get, url)
    }

    /**
     * Creates a new deployment in the specified namespace.
     *
     * @param deployment Deployment object to create
     * @param namespace Kubernetes namespace (defaults to the service account namespace)
     * @return The created Deployment object
     */
    suspend fun createDeployment(deployment: Deployment, namespace: String = defaultNamespace): Deployment {
        logger.info("Creating deployment: ${deployment.metadata.name} in namespace: $namespace")
        val url = "$apiServer/apis/apps/v1/namespaces/$namespace/deployments"
        return executeRequest(HttpMethod.Post, url, deployment)
    }

    /**
     * Deletes a deployment by name in the specified namespace.
     *
     * @param name Name of the deployment to delete
     * @param namespace Kubernetes namespace (defaults to the service account namespace)
     * @param gracePeriodSeconds Grace period for deletion (default: 30)
     * @param propagationPolicy Propagation policy (e.g., "Foreground", "Background")
     * @return Status object indicating the result
     */
    suspend fun deleteDeployment(
        name: String,
        namespace: String = defaultNamespace,
        gracePeriodSeconds: Int = 30,
        propagationPolicy: String? = null
    ): Status {
        logger.info("Deleting deployment: $name in namespace: $namespace")
        var url = "$apiServer/apis/apps/v1/namespaces/$namespace/deployments/$name"
        url += "?gracePeriodSeconds=$gracePeriodSeconds"
        propagationPolicy?.let {
            url += "&propagationPolicy=${it.encodeURLParameter()}"
        }
        return executeRequest(HttpMethod.Delete, url)
    }

    /**
     * Updates an existing deployment.
     *
     * @param deployment Deployment object with updated fields
     * @param namespace Kubernetes namespace (defaults to the service account namespace)
     * @return The updated Deployment object
     */
    suspend fun updateDeployment(deployment: Deployment, namespace: String = defaultNamespace): Deployment {
        val name = deployment.metadata.name ?: throw KubernetesException("Deployment name is required")
        logger.info("Updating deployment: $name in namespace: $namespace")
        val url = "$apiServer/apis/apps/v1/namespaces/$namespace/deployments/$name"
        return executeRequest(HttpMethod.Put, url, deployment)
    }

    /**
     * Scales a deployment to the specified number of replicas.
     *
     * @param name Name of the deployment to scale
     * @param replicas Number of replicas
     * @param namespace Kubernetes namespace (defaults to the service account namespace)
     * @return The updated Deployment object
     */
    suspend fun scaleDeployment(
        name: String,
        replicas: Int,
        namespace: String = defaultNamespace
    ): Deployment {
        logger.info("Scaling deployment: $name to $replicas replicas in namespace: $namespace")
        val deployment = getDeployment(name, namespace)
        val updatedDeployment = deployment.copy(
            spec = deployment.spec?.copy(replicas = replicas)
        )
        return updateDeployment(updatedDeployment, namespace)
    }

    // ==================== ResourceQuota Operations ====================

    /**
     * Gets a resource quota by name in the specified namespace.
     *
     * @param name Name of the resource quota
     * @param namespace Kubernetes namespace (defaults to the service account namespace)
     * @return The ResourceQuota object
     * @throws KubernetesNotFoundException if the resource quota is not found
     */
    suspend fun getResourceQuota(name: String, namespace: String = defaultNamespace): ResourceQuota {
        logger.debug("Getting resource quota: $name in namespace: $namespace")
        val url = "$apiServer/api/v1/namespaces/$namespace/resourcequotas/$name"
        return executeRequest(HttpMethod.Get, url)
    }

    /**
     * Lists all resource quotas in the specified namespace.
     *
     * @param namespace Kubernetes namespace (defaults to the service account namespace)
     * @param labelSelector Label selector to filter resource quotas
     * @return ResourceQuotaList containing all matching resource quotas
     */
    suspend fun listResourceQuotas(
        namespace: String = defaultNamespace,
        labelSelector: String? = null
    ): ResourceQuotaList {
        logger.debug("Listing resource quotas in namespace: $namespace with selector: $labelSelector")
        var url = "$apiServer/api/v1/namespaces/$namespace/resourcequotas"
        labelSelector?.let {
            url += "?labelSelector=${it.encodeURLParameter()}"
        }
        return executeRequest(HttpMethod.Get, url)
    }

    /**
     * Creates a new resource quota in the specified namespace.
     *
     * @param resourceQuota ResourceQuota object to create
     * @param namespace Kubernetes namespace (defaults to the service account namespace)
     * @return The created ResourceQuota object
     */
    suspend fun createResourceQuota(resourceQuota: ResourceQuota, namespace: String = defaultNamespace): ResourceQuota {
        logger.info("Creating resource quota: ${resourceQuota.metadata.name} in namespace: $namespace")
        val url = "$apiServer/api/v1/namespaces/$namespace/resourcequotas"
        return executeRequest(HttpMethod.Post, url, resourceQuota)
    }

    /**
     * Deletes a resource quota by name in the specified namespace.
     *
     * @param name Name of the resource quota to delete
     * @param namespace Kubernetes namespace (defaults to the service account namespace)
     * @param gracePeriodSeconds Grace period for deletion (default: 30)
     * @param propagationPolicy Propagation policy (e.g., "Foreground", "Background")
     * @return Status object indicating the result
     */
    suspend fun deleteResourceQuota(
        name: String,
        namespace: String = defaultNamespace,
        gracePeriodSeconds: Int = 30,
        propagationPolicy: String? = null
    ): Status {
        logger.info("Deleting resource quota: $name in namespace: $namespace")
        var url = "$apiServer/api/v1/namespaces/$namespace/resourcequotas/$name"
        url += "?gracePeriodSeconds=$gracePeriodSeconds"
        propagationPolicy?.let {
            url += "&propagationPolicy=${it.encodeURLParameter()}"
        }
        return executeRequest(HttpMethod.Delete, url)
    }

    /**
     * Updates an existing resource quota.
     *
     * @param resourceQuota ResourceQuota object with updated fields
     * @param namespace Kubernetes namespace (defaults to the service account namespace)
     * @return The updated ResourceQuota object
     */
    suspend fun updateResourceQuota(resourceQuota: ResourceQuota, namespace: String = defaultNamespace): ResourceQuota {
        val name = resourceQuota.metadata.name ?: throw KubernetesException("ResourceQuota name is required")
        logger.info("Updating resource quota: $name in namespace: $namespace")
        val url = "$apiServer/api/v1/namespaces/$namespace/resourcequotas/$name"
        return executeRequest(HttpMethod.Put, url, resourceQuota)
    }

    // ==================== Watch Operations ====================

    /**
     * Watches pods in the specified namespace for changes.
     *
     * @param namespace Kubernetes namespace (defaults to the service account namespace)
     * @param labelSelector Label selector to filter pods
     * @param resourceVersion Resource version to start watching from
     * @return Flow of WatchEvent<Pod> representing changes to pods
     */
    fun watchPods(
        namespace: String = defaultNamespace,
        labelSelector: String? = null,
        resourceVersion: String? = null
    ): Flow<WatchEvent<Pod>> {
        logger.debug("Watching pods in namespace: $namespace with selector: $labelSelector")
        var url = "$apiServer/api/v1/namespaces/$namespace/pods?watch=true"
        labelSelector?.let {
            url += "&labelSelector=${it.encodeURLParameter()}"
        }
        resourceVersion?.let {
            url += "&resourceVersion=${it.encodeURLParameter()}"
        }
        return watchResource(url)
    }

    /**
     * Watches all pods across all namespaces for changes.
     *
     * @param labelSelector Label selector to filter pods
     * @param resourceVersion Resource version to start watching from
     * @return Flow of WatchEvent<Pod> representing changes to pods
     */
    fun watchAllPods(
        labelSelector: String? = null,
        resourceVersion: String? = null
    ): Flow<WatchEvent<Pod>> {
        logger.debug("Watching all pods with selector: $labelSelector")
        var url = "$apiServer/api/v1/pods?watch=true"
        labelSelector?.let {
            url += "&labelSelector=${it.encodeURLParameter()}"
        }
        resourceVersion?.let {
            url += "&resourceVersion=${it.encodeURLParameter()}"
        }
        return watchResource(url)
    }

    /**
     * Watches services in the specified namespace for changes.
     *
     * @param namespace Kubernetes namespace (defaults to the service account namespace)
     * @param labelSelector Label selector to filter services
     * @param resourceVersion Resource version to start watching from
     * @return Flow of WatchEvent<Service> representing changes to services
     */
    fun watchServices(
        namespace: String = defaultNamespace,
        labelSelector: String? = null,
        resourceVersion: String? = null
    ): Flow<WatchEvent<Service>> {
        logger.debug("Watching services in namespace: $namespace with selector: $labelSelector")
        var url = "$apiServer/api/v1/namespaces/$namespace/services?watch=true"
        labelSelector?.let {
            url += "&labelSelector=${it.encodeURLParameter()}"
        }
        resourceVersion?.let {
            url += "&resourceVersion=${it.encodeURLParameter()}"
        }
        return watchResource(url)
    }

    /**
     * Watches deployments in the specified namespace for changes.
     *
     * @param namespace Kubernetes namespace (defaults to the service account namespace)
     * @param labelSelector Label selector to filter deployments
     * @param resourceVersion Resource version to start watching from
     * @return Flow of WatchEvent<Deployment> representing changes to deployments
     */
    fun watchDeployments(
        namespace: String = defaultNamespace,
        labelSelector: String? = null,
        resourceVersion: String? = null
    ): Flow<WatchEvent<Deployment>> {
        logger.debug("Watching deployments in namespace: $namespace with selector: $labelSelector")
        var url = "$apiServer/apis/apps/v1/namespaces/$namespace/deployments?watch=true"
        labelSelector?.let {
            url += "&labelSelector=${it.encodeURLParameter()}"
        }
        resourceVersion?.let {
            url += "&resourceVersion=${it.encodeURLParameter()}"
        }
        return watchResource(url)
    }

    /**
     * Watches resource quotas in the specified namespace for changes.
     *
     * @param namespace Kubernetes namespace (defaults to the service account namespace)
     * @param labelSelector Label selector to filter resource quotas
     * @param resourceVersion Resource version to start watching from
     * @return Flow of WatchEvent<ResourceQuota> representing changes to resource quotas
     */
    fun watchResourceQuotas(
        namespace: String = defaultNamespace,
        labelSelector: String? = null,
        resourceVersion: String? = null
    ): Flow<WatchEvent<ResourceQuota>> {
        logger.debug("Watching resource quotas in namespace: $namespace with selector: $labelSelector")
        var url = "$apiServer/api/v1/namespaces/$namespace/resourcequotas?watch=true"
        labelSelector?.let {
            url += "&labelSelector=${it.encodeURLParameter()}"
        }
        resourceVersion?.let {
            url += "&resourceVersion=${it.encodeURLParameter()}"
        }
        return watchResource(url)
    }

    /**
     * Internal method to watch a resource and emit events as a Flow.
     */
    private inline fun <reified T> watchResource(url: String): Flow<WatchEvent<T>> = flow {
        try {
            val response = httpClient.prepareGet(url) {
                headers {
                    append("Authorization", "Bearer $token")
                }
            }.execute { httpResponse ->
                if (httpResponse.status.value !in 200..299) {
                    val errorBody = try {
                        httpResponse.bodyAsText()
                    } catch (e: Exception) {
                        "Unable to read error body"
                    }
                    throw KubernetesApiException(httpResponse.status.value, errorBody)
                }

                val channel: ByteReadChannel = httpResponse.bodyAsChannel()
                val json = Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }

                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: break
                    if (line.isNotBlank()) {
                        try {
                            val event = json.decodeFromString<WatchEvent<T>>(line)
                            emit(event)
                        } catch (e: Exception) {
                            logger.error("Failed to parse watch event: $line", e)
                        }
                    }
                }
            }
        } catch (e: KubernetesException) {
            throw e
        } catch (e: Exception) {
            throw KubernetesException("Watch failed: ${e.message}", e)
        }
    }

    // ==================== Log Operations ====================

    /**
     * Gets logs from a pod container.
     *
     * @param name Name of the pod
     * @param namespace Kubernetes namespace (defaults to the service account namespace)
     * @param container Name of the container (optional, defaults to first container)
     * @param follow Whether to follow (stream) the logs
     * @param previous Whether to get logs from previous terminated container
     * @param sinceSeconds Only return logs newer than relative duration in seconds
     * @param tailLines Number of lines from the end of the logs to show
     * @param timestamps Include timestamps in log lines
     * @return Flow of log lines as strings
     */
    fun getPodLogs(
        name: String,
        namespace: String = defaultNamespace,
        container: String? = null,
        follow: Boolean = false,
        previous: Boolean = false,
        sinceSeconds: Int? = null,
        tailLines: Int? = null,
        timestamps: Boolean = false
    ): Flow<String> = flow {
        logger.debug("Getting logs for pod: $name in namespace: $namespace, container: $container, follow: $follow")

        var url = "$apiServer/api/v1/namespaces/$namespace/pods/$name/log?"
        val params = mutableListOf<String>()

        container?.let { params.add("container=${it.encodeURLParameter()}") }
        if (follow) params.add("follow=true")
        if (previous) params.add("previous=true")
        sinceSeconds?.let { params.add("sinceSeconds=$it") }
        tailLines?.let { params.add("tailLines=$it") }
        if (timestamps) params.add("timestamps=true")

        url += params.joinToString("&")

        try {
            httpClient.prepareGet(url) {
                headers {
                    append("Authorization", "Bearer $token")
                }
            }.execute { httpResponse ->
                if (httpResponse.status.value !in 200..299) {
                    val errorBody = try {
                        httpResponse.bodyAsText()
                    } catch (e: Exception) {
                        "Unable to read error body"
                    }

                    when (httpResponse.status.value) {
                        404 -> throw KubernetesNotFoundException("Pod", name)
                        401, 403 -> throw KubernetesAuthenticationException("Unauthorized: ${httpResponse.status}")
                        else -> throw KubernetesApiException(httpResponse.status.value, errorBody)
                    }
                }

                val channel: ByteReadChannel = httpResponse.bodyAsChannel()

                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: break
                    emit(line)
                }
            }
        } catch (e: KubernetesException) {
            throw e
        } catch (e: Exception) {
            throw KubernetesException("Failed to get logs: ${e.message}", e)
        }
    }

    /**
     * Closes the HTTP client and releases resources.
     */
    fun close() {
        httpClient.close()
    }
}
