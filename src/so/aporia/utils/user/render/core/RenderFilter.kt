package so.aporia.utils.user.render.core
import net.minecraft.world.entity.Entity
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Predicate
import com.chaos.annotation.ChaosNative
@ChaosNative
object RenderFilter {

    private val filters = CopyOnWriteArrayList<Predicate<Entity>>()

    @JvmStatic
    fun addFilter(filter: Predicate<Entity>) {
        filters.add(filter)
    }

    @JvmStatic
    fun removeFilter(filter: Predicate<Entity>) {
        filters.remove(filter)
    }

    @JvmStatic
    fun clearFilters() {
        filters.clear()
    }

    @JvmStatic
    fun shouldSkip(entity: Entity): Boolean {
        return filters.any { it.test(entity) }
    }
}