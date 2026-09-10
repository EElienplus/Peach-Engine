package net.meowsers.Peach.Audio;

import java.util.ArrayList;
import java.util.List;

public class AudioManager {
    private static List<PeachAudio> audios = new ArrayList<>();

    public static void update() {
        for(PeachAudio audio : audios) {
            audio.update();
        }
    }

    public static void addAudio(PeachAudio audio) {
        audios.add(audio);
    }
    public static void removeAudio(PeachAudio audio) {
        audios.remove(audio);
    }

}
