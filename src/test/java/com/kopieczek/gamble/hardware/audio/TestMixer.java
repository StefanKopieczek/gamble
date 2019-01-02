package com.kopieczek.gamble.hardware.audio;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertArrayEquals;
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
        mixer.tick();
        verifyTicks(channels, 1);
    }

    @Test
    public void test_successive_mixer_ticks_are_passed_to_channels() {
        List<Channel> channels = createDummyChannels(8);
        Mixer mixer = new Mixer(channels);
        mixer.tick();
        mixer.tick();
        mixer.tick();
        mixer.tick();
        verifyTicks(channels, 4);
    }

    @Test
    public void test_unticked_mixer_does_not_tick_channels() {
        List<Channel> channels = createDummyChannels(11);
        new Mixer(channels);
        verifyTicks(channels, 0);
    }

    @Test
    public void test_mixer_with_one_channel_returns_samples_from_channel() {
        Channel channel = new MockChannelBuilder()
                .withLeftSamples(-0x1234, 0x5678)
                .withRightSamples(0x6abc, -0x0def)
                .build();
        Mixer mixer = new Mixer(ImmutableList.of(channel));
        assertArrayEquals(new short[]{-0x1234, 0x6abc}, mixer.tick());
        assertArrayEquals(new short[]{0x5678, -0x0def}, mixer.tick());
    }

    @Test
    public void test_mixer_with_one_active_and_one_disabled_channel_returns_the_active_channel_with_50_percent_volume() {
        Channel active = new MockChannelBuilder()
                .withLeftSamples(-0x2468, 0x68ac)
                .withRightSamples(-0x4c02, 0x44ca)
                .build();
        Channel disabled = new MockChannelBuilder()
                .withLeftSamples(0x0000, 0x0000)
                .withRightSamples(0x0000, 0x0000)
                .build();
        Channel expected = new MockChannelBuilder()
                .withLeftSamples(-0x1234, 0x3456)
                .withRightSamples(-0x2601, 0x2265)
                .build();
        Mixer mixer = new Mixer(ImmutableList.of(active, disabled));
        assertEquals(expected, mixer, 2);
    }

    @Test
    public void test_mixer_with_one_disabled_and_one_active_channel_returns_the_active_channel_with_50_percent_volume() {
        Channel active = new MockChannelBuilder()
                .withLeftSamples(-0x2468, 0x68ac)
                .withRightSamples(-0x4c02, 0x44ca)
                .build();
        Channel disabled = new MockChannelBuilder()
                .withLeftSamples(0x0000, 0x0000)
                .withRightSamples(0x0000, 0x0000)
                .build();
        Channel expected = new MockChannelBuilder()
                .withLeftSamples(-0x1234, 0x3456)
                .withRightSamples(-0x2601, 0x2265)
                .build();
        Mixer mixer = new Mixer(ImmutableList.of(disabled, active));
        assertEquals(expected, mixer, 2);
    }

    @Test
    public void test_mixer_with_two_active_channels_mixes_them_correctly() {
        Channel channel1 = new MockChannelBuilder()
                .withLeftSamples(0x1000, 0x2000, 0x3000, -0x1000, -0x2000, -0x3000)
                .withRightSamples(0x4000, 0x5000, 0x6000, -0x4000, -0x5000, -0x6000)
                .build();
        Channel channel2 = new MockChannelBuilder()
                .withLeftSamples(0x0100, 0x0200, 0x0300, 0x0400, 0x0500, 0x0600)
                .withRightSamples(-0x0100, -0x0200, -0x0300, -0x0400, -0x0500, -0x0600)
                .build();
        Channel expected = new MockChannelBuilder()
                .withLeftSamples(0x0880, 0x1100, 0x1980, -0x0600, -0x0d80, -0x1500)
                .withRightSamples(0x1f80, 0x2700, 0x2e80, -0x2200, -0x2a80, -0x3300)
                .build();
        Mixer mixer = new Mixer(ImmutableList.of(channel1, channel2));
        assertEquals(expected, mixer, 6);
    }

    @Test
    public void test_mixer_with_three_active_channels_mixes_them_correctly() {
        Channel channel1 = new MockChannelBuilder()
                .withLeftSamples(0x3000, 0x6000, -0x3000, 0x03c0)
                .withRightSamples(0x6000, -0x6000, 0x3000, -0x3000)
                .build();
        Channel channel2 = new MockChannelBuilder()
                .withLeftSamples(0x0300, 0x0600, 0x0900, 0x03c0)
                .withRightSamples(-0x6c00, -0x66c3, 0x30c0, -0x3ccc)
                .build();
        Channel channel3 = new MockChannelBuilder()
                .withLeftSamples(0x0306, 0x660c, 0x09c0, 0x03c0)
                .withRightSamples(0x360c, 0x06c3, -0x30c0, -0x3ccc)
                .build();
        Channel expected = new MockChannelBuilder()
                .withLeftSamples(0x1202, 0x4404, -0x09c0, 0x03c0)
                .withRightSamples(0x0e04, -0x4000, 0x1000, -0x3888)
                .build();
        Mixer mixer = new Mixer(ImmutableList.of(channel1, channel2, channel3));
        assertEquals(expected, mixer, 4);
    }


    private static List<Channel> createDummyChannels(int numChannels) {
        List<Channel> channels = IntStream.range(0, numChannels)
                .mapToObj(idx -> mock(Channel.class))
                .collect(Collectors.toList());
        channels.forEach(channel -> {
            when(channel.tick()).thenReturn(new short[] {0x0000, 0x0000});
        });
        return channels;
    }

    private void verifyTicks(List<Channel> channels, int tickCount) {
        channels.forEach(channel -> {
            verify(channel, times(tickCount)).tick();
        });
    }

    private void assertEquals(Channel expected, Channel actual, int length) {
        for (int idx = 0; idx < length; idx++) {
            short[] expectedSample = expected.tick();
            short[] actualSample = actual.tick();
            assertArrayEquals("Channels differ at sample " + idx, expectedSample, actualSample);
        }
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
