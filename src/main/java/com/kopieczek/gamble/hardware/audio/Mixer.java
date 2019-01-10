package com.kopieczek.gamble.hardware.audio;

import java.util.List;

class Mixer implements Channel {
    private final List<Channel> channels;

    Mixer(List<Channel> channels) {
        if (channels.size() >= Short.MAX_VALUE) {
            // Mixing might overflow if too many channels are permitted
            throw new IllegalArgumentException("At most " + Short.MAX_VALUE + " channels allowed");
        }

        this.channels = channels;
    }

    @Override
    public short[] tick() {
        int leftTotal = 0;
        int rightTotal = 0;
        for (int chanIdx = 0; chanIdx < channels.size(); chanIdx++) {
            short[] sample = channels.get(chanIdx).tick();
            leftTotal += sample[0];
            rightTotal += sample[1];
        }

        return new short[] {
                (short)(leftTotal / channels.size()),
                (short)(rightTotal / channels.size())
        };
    }
}
