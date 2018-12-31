package com.kopieczek.gamble.hardware.audio;

import java.util.List;

class Mixer extends Channel {
    private final List<Channel> channels;

    Mixer(List<Channel> channels) {
        this.channels = channels;
    }

    @Override
    public byte[][][] stepAhead(int ticks) {
        // TODO
        channels.forEach(c -> c.stepAhead(ticks));
        return new byte[0][][];
    }
}
