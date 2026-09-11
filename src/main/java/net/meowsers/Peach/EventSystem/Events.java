package net.meowsers.Peach.EventSystem;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public final class Events {

    private Events() {}

    /**
     * Annotation used to mark methods as event listeners.
     * The annotated method MUST have exactly one parameter (the event).
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Subscribe {}

    private record Listener(Object instance, Method method) {}

    private static final Map<Class<?>, List<Listener>> LISTENERS = new HashMap<>();

    /**
     * Registers all instance-level @Subscribe methods on an object.
     *
     * @param listenerInstance The object instance containing @Subscribe methods.
     */
    public static void register(Object listenerInstance) {
        if (listenerInstance == null) return;
        registerMethods(listenerInstance.getClass(), listenerInstance);
    }

    /**
     * Registers all static @Subscribe methods on a Class.
     *
     * @param clazz The class containing static @Subscribe methods.
     */
    public static void register(Class<?> clazz) {
        if (clazz == null) return;
        registerMethods(clazz, null);
    }

    /**
     * Unregisters an object instance and removes its instance listeners.
     *
     * @param listenerInstance The object instance to unregister.
     */
    public static void unregister(Object listenerInstance) {
        if (listenerInstance == null) return;
        LISTENERS.values().forEach(list ->
                list.removeIf(listener -> listener.instance() == listenerInstance)
        );
    }

    /**
     * Unregisters static listeners for a class.
     *
     * @param clazz The class to unregister static listeners for.
     */
    public static void unregister(Class<?> clazz) {
        if (clazz == null) return;
        LISTENERS.values().forEach(list ->
                list.removeIf(listener -> listener.instance() == null && listener.method().getDeclaringClass() == clazz)
        );
    }

    /**
     * Dispatches an event synchronously to all registered listeners.
     *
     * @param event The event object to post.
     */
    public static void post(Object event) {
        if (event == null) return;

        List<Listener> targets = LISTENERS.get(event.getClass());
        if (targets == null || targets.isEmpty()) return;

        for (Listener listener : targets) {
            try {
                listener.method().invoke(listener.instance(), event);
            } catch (Exception e) {
                System.err.println("Error dispatching event [" + event.getClass().getSimpleName() + "] to "
                        + listener.method().getDeclaringClass().getSimpleName() + "#" + listener.method().getName());
                e.printStackTrace();
            }
        }
    }

    private static void registerMethods(Class<?> clazz, Object instance) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(Subscribe.class)) {
                continue;
            }

            boolean isStatic = Modifier.isStatic(method.getModifiers());

            if (instance == null && !isStatic) {
                continue;
            }
            if (instance != null && isStatic) {
                continue;
            }

            if (method.getParameterCount() != 1) {
                throw new IllegalArgumentException("Method " + clazz.getName() + "#" + method.getName()
                        + " has @Subscribe but must accept exactly 1 event parameter.");
            }

            Class<?> eventType = method.getParameterTypes()[0];
            method.setAccessible(true);

            LISTENERS.computeIfAbsent(eventType, k -> new ArrayList<>())
                    .add(new Listener(instance, method));
        }
    }
}