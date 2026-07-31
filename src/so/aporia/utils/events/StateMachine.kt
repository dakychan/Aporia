package so.aporia.utils.events
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions
import com.chaos.annotation.ChaosNative
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class StateMachine

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@kotlin.annotation.Repeatable
annotation class StateMachineRegister(
    val from: String = "",
    val to: String = "",
    val on: KClass<*> = Any::class
)
@ChaosNative
class StateMachineEngine<S : Enum<S>>(
    @PublishedApi internal val owner: Any,
    private val stateClass: KClass<S>,
    initialState: S
) {
    @PublishedApi internal var currentState: S = initialState
    @PublishedApi internal val transitions = mutableMapOf<Pair<S, KClass<*>>, S>()

    init {
        registerAnnotated()
    }

    private fun registerAnnotated() {
        val methods = owner::class.memberFunctions
        for (method in methods) {
            for (ann in method.annotations.filterIsInstance<StateMachineRegister>()) {
                if (ann.from.isNotEmpty() && ann.to.isNotEmpty()) {
                    val fromState = parseState(ann.from) ?: continue
                    val toState = parseState(ann.to) ?: continue
                    val eventType = ann.on
                    transitions[fromState to eventType] = toState
                }
            }
        }
    }

    fun state(): S = currentState

    fun transition(event: Any, vararg args: Any?) {
        val key = currentState to event::class
        val next = transitions[key] ?: return
        currentState = next
        EventBus.post(StateTransitionEvent(owner, currentState, event, args))
    }

    fun isIn(vararg states: S): Boolean = states.any { it == currentState }

    private fun parseState(name: String): S? =
        stateClass.java.enumConstants.find { it.name.equals(name, ignoreCase = true) }

    data class StateTransitionEvent(
        val source: Any,
        val newState: Enum<*>,
        val trigger: Any,
        val args: Array<out Any?> = emptyArray()
    )
}