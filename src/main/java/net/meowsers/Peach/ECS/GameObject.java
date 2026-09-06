package net.meowsers.Peach.ECS;

import net.meowsers.Peach.ECS.Components.BehaviorComponent;
import net.meowsers.Peach.ECS.Components.TransformComponent;
import net.meowsers.Peach.GameEngine.PeachLevel;
import net.meowsers.Peach.Utils.PeachException;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class GameObject {

    private final List<Component> components;
    public TransformComponent transform;

    public String name;
    private boolean started = false;
    private boolean active = true;
    private PeachLevel level;

    public GameObject(String name) {
        this.name = name;
        this.components = new ArrayList<>();
        this.transform = new TransformComponent();
        addComponent(this.transform);
    }

    public GameObject() {
        this("GameObject");
    }

    public GameObject(String name, Component... components) {
        this(name);
        if (components != null) {
            for (Component c : components) {
                if (c != null) {
                    addComponent(c);
                }
            }
        }
    }

    public GameObject(Component... components) {
        this("GameObject", components);
    }

    public static GameObject create(String name, Component... components) {
        return new GameObject(name, components);
    }

    public static GameObject create(Component... components) {
        return new GameObject("GameObject", components);
    }

    public GameObject with(Component... components) {
        if (components != null) {
            for (Component c : components) {
                if (c != null) {
                    addComponent(c);
                }
            }
        }
        return this;
    }

    public GameObject withComponent(Component component) {
        if (component != null) {
            addComponent(component);
        }
        return this;
    }

    public <T extends Component> GameObject with(Class<T> componentClass, Object... args) {
        addComponent(componentClass, args);
        return this;
    }

    public void setLevel(PeachLevel level) {
        this.level = level;
    }

    public PeachLevel getLevel() {
        return level;
    }

    public BehaviorComponent addBehavior(ComponentUpdater updater) {
        if (updater == null) return null;
        BehaviorComponent comp = new BehaviorComponent(updater);
        return addComponent(comp);
    }

    public BehaviorComponent addBehavior(ComponentBehavior behavior) {
        if (behavior == null) return null;
        BehaviorComponent comp = new BehaviorComponent(behavior);
        return addComponent(comp);
    }

    public GameObject onUpdate(ComponentUpdater updater) {
        if (updater != null) {
            addBehavior(updater);
        }
        return this;
    }

    public GameObject onUpdate(ComponentBehavior behavior) {
        if (behavior != null) {
            addBehavior(behavior);
        }
        return this;
    }

    public GameObject onStart(Runnable onStart) {
        if (onStart != null) {
            BehaviorComponent comp = new BehaviorComponent();
            comp.onStart(onStart);
            addComponent(comp);
        }
        return this;
    }

    public GameObject onStart(Consumer<GameObject> onStart) {
        if (onStart != null) {
            BehaviorComponent comp = new BehaviorComponent();
            comp.onStart(onStart);
            addComponent(comp);
        }
        return this;
    }

    public GameObject onDestroy(Runnable onDestroy) {
        if (onDestroy != null) {
            BehaviorComponent comp = new BehaviorComponent();
            comp.onDestroy(onDestroy);
            addComponent(comp);
        }
        return this;
    }

    public GameObject onDestroy(Consumer<GameObject> onDestroy) {
        if (onDestroy != null) {
            BehaviorComponent comp = new BehaviorComponent();
            comp.onDestroy(onDestroy);
            addComponent(comp);
        }
        return this;
    }

    public <T extends Component> T addComponent(T component) {
        if (component == null) return null;
        component.setGameObject(this);
        if (component instanceof TransformComponent) {
            this.transform = (TransformComponent) component;
        }
        if (!components.contains(component)) {
            components.add(component);
        }
        component.onAdded();
        if (started && active && component.isEnabled()) {
            component.start();
        }
        return component;
    }

    public <T extends Component> T addComponent(Class<T> componentClass) {
        if (componentClass == null) return null;
        T instance = instantiateComponent(componentClass);
        return addComponent(instance);
    }

    public <T extends Component> T addComponent(Class<T> componentClass, Object... args) {
        if (componentClass == null) return null;
        if (args == null || args.length == 0) {
            return addComponent(componentClass);
        }
        T instance = instantiateComponent(componentClass, args);
        return addComponent(instance);
    }

    public GameObject addComponents(Component... components) {
        if (components != null) {
            for (Component c : components) {
                if (c != null) {
                    addComponent(c);
                }
            }
        }
        return this;
    }

    public GameObject addComponents(Iterable<? extends Component> components) {
        if (components != null) {
            for (Component c : components) {
                if (c != null) {
                    addComponent(c);
                }
            }
        }
        return this;
    }

    public <T extends Component> T getOrAddComponent(Class<T> componentClass) {
        T existing = getComponent(componentClass);
        if (existing != null) {
            return existing;
        }
        return addComponent(componentClass);
    }

    public <T extends Component> T getOrCreateComponent(Class<T> componentClass) {
        return getOrAddComponent(componentClass);
    }

    public boolean removeComponent(Component component) {
        if (component == null) return false;
        boolean removed = components.remove(component);
        if (removed) {
            component.onRemoved();
            component.onDestroy();
            component.destroy();
            component.setGameObject(null);
        }
        return removed;
    }

    public <T extends Component> boolean removeComponent(Class<T> componentClass) {
        T component = getComponent(componentClass);
        if (component != null) {
            return removeComponent(component);
        }
        return false;
    }

    public <T extends Component> boolean hasComponent(Class<T> componentClass) {
        return getComponent(componentClass) != null;
    }

    public boolean hasComponent(String name) {
        return getComponentByName(name) != null;
    }

    @SuppressWarnings("unchecked")
    public <T extends Component> T getComponent(Class<T> componentClass) {
        if (componentClass == null) return null;
        for (Component c : components) {
            if (componentClass.isInstance(c)) {
                return (T) c;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T extends Component> List<T> getComponents(Class<T> componentClass) {
        if (componentClass == null) return Collections.emptyList();
        List<T> result = new ArrayList<>();
        for (Component c : components) {
            if (componentClass.isInstance(c)) {
                result.add((T) c);
            }
        }
        return result;
    }

    public Component getComponentByName(String name) {
        if (name == null) return null;
        for (Component component : components) {
            if (Objects.equals(component.getName(), name) ||
                    component.getClass().getSimpleName().equalsIgnoreCase(name)) {
                return component;
            }
        }
        return null;
    }

    public List<Component> getComponents() {
        return components;
    }

    public void clearComponents() {
        for (Component component : new ArrayList<>(components)) {
            removeComponent(component);
        }
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isStarted() {
        return started;
    }

    public void start() {
        if (!active) return;
        started = true;
        for (int i = 0; i < components.size(); i++) {
            Component c = components.get(i);
            if (c.isEnabled()) {
                c.start();
            }
        }
    }

    public void update(float dt) {
        if (!active) return;
        for (int i = 0; i < components.size(); i++) {
            Component component = components.get(i);
            if (component.isEnabled()) {
                component.update(dt);
            }
        }
    }

    public void destroy() {
        clearComponents();
        if (level != null) {
            level.removeGameObject(this);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Component> T instantiateComponent(Class<T> clazz, Object... args) {
        if (clazz == null) return null;
        try {
            if (args == null || args.length == 0) {
                Constructor<T> ctor = clazz.getDeclaredConstructor();
                ctor.setAccessible(true);
                return ctor.newInstance();
            }
            Constructor<?>[] ctors = clazz.getDeclaredConstructors();
            for (Constructor<?> ctor : ctors) {
                if (ctor.getParameterCount() == args.length) {
                    Class<?>[] paramTypes = ctor.getParameterTypes();
                    boolean matches = true;
                    for (int i = 0; i < args.length; i++) {
                        if (args[i] != null && !isAssignable(paramTypes[i], args[i].getClass())) {
                            matches = false;
                            break;
                        }
                    }
                    if (matches) {
                        ctor.setAccessible(true);
                        return (T) ctor.newInstance(args);
                    }
                }
            }
            Class<?>[] paramTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                paramTypes[i] = args[i] != null ? args[i].getClass() : Object.class;
            }
            Constructor<T> ctor = clazz.getDeclaredConstructor(paramTypes);
            ctor.setAccessible(true);
            return ctor.newInstance(args);
        } catch (Exception e) {
            throw new PeachException("Failed to instantiate component " + clazz.getName(), e);
        }
    }

    private static boolean isAssignable(Class<?> target, Class<?> source) {
        if (target.isAssignableFrom(source)) return true;
        if (target == int.class && (source == Integer.class || source == Short.class || source == Byte.class)) return true;
        if (target == float.class && (source == Float.class || source == Double.class || source == Integer.class)) return true;
        if (target == double.class && (source == Double.class || source == Float.class || source == Integer.class)) return true;
        if (target == boolean.class && source == Boolean.class) return true;
        if (target == long.class && (source == Long.class || source == Integer.class)) return true;
        if (target == short.class && source == Short.class) return true;
        if (target == byte.class && source == Byte.class) return true;
        if (target == char.class && source == Character.class) return true;
        return false;
    }
}
