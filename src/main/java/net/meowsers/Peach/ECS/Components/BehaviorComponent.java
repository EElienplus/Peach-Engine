package net.meowsers.Peach.ECS.Components;

import net.meowsers.Peach.ECS.Component;
import net.meowsers.Peach.ECS.ComponentBehavior;
import net.meowsers.Peach.ECS.ComponentUpdater;
import net.meowsers.Peach.ECS.GameObject;

import java.util.function.Consumer;

public class BehaviorComponent extends Component {

    private transient ComponentUpdater updater;
    private transient ComponentBehavior behavior;
    private transient Runnable onStartAction;
    private transient Consumer<GameObject> onStartBehavior;
    private transient Runnable onDestroyAction;
    private transient Consumer<GameObject> onDestroyBehavior;
    private transient Runnable onEnableAction;
    private transient Consumer<GameObject> onEnableBehavior;
    private transient Runnable onDisableAction;
    private transient Consumer<GameObject> onDisableBehavior;
    private transient Runnable onAddedAction;
    private transient Consumer<GameObject> onAddedBehavior;
    private transient Runnable onRemovedAction;
    private transient Consumer<GameObject> onRemovedBehavior;

    public BehaviorComponent() {
    }

    public BehaviorComponent(ComponentUpdater updater) {
        this.updater = updater;
    }

    public BehaviorComponent(ComponentBehavior behavior) {
        this.behavior = behavior;
    }

    public BehaviorComponent(Runnable onStart, ComponentUpdater updater) {
        this.onStartAction = onStart;
        this.updater = updater;
    }

    public BehaviorComponent(Consumer<GameObject> onStart, ComponentBehavior behavior) {
        this.onStartBehavior = onStart;
        this.behavior = behavior;
    }

    public BehaviorComponent onStart(Runnable action) {
        this.onStartAction = action;
        return this;
    }

    public BehaviorComponent onStart(Consumer<GameObject> action) {
        this.onStartBehavior = action;
        return this;
    }

    public BehaviorComponent onUpdate(ComponentUpdater updater) {
        this.updater = updater;
        return this;
    }

    public BehaviorComponent onUpdate(ComponentBehavior behavior) {
        this.behavior = behavior;
        return this;
    }

    public BehaviorComponent onDestroy(Runnable action) {
        this.onDestroyAction = action;
        return this;
    }

    public BehaviorComponent onDestroy(Consumer<GameObject> action) {
        this.onDestroyBehavior = action;
        return this;
    }

    public BehaviorComponent onEnable(Runnable action) {
        this.onEnableAction = action;
        return this;
    }

    public BehaviorComponent onEnable(Consumer<GameObject> action) {
        this.onEnableBehavior = action;
        return this;
    }

    public BehaviorComponent onDisable(Runnable action) {
        this.onDisableAction = action;
        return this;
    }

    public BehaviorComponent onDisable(Consumer<GameObject> action) {
        this.onDisableBehavior = action;
        return this;
    }

    public BehaviorComponent onAdded(Runnable action) {
        this.onAddedAction = action;
        return this;
    }

    public BehaviorComponent onAdded(Consumer<GameObject> action) {
        this.onAddedBehavior = action;
        return this;
    }

    public BehaviorComponent onRemoved(Runnable action) {
        this.onRemovedAction = action;
        return this;
    }

    public BehaviorComponent onRemoved(Consumer<GameObject> action) {
        this.onRemovedBehavior = action;
        return this;
    }

    @Override
    public void start() {
        super.start();
        if (onStartAction != null) {
            onStartAction.run();
        }
        if (onStartBehavior != null && gameObject != null) {
            onStartBehavior.accept(gameObject);
        }
    }

    @Override
    public void update(float dt) {
        super.update(dt);
        if (updater != null) {
            updater.update(dt);
        }
        if (behavior != null && gameObject != null) {
            behavior.update(gameObject, dt);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (onDestroyAction != null) {
            onDestroyAction.run();
        }
        if (onDestroyBehavior != null && gameObject != null) {
            onDestroyBehavior.accept(gameObject);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (onEnableAction != null) {
            onEnableAction.run();
        }
        if (onEnableBehavior != null && gameObject != null) {
            onEnableBehavior.accept(gameObject);
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (onDisableAction != null) {
            onDisableAction.run();
        }
        if (onDisableBehavior != null && gameObject != null) {
            onDisableBehavior.accept(gameObject);
        }
    }

    @Override
    public void onAdded() {
        super.onAdded();
        if (onAddedAction != null) {
            onAddedAction.run();
        }
        if (onAddedBehavior != null && gameObject != null) {
            onAddedBehavior.accept(gameObject);
        }
    }

    @Override
    public void onRemoved() {
        super.onRemoved();
        if (onRemovedAction != null) {
            onRemovedAction.run();
        }
        if (onRemovedBehavior != null && gameObject != null) {
            onRemovedBehavior.accept(gameObject);
        }
    }
}
