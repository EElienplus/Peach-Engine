package net.meowsers.Peach.Audio;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.stb.STBVorbis.*;

public class PeachAudioLoader {
    public static int loadOGG(String filePath) {
        int bufferId = alGenBuffers();
        long decoder = 0;

        try {
            int channels;
            int sampleRate;
            int lengthSamples;

            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer error = stack.mallocInt(1);
                decoder = stb_vorbis_open_filename(filePath, error, null);
                if (decoder == 0) {
                    throw new RuntimeException("Failed to open OGG file. Error code: " + error.get(0));
                }

                STBVorbisInfo info = STBVorbisInfo.malloc(stack);
                stb_vorbis_get_info(decoder, info);

                channels = info.channels();
                sampleRate = info.sample_rate();
                lengthSamples = stb_vorbis_stream_length_in_samples(decoder);
            }

            ShortBuffer pcm = BufferUtils.createShortBuffer(lengthSamples * channels);
            stb_vorbis_get_samples_short_interleaved(decoder, channels, pcm);

            int format = -1;
            if (channels == 1) {
                format = AL_FORMAT_MONO16;
            } else if (channels == 2) {
                format = AL_FORMAT_STEREO16;
            }

            alBufferData(bufferId, format, pcm, sampleRate);

        } catch (Exception e) {
            alDeleteBuffers(bufferId);
            throw new RuntimeException("Failed to load OGG audio: " + filePath, e);
        } finally {
            if (decoder != 0) {
                stb_vorbis_close(decoder);
            }
        }

        return bufferId;
    }
}