package com.kopieczek.gamble.hardware.audio;

import java.util.List;

class Mixer extends Channel {
    private final List<Channel> channels;

    Mixer(List<Channel> channels) {
        if (channels.size() >= Short.MAX_VALUE) {
            // Mixing might overflow if too many channels are permitted
            throw new IllegalArgumentException("At most " + Short.MAX_VALUE + " channels allowed");
        }

        this.channels = channels;
    }

    @Override
    public short[][][] stepAhead(int ticks) {
        short[][][][] buffersForEachChannel = new short[channels.size()][][][];
        for (int idx = 0; idx < channels.size(); idx++) {
            buffersForEachChannel[idx] = channels.get(idx).stepAhead(ticks);
        }

        return mix(buffersForEachChannel);
    }

    private static short[][][] mix(short[][][][] buffersForEachChannel) {
        // Here we assume (but don't assert) that each channel returns the same number of buffers at a given instant.
        int numBuffers = buffersForEachChannel[0].length;
        short[][][] output = new short[numBuffers][][];

        for (int bufIdx = 0; bufIdx < numBuffers; bufIdx++) {
            output[bufIdx] = mixBuffer(buffersForEachChannel, bufIdx);
        }

        return output;
    }

    private static short[][] mixBuffer(short[][][][] buffersForEachChannel, int bufIdx) {
        // Here we assume (but don't assert) that each channel returns equally sized buffers, and that the left and
        // right buffers are equal as well.
        int numChannels = buffersForEachChannel.length;
        int bufferSize = buffersForEachChannel[0][0][0].length;

        // First sum all amplitudes for each sample across channels.
        int[] leftTotals = new int[bufferSize];
        int[] rightTotals = new int[bufferSize];
        for (int chanIdx = 0; chanIdx < numChannels; chanIdx++) {
            for (int sampleIdx = 0; sampleIdx < bufferSize; sampleIdx++) {
                leftTotals[sampleIdx] += buffersForEachChannel[chanIdx][bufIdx][0][sampleIdx];
                rightTotals[sampleIdx] += buffersForEachChannel[chanIdx][bufIdx][1][sampleIdx];
            }
        }

        // Now scale the amplitudes according to the number of channels
        short[] left = new short[bufferSize];
        short[] right = new short[bufferSize];
        for (int idx = 0; idx < bufferSize; idx++) {
            left[idx] = (short)(leftTotals[idx] / numChannels);
            right[idx] = (short)(rightTotals[idx] / numChannels);
        }

        return new short[][] { left, right };
    }
}
