package com.kopieczek.gamble.hardware.audio;

import com.google.common.collect.ImmutableList;
import com.kopieczek.gamble.hardware.memory.Io;

import java.util.List;

public class Apu {
    private final Io io;
    private final Renderer renderer;
    private final Mixer mixer;

    public Apu(Io io, Renderer renderer, List<Channel> channels) {
        this.io = io;
        this.renderer = renderer;
        this.mixer = new Mixer(channels);
    }

    public Apu(Io io, Renderer renderer) {
        this(io, renderer, buildStandardChannels(io));
    }

    public void stepAhead(int cycleDelta) {
        for (int tick = 0; tick < cycleDelta; tick++) {
            short[] sample = mixer.tick();
            renderer.render(sample);
        }
    }

    private static List<Channel> buildStandardChannels(Io io) {
        return ImmutableList.of(
                new Square1Channel(io),
                new Square2Channel(io),
                new WaveChannel(io)
        );
    }
}
