package net.meowsers;

import net.meowsers.Peach.ECS.Components.MeshRendererComponent;
import net.meowsers.Peach.ECS.GameObject;
import net.meowsers.Peach.Graphics.*;
import net.meowsers.Peach.PeachApplication;
import net.meowsers.Peach.Structures.Color;
import org.joml.Vector3f;

public class MyGame extends PeachApplication {

    MyLevel level = new MyLevel();

    @Override
    public void start() {
        addLevel(level);
    }
}
