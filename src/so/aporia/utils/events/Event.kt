package so.aporia.utils.events

class Event<T> {

    private val listeners = mutableListOf<(T) -> Unit>()

    fun register(listener: (T) -> Unit) {
        listeners.add(listener)
    }

    fun fire(event: T) {
        for (l in listeners) l(event)
    }
}
