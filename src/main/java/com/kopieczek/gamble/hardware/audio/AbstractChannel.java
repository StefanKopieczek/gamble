package com.kopieczek.gamble.hardware.audio;

import com.kopieczek.gamble.hardware.memory.Io;

public abstract class AbstractChannel implements Channel, MasterAudioListener {
    private final Io io;
    private boolean isAudioOutputEnabled = false;
    private AudioOutputMode audioOutputMode = AudioOutputMode.NONE;

    public AbstractChannel(Io io) {
        this.io = io;
        io.register(this);
    }

    @Override
    public final short[] tick() {
        if (isAudioOutputEnabled) {
            short sample = getSample();
            switch (audioOutputMode) {
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

    @Override
    public void onAudioEnableChanged(boolean isNowEnabled) {
        isAudioOutputEnabled = isNowEnabled;
    }

    protected void updateOutputMode(AudioOutputMode newOutputMode) {
        audioOutputMode = newOutputMode;
    }

    protected abstract short getSample();
}
