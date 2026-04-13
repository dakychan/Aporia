/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package aporia.cc.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import so.aporia.utils.files.FilesManager;
import so.aporia.utils.user.logger.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * EthernetUtils — главный управляющий множеством HTTP серверов на разных портах.
 * <p>
 * EthernetUtils — main manager for multiple HTTP servers on different ports.
 * <p>
 * Фасад для NetworkServerManager, предоставляет старый API для обратной совместимости
 * и новый API для управления множеством серверов.
 * <p>
 * Facade for NetworkServerManager, provides old API for backward compatibility
 * and new API for managing multiple servers.
 */
public class EthernetUtils {
    
    private static final int DEFAULT_PORT = 61033;
    private static final Map<Integer, NetworkServer> managedServers = new ConcurrentHashMap<>();
    private static NetworkServer defaultServer;
    
    /**
     * Инициализация HTTP сервера на указанном порту.
     * <p>
     * Initialize HTTP server on specified port.
     * 
     * @param port порт для прослушивания
     * @return true если успешно, false при ошибке
     */
    public static boolean init(int port) {
        if (managedServers.containsKey(port)) {
            Logger.warn("Server already running on port " + port);
            return false;
        }
        
        NetworkServer server = NetworkServerManager.createServer(port);
        if (server != null) {
            managedServers.put(port, server);
            if (port == DEFAULT_PORT) {
                defaultServer = server;
            }
            return true;
        }
        return false;
    }
    
    /**
     * Инициализация HTTP сервера на порту по умолчанию (61033).
     * <p>
     * Initialize HTTP server on default port (61033).
     * 
     * @return true если успешно, false при ошибке
     */
    public static boolean init() {
        return init(DEFAULT_PORT);
    }
    
    /**
     * Регистрация эндпоинта по указанному пути на дефолтном порту.
     * <p>
     * Register endpoint at specified path on default port.
     * 
     * @param path путь эндпоинта (например, "/api/data")
     * @param handler обработчик запросов
     */
    public static void registerEndpoint(String path, BiConsumer<FullHttpRequest, ChannelHandlerContext> handler) {
        if (defaultServer == null) {
            Logger.error("Default server not initialized. Call init() first.");
            return;
        }
        
        defaultServer.registerEndpoint(path, handler);
    }
    
    /**
     * Регистрация эндпоинта по указанному пути на конкретном порту.
     * <p>
     * Register endpoint at specified path on specific port.
     * 
     * @param port порт сервера
     * @param path путь эндпоинта
     * @param handler обработчик запросов
     * @return true если успешно
     */
    public static boolean registerEndpoint(int port, String path, BiConsumer<FullHttpRequest, ChannelHandlerContext> handler) {
        NetworkServer server = managedServers.get(port);
        if (server == null) {
            Logger.error("Server not found on port " + port);
            return false;
        }
        
        server.registerEndpoint(path, handler);
        return true;
    }
    
    /**
     * Удаление эндпоинта по пути с дефолтного порта.
     * <p>
     * Remove endpoint by path from default port.
     * 
     * @param path путь эндпоинта
     */
    public static void removeEndpoint(String path) {
        if (defaultServer == null) {
            return;
        }
        
        defaultServer.removeEndpoint(path);
    }
    
    /**
     * Удаление эндпоинта по пути с конкретного порта.
     * <p>
     * Remove endpoint by path from specific port.
     * 
     * @param port порт сервера
     * @param path путь эндпоинта
     * @return true если успешно
     */
    public static boolean removeEndpoint(int port, String path) {
        NetworkServer server = managedServers.get(port);
        if (server == null) {
            return false;
        }
        
        server.removeEndpoint(path);
        return true;
    }
    
    /**
     * Остановка HTTP сервера на указанном порту.
     * <p>
     * Stop HTTP server on specified port.
     * 
     * @param port порт сервера
     * @return true если успешно
     */
    public static boolean shutdown(int port) {
        boolean success = NetworkServerManager.stopServer(port);
        if (success) {
            managedServers.remove(port);
            if (port == DEFAULT_PORT) {
                defaultServer = null;
            }
        }
        return success;
    }
    
    /**
     * Остановка HTTP сервера на дефолтном порту.
     * <p>
     * Stop HTTP server on default port.
     */
    public static void shutdown() {
        shutdown(DEFAULT_PORT);
    }
    
    /**
     * Остановка всех HTTP серверов.
     * <p>
     * Stop all HTTP servers.
     */
    public static void shutdownAll() {
        NetworkServerManager.stopAllServers();
        managedServers.clear();
        defaultServer = null;
    }
    
    /**
     * Получение текущего порта дефолтного сервера.
     * <p>
     * Get current default server port.
     * 
     * @return порт сервера или -1 если сервер не запущен
     */
    public static int getPort() {
        return defaultServer != null ? defaultServer.getPort() : -1;
    }
    
    /**
     * Получение порта указанного сервера.
     * <p>
     * Get port of specified server.
     * 
     * @param server сервер
     * @return порт сервера или -1 если сервер null
     */
    public static int getPort(NetworkServer server) {
        return server != null ? server.getPort() : -1;
    }
    
