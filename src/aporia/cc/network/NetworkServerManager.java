/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package aporia.cc.network;

import so.aporia.utils.files.FilesManager;
import so.aporia.utils.user.logger.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NetworkServerManager — менеджер множества HTTP серверов на разных портах.
 * <p>
 * NetworkServerManager — manager for multiple HTTP servers on different ports.
 * <p>
 * Позволяет создавать, управлять и отслеживать множество NetworkServer инстансов.
 * <p>
 * Allows creating, managing and tracking multiple NetworkServer instances.
 */
public class NetworkServerManager {
    
    private static final Map<Integer, NetworkServer> servers = new ConcurrentHashMap<>();
    
    /**
     * Создание и запуск нового HTTP сервера на указанном порту.
     * <p>
     * Create and start new HTTP server on specified port.
     * 
     * @param port порт для прослушивания
     * @return NetworkServer инстанс или null при ошибке
     */
    public static NetworkServer createServer(int port) {
        if (servers.containsKey(port)) {
            Logger.warn("Server already exists on port " + port);
            return servers.get(port);
        }
        
        NetworkServer server = new NetworkServer(port);
        if (server.init()) {
            servers.put(port, server);
            Logger.success("Server created on port " + port);
            return server;
        } else {
            Logger.error("Failed to create server on port " + port);
            return null;
        }
    }
    
    /**
     * Создание и запуск нового HTTP сервера на порту по умолчанию (61033).
     * <p>
     * Create and start new HTTP server on default port (61033).
     * 
     * @return NetworkServer инстанс или null при ошибке
     */
    public static NetworkServer createServer() {
        return createServer(61033);
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
        return servers.get(port);
    }
    
    /**
     * Остановка сервера на указанном порту.
     * <p>
     * Stop server on specified port.
     * 
     * @param port порт сервера
     * @return true если успешно, false если сервер не найден
     */
    public static boolean stopServer(int port) {
        NetworkServer server = servers.remove(port);
        if (server != null) {
            server.shutdown();
            Logger.success("Server stopped on port " + port);
            return true;
        }
        Logger.warn("Server not found on port " + port);
        return false;
    }
    
    /**
     * Остановка всех серверов.
     * <p>
     * Stop all servers.
     */
    public static void stopAllServers() {
        Logger.info("Stopping all NetworkServers (" + servers.size() + " servers)...");
        
        for (NetworkServer server : servers.values()) {
            try {
                server.shutdown();
            } catch (Exception e) {
                Logger.error("Error stopping server on port " + server.getPort() + ": " + e.getMessage());
            }
        }
        
        servers.clear();
        Logger.success("All NetworkServers stopped");
    }
    
    /**
     * Получение количества активных серверов.
     * <p>
     * Get number of active servers.
     * 
     * @return количество серверов
     */
    public static int getServerCount() {
        return servers.size();
    }
    
    /**
     * Получение списка всех портов с активными серверами.
     * <p>
     * Get list of all ports with active servers.
     * 
     * @return массив портов
     */
    public static int[] getAllPorts() {
        return servers.keySet().stream().mapToInt(Integer::intValue).toArray();
    }
    
    /**
     * Проверка, запущен ли сервер на указанном порту.
     * <p>
     * Check if server is running on specified port.
     * 
     * @param port порт для проверки
     * @return true если сервер запущен
     */
    public static boolean isServerRunning(int port) {
        NetworkServer server = servers.get(port);
        return server != null && server.isRunning();
    }
    
    /**
     * Регистрация эндпоинта на указанном порту.
     * <p>
     * Register endpoint on specified port.
     * 
     * @param port порт сервера
     * @param path путь эндпоинта
     * @param handler обработчик запросов
     * @return true если успешно, false при ошибке
     */
    public static boolean registerEndpoint(int port, String path, java.util.function.BiConsumer<io.netty.handler.codec.http.FullHttpRequest, io.netty.channel.ChannelHandlerContext> handler) {
        NetworkServer server = servers.get(port);
        if (server == null) {
            Logger.error("Server not found on port " + port);
            return false;
        }
        
        server.registerEndpoint(path, handler);
        return true;
    }
    
    /**
     * Удаление эндпоинта с указанного порта.
     * <p>
     * Remove endpoint from specified port.
     * 
     * @param port порт сервера
     * @param path путь эндпоинта
     * @return true если успешно, false при ошибке
     */
    public static boolean removeEndpoint(int port, String path) {
        NetworkServer server = servers.get(port);
        if (server == null) {
            Logger.error("Server not found on port " + port);
            return false;
        }
        
        server.removeEndpoint(path);
        return true;
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
        stats.append("NetworkServerManager Statistics:\n");
        stats.append("Total servers: ").append(servers.size()).append("\n");
        
        for (NetworkServer server : servers.values()) {
            stats.append(String.format("  Port %d: running=%s, endpoints=%d\n",
                server.getPort(),
                server.isRunning(),
                server.getEndpointCount()
            ));
        }
        
        return stats.toString();
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
     * Получение пути кэша для указанного порта.
     * <p>
     * Get cache path for specified port.
     * 
     * @param port порт сервера
     * @return Path к директории кэша порта
     */
    public static Path getPortCachePath(int port) {
        return getNetworkStoragePath().resolve("cache").resolve("port_" + port);
    }
    
    /**
     * Пример использования: создание нескольких серверов с разными эндпоинтами.
     * <p>
     * Example usage: creating multiple servers with different endpoints.
     */
    public static void exampleUsage() {
        Logger.info("=== NetworkServerManager Example Usage ===");
        
        // Create server on port 8080
        NetworkServer server1 = createServer(8080);
        if (server1 != null) {
            server1.registerEndpoint("/api/v1/data", (request, ctx) -> {
                String response = "{\"data\":\"from port 8080\"}";
                NetworkServer.sendResponseStatic(ctx, io.netty.handler.codec.http.HttpResponseStatus.OK, response, "application/json");
            });
        }
        
        // Create server on port 8081
        NetworkServer server2 = createServer(8081);
        if (server2 != null) {
            server2.registerEndpoint("/api/v2/info", (request, ctx) -> {
                String response = "{\"info\":\"from port 8081\"}";
                NetworkServer.sendResponseStatic(ctx, io.netty.handler.codec.http.HttpResponseStatus.OK, response, "application/json");
            });
        }
        
        // Create default server on port 61033
        NetworkServer defaultServer = createServer();
        
        Logger.info("Created " + getServerCount() + " servers");
        Logger.info("Stats:\n" + getStats());
        
        Logger.info("=== Example Complete ===");
    }
}