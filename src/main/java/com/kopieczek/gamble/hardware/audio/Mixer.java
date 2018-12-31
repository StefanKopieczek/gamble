package com.kopieczek.gamble.hardware.audio;

import java.util.List;

class Mixer extends Channel {
    private final List<Channel> channels;

    Mixer(List<Channel> channels) {
        this.channels = channels;
    }

    @Override
    public short[][][] stepAhead(int ticks) {
        short[][][][] buffersForEachChannel = new short[channels.size()][][][];
        for (int idx = 0; idx < channels.size(); idx++) {
            buffersForEachChannel[idx] = channels.get(idx).stepAhead(ticks);
        }

        return mix(buffersForEachChannel);
    }

    private static short[][][] mix(short[][][][] buffersForEachChannel) {
        // TODO
        return buffersForEachChannel[0];
    }
}
