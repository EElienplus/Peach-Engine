package net.meowsers.Peach.ECS.Components;

import net.meowsers.Peach.Audio.AudioManager;
import net.meowsers.Peach.Audio.PeachAudio;
import net.meowsers.Peach.Audio.PeachAudioPlayer;
import net.meowsers.Peach.GUI.Editor;
import net.meowsers.Peach.Utils.Log;

import java.util.Objects;

public class AudioPlayerComponent extends BehaviorComponent {

    private PeachAudio audio = new PeachAudio(0);
    private String filePath;
    @Editor private boolean playing = false;
    @Editor private boolean paused = false;

    // Linked to the GUI via callback so dragging the slider updates OpenAL immediately
    @Editor(callbackMethodName = "onVolumeChanged")
    public float volume = 1.f;

    @Editor(argument = "Boolean-Button", callbackMethodName = "onPlayButton")
    private boolean play = false;

    public AudioPlayerComponent(String filePath) {
        this.filePath = filePath;
    }
    public AudioPlayerComponent() { this(""); }

    @Override
    public void start() {
        if (Objects.equals(filePath, "")) return;
        audio = PeachAudioPlayer.loadOGG(filePath);
        audio.setVolume(volume); // Set initial volume
        audio.onFinishPlaying(this::onAudioFinished);
        addToAudioManager();
    }

    public void update(float dt) {
        if (audio != null) {
            audio.update();
        }
    }

    private void onAudioFinished() {
        playing = false;
        play = false; // Matches your field name 'play'
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
        audio = PeachAudioPlayer.loadOGG(filePath);
        audio.setVolume(volume);
    }

    public void play() {
        playing = true;
        paused = false;
        play = true;
        audio.play();
    }

    public void stop() {
        playing = false;
        play = false;
        audio.stop();
    }

    public void pause() {
        paused = true;
        audio.pause();
    }

    public void togglePlaying() {
        playing = !playing;
        play = playing;
        audio.togglePlaying();
    }

    public void togglePaused() {
        paused = !paused;
        audio.togglePaused();
    }

    public void setVolume(float volume) {
        this.volume = volume;
        if (audio != null) {
            audio.setVolume(volume);
        }
    }

    public void setPaused(boolean val) {
        paused = val;
        audio.setPaused(val);
    }

    public void setPlaying(boolean val) {
        playing = val;
        play = val;
        audio.setPlaying(val);
    }

    public float getVolume() {
        return volume;
    }

    public boolean isPlaying() {
        return playing;
    }

    public int getBufferId() {
        return audio.getBufferId();
    }

    public void addToAudioManager() {
        AudioManager.addAudio(audio);
    }

    // Automatically called by the GUI manager when the volume slider is adjusted
    private void onVolumeChanged() {
        if (audio != null) {
            audio.setVolume(volume);
        }
    }

    // Automatically called by the GUI manager when the 'play' button is clicked
    private void onPlayButton() {
        Log.message("Play button clicked. State: " + play);
        if (play) {
            play();
        } else {
            stop();
        }
    }
}