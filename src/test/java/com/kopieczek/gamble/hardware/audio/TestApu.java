package com.kopieczek.gamble.hardware.audio;

import com.kopieczek.gamble.hardware.cpu.Interrupt;
import com.kopieczek.gamble.hardware.memory.Io;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

public class TestApu {
    @Mock
    Io io;

    @Mock
    Renderer renderer;

    @Test
    public void test_constructing_apu_does_not_tick_channels() {
        List<Channel> channels = makeMockChannels();
        Apu apu = new Apu(io, renderer, channels);
        assertTotalTicks(0, channels);
    }

    @Test
    public void test_ticking_apu_with_zero_ticks_does_not_tick_channels() {
        List<Channel> channels = makeMockChannels();
        Apu apu = new Apu(io, renderer, channels);
        apu.stepAhead(0);
        apu.stepAhead(0);
        apu.stepAhead(0);
        apu.stepAhead(0);
        channels.forEach(chan -> verify(chan, never()).stepAhead(anyInt()));
    }

    @Test
    public void test_two_apu_ticks_together_tick_channels_once() {
        List<Channel> channels = makeMockChannels();
        Apu apu = new Apu(io, renderer, channels);
        apu.stepAhead(2);
        assertTotalTicks(1, channels);
    }

    @Test
    public void test_one_apu_tick_alone_does_not_tick_channels() {
        List<Channel> channels = makeMockChannels();
        Apu apu = new Apu(io, renderer, channels);
        apu.stepAhead(1);
        assertTotalTicks(0, channels);
    }

    @Test
    public void test_two_successive_apu_ticks_tick_channels_once() {
        List<Channel> channels = makeMockChannels();
        Apu apu = new Apu(io, renderer, channels);
        apu.stepAhead(1);
        apu.stepAhead(1);
        assertTotalTicks(1, channels);
    }

    @Test
    public void test_100_apu_ticks_together_tick_channels_50_times() {
        List<Channel> channels = makeMockChannels();
        Apu apu = new Apu(io, renderer, channels);
        apu.stepAhead(100);
        assertTotalTicks(50, channels);
    }

    @Test
    public void test_101_apu_ticks_together_tick_channels_50_times() {
        List<Channel> channels = makeMockChannels();
        Apu apu = new Apu(io, renderer, channels);
        apu.stepAhead(101);
        assertTotalTicks(50, channels);
    }

    @Test
    public void test_101_apu_ticks_followed_by_single_tick_ticks_channels_51_times() {
        List<Channel> channels = makeMockChannels();
        Apu apu = new Apu(io, renderer, channels);
        apu.stepAhead(101);
        apu.stepAhead(1);
        assertTotalTicks(51, channels);
    }

    @Test
    public void test_100_successive_apu_ticks_tick_channels_50_times() {
        List<Channel> channels = makeMockChannels();
        Apu apu = new Apu(io, renderer, channels);
        IntStream.range(0, 100).forEach(idx -> apu.stepAhead(1));
        assertTotalTicks(50, channels);
    }

    private static List<Channel> makeMockChannels() {
        List<Channel> channels = IntStream.range(0, 4)
                .mapToObj(idx -> mock(Channel.class))
                .collect(Collectors.toList());
        channels.forEach(chan -> when(chan.stepAhead(anyInt())).thenReturn(new byte[0][][]));
        return channels;
    }

    private void assertTotalTicks(int ticks, List<Channel> mockChannels) {
        mockChannels.forEach(chan -> {
            ArgumentCaptor<Integer> args = ArgumentCaptor.forClass(Integer.class);
            verify(chan, atLeast(0)).stepAhead(args.capture());
            assertEquals("Unexpected number of ticks to channel", ticks, (long)args.getAllValues().stream().reduce(0, (a, b) -> a + b));
        });
    }
}
