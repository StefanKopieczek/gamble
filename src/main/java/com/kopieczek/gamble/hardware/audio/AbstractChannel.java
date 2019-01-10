package com.kopieczek.gamble.hardware.audio;

import com.kopieczek.gamble.hardware.memory.Io;

public abstract class AbstractChannel implements Channel, MasterAudioListener {
    private final Io io;
    private boolean isAudioOutputEnabled = false;
    private AudioOutputMode audioOutputMode = AudioOutputMode.NONE;

    // Performance optimization so we don't have to create new stereo arrays for each sample on each tick.
    // Instead, create a table that takes audio mode and sample, and returns the corresponding stereo array.
    private final short[][][] sampleLookupTable = buildSampleLookupTable();

    public AbstractChannel(Io io) {
        this.io = io;
        io.register(this);
    }

    @Override
    public final short[] tick() {
        if (isAudioOutputEnabled) {
            short sample = getSample();
            return sampleLookupTable[audioOutputMode.ordinal()][sample];
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

    private static short[][][] buildSampleLookupTable() {
        short[][][] table = new short[AudioOutputMode.values().length][][];
        for (int modeInt = 0; modeInt < AudioOutputMode.values().length; modeInt++) {
            table[modeInt] = new short[Short.MAX_VALUE][];
            AudioOutputMode mode = AudioOutputMode.values()[modeInt];
            for (short sample = 0; sample < Short.MAX_VALUE; sample++) {
                short[] stereo;
                switch (mode) {
                    case NONE:
                        stereo = new short[] {0x00, 0x00};
                        break;
                    case LEFT_ONLY:
                        stereo = new short[] {sample, 0x00};
                        break;
                    case RIGHT_ONLY:
                        stereo = new short[] {0x00, sample};
                        break;
                    case STEREO:
                        stereo = new short[] {sample, sample};
                        break;
                    default:
                        throw new IllegalStateException("Unknown audio output mode " + mode);
                }
                table[modeInt][sample] = stereo;
            }
        }

        return table;
    }

    protected abstract short getSample();
}
