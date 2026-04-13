/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package aporia.cc.network;

import so.aporia.utils.user.logger.Logger;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ThreadManager — управление потоками с таймаутом неактивности.
 * <p>
 * ThreadManager — thread management with inactivity timeout.
 * <p>
 * Создает пулы потоков, которые автоматически закрывают потоки
 * после 5 минут неактивности для экономии ресурсов.
 * <p>
 * Creates thread pools that automatically close threads
 * after 5 minutes of inactivity to save resources.
 */
public class ThreadManager {
    
    private static final int CORE_POOL_SIZE = 2;
    private static final int MAX_POOL_SIZE = 10;
    private static final long KEEP_ALIVE_TIME = 5 * 60; // 5 minutes in seconds
    private static final TimeUnit KEEP_ALIVE_UNIT = TimeUnit.SECONDS;
    
    private static ThreadPoolExecutor backgroundExecutor;
    private static ScheduledExecutorService scheduledExecutor;
    
    static {
        init();
    }
    
    /**
     * Инициализация менеджера потоков.
     * <p>
     * Initialize thread manager.
     */
    private static void init() {
        // Create background thread pool with keep-alive time
        backgroundExecutor = new ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAX_POOL_SIZE,
            KEEP_ALIVE_TIME,
            KEEP_ALIVE_UNIT,
            new LinkedBlockingQueue<>(100),
            new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(1);
                
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, "aporia-network-thread-" + counter.getAndIncrement());
                    thread.setDaemon(true);
                    thread.setPriority(Thread.NORM_PRIORITY);
                    return thread;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        // Allow core threads to timeout (so they can be destroyed after keep-alive time)
        backgroundExecutor.allowCoreThreadTimeOut(true);
        
        // Create scheduled executor for periodic tasks
        scheduledExecutor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(1);
            
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "aporia-network-scheduler-" + counter.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        });
        
        Logger.debug("ThreadManager initialized with keep-alive: " + KEEP_ALIVE_TIME + " seconds");
    }
    
    /**
     * Выполнение задачи в фоновом потоке.
     * <p>
     * Execute task in background thread.
     * 
     * @param task задача для выполнения
     * @return Future для отслеживания результата
     */
    public static Future<?> submit(Runnable task) {
        if (backgroundExecutor.isShutdown()) {
            Logger.warn("ThreadManager executor is shutdown, executing in caller thread");
            task.run();
            return CompletableFuture.completedFuture(null);
        }
        return backgroundExecutor.submit(task);
    }
    
    /**
     * Выполнение задачи с возвращаемым значением.
     * <p>
     * Execute task with return value.
     * 
     * @param <T> тип возвращаемого значения
     * @param task задача для выполнения
     * @return Future с результатом
     */
    public static <T> Future<T> submit(Callable<T> task) {
        if (backgroundExecutor.isShutdown()) {
            Logger.warn("ThreadManager executor is shutdown, executing in caller thread");
            try {
                T result = task.call();
                return CompletableFuture.completedFuture(result);
            } catch (Exception e) {
                CompletableFuture<T> future = new CompletableFuture<>();
                future.completeExceptionally(e);
                return future;
            }
        }
        return backgroundExecutor.submit(task);
    }
    
    /**
     * Планирование периодической задачи.
     * <p>
     * Schedule periodic task.
     * 
     * @param task задача для выполнения
     * @param initialDelay начальная задержка
     * @param period период выполнения
     * @param unit единица времени
     * @return ScheduledFuture для управления задачей
     */
    public static ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        return scheduledExecutor.scheduleAtFixedRate(task, initialDelay, period, unit);
    }
    
    /**
     * Планирование задачи с задержкой.
     * <p>
     * Schedule delayed task.
     * 
     * @param task задача для выполнения
     * @param delay задержка
     * @param unit единица времени
     * @return ScheduledFuture для управления задачей
     */
    public static ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        return scheduledExecutor.schedule(task, delay, unit);
    }
    
    /**
     * Получение статистики пула потоков.
     * <p>
     * Get thread pool statistics.
     * 
     * @return строка со статистикой
     */
    public static String getStats() {
        if (backgroundExecutor.isShutdown()) {
            return "ThreadManager executor is shutdown";
        }
        
        return String.format(
            "ThreadPool: [active=%d, pool=%d, core=%d, max=%d, queue=%d, completed=%d]",
            backgroundExecutor.getActiveCount(),
            backgroundExecutor.getPoolSize(),
            backgroundExecutor.getCorePoolSize(),
            backgroundExecutor.getMaximumPoolSize(),
            backgroundExecutor.getQueue().size(),
            backgroundExecutor.getCompletedTaskCount()
        );
    }
    
    /**
     * Остановка менеджера потоков.
     * <p>
     * Shutdown thread manager.
     */
    public static void shutdown() {
        Logger.debug("Shutting down ThreadManager...");
        
        if (backgroundExecutor != null && !backgroundExecutor.isShutdown()) {
            backgroundExecutor.shutdown();
            try {
                if (!backgroundExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    backgroundExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                backgroundExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        if (scheduledExecutor != null && !scheduledExecutor.isShutdown()) {
            scheduledExecutor.shutdown();
            try {
                if (!scheduledExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    scheduledExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduledExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        Logger.debug("ThreadManager shutdown complete");
    }
    
    /**
     * Перезапуск менеджера потоков.
     * <p>
     * Restart thread manager.
     */
    public static void restart() {
        shutdown();
        init();
        Logger.debug("ThreadManager restarted");
    }
    
    /**
     * Проверка, активен ли менеджер потоков.
     * <p>
     * Check if thread manager is active.
     * 
     * @return true если активен
     */
    public static boolean isActive() {
        return backgroundExecutor != null && !backgroundExecutor.isShutdown() &&
               scheduledExecutor != null && !scheduledExecutor.isShutdown();
    }
}