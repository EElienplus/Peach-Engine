package net.meowsers.Peach.ECS;

@FunctionalInterface
public interface ComponentBehavior {
    void update(GameObject gameObject, float dt);
}
