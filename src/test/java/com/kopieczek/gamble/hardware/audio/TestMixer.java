package com.kopieczek.gamble.hardware.audio;

import org.apache.logging.log4j.core.util.Integers;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TestMixer {
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
