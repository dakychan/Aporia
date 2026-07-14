package aporia.cc.network

import com.chaos.annotation.Obfuscate
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.*
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.netty.handler.timeout.IdleStateHandler
import io.netty.util.CharsetUtil
import so.aporia.utils.user.logger.Logger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.function.BiConsumer
import com.chaos.annotation.ChaosNative
@Obfuscate
@ChaosNative
class NetworkServer(val port: Int) {

    private var serverChannel: Channel? = null
    private var bossGroup: EventLoopGroup? = null
    private var workerGroup: EventLoopGroup? = null
    private val endpointHandlers = ConcurrentHashMap<String, BiConsumer<FullHttpRequest, ChannelHandlerContext>>()

    private inner class HttpServerHandler : SimpleChannelInboundHandler<FullHttpRequest>() {
        override fun channelRead0(ctx: ChannelHandlerContext, request: FullHttpRequest) {
            val path = request.uri()
            val handler = findHandler(path)

            if (handler != null) {
                try {
                    handler.accept(request, ctx)
                } catch (e: Exception) {
                    Logger.error("Handler error for $path on port $port: ${e.message}")
                    sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR)
                }
            } else {
                Logger.debug("No handler for path: $path on port $port")
                sendError(ctx, HttpResponseStatus.NOT_FOUND)
            }
        }

        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            Logger.error("HTTP handler error on port $port: ${cause.message}")
            ctx.close()
        }

        private fun findHandler(path: String): BiConsumer<FullHttpRequest, ChannelHandlerContext>? {
            endpointHandlers[path]?.let { return it }

            for ((prefix, handler) in endpointHandlers) {
                if (path.startsWith(prefix)) {
                    if (prefix.endsWith("/") ||
                        path.length == prefix.length ||
                        path[prefix.length] == '/') {
                        return handler
                    }
                }
            }

            return null
        }

        private fun sendError(ctx: ChannelHandlerContext, status: HttpResponseStatus) {
            val response = DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                status,
                Unpooled.copiedBuffer("Error: $status\r\n", CharsetUtil.UTF_8)
            )
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8")
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes())
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
        }
    }

    fun init(): Boolean {
        if (serverChannel?.isActive == true) {
            Logger.warn("Server already running on port $port")
            return false
        }

        try {
            @Suppress("DEPRECATION")
            bossGroup = NioEventLoopGroup(1)
            @Suppress("DEPRECATION")
            workerGroup = NioEventLoopGroup()

            val bootstrap = ServerBootstrap()
            bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .handler(LoggingHandler(LogLevel.DEBUG))
                .childHandler(object : ChannelInitializer<Channel>() {
                    override fun initChannel(ch: Channel) {
                        val pipeline = ch.pipeline()
                        pipeline.addLast(IdleStateHandler(300, 300, 300, TimeUnit.SECONDS))
                        pipeline.addLast(HttpServerCodec())
                        pipeline.addLast(HttpObjectAggregator(65536))
                        pipeline.addLast(HttpServerHandler())
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)

            val future = bootstrap.bind(port)
            if (!future.await(5, TimeUnit.SECONDS)) {
                throw Exception("Bind timeout after 5 seconds on port $port")
            }

            if (!future.isSuccess) {
                throw if (future.cause() != null) {
                    Exception("Bind failed on port $port: ${future.cause()?.message}", future.cause())
                } else {
                    Exception("Bind failed on port $port")
                }
            }

            serverChannel = future.channel()
            registerDefaultEndpoints()

            Logger.success("NetworkServer started on port $port")

            Runtime.getRuntime().addShutdownHook(Thread { shutdown() })

            return true
        } catch (e: Exception) {
            Logger.error("Failed to start NetworkServer on port $port: ${e.message}")
            shutdown()
            return false
        }
    }

    fun shutdown() {
        try {
            serverChannel?.close()?.await(5, TimeUnit.SECONDS)
        } catch (_: Exception) {}
        serverChannel = null

        try {
            bossGroup?.shutdownGracefully()?.await(5, TimeUnit.SECONDS)
        } catch (_: Exception) {}
        bossGroup = null

        try {
            workerGroup?.shutdownGracefully()?.await(5, TimeUnit.SECONDS)
        } catch (_: Exception) {}
        workerGroup = null

        endpointHandlers.clear()

        Logger.success("NetworkServer stopped on port $port")
    }

    fun registerEndpoint(path: String, handler: BiConsumer<FullHttpRequest, ChannelHandlerContext>) {
        if (serverChannel == null || !serverChannel!!.isActive) {
            Logger.error("Server on port $port not initialized. Call init() first.")
            return
        }

        endpointHandlers[path] = handler
        Logger.info("Registered endpoint: $path on port $port")
    }

    fun removeEndpoint(path: String) {
        if (endpointHandlers.remove(path) != null) {
            Logger.info("Removed endpoint: $path from port $port")
        }
    }

    fun isRunning(): Boolean = serverChannel?.isActive == true

    fun getEndpointCount(): Int = endpointHandlers.size

    private fun registerDefaultEndpoints() {
        registerEndpoint("/") { _, ctx ->
            val response = "<html><body><h1>Aporia.cc NetworkServer</h1><p>Server is running on port $port</p></body></html>"
            sendResponse(ctx, HttpResponseStatus.OK, response, "text/html")
        }

        registerEndpoint("/health") { _, ctx ->
            val response = """{"status":"ok","port":$port,"endpoints":${endpointHandlers.size}}"""
            sendResponse(ctx, HttpResponseStatus.OK, response, "application/json")
        }

        registerEndpoint("/api/info") { _, ctx ->
            val response = """{"name":"Aporia.cc NetworkServer","port":$port,"endpoints":${endpointHandlers.size}}"""
            sendResponse(ctx, HttpResponseStatus.OK, response, "application/json")
        }
    }

    private fun sendResponse(ctx: ChannelHandlerContext, status: HttpResponseStatus, content: String, contentType: String) {
        val response = DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            status,
            Unpooled.copiedBuffer(content, CharsetUtil.UTF_8)
        )
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "$contentType; charset=UTF-8")
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes())
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)

        ctx.writeAndFlush(response)
    }

    companion object {
        @JvmStatic
        fun sendResponseStatic(ctx: ChannelHandlerContext, status: HttpResponseStatus, content: String, contentType: String) {
            val response = DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                status,
                Unpooled.copiedBuffer(content, CharsetUtil.UTF_8)
            )
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "$contentType; charset=UTF-8")
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes())
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
            ctx.writeAndFlush(response)
        }
    }
}
