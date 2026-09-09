package net.meowsers;

import net.meowsers.Peach.ECS.Components.CameraComponent;
import net.meowsers.Peach.ECS.Components.LightComponent;
import net.meowsers.Peach.ECS.Components.MeshRendererComponent;
import net.meowsers.Peach.ECS.Components.TransformComponent;
import net.meowsers.Peach.ECS.GameObject;
import net.meowsers.Peach.GUI.PeachGui;
import net.meowsers.Peach.GameEngine.PeachLevel;
import net.meowsers.Peach.Graphics.Camera;
import net.meowsers.Peach.Graphics.Model;
import net.meowsers.Peach.Serialization.PeachSerializer;
import net.meowsers.Peach.Structures.Color;
import net.meowsers.Peach.Structures.WindowParams;
import org.joml.Vector3f;


import java.util.List;

public class MyLevel extends PeachLevel {

    @Override
    public List<GameObject> setGameObjects() {
        return List.of(camera, light, bunny);
    }

    GameObject camera = new GameObject("camera");
    GameObject light = new GameObject("light");
    GameObject bunny = new GameObject("bunny");

    @Override
    public void start() {
        WindowParams.maximized = true;

        camera.addComponent(new CameraComponent());
        camera.getComponent(CameraComponent.class).setCurrent();
        camera.getComponent(CameraComponent.class).setMovementSpeed(80);
        camera.getComponent(CameraComponent.class).transform.translate(new Vector3f(0, 15, 10));

        light.addComponent(new LightComponent());
        light.getComponent(LightComponent.class).setColor(Color.Pink);

        bunny.addComponent(new MeshRendererComponent());
        bunny.getComponent(MeshRendererComponent.class).setModel(new Model("src/main/resources/Models/bunny.obj"));
        bunny.getComponent(MeshRendererComponent.class).model.setScale(100);
    }

    @Override
    public void update(float dt) {
        super.update(dt);
        PeachGui.debugEditor(this);
        PeachGui.window("Save Test", () -> {
            if(PeachGui.button("Save")) {
                save("saves/level1.json");
            } else if(PeachGui.button("Load")) {
                load("saves/level1.json");
            }
        });

    }
}