    /**
     * Проверка, запущен ли дефолтный сервер.
     * <p>
     * Check if default server is running.
     * 
     * @return true если сервер запущен
     */
    public static boolean isRunning() {
        return defaultServer != null && defaultServer.isRunning();
    }
    
    /**
     * Проверка, запущен ли сервер на указанном порту.
     * <p>
     * Check if server is running on specified port.
     * 
     * @param port порт для проверки
     * @return true если сервер запущен
     */
    public static boolean isRunning(int port) {
        NetworkServer server = managedServers.get(port);
        return server != null && server.isRunning();
    }
    
    /**
     * Создание нового сервера на указанном порту.
     * <p>
     * Create new server on specified port.
     * 
     * @param port порт для сервера
     * @return NetworkServer инстанс или null при ошибке
     */
    public static NetworkServer createServerOnPort(int port) {
        NetworkServer server = NetworkServerManager.createServer(port);
        if (server != null) {
            managedServers.put(port, server);
        }
        return server;
    }
    
    /**
     * Получение сервера по порту.
     * <p>
     * Get server by port.
     * 
     * @param port порт сервера
     * @return NetworkServer инстанс или null если не найден
     */
    public static NetworkServer getServer(int port) {
        return managedServers.get(port);
    }
    
    /**
     * Получение дефолтного сервера.
     * <p>
     * Get default server.
     * 
     * @return NetworkServer инстанс или null если не инициализирован
     */
    public static NetworkServer getDefaultServer() {
        return defaultServer;
    }
    
    /**
     * Получение количества управляемых серверов.
     * <p>
     * Get number of managed servers.
     * 
     * @return количество серверов
     */
    public static int getServerCount() {
        return managedServers.size();
    }
    
    /**
     * Получение всех портов с активными серверами.
     * <p>
     * Get all ports with active servers.
     * 
     * @return массив портов
     */
    public static int[] getAllPorts() {
        return managedServers.keySet().stream().mapToInt(Integer::intValue).toArray();
    }
    
    /**
     * Отправка HTTP ответа через Netty (статический хелпер).
     * <p>
     * Send HTTP response via Netty (static helper).
     * 
     * @param ctx контекст ChannelHandler
     * @param status статус HTTP
     * @param content тело ответа
     * @param contentType тип контента
     */
    public static void sendResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String content, String contentType) {
        NetworkServer.sendResponseStatic(ctx, status, content, contentType);
    }
    
    /**
     * Получение базового пути для хранения сетевых файлов.
     * <p>
     * Get base path for network files storage.
     * 
     * @return Path к директории network в FilesManager.ROOT
     */
    public static Path getNetworkStoragePath() {
        return FilesManager.ROOT.resolve("network");
    }
    
    /**
     * Создание структуры директорий для сетевых файлов.
     * <p>
     * Create directory structure for network files.
     * 
     * @return true если успешно, false при ошибке
     */
    public static boolean createNetworkStorage() {
        try {
            Path networkPath = getNetworkStoragePath();
            Path htmlPath = networkPath.resolve("html");
            Path cssPath = networkPath.resolve("css");
            Path cachePath = networkPath.resolve("cache");
            
            Files.createDirectories(htmlPath);
            Files.createDirectories(cssPath);
            Files.createDirectories(cachePath);
            
            Logger.success("Network storage created at: " + networkPath);
            return true;
        } catch (IOException e) {
            Logger.error("Failed to create network storage: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Получение статистики всех серверов.
     * <p>
     * Get statistics for all servers.
     * 
     * @return строка со статистикой
     */
    public static String getStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("EthernetUtils Managed Servers:\n");
        stats.append("Total servers: ").append(managedServers.size()).append("\n");
        stats.append("Default port: ").append(DEFAULT_PORT).append("\n");
        stats.append("Default server running: ").append(defaultServer != null && defaultServer.isRunning()).append("\n");
        
        for (NetworkServer server : managedServers.values()) {
            stats.append(String.format("  Port %d: running=%s, endpoints=%d%s\n",
                server.getPort(),
                server.isRunning(),
                server.getEndpointCount(),
                server == defaultServer ? " (default)" : ""
            ));
        }
        
        return stats.toString();
    }
    
    /**
     * Инициализация всей сетевой инфраструктуры.
     * <p>
     * Initialize entire network infrastructure.
     * 
     * @return true если успешно
     */
    public static boolean initializeNetworkInfrastructure() {
        Logger.info("Initializing Aporia.cc network infrastructure...");
        
        boolean success = true;
        
        // 1. Create network storage
        if (!createNetworkStorage()) {
            Logger.error("Failed to create network storage");
            success = false;
        }
        
        // 2. Initialize default HTML/CSS files
        if (!HTMLLogic.initializeDefaultFiles()) {
            Logger.warn("Failed to initialize some default files");
            // Don't fail completely
        }
        
        // 3. Clear old cache
        Requests.clearCache();
        
        if (success) {
            Logger.success("Network infrastructure initialized");
        } else {
            Logger.error("Network infrastructure initialization failed");
        }
        
        return success;
    }
}