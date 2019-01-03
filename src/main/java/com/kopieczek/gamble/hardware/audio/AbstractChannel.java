package com.kopieczek.gamble.hardware.audio;

import com.kopieczek.gamble.hardware.memory.Io;

public abstract class AbstractChannel implements Channel {
    private final Io io;


    public AbstractChannel(Io io) {
        this.io = io;
    }

    @Override
    public final short[] tick() {
        short sample = getSample();

        if (io.isAudioOutputEnabled()) {
            AudioOutputMode outputMode = getOutputMode();
            switch (outputMode) {
                case STEREO:
                    return new short[] {sample, sample};
                case LEFT_ONLY:
                    return new short[] {sample, 0x00};
                case RIGHT_ONLY:
                    return new short[] {0x00, sample};
            }
        }

        return new short[] {0x00, 0x00};
    }

    protected abstract short getSample();
    protected abstract AudioOutputMode getOutputMode();
}
