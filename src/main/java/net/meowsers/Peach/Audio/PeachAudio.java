package net.meowsers.Peach.Audio;

import net.meowsers.Peach.Utils.Disposable;

import static org.lwjgl.openal.AL10.*;

public class PeachAudio implements Disposable {
    private int bufferId = 0;
    private int sourceId = 0;
    private boolean playing = false;
    private boolean paused;
    private float volume = 1.f;

    private Runnable onFinishCallback = null;

    public PeachAudio(int bufferId) {
        this.bufferId = bufferId;
        this.sourceId = alGenSources();
        alSourcei(sourceId, AL_BUFFER, bufferId);
    }

    public PeachAudio(String filePath) {
        this.bufferId = PeachAudioLoader.loadOGG(filePath);
        this.sourceId = alGenSources();
        alSourcei(sourceId, AL_BUFFER, bufferId);
    }

    public void play() {
        playing = true;
        paused = false;
        alSourcePlay(sourceId);
    }

    public void stop() {
        playing = false;
        alSourceStop(sourceId);
    }

    public void pause() {
        paused = true;
        alSourcePause(sourceId);
    }

    public void togglePlaying() {
        playing = !playing;
        if(playing) play();
        else stop();
    }

    public void togglePaused() {
        paused = !paused;
        if(paused) pause();
        else play();
    }

    public void update() {
        if (playing && !paused) {
            int state = alGetSourcei(sourceId, AL_SOURCE_STATE);
            if (state == AL_STOPPED) {
                playing = false;
                if (onFinishCallback != null) {
                    onFinishCallback.run();
                }
            }
        }
    }

    public void onFinishPlaying(Runnable callback) {
        this.onFinishCallback = callback;
    }

    public void setVolume(float volume) {
        this.volume = volume;
        alSourcef(sourceId, AL_GAIN, volume);
    }

    public void setPaused(boolean val) {
        paused = val;
        if (val) pause();
        else play();
    }

    public void setPlaying(boolean val) {
        playing = val;
        if(playing) play();
        else stop();
    }

    public float getVolume() {
        return volume;
    }

    public boolean isPlaying() {
        return playing;
    }

    public int getBufferId() {
        return bufferId;
    }

    public int getSourceId() {
        return sourceId;
    }

    public void cleanup() {
        alDeleteSources(sourceId);
        alDeleteBuffers(bufferId);
    }

    @Override
    public void dispose() {
        cleanup();
    }
}