package aporia.cc.network
import com.chaos.annotation.Obfuscate
import so.aporia.utils.user.logger.Logger
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import com.chaos.annotation.ChaosNative
@Obfuscate
@ChaosNative
object ThreadManager {

    private const val CORE_POOL_SIZE = 2
    private const val MAX_POOL_SIZE = 10
    private const val KEEP_ALIVE_TIME = 5 * 60L
    private val KEEP_ALIVE_UNIT = TimeUnit.SECONDS

    private lateinit var backgroundExecutor: ThreadPoolExecutor
    private lateinit var scheduledExecutor: ScheduledExecutorService

    init {
        init()
    }

    private fun init() {
        backgroundExecutor = ThreadPoolExecutor(
            CORE_POOL_SIZE, MAX_POOL_SIZE,
            KEEP_ALIVE_TIME, KEEP_ALIVE_UNIT,
            LinkedBlockingQueue(100),
            object : ThreadFactory {
                private val counter = AtomicInteger(1)
                override fun newThread(r: Runnable): Thread {
                    return Thread(r, "aporia-network-thread-${counter.getAndIncrement()}").apply {
                        isDaemon = true
                        priority = Thread.NORM_PRIORITY
                    }
                }
            },
            ThreadPoolExecutor.CallerRunsPolicy()
        )
        backgroundExecutor.allowCoreThreadTimeOut(true)

        scheduledExecutor = Executors.newScheduledThreadPool(1, object : ThreadFactory {
            private val counter = AtomicInteger(1)
            override fun newThread(r: Runnable): Thread {
                return Thread(r, "aporia-network-scheduler-${counter.getAndIncrement()}").apply {
                    isDaemon = true
                }
            }
        })

        Logger.debug("ThreadManager initialized with keep-alive: $KEEP_ALIVE_TIME seconds")
    }

    @JvmStatic
    fun submit(task: Runnable): Future<*> {
        if (backgroundExecutor.isShutdown) {
            Logger.warn("ThreadManager executor is shutdown, executing in caller thread")
            task.run()
            return CompletableFuture.completedFuture(null)
        }
        return backgroundExecutor.submit(task)
    }

    @JvmStatic
    fun <T> submit(task: Callable<T>): Future<T> {
        if (backgroundExecutor.isShutdown) {
            Logger.warn("ThreadManager executor is shutdown, executing in caller thread")
            return try {
                CompletableFuture.completedFuture(task.call())
            } catch (e: Exception) {
                CompletableFuture<T>().apply { completeExceptionally(e) }
            }
        }
        return backgroundExecutor.submit(task)
    }

    @JvmStatic
    fun scheduleAtFixedRate(task: Runnable, initialDelay: Long, period: Long, unit: TimeUnit): ScheduledFuture<*> {
        return scheduledExecutor.scheduleAtFixedRate(task, initialDelay, period, unit)
    }

    @JvmStatic
    fun schedule(task: Runnable, delay: Long, unit: TimeUnit): ScheduledFuture<*> {
        return scheduledExecutor.schedule(task, delay, unit)
    }

    @JvmStatic
    fun getStats(): String {
        if (backgroundExecutor.isShutdown) return "ThreadManager executor is shutdown"
        return String.format(
            "ThreadPool: [active=%d, pool=%d, core=%d, max=%d, queue=%d, completed=%d]",
            backgroundExecutor.activeCount, backgroundExecutor.poolSize,
            backgroundExecutor.corePoolSize, backgroundExecutor.maximumPoolSize,
            backgroundExecutor.queue.size, backgroundExecutor.completedTaskCount
        )
    }

    @JvmStatic
    fun shutdown() {
        Logger.debug("Shutting down ThreadManager...")
        if (::backgroundExecutor.isInitialized && !backgroundExecutor.isShutdown) {
            backgroundExecutor.shutdown()
            try {
                if (!backgroundExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    backgroundExecutor.shutdownNow()
                }
            } catch (e: InterruptedException) {
                backgroundExecutor.shutdownNow()
                Thread.currentThread().interrupt()
            }
        }
        if (::scheduledExecutor.isInitialized && !scheduledExecutor.isShutdown) {
            scheduledExecutor.shutdown()
            try {
                if (!scheduledExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    scheduledExecutor.shutdownNow()
                }
            } catch (e: InterruptedException) {
                scheduledExecutor.shutdownNow()
                Thread.currentThread().interrupt()
            }
        }
        Logger.debug("ThreadManager shutdown complete")
    }

    @JvmStatic
    fun restart() {
        shutdown()
        init()
        Logger.debug("ThreadManager restarted")
    }

    @JvmStatic
    fun isActive(): Boolean {
        return ::backgroundExecutor.isInitialized && !backgroundExecutor.isShutdown &&
                ::scheduledExecutor.isInitialized && !scheduledExecutor.isShutdown
    }
}