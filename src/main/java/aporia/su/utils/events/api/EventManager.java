package aporia.su.utils.events.api;

import aporia.su.utils.events.api.Event.EventHandler;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Менеджер событий - система для регистрации и вызова событий.
 * Поддерживает два способа регистрации:
 * 1. Через аннотацию @EventHandler на методах
 * 2. Через прямую регистрацию Consumer'ов
 * 
 * @author Aporia
 */
public class EventManager {
    private static final Map<Class<? extends Event>, List<EventListener>> listeners = new HashMap<>();
    
    /**
     * Регистрирует все методы с аннотацией @EventHandler в указанном объекте.
     * 
     * @param listener объект, содержащий методы-обработчики событий
     */
    public static void registerListener(Object listener) {
        Class<?> clazz = listener.getClass();
        
        for (Method method : clazz.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(EventHandler.class)) {
                continue;
            }
            
            // Проверяем что метод принимает ровно один параметр типа Event
            if (method.getParameterCount() != 1) {
                System.err.println("[EventManager] Method " + method.getName() + " has @EventHandler but doesn't have exactly 1 parameter!");
                continue;
            }
            
            Class<?> paramType = method.getParameterTypes()[0];
            if (!Event.class.isAssignableFrom(paramType)) {
                System.err.println("[EventManager] Method " + method.getName() + " has @EventHandler but parameter is not an Event!");
                continue;
            }
            
            @SuppressWarnings("unchecked")
            Class<? extends Event> eventClass = (Class<? extends Event>) paramType;
            EventHandler annotation = method.getAnnotation(EventHandler.class);
            int priority = annotation.priority();
            
            method.setAccessible(true);
            
            EventListener eventListener = new EventListener(listener, method, priority);
            listeners.computeIfAbsent(eventClass, k -> new ArrayList<>()).add(eventListener);
            
            // Сортируем по приоритету (меньше = выше приоритет)
            listeners.get(eventClass).sort(Comparator.comparingInt(l -> l.priority));
        }
    }
    
    /**
     * Вызывает событие, уведомляя всех зарегистрированных слушателей.
     * 
     * @param <T> тип события
     * @param event экземпляр события для вызова
     */
    public static <T extends Event> void callEvent(T event) {
        List<EventListener> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null) {
            for (EventListener listener : eventListeners) {
                try {
                    listener.method.invoke(listener.instance, event);
                } catch (Exception e) {
                    System.err.println("[EventManager] Error calling event handler: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Отменяет регистрацию всех обработчиков из указанного объекта.
     * 
     * @param listener объект для отмены регистрации
     */
    public static void unregisterListener(Object listener) {
        listeners.values().forEach(list -> list.removeIf(l -> l.instance == listener));
    }
    
    /**
     * Внутренний класс для хранения информации об обработчике события.
     */
    private static class EventListener {
        final Object instance;
        final Method method;
        final int priority;
        
        EventListener(Object instance, Method method, int priority) {
            this.instance = instance;
            this.method = method;
            this.priority = priority;
        }
    }
}
