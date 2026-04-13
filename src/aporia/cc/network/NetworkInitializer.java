/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package aporia.cc.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import so.aporia.utils.user.logger.Logger;

import java.nio.file.Files;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * NetworkInitializer — инициализация всей сетевой подсистемы Aporia.cc.
 * <p>
 * NetworkInitializer — initialization of the entire network subsystem of Aporia.cc.
 * <p>
 * Этот класс предоставляет единую точку входа для настройки и запуска
 * всех компонентов сети: HTTP сервера, хранилища файлов, HTML/CSS логики.
 * <p>
 * This class provides a single entry point for configuring and launching
 * all network components: HTTP server, file storage, HTML/CSS logic.
 */
public class NetworkInitializer {
    
    /**
     * Инициализация всей сетевой подсистемы с портом по умолчанию (61033).
     * <p>
     * Initialize entire network subsystem with default port (61033).
     * 
     * @return true если все компоненты успешно инициализированы
     */
    public static boolean init() {
        return init(61033);
    }
    
    /**
     * Инициализация всей сетевой подсистемы с указанным портом.
     * <p>
     * Initialize entire network subsystem with specified port.
     * 
     * @param port порт для HTTP сервера
     * @return true если все компоненты успешно инициализированы
     */
    public static boolean init(int port) {
        Logger.info("Initializing Aporia.cc network subsystem...");
        
        boolean success = true;
        
        // 1. Initialize network infrastructure
        Logger.debug("Initializing network infrastructure...");
        if (!EthernetUtils.initializeNetworkInfrastructure()) {
            Logger.error("Failed to initialize network infrastructure");
            success = false;
        }
        
        // 2. Start HTTP server on specified port
        Logger.debug("Starting HTTP server on port " + port + "...");
        if (!EthernetUtils.init(port)) {
            Logger.error("Failed to start HTTP server");
            success = false;
        } else {
            // Register additional endpoints on this port
            registerNetworkEndpoints(port);
        }
        
        if (success) {
            Logger.success("Aporia.cc network subsystem initialized successfully");
            Logger.info("Server running on http://localhost:" + port);
            Logger.info("Dashboard: http://localhost:" + port + "/dashboard.html");
        } else {
            Logger.error("Aporia.cc network subsystem initialization failed");
        }
        
        return success;
    }
    
    /**
     * Инициализация сетевой подсистемы с несколькими портами.
     * <p>
     * Initialize network subsystem with multiple ports.
     * 
     * @param ports массив портов для запуска серверов
     * @return true если все серверы успешно инициализированы
     */
    public static boolean initMultiple(int[] ports) {
        Logger.info("Initializing Aporia.cc network subsystem with " + ports.length + " ports...");
        
        boolean success = true;
        
        // 1. Initialize network infrastructure
        Logger.debug("Initializing network infrastructure...");
        if (!EthernetUtils.initializeNetworkInfrastructure()) {
            Logger.error("Failed to initialize network infrastructure");
            success = false;
        }
        
        // 2. Start HTTP servers on all specified ports
        for (int port : ports) {
            Logger.debug("Starting HTTP server on port " + port + "...");
            if (!EthernetUtils.init(port)) {
                Logger.error("Failed to start HTTP server on port " + port);
                success = false;
            } else {
                // Register default endpoints for this port
                registerNetworkEndpoints(port);
            }
        }
        
        if (success) {
            Logger.success("Aporia.cc network subsystem initialized with " + ports.length + " servers");
            for (int port : ports) {
                Logger.info("Server running on http://localhost:" + port);
            }
        } else {
            Logger.error("Aporia.cc network subsystem initialization failed");
        }
        
        return success;
    }
    
    /**
     * Остановка всей сетевой подсистемы (дефолтного порта).
     * <p>
     * Shutdown entire network subsystem (default port).
     */
    public static void shutdown() {
        Logger.info("Shutting down Aporia.cc network subsystem...");
        
        EthernetUtils.shutdown();
        ThreadManager.shutdown();
        
        Logger.success("Aporia.cc network subsystem shutdown complete");
    }
    
    /**
     * Остановка всей сетевой подсистемы (всех портов).
     * <p>
     * Shutdown entire network subsystem (all ports).
     */
    public static void shutdownAll() {
        Logger.info("Shutting down ALL Aporia.cc network servers...");
        
        EthernetUtils.shutdownAll();
        ThreadManager.shutdown();
        
        Logger.success("All Aporia.cc network servers shutdown complete");
    }
    
    /**
     * Остановка сервера на указанном порту.
     * <p>
     * Stop server on specified port.
     * 
     * @param port порт сервера
     * @return true если успешно
     */
    public static boolean shutdown(int port) {
        return EthernetUtils.shutdown(port);
    }
    
    /**
     * Проверка статуса дефолтного сервера.
     * <p>
     * Check default server status.
     * 
     * @return true если сервер активен
     */
    public static boolean isRunning() {
        return EthernetUtils.isRunning();
    }
    
    /**
     * Проверка статуса сервера на указанном порту.
     * <p>
     * Check server status on specified port.
     * 
     * @param port порт сервера
     * @return true если сервер активен
     */
    public static boolean isRunning(int port) {
        return EthernetUtils.isRunning(port);
    }
    
    /**
     * Получение текущего порта дефолтного сервера.
     * <p>
     * Get current default server port.
     * 
     * @return порт сервера или -1 если не запущен
     */
    public static int getPort() {
        return EthernetUtils.getPort();
    }
    
