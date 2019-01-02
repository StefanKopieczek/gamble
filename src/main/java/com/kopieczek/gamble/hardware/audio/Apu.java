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
    private int leftoverTicks = 0;

    public Apu(Io io, Renderer renderer, List<Channel> channels) {
        this.io = io;
        this.renderer = renderer;
        this.mixer = new Mixer(channels);
    }

    public Apu(Io io, Renderer renderer) {
        this(io, renderer, buildStandardChannels(io));
    }

    public void stepAhead(int cycleDelta) {
        cycleDelta += leftoverTicks;
        int apuTicks = cycleDelta / CPU_TICKS_PER_APU_TICK;
        leftoverTicks = cycleDelta % CPU_TICKS_PER_APU_TICK;
        for (int tick = 0; tick < apuTicks; tick++) {
            short[] sample = mixer.tick();
            renderer.render(sample);
        }
    }

    private static List<Channel> buildStandardChannels(Io io) {
        // TODO
        return Collections.emptyList();
    }
}
