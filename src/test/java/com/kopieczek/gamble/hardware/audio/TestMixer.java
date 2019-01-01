package com.kopieczek.gamble.hardware.audio;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TestMixer {
    private static final short[][] BUFFER_1 = new short[][] {
            new short[] {0x12, 0x34, 0x56},
            new short[] {0x78, 0x9a, 0xbc}
    };
    private static final short[][] BUFFER_2 = new short[][] {
            new short[] {0x11, 0x33, 0x55},
            new short[] {0x22, 0x44, 0x66}
    };
    private static final short[][] BUFFER_3 = new short[][] {
            new short[] {0x98, 0x76, 0x54},
            new short[] {0xfe, 0xde, 0xcb}
    };

    @Test
    public void test_single_mixer_tick_ticks_channels() {
        List<Channel> channels = createDummyChannels(7);
        Mixer mixer = new Mixer(channels);
        mixer.stepAhead(1);
        verifyTicks(channels, 1);
    }

    @Test
    public void test_mixer_step_ahead_with_multiple_ticks_ticks_channels_correctly() {
        List<Channel> channels = createDummyChannels(3);
        Mixer mixer = new Mixer(channels);
        mixer.stepAhead(17);
        verifyTicks(channels, 17);
    }

    @Test
    public void test_successive_mixer_ticks_are_passed_to_channels() {
        List<Channel> channels = createDummyChannels(8);
        Mixer mixer = new Mixer(channels);
        mixer.stepAhead(1);
        mixer.stepAhead(1);
        mixer.stepAhead(1);
        mixer.stepAhead(1);
        verifyTicks(channels, 1, 1, 1, 1);
    }

    @Test
    public void test_unticked_mixer_does_not_tick_channels() {
        List<Channel> channels = createDummyChannels(11);
        new Mixer(channels);
        verifyTicks(channels);
    }

    @Test
    public void test_successive_step_aheads_passed_to_channels_correctly() {
        List<Channel> channels = createDummyChannels(6);
        Mixer mixer = new Mixer(channels);
        mixer.stepAhead(15);
        mixer.stepAhead(4);
        mixer.stepAhead(1);
        mixer.stepAhead(19);
        verifyTicks(channels, 15, 4, 1, 19);
    }

    @Test
    public void test_mixer_with_one_channel_returns_no_sample_buffers_if_child_returns_none() {
        Channel channel = new MockChannelBuilder().build();
        Mixer mixer = new Mixer(ImmutableList.of(channel));
        assertArrayEquals(new short[0][][], mixer.stepAhead(1));
    }

    @Test
    public void test_mixer_with_one_channel_returns_no_sample_buffers_if_child_returns_none_even_when_repeated() {
        Channel channel = new MockChannelBuilder().build();
        Mixer mixer = new Mixer(ImmutableList.of(channel));
        assertArrayEquals(new short[0][][], mixer.stepAhead(1));
        assertArrayEquals(new short[0][][], mixer.stepAhead(1));
        assertArrayEquals(new short[0][][], mixer.stepAhead(17));
        assertArrayEquals(new short[0][][], mixer.stepAhead(4));
        assertArrayEquals(new short[0][][], mixer.stepAhead(1));
    }

    @Test
    public void test_mixer_with_one_channel_returns_sample_buffer_from_channel_when_emitted() {
        Channel channel = new MockChannelBuilder()
                .onTick(1).thenReturn(BUFFER_1)
                .build();
        Mixer mixer = new Mixer(ImmutableList.of(channel));
        short[][][] result = mixer.stepAhead(1);
        assertArrayEquals(new short[][][]{BUFFER_1}, result);
    }

    @Test
    public void test_mixer_with_one_channel_returns_sample_buffer_from_channel_when_eventually_emitted() {
        Channel channel = new MockChannelBuilder()
                .onTick(10).thenReturn(BUFFER_1)
                .build();
        Mixer mixer = new Mixer(ImmutableList.of(channel));
        assertArrayEquals(new short[0][][], mixer.stepAhead(6));
        assertArrayEquals(new short[0][][], mixer.stepAhead(1));
        assertArrayEquals(new short[0][][], mixer.stepAhead(2));
        assertArrayEquals(new short[][][]{BUFFER_1}, mixer.stepAhead(1));
    }

    @Test
    public void test_mixer_with_one_channel_returns_sample_buffer_from_channel_when_ticked_past() {
        Channel channel = new MockChannelBuilder()
                .onTick(3).thenReturn(BUFFER_1)
                .build();
        Mixer mixer = new Mixer(ImmutableList.of(channel));
        short[][][] result = mixer.stepAhead(4);
        assertArrayEquals(new short[][][]{BUFFER_1}, result);
    }

    @Test
    public void test_mixer_with_one_channel_returns_several_sample_buffers_from_channel_when_ticked_past() {
        Channel channel = new MockChannelBuilder()
                .onTick(1).thenReturn(BUFFER_1)
                .onTick(2).thenReturn(BUFFER_2)
                .onTick(4).thenReturn(BUFFER_2)
                .onTick(5).thenReturn(BUFFER_3)
                .build();
        Mixer mixer = new Mixer(ImmutableList.of(channel));
        short[][][] result = mixer.stepAhead(4);
        assertArrayEquals(new short[][][]{BUFFER_1, BUFFER_2, BUFFER_2}, result);
    }

    @Test
    public void test_mixer_with_one_channel_returns_several_sample_buffers_successively() {
        Channel channel = new MockChannelBuilder()
                .onTick(1).thenReturn(BUFFER_1)
                .onTick(2).thenReturn(BUFFER_2)
                .onTick(3).thenReturn(BUFFER_2)
                .onTick(5).thenReturn(BUFFER_3)
                .build();
        Mixer mixer = new Mixer(ImmutableList.of(channel));
        assertArrayEquals(new short[][][]{BUFFER_1}, mixer.stepAhead(1));
        assertArrayEquals(new short[][][]{BUFFER_2}, mixer.stepAhead(1));
        assertArrayEquals(new short[][][]{BUFFER_2}, mixer.stepAhead(1));
        assertArrayEquals(new short[0][][], mixer.stepAhead(1));
        assertArrayEquals(new short[][][]{BUFFER_3}, mixer.stepAhead(1));
    }

    private static List<Channel> createDummyChannels(int numChannels) {
        return IntStream.range(0, 20)
                .mapToObj(idx -> mock(Channel.class))
                .collect(Collectors.toList());
    }

    private void verifyTicks(List<Channel> channels, Integer... ticks) {
        channels.forEach(channel -> {
            ArgumentCaptor<Integer> args = ArgumentCaptor.forClass(Integer.class);
            verify(channel, atLeast(0)).stepAhead(args.capture());
            assertEquals(Arrays.asList(ticks), args.getAllValues());
        });
    }
}
