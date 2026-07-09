package so.aporia.utils.events
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import com.chaos.annotation.ChaosNative
@ChaosNative
object EventBus {

    private val listeners = ConcurrentHashMap<Class<*>, MutableList<Listener>>()

    fun register(obj: Any) {
        for (m in obj.javaClass.methods) {
            if (!m.isAnnotationPresent(EventHandler::class.java)) continue
            if (m.parameterCount != 1) continue
            val eventType = m.parameterTypes[0]
            listeners.computeIfAbsent(eventType) { CopyOnWriteArrayList() }
                .add(Listener(obj, m))
        }
    }

    fun unregister(obj: Any) {
        listeners.values.forEach { list -> list.removeAll { it.instance == obj } }
    }

    fun post(event: Any) {
        val list = listeners[event.javaClass] ?: return
        for (l in list) {
            try {
                l.method.invoke(l.instance, event)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private data class Listener(val instance: Any, val method: Method)
}