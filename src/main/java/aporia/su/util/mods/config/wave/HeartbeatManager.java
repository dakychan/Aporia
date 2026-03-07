package aporia.su.util.mods.config.wave;

import aporia.su.util.interfaces.IMinecraft;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * HeartbeatManager - отправляет периодические сообщения в чат с градиентом.
 * 
 * <p>Особенности:</p>
 * <ul>
 *   <li>Градиент оранжевого цвета</li>
 *   <li>Разные фразы на русском и английском</li>
 *   <li>Кавайные смайлики (●'◡'●)</li>
 *   <li>Интервал 20-40 минут</li>
 * </ul>
 */
public class HeartbeatManager implements IMinecraft {
    
    private static HeartbeatManager instance;
    private static ScheduledExecutorService scheduler;
    private static final Random random = new Random();
    
    /** Фразы на русском */
    private static final String[] PHRASES_RU = {
        "я тут (●'◡'●)",
        "я жив ♡(ӦｖӦ｡)",
        "я ахуенен (◕‿◕✿)",
        "все норм (｡◕‿◕｡)",
        "работаю (◠‿◠)",
        "на связи ヾ(≧▽≦*)o",
        "все четко (｡♥‿♥｡)",
        "я здесь (◕ᴗ◕✿)",
        "живой (◠ω◠✿)",
        "в деле (◕‿◕)♡"
    };
    
    /** Фразы на английском */
    private static final String[] PHRASES_EN = {
        "i'm here (●'◡'●)",
        "i'm alive ♡(ӦｖӦ｡)",
        "i'm awesome (◕‿◕✿)",
        "all good (｡◕‿◕｡)",
        "working (◠‿◠)",
        "online ヾ(≧▽≦*)o",
        "all clear (｡♥‿♥｡)",
        "i'm present (◕ᴗ◕✿)",
        "alive (◠ω◠✿)",
        "in action (◕‿◕)♡"
    };
    
    /** Градиент оранжевого (от светлого к темному) */
    private static final int[] ORANGE_GRADIENT = {
        0xFFFFCC99, // Светло-оранжевый
        0xFFFFBB77,
        0xFFFFAA55,
        0xFFFF9933,
        0xFFFF8811,
        0xFFEE7700,
        0xFFDD6600,
        0xFFCC5500  // Темно-оранжевый
    };
    
    private HeartbeatManager() {}
    
    /**
     * Получить экземпляр HeartbeatManager.
     */
    public static HeartbeatManager getInstance() {
        if (instance == null) {
            instance = new HeartbeatManager();
        }
        return instance;
    }
    
