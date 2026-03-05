package aporia.su.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * Runtime маппер для деобфускации stacktrace.
 * <p>
 * Загружает маппинги из встроенной статики (генерируется ChaosObfuscator).
 * Используется только для отладки крашей.
 * </p>
 * 
 * <h3>Использование:</h3>
 * <pre>{@code
 * // Деобфусцировать stacktrace
 * String obfuscated = "at a.b.c.D3f$.method(D3f$.java:42)";
 * String deobfuscated = RuntimeMapper.deobfuscate(obfuscated);
 * 
 * // Деобфусцировать имя класса
 * String className = RuntimeMapper.deobfuscateClass("a/b/c/D3f$");
 * }</pre>
 * 
 * @author protect3ed
 * @version 2.0
 * @since 2.0
 */
public class RuntimeMapper {
    private static final long SEED = 0L;
    private static final Map<String, String> MAPPINGS = new HashMap<>();
    
    /**
     * Деобфусцирует имя класса.
     * 
     * @param obfuscatedName обфусцированное имя
     * @return оригинальное имя или обфусцированное если маппинг не найден
     */
    public static String deobfuscateClass(String obfuscatedName) {
        return MAPPINGS.getOrDefault(obfuscatedName, obfuscatedName);
    }
    
    /**
     * Деобфусцирует строку stacktrace.
     * 
     * @param stacktrace строка stacktrace
     * @return деобфусцированная строка
     */
    public static String deobfuscate(String stacktrace) {
        if (MAPPINGS.isEmpty()) {
            return stacktrace;
        }
        
        String result = stacktrace;
        for (Map.Entry<String, String> entry : MAPPINGS.entrySet()) {
            String obfuscated = entry.getKey().replace("/", ".");
            String original = entry.getValue().replace("/", ".");
            result = result.replace(obfuscated, original);
        }
        
        return result;
    }
    
    /**
     * Деобфусцирует весь stacktrace исключения.
     * 
     * @param throwable исключение
     * @return деобфусцированное описание stacktrace
     */
    public static String deobfuscateStackTrace(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.getClass().getName()).append(": ").append(throwable.getMessage()).append("\n");
        
        for (StackTraceElement element : throwable.getStackTrace()) {
            String className = element.getClassName();
            String deobfuscatedClass = deobfuscateClass(className.replace(".", "/")).replace("/", ".");
            
            sb.append("\tat ").append(deobfuscatedClass)
              .append(".").append(element.getMethodName())
              .append("(").append(element.getFileName())
              .append(":").append(element.getLineNumber())
              .append(")\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Проверяет, загружены ли маппинги.
     * 
     * @return true если маппинги загружены
     */
    public static boolean hasMappings() {
        return !MAPPINGS.isEmpty();
    }
    
    /**
     * Получает количество загруженных маппингов.
     * 
     * @return количество маппингов
     */
    public static int getMappingsCount() {
        return MAPPINGS.size();
    }
    
    /**
     * Получает seed текущего дня.
     * 
     * @return seed
     */
    public static long getSeed() {
        return SEED;
    }
}
