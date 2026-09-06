package net.meowsers.Peach.ECS;

import net.meowsers.Peach.ECS.Components.BehaviorComponent;
import net.meowsers.Peach.ECS.Components.TransformComponent;
import net.meowsers.Peach.GameEngine.PeachLevel;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public abstract class Component {

    public GameObject gameObject;
    public TransformComponent transform;
    private boolean isEnabled = true;
    private String name;

    public void start() {

    }

    public void update(float dt) {

    }

    public void destroy() {
        if (gameObject != null) {
            gameObject.removeComponent(this);
        }
    }

    public void onDestroy() {

    }

    public void onAdded() {

    }

    public void onRemoved() {

    }

    public void onEnable() {

    }

    public void onDisable() {

    }

    public void setGameObject(GameObject go) {
        this.gameObject = go;
        this.transform = (go != null) ? go.transform : null;
    }

    public GameObject getGameObject() {
        return gameObject;
    }

    public PeachLevel getLevel() {
        return gameObject != null ? gameObject.getLevel() : null;
    }

    public TransformComponent getTransform() {
        if (gameObject != null && gameObject.transform != null) {
            this.transform = gameObject.transform;
            return gameObject.transform;
        }
        return this.transform;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.isEnabled != enabled) {
            this.isEnabled = enabled;
            if (gameObject != null) {
                if (enabled) {
                    onEnable();
                } else {
                    onDisable();
                }
            }
        }
    }

    public String getName() {
        if (name != null) return name;
        return this.getClass().getSimpleName();
    }

    public void setName(String name) {
        this.name = name;
    }

    public <T extends Component> T getComponent(Class<T> componentClass) {
        return gameObject != null ? gameObject.getComponent(componentClass) : null;
    }

    public <T extends Component> List<T> getComponents(Class<T> componentClass) {
        return gameObject != null ? gameObject.getComponents(componentClass) : Collections.emptyList();
    }

    public <T extends Component> boolean hasComponent(Class<T> componentClass) {
        return gameObject != null && gameObject.hasComponent(componentClass);
    }

    public <T extends Component> T addComponent(T component) {
        return gameObject != null ? gameObject.addComponent(component) : null;
    }

    public <T extends Component> T addComponent(Class<T> componentClass, Object... args) {
        return gameObject != null ? gameObject.addComponent(componentClass, args) : null;
    }

    public <T extends Component> T getOrAddComponent(Class<T> componentClass) {
        return gameObject != null ? gameObject.getOrAddComponent(componentClass) : null;
    }

    public <T extends Component> boolean removeComponent(Class<T> componentClass) {
        return gameObject != null && gameObject.removeComponent(componentClass);
    }

    public boolean removeComponent(Component component) {
        return gameObject != null && gameObject.removeComponent(component);
    }

    public GameObject findGameObject(String name) {
        PeachLevel level = getLevel();
        return level != null ? level.findGameObject(name) : null;
    }

    public <T extends Component> T findComponentOfType(Class<T> componentClass) {
        PeachLevel level = getLevel();
        return level != null ? level.findComponentOfType(componentClass) : null;
    }

    public static BehaviorComponent create(ComponentUpdater updater) {
        return new BehaviorComponent(updater);
    }

    public static BehaviorComponent create(ComponentBehavior behavior) {
        return new BehaviorComponent(behavior);
    }

    public static BehaviorComponent create(Runnable onStart, ComponentUpdater updater) {
        return new BehaviorComponent(onStart, updater);
    }

    public static BehaviorComponent create(Consumer<GameObject> onStart, ComponentBehavior behavior) {
        return new BehaviorComponent(onStart, behavior);
    }
}
