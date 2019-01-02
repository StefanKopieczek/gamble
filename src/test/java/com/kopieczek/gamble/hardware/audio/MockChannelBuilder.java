package com.kopieczek.gamble.hardware.audio;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class MockChannelBuilder {
    private List<Short> leftSamples;
    private List<Short> rightSamples;

    MockChannelBuilder withLeftSamples(Integer... samples) {
        leftSamples = Stream.of(samples).map(Integer::shortValue).collect(Collectors.toList());
        return this;
    }

    MockChannelBuilder withRightSamples(Integer... samples) {
        rightSamples = Stream.of(samples).map(Integer::shortValue).collect(Collectors.toList());
        return this;
    }

    Channel build() {
        return new Channel() {
            private int currentTick = 0;

            @Override
            public short[] tick() {
                short left = currentTick < leftSamples.size() ? leftSamples.get(currentTick) : 0x0000;
                short right = currentTick < rightSamples.size() ? rightSamples.get(currentTick) : 0x0000;
                short[] result = new short[] { left, right };
                currentTick++;
                return result;
            }
        };
    }
}
