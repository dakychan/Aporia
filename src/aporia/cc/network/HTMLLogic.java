/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package aporia.cc.network;

import so.aporia.utils.files.FilesManager;
import so.aporia.utils.user.logger.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTMLLogic — логика HTML, их сохранение по filesmanager.root./network/,
 * CSS стили, шрифты все сюда.
 * <p>
 * HTMLLogic — HTML logic, their storage at filesmanager.root./network/,
 * CSS styles, fonts everything here.
 * <p>
 * Features:
 * <ul>
 *   <li>HTML file management (create, read, update, delete)</li>
 *   <li>CSS file management</li>
 *   <li>Template system with variable substitution</li>
 *   <li>Default HTML/CSS templates</li>
 *   <li>File caching and optimization</li>
 * </ul>
 * <p>
 * Возможности:
 * <ul>
 *   <li>Управление HTML файлами (создание, чтение, обновление, удаление)</li>
 *   <li>Управление CSS файлами</li>
 *   <li>Система шаблонов с подстановкой переменных</li>
 *   <li>Стандартные HTML/CSS шаблоны</li>
 *   <li>Кэширование и оптимизация файлов</li>
 * </ul>
 */
public class HTMLLogic {
    
    /**
     * Сохранение HTML файла в сетевую директорию.
     * <p>
     * Save HTML file to network directory.
     * 
     * @param filename имя файла (без расширения или с .html)
     * @param content содержимое HTML
     * @return true если успешно
     */
    public static boolean saveHTML(String filename, String content) {
        try {
            Path htmlPath = getHTMLStoragePath();
            Files.createDirectories(htmlPath);
            
            // Ensure .html extension
            if (!filename.toLowerCase().endsWith(".html")) {
                filename += ".html";
            }
            
            Path filePath = htmlPath.resolve(filename);
            Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));
            Logger.info("HTML saved: " + filePath);
            return true;
        } catch (IOException e) {
            Logger.error("Failed to save HTML: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Сохранение CSS файла в сетевую директорию.
     * <p>
     * Save CSS file to network directory.
     * 
     * @param filename имя файла (без расширения или с .css)
     * @param content содержимое CSS
     * @return true если успешно
     */
    public static boolean saveCSS(String filename, String content) {
        try {
            Path cssPath = getCSSStoragePath();
            Files.createDirectories(cssPath);
            
            // Ensure .css extension
            if (!filename.toLowerCase().endsWith(".css")) {
                filename += ".css";
            }
            
            Path filePath = cssPath.resolve(filename);
            Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));
            Logger.info("CSS saved: " + filePath);
            return true;
        } catch (IOException e) {
            Logger.error("Failed to save CSS: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Чтение HTML файла из сетевой директории.
     * <p>
     * Read HTML file from network directory.
     * 
     * @param filename имя файла
     * @return содержимое файла или null при ошибке
     */
    public static String readHTML(String filename) {
        try {
            Path htmlPath = getHTMLStoragePath();
            
            // Try with and without .html extension
            Path filePath = htmlPath.resolve(filename);
            if (!Files.exists(filePath) && !filename.toLowerCase().endsWith(".html")) {
                filePath = htmlPath.resolve(filename + ".html");
            }
            
            if (Files.exists(filePath)) {
                String content = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
                Logger.debug("HTML read: " + filePath + " (" + content.length() + " bytes)");
                return content;
            } else {
                Logger.warn("HTML file not found: " + filename);
                return null;
            }
        } catch (IOException e) {
            Logger.error("Failed to read HTML: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Чтение CSS файла из сетевой директории.
     * <p>
     * Read CSS file from network directory.
     * 
     * @param filename имя файла
     * @return содержимое файла или null при ошибке
     */
    public static String readCSS(String filename) {
        try {
            Path cssPath = getCSSStoragePath();
            
            // Try with and without .css extension
            Path filePath = cssPath.resolve(filename);
            if (!Files.exists(filePath) && !filename.toLowerCase().endsWith(".css")) {
                filePath = cssPath.resolve(filename + ".css");
            }
            
            if (Files.exists(filePath)) {
                String content = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
                Logger.debug("CSS read: " + filePath + " (" + content.length() + " bytes)");
                return content;
            } else {
                Logger.warn("CSS file not found: " + filename);
                return null;
            }
        } catch (IOException e) {
            Logger.error("Failed to read CSS: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Удаление HTML файла.
     * <p>
     * Delete HTML file.
     * 
     * @param filename имя файла
     * @return true если успешно
     */
    public static boolean deleteHTML(String filename) {
        try {
            Path htmlPath = getHTMLStoragePath();
            Path filePath = htmlPath.resolve(filename);
            
            if (!Files.exists(filePath) && !filename.toLowerCase().endsWith(".html")) {
                filePath = htmlPath.resolve(filename + ".html");
            }
            
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                Logger.info("HTML deleted: " + filePath);
                return true;
            } else {
                Logger.warn("HTML file not found for deletion: " + filename);
                return false;
            }
        } catch (IOException e) {
            Logger.error("Failed to delete HTML: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Удаление CSS файла.
     * <p>
     * Delete CSS file.
     * 
     * @param filename имя файла
     * @return true если успешно
     */
    public static boolean deleteCSS(String filename) {
        try {
            Path cssPath = getCSSStoragePath();
            Path filePath = cssPath.resolve(filename);
            
            if (!Files.exists(filePath) && !filename.toLowerCase().endsWith(".css")) {
                filePath = cssPath.resolve(filename + ".css");
            }
            
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                Logger.info("CSS deleted: " + filePath);
                return true;
            } else {
                Logger.warn("CSS file not found for deletion: " + filename);
                return false;
            }
        } catch (IOException e) {
            Logger.error("Failed to delete CSS: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Создание HTML страницы из шаблона с подстановкой переменных.
     * <p>
     * Create HTML page from template with variable substitution.
     * 
     * @param templateName имя шаблона
     * @param variables карта переменных для подстановки
     * @return сгенерированный HTML или null при ошибке
     */
    public static String createFromTemplate(String templateName, Map<String, String> variables) {
        String template = getTemplate(templateName);
        if (template == null) {
            return null;
        }
        
        if (variables.isEmpty()) {
            return template;
        }
        
        // Используем Matcher.appendReplacement для безопасной замены
        String result = template;
        
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String pattern = "\\{\\{" + Pattern.quote(entry.getKey()) + "\\}\\}";
            String replacement = Matcher.quoteReplacement(entry.getValue());
            
            Matcher matcher = Pattern.compile(pattern).matcher(result);
            StringBuffer buffer = new StringBuffer();
            while (matcher.find()) {
                matcher.appendReplacement(buffer, replacement);
            }
            matcher.appendTail(buffer);
            result = buffer.toString();
        }
        
        return result;
    }
    
    /**
     * Получение стандартного шаблона по имени.
     * <p>
     * Get standard template by name.
     * 
     * @param templateName имя шаблона
     * @return содержимое шаблона или null если не найден
     */
    public static String getTemplate(String templateName) {
        Map<String, String> templates = new HashMap<>();
        
        // Default templates
        templates.put("dashboard", """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>{{title}} - Aporia.cc</title>
                <link rel="stylesheet" href="/css/styles.css">
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background: #1a1a1a; color: #fff; }
                    .container { max-width: 1200px; margin: 0 auto; }
                    .header { background: #2a2a2a; padding: 20px; border-radius: 10px; margin-bottom: 20px; }
                    .stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin-bottom: 20px; }
                    .stat-card { background: #2a2a2a; padding: 20px; border-radius: 10px; text-align: center; }
                    .stat-value { font-size: 2em; font-weight: bold; color: #4CAF50; }
                    .stat-label { font-size: 0.9em; color: #aaa; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>{{title}}</h1>
                        <p>{{description}}</p>
                    </div>
                    <div class="stats">
                        <div class="stat-card">
                            <div class="stat-value">{{bot_status}}</div>
                            <div class="stat-label">Bot Status</div>
                        </div>
                        <div class="stat-card">
                            <div class="stat-value">{{requests_count}}</div>
                            <div class="stat-label">Requests</div>
                        </div>
                        <div class="stat-card">
                            <div class="stat-value">{{chat_messages}}</div>
                            <div class="stat-label">Chat Messages</div>
                        </div>
                    </div>
                    <div class="content">
                        {{content}}
                    </div>
                </div>
            </body>
            </html>
            """);
        
        templates.put("simple", """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>{{title}}</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 40px; background: #f5f5f5; color: #333; }
                    .container { max-width: 800px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>{{title}}</h1>
                    <p>{{message}}</p>
                </div>
            </body>
            </html>
            """);
        
        return templates.get(templateName.toLowerCase());
    }
    
    /**
     * Получение стандартного CSS стиля.
     * <p>
     * Get standard CSS style.
     * 
     * @return содержимое CSS
     */
    public static String getDefaultCSS() {
        return """
            /* Aporia.cc Network - Default CSS */
            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
            }
            
            body {
                font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
                color: #ffffff;
                min-height: 100vh;
                line-height: 1.6;
            }
            
            .container {
                max-width: 1200px;
                margin: 0 auto;
                padding: 20px;
            }
            
            .card {
                background: rgba(255, 255, 255, 0.1);
                backdrop-filter: blur(10px);
                border-radius: 15px;
                padding: 25px;
                margin-bottom: 20px;
                border: 1px solid rgba(255, 255, 255, 0.2);
                transition: transform 0.3s ease, box-shadow 0.3s ease;
            }
            
            .card:hover {
                transform: translateY(-5px);
                box-shadow: 0 10px 20px rgba(0, 0, 0, 0.3);
            }
            
            .btn {
                display: inline-block;
                padding: 12px 24px;
                background: linear-gradient(135deg, #4CAF50 0%, #45a049 100%);
                color: white;
                text-decoration: none;
                border-radius: 8px;
                border: none;
                cursor: pointer;
                font-weight: 600;
                transition: all 0.3s ease;
            }
            
            .btn:hover {
                background: linear-gradient(135deg, #45a049 0%, #3d8b40 100%);
                transform: scale(1.05);
            }
            
            .header {
                text-align: center;
                margin-bottom: 40px;
                padding: 40px 20px;
                background: rgba(0, 0, 0, 0.3);
                border-radius: 20px;
            }
            
            .header h1 {
                font-size: 3em;
                margin-bottom: 10px;
                background: linear-gradient(135deg, #00b4db 0%, #0083b0 100%);
                -webkit-background-clip: text;
                -webkit-text-fill-color: transparent;
            }
            
            .stat-grid {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
                gap: 20px;
                margin-bottom: 30px;
            }
            
            .stat-item {
                text-align: center;
                padding: 20px;
            }
            
            .stat-value {
                font-size: 2.5em;
                font-weight: bold;
                margin-bottom: 5px;
            }
            
            .stat-label {
                font-size: 0.9em;
                color: #aaa;
                text-transform: uppercase;
                letter-spacing: 1px;
            }
            
            @media (max-width: 768px) {
                .container {
                    padding: 10px;
                }
                
                .header h1 {
                    font-size: 2em;
                }
                
                .stat-grid {
                    grid-template-columns: 1fr;
                }
            }
            """;
    }
    
    /**
     * Инициализация стандартных HTML/CSS файлов.
     * <p>
     * Initialize standard HTML/CSS files.
     * 
     * @return true если успешно
     */
    public static boolean initializeDefaultFiles() {
        boolean success = true;
        
        // Create default CSS
        if (!saveCSS("styles.css", getDefaultCSS())) {
            success = false;
        }
        
        // Create default dashboard HTML
        Map<String, String> vars = new HashMap<>();
        vars.put("title", "Aporia.cc Dashboard");
        vars.put("description", "Network API Control Panel");
        vars.put("bot_status", "Online");
        vars.put("requests_count", "0");
        vars.put("chat_messages", "0");
        vars.put("content", "<p>Welcome to Aporia.cc Network API</p>");
        
        String dashboard = createFromTemplate("dashboard", vars);
        if (dashboard != null) {
            if (!saveHTML("dashboard.html", dashboard)) {
                success = false;
            }
        } else {
            success = false;
        }
        
        // Create simple welcome page
        vars.clear();
        vars.put("title", "Welcome to Aporia.cc");
        vars.put("message", "Network API is running successfully.");
        String simple = createFromTemplate("simple", vars);
        if (simple != null) {
            if (!saveHTML("index.html", simple)) {
                success = false;
            }
        } else {
            success = false;
        }
        
        if (success) {
            Logger.success("Default HTML/CSS files initialized");
        } else {
            Logger.warn("Some default files failed to initialize");
        }
        
        return success;
    }
    
    /**
     * Получение пути к директории HTML.
     * <p>
     * Get path to HTML directory.
     * 
     * @return Path к директории html в network storage
     */
    public static Path getHTMLStoragePath() {
        return EthernetUtils.getNetworkStoragePath().resolve("html");
    }
    
    /**
     * Получение пути к директории CSS.
     * <p>
     * Get path to CSS directory.
     * 
     * @return Path к директории css в network storage
     */
    public static Path getCSSStoragePath() {
        return EthernetUtils.getNetworkStoragePath().resolve("css");
    }
    
    /**
     * Проверка существования HTML/CSS файлов.
     * <p>
     * Check existence of HTML/CSS files.
     * 
     * @return true если основные файлы существуют
     */
    public static boolean checkFilesExist() {
        Path htmlPath = getHTMLStoragePath();
        Path cssPath = getCSSStoragePath();
        
        boolean htmlDirExists = Files.exists(htmlPath) && Files.isDirectory(htmlPath);
        boolean cssDirExists = Files.exists(cssPath) && Files.isDirectory(cssPath);
        
        if (htmlDirExists && cssDirExists) {
            Logger.debug("HTML/CSS directories exist");
            return true;
        } else {
            Logger.warn("HTML/CSS directories missing");
            return false;
        }
    }
}