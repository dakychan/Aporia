package aporia.cc.network
import com.chaos.annotation.Obfuscate
import so.aporia.utils.user.logger.Logger
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.GZIPInputStream
import com.chaos.annotation.ChaosNative
@Obfuscate
@ChaosNative
object Requests {

    enum class Method {
        GET, POST, PUT, DELETE, PATCH
    }

    class RequestData(var url: String) {
        var method: Method = Method.GET
        val headers: MutableMap<String, String> = mutableMapOf()
        var body: String? = null
        var timeout: Int = 10000
        var followRedirects: Boolean = true
        var gzip: Boolean = true

        fun method(method: Method) = apply { this.method = method }
        fun header(key: String, value: String) = apply { headers[key] = value }
        fun headers(headers: Map<String, String>) = apply { this.headers.putAll(headers) }
        fun body(body: String) = apply { this.body = body }
        fun timeout(timeout: Int) = apply { this.timeout = timeout }
        fun followRedirects(followRedirects: Boolean) = apply { this.followRedirects = followRedirects }
        fun gzip(gzip: Boolean) = apply { this.gzip = gzip }
    }

    class Response private constructor(
        val statusCode: Int,
        val body: String?,
        val headers: Map<String, String>,
        val responseTime: Long,
        val success: Boolean,
        val errorMessage: String?
    ) {
        constructor(statusCode: Int, body: String, headers: Map<String, String>, responseTime: Long) :
                this(statusCode, body, headers, responseTime, statusCode in 200 until 300, null)

        constructor(errorMessage: String, responseTime: Long) :
                this(0, null, emptyMap(), responseTime, false, errorMessage)

        override fun toString(): String {
            return if (success) {
                "Response{status=$statusCode, bodyLength=${body?.length ?: 0}, time=${responseTime}ms}"
            } else {
                "Response{error=\"$errorMessage\", time=${responseTime}ms}"
            }
        }

        companion object {
            @JvmStatic
            fun success(statusCode: Int, body: String, headers: Map<String, String>, responseTime: Long): Response {
                return Response(statusCode, body, headers, responseTime)
            }

            @JvmStatic
            fun error(errorMessage: String, responseTime: Long): Response {
                return Response(errorMessage, responseTime)
            }
        }
    }

    @JvmStatic
    fun sendReq(data: RequestData): Response {
        val startTime = System.currentTimeMillis()

        return try {
            val url = URI(data.url).toURL()
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = data.method.name
            connection.connectTimeout = data.timeout
            connection.readTimeout = data.timeout
            connection.instanceFollowRedirects = data.followRedirects

            if (data.gzip) {
                connection.setRequestProperty("Accept-Encoding", "gzip")
            }
            for ((key, value) in data.headers) {
                connection.setRequestProperty(key, value)
            }

            if (data.body != null && data.method in listOf(Method.POST, Method.PUT, Method.PATCH)) {
                connection.doOutput = true
                connection.outputStream.use { os ->
                    os.write(data.body!!.toByteArray(StandardCharsets.UTF_8))
                }
            }

            val responseCode = connection.responseCode
            val responseTime = System.currentTimeMillis() - startTime

            val responseHeaders = mutableMapOf<String, String>()
            connection.headerFields.forEach { (key, values) ->
                if (key != null && values != null && values.isNotEmpty()) {
                    responseHeaders[key] = values.joinToString(", ")
                }
            }

            val rawStream: InputStream = connection.inputStream
            val inputStream: InputStream = if ("gzip".equals(connection.contentEncoding, ignoreCase = true)) {
                GZIPInputStream(rawStream)
            } else {
                rawStream
            }

            val responseBody = inputStream.use { readStream(it) }

            Logger.debug("HTTP ${data.method} ${data.url} -> $responseCode (${responseTime}ms)")

            Response.success(responseCode, responseBody, responseHeaders, responseTime)

        } catch (e: Exception) {
            val responseTime = System.currentTimeMillis() - startTime
            Logger.error("HTTP request failed: ${data.url} - ${e.message}")
            Response.error(e.message ?: "Unknown error", responseTime)
        }
    }

    @JvmStatic
    fun get(url: String): Response {
        return sendReq(RequestData(url).method(Method.GET))
    }

    @JvmStatic
    fun post(url: String, body: String): Response {
        return sendReq(RequestData(url).method(Method.POST).body(body))
    }

    @JvmStatic
    fun postJson(url: String, json: String): Response {
        return sendReq(RequestData(url)
            .method(Method.POST)
            .body(json)
            .header("Content-Type", "application/json"))
    }

    @JvmStatic
    fun isUrlAvailable(url: String, timeout: Int): Boolean {
        return try {
            val connection = URI(url).toURL().openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = timeout
            connection.readTimeout = timeout
            val responseCode = connection.responseCode
            val responseTime = System.currentTimeMillis() - System.currentTimeMillis()
            Logger.debug("URL check: $url -> $responseCode (${responseTime}ms)")
            responseCode in 200 until 400
        } catch (e: Exception) {
            Logger.debug("URL check failed: $url - ${e.message}")
            false
        }
    }

    @JvmStatic
    fun saveResponseToFile(response: Response, filePath: Path): Boolean {
        if (response.body == null) return false

        return try {
            Files.createDirectories(filePath.parent)
            Files.write(filePath, response.body.toByteArray(StandardCharsets.UTF_8))
            Logger.info("Response saved to: $filePath")
            true
        } catch (e: IOException) {
            Logger.error("Failed to save response to file: ${e.message}")
            false
        }
    }

    private fun readStream(inputStream: InputStream): String {
        inputStream.use { input ->
            ByteArrayOutputStream().use { result ->
                val buffer = ByteArray(1024)
                var length: Int
                while (input.read(buffer).also { length = it } != -1) {
                    result.write(buffer, 0, length)
                }
                return result.toString(StandardCharsets.UTF_8.name())
            }
        }
    }

    @JvmStatic
    fun getCachePath(): Path = EthernetUtils.getNetworkStoragePath().resolve("cache")

    @JvmStatic
    fun clearCache(): Boolean {
        return try {
            val cachePath = getCachePath()
            if (Files.exists(cachePath)) {
                Files.walk(cachePath)
                    .sorted(Comparator.reverseOrder())
                    .forEach { path ->
                        try {
                            Files.delete(path)
                        } catch (e: IOException) {
                            Logger.warn("Failed to delete cache file: $path")
                        }
                    }
                Logger.info("Request cache cleared")
                true
            } else {
                true
            }
        } catch (e: IOException) {
            Logger.error("Failed to clear cache: ${e.message}")
            false
        }
    }
}