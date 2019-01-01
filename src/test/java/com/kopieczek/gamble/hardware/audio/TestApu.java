package com.kopieczek.gamble.hardware.audio;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Shorts;
import com.kopieczek.gamble.hardware.memory.Io;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
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
        channels.forEach(chan -> verify(chan, never()).stepAhead(anyInt()));
    }

    @Test
    public void test_two_apu_ticks_together_tick_channels_once() {
        Apu apu = new Apu(io, renderer, channels);
        apu.stepAhead(2);
        assertTotalTicks(1, channels);
    }

    @Test
    public void test_one_apu_tick_alone_does_not_tick_channels() {
        Apu apu = new Apu(io, renderer, channels);
        apu.stepAhead(1);
        assertTotalTicks(0, channels);
    }

    @Test
    public void test_two_successive_apu_ticks_tick_channels_once() {
        Apu apu = new Apu(io, renderer, channels);
        apu.stepAhead(1);
        apu.stepAhead(1);
        assertTotalTicks(1, channels);
    }

    @Test
    public void test_100_apu_ticks_together_tick_channels_50_times() {
        Apu apu = new Apu(io, renderer, channels);
        apu.stepAhead(100);
        assertTotalTicks(50, channels);
    }

    @Test
    public void test_101_apu_ticks_together_tick_channels_50_times() {
        Apu apu = new Apu(io, renderer, channels);
        apu.stepAhead(101);
        assertTotalTicks(50, channels);
    }

    @Test
    public void test_101_apu_ticks_followed_by_single_tick_ticks_channels_51_times() {
        Apu apu = new Apu(io, renderer, channels);
        apu.stepAhead(101);
        apu.stepAhead(1);
        assertTotalTicks(51, channels);
    }

    @Test
    public void test_100_successive_apu_ticks_tick_channels_50_times() {
        Apu apu = new Apu(io, renderer, channels);
        IntStream.range(0, 100).forEach(idx -> apu.stepAhead(1));
        assertTotalTicks(50, channels);
    }

    @Test
    public void test_apu_ticks_return_all_single_channel_data_eventually() {
        // Note that we don't assert this happens on specific ticks
        Channel channel = new MockChannelBuilder()
                .onTick(1).thenReturn(new short[][] {
                        new short[] {0x1221, 0x4342, 0x0156},
                        new short[] {0x7a82, -0x769a, 0x4bce}
                })
                .onTick(3).thenReturn(new short[][] {
                        new short[] {0x3eda, -0x62f0, -0x0212},
                        new short[] {0x19fe, -0x7dac, 0x2b3a}
                })
                .onTick(100).thenReturn(new short[][] {
                        new short[] {-0x3978, 0x7776, 0x25b4},
                        new short[] {-0x32ca, -0x7fff, 0x7fff}
                })
                .build();
        MockRenderer renderer = new MockRenderer();
        Apu apu = new Apu(io, renderer, ImmutableList.of(channel));
        apu.stepAhead(200);

        renderer.assertLeftSamplesSeen(0x1221, 0x4342, 0x0156, 0x3eda, -0x62f0, -0x0212, -0x3978, 0x7776, 0x25b4)
                .assertRightSamplesSeen(0x7a82, -0x769a, 0x4bce, 0x19fe, -0x7dac, 0x2b3a, -0x32ca, -0x7fff, 0x7fff);
    }

    private static List<Channel> makeDummyChannels() {
        List<Channel> channels = IntStream.range(0, 4)
                .mapToObj(idx -> mock(Channel.class))
                .collect(Collectors.toList());
        channels.forEach(chan -> when(chan.stepAhead(anyInt())).thenReturn(new short[0][][]));
        return channels;
    }

    private void assertTotalTicks(int ticks, List<Channel> mockChannels) {
        mockChannels.forEach(chan -> {
            ArgumentCaptor<Integer> args = ArgumentCaptor.forClass(Integer.class);
            verify(chan, atLeast(0)).stepAhead(args.capture());
            assertEquals("Unexpected number of ticks to channel", ticks, (long)args.getAllValues().stream().reduce(0, (a, b) -> a + b));
        });
    }

    private static class MockRenderer implements Renderer {
        private final ArrayList<Short> leftSamplesSeen = new ArrayList<>();
        private final ArrayList<Short> rightSamplesSeen = new ArrayList<>();

        @Override
        public void render(short[][][] buffers) {
            for (short[][] buffer : buffers) {
                leftSamplesSeen.addAll(Shorts.asList(buffer[0]));
                rightSamplesSeen.addAll(Shorts.asList(buffer[1]));
            }
        }

        MockRenderer assertLeftSamplesSeen(int... samples) {
            short[] sampleShorts = new short[samples.length];
            IntStream.range(0, samples.length).forEach(idx -> sampleShorts[idx] = (short)samples[idx]);
            List<Short> expected = Shorts.asList(sampleShorts);
            assertEquals(expected, leftSamplesSeen);
            return this;
        }

        MockRenderer assertRightSamplesSeen(int... samples) {
            short[] sampleShorts = new short[samples.length];
            IntStream.range(0, samples.length).forEach(idx -> sampleShorts[idx] = (short)samples[idx]);
            List<Short> expected = Shorts.asList(sampleShorts);
            assertEquals(expected, rightSamplesSeen);
            return this;
        }
    }

}
