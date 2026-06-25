package aporia.cc.network

import com.chaos.annotation.Obfuscate
import io.netty.handler.codec.http.HttpResponseStatus
import so.aporia.utils.user.logger.Logger
import java.nio.file.Files

@Obfuscate
object NetworkInitializer {

    @JvmStatic
    fun init(): Boolean = init(61033)

    @JvmStatic
    fun init(port: Int): Boolean {
        Logger.info("Initializing Aporia.cc network subsystem...")
        var success = true

        Logger.debug("Initializing network infrastructure...")
        if (!EthernetUtils.initializeNetworkInfrastructure()) {
            Logger.error("Failed to initialize network infrastructure")
            success = false
        }

        Logger.debug("Starting HTTP server on port $port...")
        if (!EthernetUtils.init(port)) {
            Logger.error("Failed to start HTTP server")
            success = false
        } else {
            registerNetworkEndpoints(port)
        }

        if (success) {
            Logger.success("Aporia.cc network subsystem initialized successfully")
            Logger.info("Server running on http://localhost:$port")
            Logger.info("Dashboard: http://localhost:$port/dashboard.html")
        } else {
            Logger.error("Aporia.cc network subsystem initialization failed")
        }

        return success
    }

    @JvmStatic
    fun initMultiple(ports: IntArray): Boolean {
        Logger.info("Initializing Aporia.cc network subsystem with ${ports.size} ports...")
        var success = true

        Logger.debug("Initializing network infrastructure...")
        if (!EthernetUtils.initializeNetworkInfrastructure()) {
            Logger.error("Failed to initialize network infrastructure")
            success = false
        }

        for (port in ports) {
            Logger.debug("Starting HTTP server on port $port...")
            if (!EthernetUtils.init(port)) {
                Logger.error("Failed to start HTTP server on port $port")
                success = false
            } else {
                registerNetworkEndpoints(port)
            }
        }

        if (success) {
            Logger.success("Aporia.cc network subsystem initialized with ${ports.size} servers")
            for (port in ports) {
                Logger.info("Server running on http://localhost:$port")
            }
        } else {
            Logger.error("Aporia.cc network subsystem initialization failed")
        }

        return success
    }

    @JvmStatic
    fun shutdown() {
        Logger.info("Shutting down Aporia.cc network subsystem...")
        EthernetUtils.shutdown()
        ThreadManager.shutdown()
        Logger.success("Aporia.cc network subsystem shutdown complete")
    }

    @JvmStatic
    fun shutdownAll() {
        Logger.info("Shutting down ALL Aporia.cc network servers...")
        EthernetUtils.shutdownAll()
        ThreadManager.shutdown()
        Logger.success("All Aporia.cc network servers shutdown complete")
    }

    @JvmStatic
    fun shutdown(port: Int): Boolean = EthernetUtils.shutdown(port)

    @JvmStatic
    fun isRunning(): Boolean = EthernetUtils.isRunning()

    @JvmStatic
    fun isRunning(port: Int): Boolean = EthernetUtils.isRunning(port)

    @JvmStatic
    fun getPort(): Int = EthernetUtils.getPort()

    @JvmStatic
    fun getServerCount(): Int = EthernetUtils.getServerCount()

    @JvmStatic
    fun getStats(): String = EthernetUtils.getStats()

    private fun registerNetworkEndpoints(port: Int) {
        EthernetUtils.registerEndpoint(port, "/files/html/") { request, ctx ->
            val path = request.uri()
            val filename = path.replace("/files/html/", "")
            val content = HTMLLogic.readHTML(filename)
            if (content != null) {
                EthernetUtils.sendResponse(ctx, HttpResponseStatus.OK, content, "text/html")
            } else {
                EthernetUtils.sendResponse(ctx, HttpResponseStatus.NOT_FOUND,
                    "HTML file not found: $filename", "text/plain")
            }
        }

        EthernetUtils.registerEndpoint(port, "/files/css/") { request, ctx ->
            val path = request.uri()
            val filename = path.replace("/files/css/", "")
            val content = HTMLLogic.readCSS(filename)
            if (content != null) {
                EthernetUtils.sendResponse(ctx, HttpResponseStatus.OK, content, "text/css")
            } else {
                EthernetUtils.sendResponse(ctx, HttpResponseStatus.NOT_FOUND,
                    "CSS file not found: $filename", "text/plain")
            }
        }

        EthernetUtils.registerEndpoint(port, "/api/files/html/list") { _, ctx ->
            try {
                val response = StringBuilder("{\"files\":[")
                val htmlPath = HTMLLogic.getHTMLStoragePath()
                if (Files.exists(htmlPath)) {
                    Files.list(htmlPath).use { stream ->
                        val files = stream.filter { it.toString().lowercase().endsWith(".html") }
                            .map { "\"${it.fileName}\"" }
                            .toList()
                        response.append(files.joinToString(","))
                    }
                }
                response.append("]}")
                EthernetUtils.sendResponse(ctx, HttpResponseStatus.OK, response.toString(), "application/json")
            } catch (e: Exception) {
                EthernetUtils.sendResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                    """{"error":"${e.message?.replace("\"", "\\\"")}"}""", "application/json")
            }
        }

        EthernetUtils.registerEndpoint(port, "/api/health/detailed") { _, ctx ->
            val response = String.format(
                """{"status":"ok","server_running":%s,"port":%d,"storage_exists":%s,"cache_cleared":%s}""",
                EthernetUtils.isRunning(port), port, HTMLLogic.checkFilesExist(), true
            )
            EthernetUtils.sendResponse(ctx, HttpResponseStatus.OK, response, "application/json")
        }

        Logger.info("Network endpoints registered on port $port")
    }

    @JvmStatic
    fun exampleUsage() {
        Logger.info("=== Network API Example Usage (Single Server) ===")
        if (init()) {
            val response = Requests.get("http://localhost:${getPort()}/health")
            Logger.info("Health check: $response")
            HTMLLogic.saveHTML("test.html", "<html><body><h1>Test Page</h1><p>Generated by Aporia.cc</p></body></html>")
            val content = HTMLLogic.readHTML("test.html")
            Logger.info("Read back HTML: ${if (content != null) "${content.length} bytes" else "failed"}")
        }
        Logger.info("=== Example Complete ===")
    }

    @JvmStatic
    fun exampleUsageMultipleServers() {
        Logger.info("=== Network API Example Usage (Multiple Servers) ===")
        val ports = intArrayOf(61033, 61034, 61035)
        if (initMultiple(ports)) {
            for (port in ports) {
                val response = Requests.get("http://localhost:$port/health")
                Logger.info("Health check port $port: $response")
            }
            val server8080 = EthernetUtils.createServerOnPort(8080)
            server8080?.registerEndpoint("/custom/hello") { _, ctx ->
                val response = """{"message":"Hello from port 8080!"}"""
                NetworkServer.sendResponseStatic(ctx, HttpResponseStatus.OK, response, "application/json")
            }
            Logger.info("Custom endpoint registered on port 8080")
            Logger.info("Server statistics:\n${getStats()}")
        }
        Logger.info("=== Example Complete ===")
    }
}
