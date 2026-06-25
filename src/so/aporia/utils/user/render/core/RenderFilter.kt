package so.aporia.utils.user.render.core

import net.minecraft.world.entity.Entity
import java.util.function.Predicate

object RenderFilter {

    private var entityFilter: Predicate<Entity>? = null

    @JvmStatic
    fun setEntityFilter(filter: Predicate<Entity>?) {
        entityFilter = filter
    }

    @JvmStatic
    fun clearEntityFilter() {
        entityFilter = null
    }

    @JvmStatic
    fun shouldSkip(entity: Entity): Boolean {
        return entityFilter?.test(entity) == true
    }
}
