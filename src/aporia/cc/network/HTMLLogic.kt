package aporia.cc.network

import com.chaos.annotation.Obfuscate
import so.aporia.utils.user.logger.Logger
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Matcher
import java.util.regex.Pattern

@Obfuscate
object HTMLLogic {

    @JvmStatic
    fun saveHTML(filename: String, content: String): Boolean {
        return try {
            val htmlPath = getHTMLStoragePath()
            Files.createDirectories(htmlPath)
            val name = if (!filename.lowercase().endsWith(".html")) "$filename.html" else filename
            val filePath = htmlPath.resolve(name)
            Files.write(filePath, content.toByteArray(StandardCharsets.UTF_8))
            Logger.info("HTML saved: $filePath")
            true
        } catch (e: IOException) {
            Logger.error("Failed to save HTML: ${e.message}")
            false
        }
    }

    @JvmStatic
    fun saveCSS(filename: String, content: String): Boolean {
        return try {
            val cssPath = getCSSStoragePath()
            Files.createDirectories(cssPath)
            val name = if (!filename.lowercase().endsWith(".css")) "$filename.css" else filename
            val filePath = cssPath.resolve(name)
            Files.write(filePath, content.toByteArray(StandardCharsets.UTF_8))
            Logger.info("CSS saved: $filePath")
            true
        } catch (e: IOException) {
            Logger.error("Failed to save CSS: ${e.message}")
            false
        }
    }

    @JvmStatic
    fun readHTML(filename: String): String? {
        return try {
            val htmlPath = getHTMLStoragePath()
            var filePath = htmlPath.resolve(filename)
            if (!Files.exists(filePath) && !filename.lowercase().endsWith(".html")) {
                filePath = htmlPath.resolve("$filename.html")
            }
            if (Files.exists(filePath)) {
                val content = String(Files.readAllBytes(filePath), StandardCharsets.UTF_8)
                Logger.debug("HTML read: $filePath (${content.length} bytes)")
                content
            } else {
                Logger.warn("HTML file not found: $filename")
                null
            }
        } catch (e: IOException) {
            Logger.error("Failed to read HTML: ${e.message}")
            null
        }
    }

    @JvmStatic
    fun readCSS(filename: String): String? {
        return try {
            val cssPath = getCSSStoragePath()
            var filePath = cssPath.resolve(filename)
            if (!Files.exists(filePath) && !filename.lowercase().endsWith(".css")) {
                filePath = cssPath.resolve("$filename.css")
            }
            if (Files.exists(filePath)) {
                val content = String(Files.readAllBytes(filePath), StandardCharsets.UTF_8)
                Logger.debug("CSS read: $filePath (${content.length} bytes)")
                content
            } else {
                Logger.warn("CSS file not found: $filename")
                null
            }
        } catch (e: IOException) {
            Logger.error("Failed to read CSS: ${e.message}")
            null
        }
    }

    @JvmStatic
    fun deleteHTML(filename: String): Boolean {
        return try {
            val htmlPath = getHTMLStoragePath()
            var filePath = htmlPath.resolve(filename)
            if (!Files.exists(filePath) && !filename.lowercase().endsWith(".html")) {
                filePath = htmlPath.resolve("$filename.html")
            }
            if (Files.exists(filePath)) {
                Files.delete(filePath)
                Logger.info("HTML deleted: $filePath")
                true
            } else {
                Logger.warn("HTML file not found for deletion: $filename")
                false
            }
        } catch (e: IOException) {
            Logger.error("Failed to delete HTML: ${e.message}")
            false
        }
    }

    @JvmStatic
    fun deleteCSS(filename: String): Boolean {
        return try {
            val cssPath = getCSSStoragePath()
            var filePath = cssPath.resolve(filename)
            if (!Files.exists(filePath) && !filename.lowercase().endsWith(".css")) {
                filePath = cssPath.resolve("$filename.css")
            }
            if (Files.exists(filePath)) {
                Files.delete(filePath)
                Logger.info("CSS deleted: $filePath")
                true
            } else {
                Logger.warn("CSS file not found for deletion: $filename")
                false
            }
        } catch (e: IOException) {
            Logger.error("Failed to delete CSS: ${e.message}")
            false
        }
    }

    @JvmStatic
    fun createFromTemplate(templateName: String, variables: Map<String, String>): String? {
        val template = getTemplate(templateName) ?: return null
        if (variables.isEmpty()) return template

        var result = template
        for ((key, value) in variables) {
            val pattern = "\\{\\{${Pattern.quote(key)}\\}\\}"
            val replacement = Matcher.quoteReplacement(value)
            val matcher = Pattern.compile(pattern).matcher(result)
            val buffer = StringBuffer()
            while (matcher.find()) {
                matcher.appendReplacement(buffer, replacement)
            }
            matcher.appendTail(buffer)
            result = buffer.toString()
        }
        return result
    }

    @JvmStatic
    fun getTemplate(templateName: String): String? {
        val templates = mapOf(
            "dashboard" to """
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
""".trimIndent(),
            "simple" to """
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
""".trimIndent()
        )
        return templates[templateName.lowercase()]
    }

    @JvmStatic
    fun getDefaultCSS(): String {
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
""".trimIndent()
    }

    @JvmStatic
    fun initializeDefaultFiles(): Boolean {
        var success = true

        if (!saveCSS("styles.css", getDefaultCSS())) {
            success = false
        }

        val vars = mutableMapOf(
            "title" to "Aporia.cc Dashboard",
            "description" to "Network API Control Panel",
            "bot_status" to "Online",
            "requests_count" to "0",
            "chat_messages" to "0",
            "content" to "<p>Welcome to Aporia.cc Network API</p>"
        )

        val dashboard = createFromTemplate("dashboard", vars)
        if (dashboard != null) {
            if (!saveHTML("dashboard.html", dashboard)) success = false
        } else {
            success = false
        }

        vars.clear()
        vars["title"] = "Welcome to Aporia.cc"
        vars["message"] = "Network API is running successfully."
        val simple = createFromTemplate("simple", vars)
        if (simple != null) {
            if (!saveHTML("index.html", simple)) success = false
        } else {
            success = false
        }

        if (success) {
            Logger.success("Default HTML/CSS files initialized")
        } else {
            Logger.warn("Some default files failed to initialize")
        }

        return success
    }

    @JvmStatic
    fun getHTMLStoragePath(): Path = EthernetUtils.getNetworkStoragePath().resolve("html")

    @JvmStatic
    fun getCSSStoragePath(): Path = EthernetUtils.getNetworkStoragePath().resolve("css")

    @JvmStatic
    fun checkFilesExist(): Boolean {
        val htmlPath = getHTMLStoragePath()
        val cssPath = getCSSStoragePath()
        val htmlDirExists = Files.exists(htmlPath) && Files.isDirectory(htmlPath)
        val cssDirExists = Files.exists(cssPath) && Files.isDirectory(cssPath)
        return if (htmlDirExists && cssDirExists) {
            Logger.debug("HTML/CSS directories exist")
            true
        } else {
            Logger.warn("HTML/CSS directories missing")
            false
        }
    }
}
