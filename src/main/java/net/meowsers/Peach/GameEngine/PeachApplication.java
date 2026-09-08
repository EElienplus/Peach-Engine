package net.meowsers.Peach.GameEngine;

import net.meowsers.Peach.GUI.PeachGui;
import net.meowsers.Peach.Graphics.Renderer;
import net.meowsers.Peach.Graphics.Window;
import net.meowsers.Peach.Utils.Input;
import net.meowsers.Peach.Utils.Time;

import java.util.ArrayList;
import java.util.List;

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
        PeachGui.init(window.getHandle());

        levels = new ArrayList<>();

        start();
        startLevels();
        window.updateProperties();

        while(window.isRunning()) {
            peach.update();
            Time.update();

            dt = Time.getDeltaTime();

            PeachGui.newFrame();

            if(levels != null && !levels.isEmpty()) {
                updateLevels(dt);
            }

            update(dt);

            Renderer.render();
            PeachGui.render();

            Input.endFrame();
            window.present();
        }

        if(levels != null && !levels.isEmpty()) {
            destroyLevels();
        }

        end();
        PeachGui.destroy();
        peach.end();
    }


    public Window getWindow() {
        return window;
    }
}
