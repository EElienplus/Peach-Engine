package net.meowsers.Peach.Audio;

import net.meowsers.Peach.GameEngine.Assets;
import org.joml.Vector3f;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.openal.AL10.AL_POSITION;
import static org.lwjgl.openal.AL10.alListener3f;
import static org.lwjgl.openal.ALC10.*;

public class PeachAudioPlayer {

    private static long device, context;

    public static void init() {
        device = alcOpenDevice((ByteBuffer) null);
        ALCCapabilities alcCapabilities = ALC.createCapabilities(device);
        context = alcCreateContext(device, (IntBuffer) null);
        alcMakeContextCurrent(context);
        AL.createCapabilities(alcCapabilities);
    }

    /** Will be run every frame to be updated with the camera position */
    public static void setListenerPosition(Vector3f position) {
        alListener3f(AL_POSITION, position.x, position.y, position.z);
    }

    public static PeachAudio loadOGG(String filePath) {
        // Caches and reuses audio using Assets, while using PeachAudio(filePath) to load via PeachAudioLoader
        return Assets.get(filePath, PeachAudio::new);
    }
}