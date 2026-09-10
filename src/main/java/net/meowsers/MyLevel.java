package net.meowsers;

import net.meowsers.Peach.Audio.AudioManager;
import net.meowsers.Peach.Audio.PeachAudio;
import net.meowsers.Peach.Audio.PeachAudioPlayer;
import net.meowsers.Peach.ECS.Components.*;
import net.meowsers.Peach.ECS.GameObject;
import net.meowsers.Peach.GUI.PeachGui;
import net.meowsers.Peach.GameEngine.PeachLevel;
import net.meowsers.Peach.Graphics.Camera;
import net.meowsers.Peach.Graphics.Mesh;
import net.meowsers.Peach.Graphics.Model;
import net.meowsers.Peach.Graphics.Texture;
import net.meowsers.Peach.Serialization.PeachSerializer;
import net.meowsers.Peach.Structures.Color;
import net.meowsers.Peach.Structures.Key;
import net.meowsers.Peach.Structures.WindowParams;
import net.meowsers.Peach.Utils.Input;
import net.meowsers.Peach.Utils.Log;
import org.joml.Vector3f;


import java.util.List;

public class MyLevel extends PeachLevel {

    @Override
    public List<GameObject> setGameObjects() {
        return List.of(camera, light, bunny, planeTest);
    }

    GameObject camera = new GameObject("camera");
    GameObject light = new GameObject("light");
    GameObject bunny = new GameObject("bunny");
    GameObject planeTest = new GameObject("plane");

    Texture tex;

    @Override
    public void start() {
        WindowParams.maximized = true;

        tex = new Texture("/Users/meowsers/Documents/Grrr.png");

        camera.addComponent(new CameraComponent());
        camera.getComponent(CameraComponent.class).setCurrent();
        camera.getComponent(CameraComponent.class).setMovementSpeed(80);
        camera.getComponent(CameraComponent.class).transform.translate(new Vector3f(0, 15, 10));

        light.addComponent(new LightComponent());
        light.getComponent(LightComponent.class).setColor(Color.Pink);

        bunny.addComponent(new MeshRendererComponent());
        bunny.getComponent(MeshRendererComponent.class).setModel(new Model("src/main/resources/Models/bunny.obj"));
        bunny.getComponent(MeshRendererComponent.class).model.setScale(100);
        bunny.addComponent(new AudioPlayerComponent("src/main/resources/Audio/yoshi.ogg"));

        planeTest.addComponent(new MeshRendererComponent());
        planeTest.getComponent(MeshRendererComponent.class).model = new Model(Mesh.createPlane(100, 100, Color.White, tex, false));
    }

    @Override
    public void update(float dt) {
        super.update(dt);
        PeachGui.debugEditor(this);

        if(Input.isKeyPressed(Key.SPACE)) bunny.getComponent(AudioPlayerComponent.class).play();
    }
}
