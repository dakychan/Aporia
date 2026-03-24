package so.aporia.utils.events;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a method as an event handler.
 * The method must have exactly one parameter — the event type.
 * Register the containing object via {@link EventBus#register(Object)}.
 *
 * <pre>{@code
 * @EventHandler
 * public void onScroll(MouseScrollEvent e) { ... }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EventHandler {}