    /**
     * Получение количества активных серверов.
     * <p>
     * Get number of active servers.
     * 
     * @return количество серверов
     */
    public static int getServerCount() {
        return EthernetUtils.getServerCount();
    }
    
    /**
     * Получение статистики всех серверов.
     * <p>
     * Get statistics for all servers.
     * 
     * @return строка со статистикой
     */
    public static String getStats() {
        return EthernetUtils.getStats();
    }
    
    /**
     * Регистрация дополнительных эндпоинтов сети на указанном порту.
     * <p>
     * Register additional network endpoints on specified port.
     * 
     * @param port порт для регистрации эндпоинтов
     */
    private static void registerNetworkEndpoints(int port) {
        // File serving endpoint for HTML
        EthernetUtils.registerEndpoint(port, "/files/html/", (request, ctx) -> {
            String path = request.uri();
            String filename = path.replace("/files/html/", "");
            
            String content = HTMLLogic.readHTML(filename);
            if (content != null) {
                EthernetUtils.sendResponse(ctx, HttpResponseStatus.OK, content, "text/html");
            } else {
                EthernetUtils.sendResponse(ctx, HttpResponseStatus.NOT_FOUND, 
                    "HTML file not found: " + filename, "text/plain");
            }
        });
        
        // File serving endpoint for CSS
        EthernetUtils.registerEndpoint(port, "/files/css/", (request, ctx) -> {
            String path = request.uri();
            String filename = path.replace("/files/css/", "");
            
            String content = HTMLLogic.readCSS(filename);
            if (content != null) {
                EthernetUtils.sendResponse(ctx, HttpResponseStatus.OK, content, "text/css");
            } else {
                EthernetUtils.sendResponse(ctx, HttpResponseStatus.NOT_FOUND,
                    "CSS file not found: " + filename, "text/plain");
            }
        });
        
        // API endpoint for listing HTML files
        EthernetUtils.registerEndpoint(port, "/api/files/html/list", (request, ctx) -> {
            try {
                StringBuilder response = new StringBuilder("{\"files\":[");
                java.nio.file.Path htmlPath = HTMLLogic.getHTMLStoragePath();
                
                if (Files.exists(htmlPath)) {
                    try (Stream<java.nio.file.Path> stream = Files.list(htmlPath)) {
                        java.util.List<String> files = stream.filter(p -> p.toString().toLowerCase().endsWith(".html"))
                                          .map(p -> "\"" + p.getFileName().toString() + "\"")
                                          .collect(Collectors.toList());
                        response.append(String.join(",", files));
                    }
                }
                
                response.append("]}");
                EthernetUtils.sendResponse(ctx, HttpResponseStatus.OK, response.toString(), "application/json");
            } catch (Exception e) {
                EthernetUtils.sendResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                    "{\"error\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}", "application/json");
            }
        });
        
        // Health check with detailed info
        EthernetUtils.registerEndpoint(port, "/api/health/detailed", (request, ctx) -> {
            String response = String.format(
                "{\"status\":\"ok\",\"server_running\":%s,\"port\":%d,\"storage_exists\":%s,\"cache_cleared\":%s}",
                EthernetUtils.isRunning(port),
                port,
                HTMLLogic.checkFilesExist(),
                true
            );
            EthernetUtils.sendResponse(ctx, HttpResponseStatus.OK, response, "application/json");
        });
        
        Logger.info("Network endpoints registered on port " + port);
    }
    
    /**
     * Пример использования сетевого API с одним сервером.
     * <p>
     * Example of network API usage with single server.
     */
    public static void exampleUsage() {
        Logger.info("=== Network API Example Usage (Single Server) ===");
        
        // Start the network subsystem on default port
        if (init()) {
            // Make a test request
            Requests.Response response = Requests.get("http://localhost:" + getPort() + "/health");
            Logger.info("Health check: " + response);
            
            // Save a custom HTML file
            HTMLLogic.saveHTML("test.html", "<html><body><h1>Test Page</h1><p>Generated by Aporia.cc</p></body></html>");
            
            // Read it back
            String content = HTMLLogic.readHTML("test.html");
            Logger.info("Read back HTML: " + (content != null ? content.length() + " bytes" : "failed"));
        }
        
        Logger.info("=== Example Complete ===");
    }
    
    /**
     * Пример использования сетевого API с несколькими серверами.
     * <p>
     * Example of network API usage with multiple servers.
     */
    public static void exampleUsageMultipleServers() {
        Logger.info("=== Network API Example Usage (Multiple Servers) ===");
        
        // Start servers on multiple ports
        int[] ports = {61033, 61034, 61035};
        if (initMultiple(ports)) {
            // Test each server
            for (int port : ports) {
                Requests.Response response = Requests.get("http://localhost:" + port + "/health");
                Logger.info("Health check port " + port + ": " + response);
            }
            
            // Create custom endpoints on specific ports
            NetworkServer server8080 = EthernetUtils.createServerOnPort(8080);
            if (server8080 != null) {
                server8080.registerEndpoint("/custom/hello", (request, ctx) -> {
                    String response = "{\"message\":\"Hello from port 8080!\"}";
                    NetworkServer.sendResponseStatic(ctx, HttpResponseStatus.OK, response, "application/json");
                });
                Logger.info("Custom endpoint registered on port 8080");
            }
            
            // Show statistics
            Logger.info("Server statistics:\n" + getStats());
        }
        
        Logger.info("=== Example Complete ===");
    }
}