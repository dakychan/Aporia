package aporia.cc.network
import com.chaos.annotation.Obfuscate
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpResponseStatus
import so.aporia.utils.files.FilesManager
import so.aporia.utils.user.logger.Logger
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.function.BiConsumer
import com.chaos.annotation.ChaosNative
@Obfuscate
@ChaosNative
object EthernetUtils {

    private const val DEFAULT_PORT = 61033
    private val managedServers = ConcurrentHashMap<Int, NetworkServer>()
    private var defaultServer: NetworkServer? = null

    @JvmStatic
    fun init(port: Int): Boolean {
        if (managedServers.containsKey(port)) {
            Logger.warn("Server already running on port $port")
            return false
        }

        val server = NetworkServerManager.createServer(port)
        if (server != null) {
            managedServers[port] = server
            if (port == DEFAULT_PORT) {
                defaultServer = server
            }
            return true
        }
        return false
    }

    @JvmStatic
    fun init(): Boolean = init(DEFAULT_PORT)

    @JvmStatic
    fun registerEndpoint(path: String, handler: BiConsumer<FullHttpRequest, ChannelHandlerContext>) {
        val server = defaultServer ?: run {
            Logger.error("Default server not initialized. Call init() first.")
            return
        }
        server.registerEndpoint(path, handler)
    }

    @JvmStatic
    fun registerEndpoint(port: Int, path: String, handler: BiConsumer<FullHttpRequest, ChannelHandlerContext>): Boolean {
        val server = managedServers[port] ?: run {
            Logger.error("Server not found on port $port")
            return false
        }
        server.registerEndpoint(path, handler)
        return true
    }

    @JvmStatic
    fun removeEndpoint(path: String) {
        defaultServer?.removeEndpoint(path)
    }

    @JvmStatic
    fun removeEndpoint(port: Int, path: String): Boolean {
        val server = managedServers[port] ?: return false
        server.removeEndpoint(path)
        return true
    }

    @JvmStatic
    fun shutdown(port: Int): Boolean {
        val success = NetworkServerManager.stopServer(port)
        if (success) {
            managedServers.remove(port)
            if (port == DEFAULT_PORT) {
                defaultServer = null
            }
        }
        return success
    }

    @JvmStatic
    fun shutdown() {
        shutdown(DEFAULT_PORT)
    }

    @JvmStatic
    fun shutdownAll() {
        NetworkServerManager.stopAllServers()
        managedServers.clear()
        defaultServer = null
    }

    @JvmStatic
    fun getPort(): Int = defaultServer?.port ?: -1

    @JvmStatic
    fun getPort(server: NetworkServer?): Int = server?.port ?: -1

    @JvmStatic
    fun isRunning(): Boolean = defaultServer?.isRunning() == true

    @JvmStatic
    fun isRunning(port: Int): Boolean {
        val server = managedServers[port]
        return server?.isRunning() == true
    }

    @JvmStatic
    fun createServerOnPort(port: Int): NetworkServer? {
        val server = NetworkServerManager.createServer(port)
        if (server != null) {
            managedServers[port] = server
        }
        return server
    }

    @JvmStatic
    fun getServer(port: Int): NetworkServer? = managedServers[port]

    @JvmStatic
    fun getDefaultServer(): NetworkServer? = defaultServer

    @JvmStatic
    fun getServerCount(): Int = managedServers.size

    @JvmStatic
    fun getAllPorts(): IntArray = managedServers.keys.toIntArray()

    @JvmStatic
    fun sendResponse(ctx: ChannelHandlerContext, status: HttpResponseStatus, content: String, contentType: String) {
        NetworkServer.sendResponseStatic(ctx, status, content, contentType)
    }

    @JvmStatic
    fun getNetworkStoragePath(): Path = FilesManager.ROOT.resolve("network")

    @JvmStatic
    fun createNetworkStorage(): Boolean {
        return try {
            val networkPath = getNetworkStoragePath()
            Files.createDirectories(networkPath.resolve("html"))
            Files.createDirectories(networkPath.resolve("css"))
            Files.createDirectories(networkPath.resolve("cache"))
            Logger.success("Network storage created at: $networkPath")
            true
        } catch (e: IOException) {
            Logger.error("Failed to create network storage: ${e.message}")
            false
        }
    }

    @JvmStatic
    fun getStats(): String {
        val stats = StringBuilder()
        stats.appendLine("EthernetUtils Managed Servers:")
        stats.appendLine("Total servers: ${managedServers.size}")
        stats.appendLine("Default port: $DEFAULT_PORT")
        stats.appendLine("Default server running: ${defaultServer?.isRunning() == true}")

        for (server in managedServers.values) {
            stats.appendLine(String.format(
                "  Port %d: running=%s, endpoints=%d%s",
                server.port, server.isRunning(), server.getEndpointCount(),
                if (server == defaultServer) " (default)" else ""
            ))
        }

        return stats.toString()
    }

    @JvmStatic
    fun initializeNetworkInfrastructure(): Boolean {
        Logger.info("Initializing Aporia.cc network infrastructure...")

        var success = true

        if (!createNetworkStorage()) {
            Logger.error("Failed to create network storage")
            success = false
        }

        if (!HTMLLogic.initializeDefaultFiles()) {
            Logger.warn("Failed to initialize some default files")
        }

        Requests.clearCache()

        if (success) {
            Logger.success("Network infrastructure initialized")
        } else {
            Logger.error("Network infrastructure initialization failed")
        }

        return success
    }
}