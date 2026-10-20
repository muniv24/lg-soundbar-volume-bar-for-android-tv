package com.muniv24.volumebar;

import android.media.AudioManager;

import java.lang.reflect.Method;

/**
 * Reads the soundbar's volume. Over HDMI-CEC the soundbar reports its own level to the TV, and
 * Android keeps it as the music volume, so the music volume is the soundbar's level.
 */
final class SoundbarVolume {
    /** Android's code for the TV's built-in speakers. */
    private static final int DEVICE_TV_SPEAKER = 0x2;

    private final AudioManager audio;

    SoundbarVolume(AudioManager audio) {
        this.audio = audio;
    }

    /** The level from 0 to 100, whatever scale the TV uses. */
    int percent() {
        int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        if (max <= 0) return 0;
        return Math.round(100f * audio.getStreamVolume(AudioManager.STREAM_MUSIC) / max);
    }

    boolean muted() {
        return audio.isStreamMute(AudioManager.STREAM_MUSIC);
    }

    /**
     * True when sound plays only on the TV's speakers. The TV then shows its own volume bar, so
     * ours stays hidden. Uses a hidden Android method; where a TV blocks it, we assume a soundbar.
     */
    boolean onTvSpeakers() {
        try {
            Method m = AudioManager.class.getMethod("getDevicesForStream", int.class);
            return (int) m.invoke(audio, AudioManager.STREAM_MUSIC) == DEVICE_TV_SPEAKER;
        } catch (ReflectiveOperationException | RuntimeException blocked) {
            return false;
        }
    }
}
