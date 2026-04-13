/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package aporia.cc.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import so.aporia.utils.user.logger.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * NetworkServer — отдельный HTTP сервер на указанном порту.
 * <p>
 * NetworkServer — separate HTTP server on specified port.
 * <p>
 * Каждый инстанс управляет своим портом, своими эндпоинтами и соединениями.
 * <p>
 * Each instance manages its own port, endpoints and connections.
 */
public class NetworkServer {
    
    private final int port;
    private Channel serverChannel;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private final Map<String, BiConsumer<FullHttpRequest, ChannelHandlerContext>> endpointHandlers = new ConcurrentHashMap<>();
    
    /**
     * Создание сервера на указанном порту.
     * <p>
     * Create server on specified port.
     * 
     * @param port порт для прослушивания
     */
    public NetworkServer(int port) {
        this.port = port;
    }
    
    /**
     * HTTP хэндлер для обработки запросов с роутингом.
     */
    private class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
            String path = request.uri();
            
            // Find matching handler
            BiConsumer<FullHttpRequest, ChannelHandlerContext> handler = findHandler(path);
            
            if (handler != null) {
                try {
                    handler.accept(request, ctx);
                } catch (Exception e) {
                    Logger.error("Handler error for " + path + " on port " + port + ": " + e.getMessage());
                    sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
                }
            } else {
                // No handler found
                Logger.debug("No handler for path: " + path + " on port " + port);
                sendError(ctx, HttpResponseStatus.NOT_FOUND);
            }
        }
        
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            Logger.error("HTTP handler error on port " + port + ": " + cause.getMessage());
            ctx.close();
        }
        
        private BiConsumer<FullHttpRequest, ChannelHandlerContext> findHandler(String path) {
            // Exact match first
            if (endpointHandlers.containsKey(path)) {
                return endpointHandlers.get(path);
            }
            
            // Check for prefix matches (for paths like /files/html/)
            // Need to ensure the prefix is a complete path segment
            for (Map.Entry<String, BiConsumer<FullHttpRequest, ChannelHandlerContext>> entry : endpointHandlers.entrySet()) {
                String prefix = entry.getKey();
                if (path.startsWith(prefix)) {
                    // Check if prefix ends with '/' or if next character after prefix is '/' or end of string
                    if (prefix.endsWith("/") || 
                        path.length() == prefix.length() || 
                        path.charAt(prefix.length()) == '/') {
                        return entry.getValue();
                    }
                }
            }
            
            return null;
        }
        
        private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
            FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                status,
                Unpooled.copiedBuffer("Error: " + status + "\r\n", CharsetUtil.UTF_8)
            );
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }
    
    /**
     * Инициализация HTTP сервера.
     * <p>
     * Initialize HTTP server.
     * 
     * @return true если успешно, false при ошибке
     */
    public boolean init() {
        if (serverChannel != null && serverChannel.isActive()) {
            Logger.warn("Server already running on port " + port);
            return false;
        }
        
        try {
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();
            
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        // Add idle state handler to close connections after 5 minutes of inactivity
                        pipeline.addLast(new IdleStateHandler(300, 300, 300, TimeUnit.SECONDS));
                        pipeline.addLast(new HttpServerCodec());
                        pipeline.addLast(new HttpObjectAggregator(65536));
                        pipeline.addLast(new HttpServerHandler());
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true);
            
            ChannelFuture future = bootstrap.bind(port);
            // Wait for bind with timeout (5 seconds)
            if (!future.await(5, TimeUnit.SECONDS)) {
                throw new Exception("Bind timeout after 5 seconds on port " + port);
            }
            
            if (!future.isSuccess()) {
                if (future.cause() != null) {
                    throw new Exception("Bind failed on port " + port + ": " + future.cause().getMessage(), future.cause());
                } else {
                    throw new Exception("Bind failed on port " + port);
                }
            }
            
            serverChannel = future.channel();
            
            // Register default endpoints for this port
            registerDefaultEndpoints();
            
            Logger.success("NetworkServer started on port " + port);
            
            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
            
            return true;
        } catch (Exception e) {
            Logger.error("Failed to start NetworkServer on port " + port + ": " + e.getMessage());
            shutdown();
            return false;
        }
    }
    
    /**
     * Остановка HTTP сервера.
     * <p>
     * Stop HTTP server.
     */
    public void shutdown() {
        if (serverChannel != null) {
            serverChannel.close().syncUninterruptibly();
            serverChannel = null;
        }
        
        if (bossGroup != null) {
            bossGroup.shutdownGracefully().syncUninterruptibly();
            bossGroup = null;
        }
        
        if (workerGroup != null) {
            workerGroup.shutdownGracefully().syncUninterruptibly();
            workerGroup = null;
        }
        
        endpointHandlers.clear();
        
        Logger.success("NetworkServer stopped on port " + port);
    }
    
    /**
     * Регистрация эндпоинта по указанному пути.
     * <p>
     * Register endpoint at specified path.
     * 
     * @param path путь эндпоинта (например, "/api/data")
     * @param handler обработчик запросов (принимает FullHttpRequest и ChannelHandlerContext)
     */
    public void registerEndpoint(String path, BiConsumer<FullHttpRequest, ChannelHandlerContext> handler) {
        if (serverChannel == null || !serverChannel.isActive()) {
            Logger.error("Server on port " + port + " not initialized. Call init() first.");
            return;
        }
        
        endpointHandlers.put(path, handler);
        Logger.info("Registered endpoint: " + path + " on port " + port);
    }
    
    /**
     * Удаление эндпоинта по пути.
     * <p>
     * Remove endpoint by path.
     * 
     * @param path путь эндпоинта
     */
    public void removeEndpoint(String path) {
        if (endpointHandlers.remove(path) != null) {
            Logger.info("Removed endpoint: " + path + " from port " + port);
        }
    }
    
    /**
     * Проверка, запущен ли сервер.
     * <p>
     * Check if server is running.
     * 
     * @return true если сервер запущен
     */
    public boolean isRunning() {
        return serverChannel != null && serverChannel.isActive();
    }
    
    /**
     * Получение порта сервера.
     * <p>
     * Get server port.
     * 
     * @return порт сервера
     */
    public int getPort() {
        return port;
    }
    
    /**
     * Получение количества зарегистрированных эндпоинтов.
     * <p>
     * Get number of registered endpoints.
     * 
     * @return количество эндпоинтов
     */
    public int getEndpointCount() {
        return endpointHandlers.size();
    }
    
    /**
     * Регистрация стандартных эндпоинтов для этого порта.
     * <p>
     * Register default endpoints for this port.
     */
    private void registerDefaultEndpoints() {
        // Root endpoint
        registerEndpoint("/", (request, ctx) -> {
            String response = "<html><body><h1>Aporia.cc NetworkServer</h1><p>Server is running on port " + port + "</p></body></html>";
            sendResponse(ctx, HttpResponseStatus.OK, response, "text/html");
        });
        
        // Health check endpoint
        registerEndpoint("/health", (request, ctx) -> {
            String response = "{\"status\":\"ok\",\"port\":" + port + ",\"endpoints\":" + endpointHandlers.size() + "}";
            sendResponse(ctx, HttpResponseStatus.OK, response, "application/json");
        });
        
        // API info endpoint
        registerEndpoint("/api/info", (request, ctx) -> {
            String response = "{\"name\":\"Aporia.cc NetworkServer\",\"port\":" + port + ",\"endpoints\":" + endpointHandlers.size() + "}";
            sendResponse(ctx, HttpResponseStatus.OK, response, "application/json");
        });
    }
    
    /**
     * Отправка HTTP ответа через Netty.
     * <p>
     * Send HTTP response via Netty.
     * 
     * @param ctx контекст ChannelHandler
     * @param status статус HTTP
     * @param content тело ответа
     * @param contentType тип контента
     */
    private void sendResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String content, String contentType) {
        FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            status,
            Unpooled.copiedBuffer(content, CharsetUtil.UTF_8)
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType + "; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        
        ctx.writeAndFlush(response);
    }
    
    /**
     * Статический метод для отправки ответа через ChannelHandlerContext.
     * Удобная обертка для использования в лямбда-выражениях.
     * <p>
     * Static method for sending response via ChannelHandlerContext.
     * Convenient wrapper for use in lambdas.
     * 
     * @param ctx контекст ChannelHandler
     * @param status статус HTTP
     * @param content тело ответа
     * @param contentType тип контента
     */
    public static void sendResponseStatic(ChannelHandlerContext ctx, HttpResponseStatus status, String content, String contentType) {
        sendResponseStatic(ctx, status, content, contentType);
    }
}