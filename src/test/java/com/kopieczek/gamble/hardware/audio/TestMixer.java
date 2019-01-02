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
import static org.mockito.Mockito.*;

public class TestMixer {
    private static final short[][] BUFFER_1 = new short[][] {
            new short[] {0x0122, -0x7a34, 0x2b56},
            new short[] {-0x5178, -0x5a9a, 0x01bc}
    };
    private static final short[][] BUFFER_2 = new short[][] {
            new short[] {0x1102, 0x3a37, -0x655c},
            new short[] {0x0224, 0x7044, 0x626a}
    };
    private static final short[][] BUFFER_3 = new short[][] {
            new short[] {0x6f98, -0x7ff6, 0x0054},
            new short[] {-0x7ffe, 0x7de2, -0x04cb}
    };
    private static final short[][] ZERO_ENERGY_BUFFER = new short[][] {
            new short[3],
            new short[3]
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

    @Test
    public void test_mixer_with_one_active_and_one_disabled_channel_returns_the_active_channel_with_50_percent_volume() {
        Channel active = new MockChannelBuilder()
                .onTick(1).thenReturn(BUFFER_1)
                .onTick(2).thenReturn(BUFFER_2)
                .build();
        Channel disabled = new MockChannelBuilder()
                .onTick(1).thenReturn(ZERO_ENERGY_BUFFER)
                .onTick(2).thenReturn(ZERO_ENERGY_BUFFER)
                .build();
        Mixer mixer = new Mixer(ImmutableList.of(active, disabled));
        short[][] BUFFER_1_ADJUSTED = adjustVolume(BUFFER_1, 2);
        short[][] BUFFER_2_ADJUSTED = adjustVolume(BUFFER_2, 2);

        assertArrayEquals(new short[][][]{BUFFER_1_ADJUSTED}, mixer.stepAhead(1));
        assertArrayEquals(new short[][][]{BUFFER_2_ADJUSTED}, mixer.stepAhead(1));
    }

    @Test
    public void test_mixer_with_one_disabled_and_one_active_channel_returns_the_active_channel_with_50_percent_volume() {
        Channel active = new MockChannelBuilder()
                .onTick(1).thenReturn(BUFFER_1)
                .onTick(2).thenReturn(BUFFER_2)
                .build();
        Channel disabled = new MockChannelBuilder()
                .onTick(1).thenReturn(ZERO_ENERGY_BUFFER)
                .onTick(2).thenReturn(ZERO_ENERGY_BUFFER)
                .build();
        Mixer mixer = new Mixer(ImmutableList.of(disabled, active));
        short[][] BUFFER_1_ADJUSTED = adjustVolume(BUFFER_1, 2);
        short[][] BUFFER_2_ADJUSTED = adjustVolume(BUFFER_2, 2);

        assertArrayEquals(new short[][][]{BUFFER_1_ADJUSTED}, mixer.stepAhead(1));
        assertArrayEquals(new short[][][]{BUFFER_2_ADJUSTED}, mixer.stepAhead(1));
    }

    @Test
    public void test_mixer_with_two_active_channels_mixes_them_correctly() {
        Channel channel1 = new MockChannelBuilder()
                .onTick(1).thenReturn(new short[][] {
                        new short[] {0x1000, 0x2000, 0x3000},
                        new short[] {0x4000, 0x5000, 0x6000}
                }).onTick(2).thenReturn(new short[][] {
                        new short[] {-0x1000, -0x2000, -0x3000},
                        new short[] {-0x4000, -0x5000, -0x6000}
                }).build();
        Channel channel2 = new MockChannelBuilder()
                .onTick(1).thenReturn(new short[][] {
                        new short[] {0x0100, 0x0200, 0x0300},
                        new short[] {-0x0100, -0x0200, -0x0300}
                }).onTick(2).thenReturn(new short[][] {
                        new short[] {0x0400, 0x0500, 0x0600},
                        new short[] {-0x0400, -0x0500, -0x0600}
                }).build();
        Mixer mixer = new Mixer(ImmutableList.of(channel1, channel2));

        short[][][] expected1 = new short[][][] {
                new short[][] {
                        new short[] {0x0880, 0x1100, 0x1980},
                        new short[] {0x1f80, 0x2700, 0x2e80}
                }
        };
        short[][][] expected2 = new short[][][] {
                new short[][] {
                        new short[] {-0x0600, -0x0d80, -0x1500},
                        new short[] {-0x2200, -0x2a80, -0x3300}
                }
        };
        assertArrayEquals(expected1, mixer.stepAhead(1));
        assertArrayEquals(expected2, mixer.stepAhead(1));
    }

    @Test
    public void test_mixer_with_three_active_channels_mixes_them_correctly() {
        Channel channel1 = new MockChannelBuilder()
                .onTick(1).thenReturn(new short[][] {
                        new short[] {0x3000, 0x6000, -0x3000, 0x03c0},
                        new short[] {0x6000, -0x6000, 0x3000, -0x3000}
                }).build();
        Channel channel2 = new MockChannelBuilder()
                .onTick(1).thenReturn(new short[][] {
                        new short[] {0x0300, 0x0600, 0x0900, 0x03c0},
                        new short[] {-0x6c00, -0x66c3, 0x30c0, -0x3ccc}
                }).build();
        Channel channel3 = new MockChannelBuilder()
                .onTick(1).thenReturn(new short[][] {
                        new short[] {0x0306, 0x660c, 0x09c0, 0x03c0},
                        new short[] {0x360c, 0x06c3, -0x30c0, -0x3ccc}
                }).build();
        Mixer mixer = new Mixer(ImmutableList.of(channel1, channel2, channel3));
        short[][][] expected = new short[][][] {
                new short[][]{
                        new short[]{0x1202, 0x4404, -0x09c0, 0x03c0},
                        new short[]{0x0e04, -0x4000, 0x1000, -0x3888}
                }
        };
        assertArrayEquals(expected, mixer.stepAhead(1));
    }


    private static List<Channel> createDummyChannels(int numChannels) {
        List<Channel> channels = IntStream.range(0, numChannels)
                .mapToObj(idx -> mock(Channel.class))
                .collect(Collectors.toList());
        channels.forEach(channel -> {
            when(channel.stepAhead(anyInt())).thenReturn(new short[0][][]);
        });
        return channels;
    }

    private void verifyTicks(List<Channel> channels, Integer... ticks) {
        channels.forEach(channel -> {
            ArgumentCaptor<Integer> args = ArgumentCaptor.forClass(Integer.class);
            verify(channel, atLeast(0)).stepAhead(args.capture());
            assertEquals(Arrays.asList(ticks), args.getAllValues());
        });
    }

    private short[][] adjustVolume(short[][] buffer, int ratio) {
        short[][] newBuffer = new short[][] { new short[3], new short[3] };
        for (int i = 0; i < 3; i++) {
            newBuffer[0][i] = (short)(buffer[0][i] / ratio);
            newBuffer[1][i] = (short)(buffer[1][i] / ratio);
        }
        return newBuffer;
    }
}
