package net.meowsers;

import net.meowsers.Peach.ECS.Components.CameraComponent;
import net.meowsers.Peach.ECS.Components.LightComponent;
import net.meowsers.Peach.ECS.Components.MeshRendererComponent;
import net.meowsers.Peach.ECS.GameObject;
import net.meowsers.Peach.GameEngine.PeachLevel;
import net.meowsers.Peach.Graphics.Model;
import net.meowsers.Peach.Graphics.Renderer;
import net.meowsers.Peach.Graphics.Texture;
import net.meowsers.Peach.Structures.Color;
import net.meowsers.Peach.Utils.Log;
import net.meowsers.Peach.Utils.Time;

import java.util.List;

public class MyLevel extends PeachLevel {
    @Override
    public List<GameObject> setGameObjects() {
        return List.of(camera, light);
    }
    GameObject camera = new GameObject("camera");
    GameObject light = new GameObject("light");

    @Override
    public void start() {

        camera.addComponent(new CameraComponent());
        camera.getComponent(CameraComponent.class).setCurrent();
        camera.getComponent(CameraComponent.class).setMovementSpeed(100);

        light.addComponent(new LightComponent());
        light.getComponent(LightComponent.class).setColor(Color.Pink);

    }

    @Override
    public void update(float dt) {
        Log.message("FPS: " + Time.getFPS() + "; " + "Draw Calls: " + Renderer.getRenderCallsAmount());
    }
}
