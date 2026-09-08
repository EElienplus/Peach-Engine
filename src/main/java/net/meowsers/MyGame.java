package net.meowsers;

import net.meowsers.Peach.GameEngine.PeachApplication;

public class MyGame extends PeachApplication {

    MyLevel level = new MyLevel();

    @Override
    public void start() {
        addLevel(level);
    }
}
