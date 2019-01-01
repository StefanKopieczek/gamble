package com.kopieczek.gamble.hardware.audio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

class MockChannelBuilder {
    private final Map<Integer, short[][]> tickMap = new HashMap<>();

    Inner onTick(int tick) {
        return new Inner(tick);
    }

    Channel build() {
        return new Channel() {
            private int currentTick = 0;

            @Override
            public short[][][] stepAhead(int ticks) {
                List<short[][]> result = new ArrayList<>();

                IntStream.range(currentTick + 1, currentTick + ticks + 1).forEach(tick -> {
                    if (tickMap.containsKey(tick)) {
                        result.add(tickMap.get(tick));
                    }
                });

                currentTick += ticks;
                return result.toArray(new short[result.size()][][]);
            }
        };
    }

    class Inner {
        private int tick;

        Inner(int tick) {
            this.tick = tick;
        }

        MockChannelBuilder thenReturn(short[][] leftRightArrays) {
            MockChannelBuilder.this.tickMap.put(tick, leftRightArrays);
            return MockChannelBuilder.this;
        }
    }
}
