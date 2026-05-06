/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package aporia.cc.network;

import so.aporia.utils.files.FilesManager;
import so.aporia.utils.user.logger.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Requests — логика запросов, их структура, байты, информация с Logger в случае неудачи,
 * GET POST и другие виды. Сделай типа requests.sendReq(дата).
 * <p>
 * Requests — request logic, their structure, bytes, information with Logger in case of failure,
 * GET POST and other types. Make like requests.sendReq(data).
 * <p>
 * Features:
 * <ul>
 *   <li>HTTP methods: GET, POST, PUT, DELETE, PATCH</li>
 *   <li>Request/response logging</li>
 *   <li>Headers support</li>
 *   <li>Timeout configuration</li>
 *   <li>GZIP compression support</li>
 *   <li>Response caching</li>
 * </ul>
 * <p>
 * Возможности:
 * <ul>
 *   <li>HTTP методы: GET, POST, PUT, DELETE, PATCH</li>
 *   <li>Логирование запросов/ответов</li>
 *   <li>Поддержка заголовков</li>
 *   <li>Настройка таймаутов</li>
 *   <li>Поддержка сжатия GZIP</li>
 *   <li>Кэширование ответов</li>
 * </ul>
 */
public class Requests {
    
    public enum Method {
        GET, POST, PUT, DELETE, PATCH
    }
    
    public static class RequestData {
        private String url;
        private Method method = Method.GET;
        private Map<String, String> headers = new HashMap<>();
        private String body;
        private int timeout = 10000; // 10 seconds
        private boolean followRedirects = true;
        private boolean gzip = true;
        
        public RequestData(String url) {
            this.url = url;
        }
        
        public RequestData url(String url) {
            this.url = url;
            return this;
        }
        
        public RequestData method(Method method) {
            this.method = method;
            return this;
        }
        
        public RequestData header(String key, String value) {
            this.headers.put(key, value);
            return this;
        }
        
        public RequestData headers(Map<String, String> headers) {
            this.headers.putAll(headers);
            return this;
        }
        
        public RequestData body(String body) {
            this.body = body;
            return this;
        }
        
        public RequestData timeout(int timeout) {
            this.timeout = timeout;
            return this;
        }
        
        public RequestData followRedirects(boolean followRedirects) {
            this.followRedirects = followRedirects;
            return this;
        }
        
        public RequestData gzip(boolean gzip) {
            this.gzip = gzip;
            return this;
        }
    }
    
    public static class Response {
        private int statusCode;
        private String body;
        private Map<String, String> headers = new HashMap<>();
        private long responseTime;
        private boolean success;
        private String errorMessage;
        
        public Response(int statusCode, String body, Map<String, String> headers, long responseTime) {
            this.statusCode = statusCode;
            this.body = body;
            this.headers = headers;
            this.responseTime = responseTime;
            this.success = statusCode >= 200 && statusCode < 300;
        }
        
        public Response(String errorMessage, long responseTime) {
            this.errorMessage = errorMessage;
            this.responseTime = responseTime;
            this.success = false;
        }
        
        public int getStatusCode() { return statusCode; }
        public String getBody() { return body; }
        public Map<String, String> getHeaders() { return headers; }
        public long getResponseTime() { return responseTime; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        
        @Override
        public String toString() {
            if (success) {
                return "Response{status=" + statusCode + ", bodyLength=" + (body != null ? body.length() : 0) + ", time=" + responseTime + "ms}";
            } else {
                return "Response{error=\"" + errorMessage + "\", time=" + responseTime + "ms}";
            }
        }
    }
    
