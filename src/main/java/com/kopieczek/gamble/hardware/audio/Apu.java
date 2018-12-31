package com.kopieczek.gamble.hardware.audio;

import com.kopieczek.gamble.hardware.memory.Io;

import java.util.Collections;
import java.util.List;

public class Apu {
    private static final int MASTER_FREQUENCY_HZ = 2097152;
    private static final int CPU_TICKS_PER_APU_TICK = 2;

    private final Io io;
    private final Renderer renderer;
    private final Mixer mixer;

    public Apu(Io io, Renderer renderer) {
        this.io = io;
        this.renderer = renderer;
        this.mixer = new Mixer(buildChannels(io));
    }

    private static List<Channel> buildChannels(Io io) {
        // TODO
        return Collections.emptyList();
    }
}
