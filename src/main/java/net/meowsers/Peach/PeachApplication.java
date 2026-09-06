package net.meowsers.Peach;

import net.meowsers.Peach.GameEngine.PeachLevel;
import net.meowsers.Peach.Graphics.Renderer;
import net.meowsers.Peach.Graphics.Window;
import net.meowsers.Peach.Utils.Input;
import net.meowsers.Peach.Utils.Log;
import net.meowsers.Peach.Utils.Time;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public abstract class PeachApplication {

    private Peach peach = new Peach();
    private Window window;
    private float dt;

    private List<PeachLevel> levels;

    public abstract void start();
    public void update(float dt) {

    }
    public void end() {

    }

    public void addLevel(PeachLevel level) {
        levels.add(level);
    }
    public void removeLevel(PeachLevel level) {
        levels.remove(level);
    }

    public void startLevels() {
        for(PeachLevel level : levels) {
            level.start();
            level.internalStart();
        }
    }
    public void updateLevels(float dt) {
        for(PeachLevel level : levels) {
            level.update(dt);
            level.internalUpdate(dt);
        }
    }
    public void destroyLevels() {
        for(PeachLevel level : levels) {
            level.destroy();
            level.internalDestroy();
        }
    }

    public void run() {
        peach.start();
        window = peach.getWindow();
        Time.start();
        Input.start(window.getHandle());

        levels = new ArrayList<>();

        start();
        startLevels();

        while(window.isRunning()) {
            peach.update();
            Time.update();

            dt = Time.getDeltaTime();


            if(levels != null && !levels.isEmpty()) {
                updateLevels(dt);
            }

            update(dt);

            Renderer.render();

            Input.endFrame();
            window.present();
        }

        if(levels != null && !levels.isEmpty()) {
            destroyLevels();
        }

        end();
        peach.end();
    }


    public Window getWindow() {
        return window;
    }
}