    /**
     * Отправка HTTP запроса с данными.
     * <p>
     * Send HTTP request with data.
     * 
     * @param data данные запроса
     * @return объект Response
     */
    public static Response sendReq(RequestData data) {
        long startTime = System.currentTimeMillis();
        
        try {
            URL url = new URL(data.url);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Configure connection
            connection.setRequestMethod(data.method.name());
            connection.setConnectTimeout(data.timeout);
            connection.setReadTimeout(data.timeout);
            connection.setInstanceFollowRedirects(data.followRedirects);
            
            // Set headers
            if (data.gzip) {
                connection.setRequestProperty("Accept-Encoding", "gzip");
            }
            for (Map.Entry<String, String> entry : data.headers.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
            
            // Send body for POST, PUT, PATCH
            if (data.body != null && (data.method == Method.POST || data.method == Method.PUT || data.method == Method.PATCH)) {
                connection.setDoOutput(true);
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(data.body.getBytes(StandardCharsets.UTF_8));
                }
            }
            
            // Get response code
            int responseCode = connection.getResponseCode();
            long responseTime = System.currentTimeMillis() - startTime;
            
            // Read headers
            Map<String, String> responseHeaders = new HashMap<>();
            connection.getHeaderFields().forEach((key, values) -> {
                if (key != null && values != null && !values.isEmpty()) {
                    responseHeaders.put(key, String.join(", ", values));
                }
            });
            
            // Read response body
            String responseBody;
            final InputStream rawStream = connection.getInputStream();
            final InputStream inputStream = "gzip".equalsIgnoreCase(connection.getContentEncoding())
                    ? new GZIPInputStream(rawStream)
                    : rawStream;
            try (inputStream) {
                responseBody = readStream(inputStream);
            }
            
            // Log request
            Logger.debug("HTTP " + data.method + " " + data.url + " -> " + responseCode + " (" + responseTime + "ms)");
            
            return new Response(responseCode, responseBody, responseHeaders, responseTime);
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            Logger.error("HTTP request failed: " + data.url + " - " + e.getMessage());
            return new Response(e.getMessage(), responseTime);
        }
    }
    
    /**
     * Упрощенный GET запрос.
     * <p>
     * Simplified GET request.
     * 
     * @param url целевой URL
     * @return объект Response
     */
    public static Response get(String url) {
        return sendReq(new RequestData(url).method(Method.GET));
    }
    
    /**
     * Упрощенный POST запрос.
     * <p>
     * Simplified POST request.
     * 
     * @param url целевой URL
     * @param body тело запроса
     * @return объект Response
     */
    public static Response post(String url, String body) {
        return sendReq(new RequestData(url).method(Method.POST).body(body));
    }
    
    /**
     * POST запрос с JSON телом.
     * <p>
     * POST request with JSON body.
     * 
     * @param url целевой URL
     * @param json JSON строка
     * @return объект Response
     */
    public static Response postJson(String url, String json) {
        return sendReq(new RequestData(url)
            .method(Method.POST)
            .body(json)
            .header("Content-Type", "application/json"));
    }
    
    /**
     * Проверка доступности URL.
     * <p>
     * Check URL availability.
     * 
     * @param url целевой URL
     * @param timeout таймаут в миллисекундах
     * @return true если доступен
     */
    public static boolean isUrlAvailable(String url, int timeout) {
        long startTime = System.currentTimeMillis();
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            int responseCode = connection.getResponseCode();
            long responseTime = System.currentTimeMillis() - startTime;
            Logger.debug("URL check: " + url + " -> " + responseCode + " (" + responseTime + "ms)");
            return responseCode >= 200 && responseCode < 400;
        } catch (Exception e) {
            Logger.debug("URL check failed: " + url + " - " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Сохранение ответа в файл.
     * <p>
     * Save response to file.
     * 
     * @param response объект Response
     * @param filePath путь к файлу
     * @return true если успешно
     */
    public static boolean saveResponseToFile(Response response, Path filePath) {
        if (response.getBody() == null) {
            return false;
        }
        
        try {
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, response.getBody().getBytes(StandardCharsets.UTF_8));
            Logger.info("Response saved to: " + filePath);
            return true;
        } catch (IOException e) {
            Logger.error("Failed to save response to file: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Чтение InputStream в строку.
     * <p>
     * Read InputStream to string.
     * 
     * @param inputStream входной поток
     * @return строка содержимого
     * @throws IOException при ошибке ввода-вывода
     */
    private static String readStream(InputStream inputStream) throws IOException {
        try (inputStream; ByteArrayOutputStream result = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            return result.toString(StandardCharsets.UTF_8.name());
        }
    }
    
    /**
     * Получение пути для кэширования запросов.
     * <p>
     * Get path for request caching.
     * 
     * @return Path к директории cache в network storage
     */
    public static Path getCachePath() {
        return EthernetUtils.getNetworkStoragePath().resolve("cache");
    }
    
    /**
     * Очистка кэша запросов.
     * <p>
     * Clear request cache.
     * 
     * @return true если успешно
     */
    public static boolean clearCache() {
        try {
            Path cachePath = getCachePath();
            if (Files.exists(cachePath)) {
                try (java.util.stream.Stream<Path> stream = Files.walk(cachePath)) {
                    stream.sorted((a, b) -> -a.compareTo(b))
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                Logger.warn("Failed to delete cache file: " + path);
                            }
                        });
                }
                Logger.info("Request cache cleared");
                return true;
            }
            return true; // Cache doesn't exist, nothing to clear
        } catch (IOException e) {
            Logger.error("Failed to clear cache: " + e.getMessage());
            return false;
        }
    }
}