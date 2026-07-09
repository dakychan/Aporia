package aporia.cc.network
import com.chaos.annotation.Obfuscate
import so.aporia.utils.user.logger.Logger
import java.util.concurrent.ConcurrentHashMap
import java.util.function.BiConsumer
import com.chaos.annotation.ChaosNative
@Obfuscate
@ChaosNative
object NetworkServerManager {

    private val servers = ConcurrentHashMap<Int, NetworkServer>()

    @JvmStatic
    fun createServer(port: Int): NetworkServer? {
        if (servers.containsKey(port)) {
            Logger.warn("Server already exists on port $port")
            return servers[port]
        }

        val server = NetworkServer(port)
        return if (server.init()) {
            servers[port] = server
            Logger.success("Server created on port $port")
            server
        } else {
            Logger.error("Failed to create server on port $port")
            null
        }
    }

    @JvmStatic
    fun createServer(): NetworkServer? = createServer(61033)

    @JvmStatic
    fun getServer(port: Int): NetworkServer? = servers[port]

    @JvmStatic
    fun stopServer(port: Int): Boolean {
        val server = servers.remove(port)
        return if (server != null) {
            server.shutdown()
            Logger.success("Server stopped on port $port")
            true
        } else {
            Logger.warn("Server not found on port $port")
            false
        }
    }

    @JvmStatic
    fun stopAllServers() {
        Logger.info("Stopping all NetworkServers (${servers.size} servers)...")
        for (server in servers.values) {
            try {
                server.shutdown()
            } catch (e: Exception) {
                Logger.error("Error stopping server on port ${server.port}: ${e.message}")
            }
        }
        servers.clear()
        Logger.success("All NetworkServers stopped")
    }

    @JvmStatic
    fun getServerCount(): Int = servers.size

    @JvmStatic
    fun getAllPorts(): IntArray = servers.keys.toIntArray()

    @JvmStatic
    fun isServerRunning(port: Int): Boolean {
        val server = servers[port]
        return server?.isRunning() == true
    }

    @JvmStatic
    fun registerEndpoint(port: Int, path: String, handler: BiConsumer<io.netty.handler.codec.http.FullHttpRequest, io.netty.channel.ChannelHandlerContext>): Boolean {
        val server = servers[port] ?: run {
            Logger.error("Server not found on port $port")
            return false
        }
        server.registerEndpoint(path, handler)
        return true
    }

    @JvmStatic
    fun removeEndpoint(port: Int, path: String): Boolean {
        val server = servers[port] ?: run {
            Logger.error("Server not found on port $port")
            return false
        }
        server.removeEndpoint(path)
        return true
    }

    @JvmStatic
    fun getStats(): String {
        val stats = StringBuilder()
        stats.appendLine("NetworkServerManager Statistics:")
        stats.appendLine("Total servers: ${servers.size}")
        for (server in servers.values) {
            stats.appendLine(String.format(
                "  Port %d: running=%s, endpoints=%d",
                server.port, server.isRunning(), server.getEndpointCount()
            ))
        }
        return stats.toString()
    }

    @JvmStatic
    fun getNetworkStoragePath() = so.aporia.utils.files.FilesManager.ROOT.resolve("network")

    @JvmStatic
    fun createNetworkStorage(): Boolean {
        return try {
            val networkPath = getNetworkStoragePath()
            java.nio.file.Files.createDirectories(networkPath.resolve("html"))
            java.nio.file.Files.createDirectories(networkPath.resolve("css"))
            java.nio.file.Files.createDirectories(networkPath.resolve("cache"))
            Logger.success("Network storage created at: $networkPath")
            true
        } catch (e: java.io.IOException) {
            Logger.error("Failed to create network storage: ${e.message}")
            false
        }
    }

    @JvmStatic
    fun getPortCachePath(port: Int) = getNetworkStoragePath().resolve("cache").resolve("port_$port")

    @JvmStatic
    fun exampleUsage() {
        Logger.info("=== NetworkServerManager Example Usage ===")
        val server1 = createServer(8080)
        server1?.registerEndpoint("/api/v1/data") { _, ctx ->
            val response = """{"data":"from port 8080"}"""
            NetworkServer.sendResponseStatic(ctx, io.netty.handler.codec.http.HttpResponseStatus.OK, response, "application/json")
        }

        val server2 = createServer(8081)
        server2?.registerEndpoint("/api/v2/info") { _, ctx ->
            val response = """{"info":"from port 8081"}"""
            NetworkServer.sendResponseStatic(ctx, io.netty.handler.codec.http.HttpResponseStatus.OK, response, "application/json")
        }

        createServer()
        Logger.info("Created ${getServerCount()} servers")
        Logger.info("Stats:\n${getStats()}")
        Logger.info("=== Example Complete ===")
    }
}