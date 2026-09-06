package net.meowsers.Peach.ECS.Components;

import net.meowsers.Peach.ECS.Component;
import net.meowsers.Peach.ECS.ComponentBehavior;
import net.meowsers.Peach.ECS.ComponentUpdater;
import net.meowsers.Peach.ECS.GameObject;

import java.util.function.Consumer;

public class BehaviorComponent extends Component {

    private ComponentUpdater updater;
    private ComponentBehavior behavior;
    private Runnable onStartAction;
    private Consumer<GameObject> onStartBehavior;
    private Runnable onDestroyAction;
    private Consumer<GameObject> onDestroyBehavior;

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
}