    /**
     * Запустить heartbeat систему.
     */
    public static void start() {
        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }
        
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "Aporia-Heartbeat");
            thread.setDaemon(true);
            return thread;
        });
        
        /** Первое сообщение через 20-40 минут */
        long initialDelay = getRandomDelay();
        
        scheduler.schedule(() -> {
            try {
                sendHeartbeat();
            } catch (Exception e) {
                /** Игнорируем ошибки */
            } finally {
                /** Всегда планируем следующее сообщение */
                scheduleNext();
            }
        }, initialDelay, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Остановить heartbeat систему.
     */
    public static void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }
    }
    
    /**
     * Запланировать следующий heartbeat.
     */
    private static void scheduleNext() {
        if (scheduler == null || scheduler.isShutdown()) {
            return;
        }
        
        long delay = getRandomDelay();
        
        scheduler.schedule(() -> {
            try {
                sendHeartbeat();
            } catch (Exception e) {
                /** Игнорируем ошибки */
            } finally {
                /** Всегда планируем следующее сообщение */
                scheduleNext();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Получить случайную задержку между 20 и 40 минутами.
     */
    private static long getRandomDelay() {
        /** 20 минут = 1,200,000 мс, 40 минут = 2,400,000 мс */
        int minDelay = 20 * 60 * 1000;
        int maxDelay = 40 * 60 * 1000;
        return minDelay + random.nextInt(maxDelay - minDelay);
    }
    
    /**
     * Отправить heartbeat сообщение в чат (локально, не на сервер).
     */
    private static void sendHeartbeat() {
        try {
            if (mc == null || mc.player == null || mc.world == null || mc.inGameHud == null) {
                return;
            }
            
            /** Определяем язык (по умолчанию русский) */
            String language = mc.options.language;
            boolean isRussian = language == null || language.startsWith("ru") || language.equals("ru_ru");
            
            /** Выбираем случайную фразу */
            String[] phrases = isRussian ? PHRASES_RU : PHRASES_EN;
            String phrase = phrases[random.nextInt(phrases.length)];
            
            /** Отправляем в главном потоке (render thread) */
            mc.execute(() -> {
                try {
                    /** Префикс клиента */
                    MutableText prefix = Text.literal("[Dak AI] ").formatted(Formatting.GRAY);
                    
                    /** Создаем текст с градиентом */
                    MutableText gradientText = createSmoothGradientText(phrase);
                    
                    /** Объединяем префикс и градиентный текст */
                    MutableText fullMessage = Text.empty().append(prefix).append(gradientText);
                    
                    /** Отправляем в чат клиента (не на сервер) */
                    if (mc.inGameHud != null && mc.inGameHud.getChatHud() != null) {
                        mc.inGameHud.getChatHud().addMessage(fullMessage);
                    }
                } catch (Exception e) {
                    /** Игнорируем ошибки */
                }
            });
            
        } catch (Exception e) {
            /** Игнорируем ошибки */
        }
    }
    
    /**
     * Создать текст с оранжевым градиентом.
     */
    private static MutableText createGradientText(String text) {
        MutableText result = Text.empty();
        
        int length = text.length();
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            
            /** Вычисляем позицию в градиенте */
            float progress = (float) i / (length - 1);
            int colorIndex = (int) (progress * (ORANGE_GRADIENT.length - 1));
            colorIndex = Math.min(colorIndex, ORANGE_GRADIENT.length - 1);
            
            int color = ORANGE_GRADIENT[colorIndex];
            
            /** Создаем стиль с цветом */
            Style style = Style.EMPTY.withColor(color);
            
            /** Добавляем символ с цветом */
            result.append(Text.literal(String.valueOf(c)).setStyle(style));
        }
        
        return result;
    }
    
    /**
     * Создать текст с плавным градиентом (интерполяция между цветами).
     */
    private static MutableText createSmoothGradientText(String text) {
        MutableText result = Text.empty();
        
        int length = text.length();
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            
            /** Вычисляем позицию в градиенте */
            float progress = (float) i / (length - 1);
            
            /** Интерполируем между цветами */
            int color = interpolateColor(progress);
            
            /** Создаем стиль с цветом */
            Style style = Style.EMPTY.withColor(color);
            
            /** Добавляем символ с цветом */
            result.append(Text.literal(String.valueOf(c)).setStyle(style));
        }
        
        return result;
    }
    
    /**
     * Интерполировать цвет в градиенте.
     */
    private static int interpolateColor(float progress) {
        progress = Math.max(0, Math.min(1, progress));
        
        /** Находим два соседних цвета */
        float scaledProgress = progress * (ORANGE_GRADIENT.length - 1);
        int index1 = (int) scaledProgress;
        int index2 = Math.min(index1 + 1, ORANGE_GRADIENT.length - 1);
        
        float localProgress = scaledProgress - index1;
        
        int color1 = ORANGE_GRADIENT[index1];
        int color2 = ORANGE_GRADIENT[index2];
        
        /** Интерполируем RGB компоненты */
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        
        int r = (int) (r1 + (r2 - r1) * localProgress);
        int g = (int) (g1 + (g2 - g1) * localProgress);
        int b = (int) (b1 + (b2 - b1) * localProgress);
        
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
