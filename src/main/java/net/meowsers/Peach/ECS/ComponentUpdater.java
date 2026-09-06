package net.meowsers.Peach.ECS;

@FunctionalInterface
public interface ComponentUpdater {
    void update(float dt);
}
