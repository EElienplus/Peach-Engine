package net.meowsers.Peach.GameEngine;

import net.meowsers.Peach.ECS.Component;
import net.meowsers.Peach.ECS.GameObject;
import net.meowsers.Peach.Graphics.Renderer;
import net.meowsers.Peach.Structures.Color;
import net.meowsers.Peach.Utils.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public abstract class PeachLevel {
    private final List<GameObject> gameObjects = new ArrayList<>();
    private boolean started = false;

    public void addGameObject(GameObject go) {
        if (go != null && !gameObjects.contains(go)) {
            gameObjects.add(go);
            go.setLevel(this);
            if (started && !go.isStarted() && go.isActive()) {
                go.start();
            }
        }
    }

    public GameObject addGameObject(GameObject go, Component... components) {
        if (go == null) return null;
        if (components != null) {
            for (Component c : components) {
                if (c != null) {
                    go.addComponent(c);
                }
            }
        }
        addGameObject(go);
        return go;
    }

    public GameObject createGameObject(String name, Component... components) {
        GameObject go = new GameObject(name, components);
        addGameObject(go);
        return go;
    }

    public GameObject createGameObject(Component... components) {
        return createGameObject("GameObject", components);
    }

    public void removeGameObject(GameObject go) {
        if (go != null) {
            gameObjects.remove(go);
        }
    }

    public GameObject findGameObject(String name) {
        if (name == null) return null;
        for (GameObject go : gameObjects) {
            if (Objects.equals(go.name, name)) {
                return go;
            }
        }
        return null;
    }

    public <T extends Component> T findComponentOfType(Class<T> componentClass) {
        if (componentClass == null) return null;
        for (GameObject go : gameObjects) {
            T c = go.getComponent(componentClass);
            if (c != null) {
                return c;
            }
        }
        return null;
    }

    public <T extends Component> List<T> findComponentsOfType(Class<T> componentClass) {
        if (componentClass == null) return Collections.emptyList();
        List<T> list = new ArrayList<>();
        for (GameObject go : gameObjects) {
            list.addAll(go.getComponents(componentClass));
        }
        return list;
    }

    public List<GameObject> getGameObjects() {
        return gameObjects;
    }

    public List<GameObject> setGameObjects() {
        return List.of();
    }

    public void start() {
    }

    public void update(float dt) {
    }

    public void destroy() {
    }

    public void internalStart() {
        List<GameObject> initial = setGameObjects();
        if (initial != null) {
            for (GameObject go : initial) {
                if (go != null && !gameObjects.contains(go)) {
                    gameObjects.add(go);
                    go.setLevel(this);
                }
            }
        }
        started = true;
        for (int i = 0; i < gameObjects.size(); i++) {
            gameObjects.get(i).start();
        }
    }

    public void internalUpdate(float dt) {
        for (int i = 0; i < gameObjects.size(); i++) {
            gameObjects.get(i).update(dt);
        }
    }

    public void internalDestroy() {
        started = false;
        for (int i = 0; i < gameObjects.size(); i++) {
            gameObjects.get(i).destroy();
        }
        gameObjects.clear();
    }

    public void setClearColor(Color color) {
        Renderer.setBgColor(color);
    }
}
