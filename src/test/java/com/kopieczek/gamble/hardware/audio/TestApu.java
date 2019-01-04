package com.kopieczek.gamble.hardware.audio;

import com.google.common.collect.ImmutableList;
import com.kopieczek.gamble.hardware.memory.Io;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class TestApu {
    Io io;
    Renderer renderer;
    List<Channel> channels;

    @Before
    public void setUp() {
        io = mock(Io.class);
        renderer = mock(Renderer.class);
        channels = makeDummyChannels();
    }

    @Test
    public void test_constructing_apu_does_not_tick_channels() {
        Apu apu = new Apu(io, renderer, channels);
        assertTotalTicks(0, channels);
    }

    @Test
    public void test_ticking_apu_with_zero_ticks_does_not_tick_channels() {
        Apu apu = new Apu(io, renderer, channels);
        apu.stepAhead(0);
        apu.stepAhead(0);
        apu.stepAhead(0);
        apu.stepAhead(0);
        assertTotalTicks(0, channels);
    }

    @Test
    public void test_apu_tick_ticks_channels() {
        Apu apu = new Apu(io, renderer, channels);
        apu.stepAhead(1);
        assertTotalTicks(1, channels);
    }

    @Test
    public void test_two_apu_ticks_together_tick_channels_twice() {
        Apu apu = new Apu(io, renderer, channels);
        apu.stepAhead(2);
        assertTotalTicks(2, channels);
    }

    @Test
    public void test_two_successive_apu_ticks_tick_channels_twice() {
        Apu apu = new Apu(io, renderer, channels);
        apu.stepAhead(1);
        apu.stepAhead(1);
        assertTotalTicks(2, channels);
    }

    @Test
    public void test_100_apu_ticks_together_tick_channels_100_times() {
        Apu apu = new Apu(io, renderer, channels);
        apu.stepAhead(100);
        assertTotalTicks(100, channels);
    }

    @Test
    public void test_101_apu_ticks_followed_by_single_tick_ticks_channels_102_times() {
        Apu apu = new Apu(io, renderer, channels);
        apu.stepAhead(101);
        apu.stepAhead(1);
        assertTotalTicks(102, channels);
    }

    @Test
    public void test_100_successive_apu_ticks_tick_channels_100_times() {
        Apu apu = new Apu(io, renderer, channels);
        IntStream.range(0, 100).forEach(idx -> apu.stepAhead(1));
        assertTotalTicks(100, channels);
    }

    @Test
    public void test_apu_ticks_return_all_single_channel_data_eventually() {
        // Note that we don't assert this happens on specific ticks
        Channel channel = new MockChannelBuilder()
                .withLeftSamples(0x1221, 0x4342, 0x0156, 0x3eda, -0x62f0, -0x212)
                .withRightSamples(0x7a82, -0x769a, 0x4bce, -0x32ca, -0x7fff, 0x7fff)
                .build();
        MockRenderer renderer = new MockRenderer();
        Apu apu = new Apu(io, renderer, ImmutableList.of(channel));
        apu.stepAhead(200);

        renderer.assertLeftSamplesSeen(0x1221, 0x4342, 0x0156, 0x3eda, -0x62f0, -0x0212)
                .assertRightSamplesSeen(0x7a82, -0x769a, 0x4bce, -0x32ca, -0x7fff, 0x7fff);
    }

    private static List<Channel> makeDummyChannels() {
        List<Channel> channels = IntStream.range(0, 4)
                .mapToObj(idx -> mock(Channel.class))
                .collect(Collectors.toList());
        channels.forEach(chan -> when(chan.tick()).thenReturn(new short[] {0x00, 0x00}));
        return channels;
    }

    private void assertTotalTicks(int ticks, List<Channel> mockChannels) {
        mockChannels.forEach(chan -> {
            verify(chan, times(ticks)).tick();
        });
    }

    private static class MockRenderer implements Renderer {
        private final ArrayList<Short> leftSamplesSeen = new ArrayList<>();
        private final ArrayList<Short> rightSamplesSeen = new ArrayList<>();

        @Override
        public void render(short[] sample) {
            leftSamplesSeen.add(sample[0]);
            rightSamplesSeen.add(sample[1]);
        }

        MockRenderer assertLeftSamplesSeen(Integer... samples) {
            if (samples.length > leftSamplesSeen.size()) {
                fail("Expected at least " + samples.length + " left samples; saw " + leftSamplesSeen.size());
            }

            for (int idx = 0; idx < Math.min(leftSamplesSeen.size(), samples.length); idx++) {
                assertEquals("Unexpected left sample at index " + idx, new Short(samples[idx].shortValue()), leftSamplesSeen.get(idx));
            }
            return this;
        }

        MockRenderer assertRightSamplesSeen(Integer... samples) {
            if (samples.length > rightSamplesSeen.size()) {
                fail("Expected at least " + samples.length + " right samples; saw " + leftSamplesSeen.size());
            }

            for (int idx = 0; idx < Math.min(rightSamplesSeen.size(), samples.length); idx++) {
                assertEquals("Unexpected right sample at index " + idx, new Short(samples[idx].shortValue()), rightSamplesSeen.get(idx));
            }
            return this;
        }
    }
}
