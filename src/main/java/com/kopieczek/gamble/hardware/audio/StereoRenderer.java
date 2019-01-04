package com.kopieczek.gamble.hardware.audio;

import com.kopieczek.gamble.hardware.governor.Governor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class StereoRenderer implements Renderer {
    private static final Logger log = LogManager.getLogger(StereoRenderer.class);
    private static final boolean DYNAMIC_FREQ_ADJUST_ENABLED = false;
    private static final int NUM_CHANNELS = 2;
    private static final int SAMPLE_WIDTH_BYTES = 2;
    private static final int FRAME_WIDTH_BYTES = NUM_CHANNELS * SAMPLE_WIDTH_BYTES;
    private static final int SAMPLE_RATE = 44000;
    private static final int BUFFER_SIZE = SAMPLE_RATE / 5;
    private static final float EXPECTED_BUFFERS_PER_SEC = SAMPLE_RATE / ((float)BUFFER_SIZE / FRAME_WIDTH_BYTES);

    private final Downsampler downsampler;
    private SourceDataLine lineOut;

    private final BlockingQueue<byte[]> buffers = new LinkedBlockingQueue<>();
    private byte[] buffer = new byte[BUFFER_SIZE];
    private int bufPtr = 0;


    private static final AudioFormat FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
        SAMPLE_RATE,
        SAMPLE_WIDTH_BYTES * 8,
         NUM_CHANNELS,
        4,
        16000,
        false);

    public StereoRenderer() {
        downsampler = new FilteringDecimator(ButterworthFilter.class);
        downsampler.setInputFrequency(Governor.FREQUENCY_HZ);
        downsampler.setOutputFrequency(SAMPLE_RATE);
    }

    public void init() throws LineUnavailableException {
        lineOut = AudioSystem.getSourceDataLine(FORMAT);
        lineOut.open(FORMAT, BUFFER_SIZE * 2);
        lineOut.start();
        new RenderThread().start();
    }

    @Override
    public void render(short[] newSample) {
        short[] downsampled = downsampler.accept(newSample);
        if (downsampled != null) {
            byte[] encoded = convert(downsampled);
            buffer(encoded);
        }
    }

    private void buffer(byte[] bytes) {
        System.arraycopy(bytes, 0, buffer, bufPtr, bytes.length);
        bufPtr += bytes.length;
        if (bufPtr == buffer.length) {
            buffers.add(buffer);
            buffer = new byte[BUFFER_SIZE];
            bufPtr = 0;
        }
    }

    private static byte[] convert(short[] sample) {
        byte[] result = new byte[FRAME_WIDTH_BYTES];
        ByteBuffer bb = ByteBuffer.wrap(result);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.asShortBuffer().put(sample);
        return result;
    }

    private class RenderThread extends Thread {
        private static final int ARRIVAL_METRIC_PERIOD = 20;
        LinkedList<Long> bufferArrivalTimes = new LinkedList<>();

        @Override
        public void run() {
            try {
                while (true) {
                    byte[] buffer = buffers.take();
                    lineOut.write(buffer, 0, buffer.length);
                    processLatencyStats();
                }
            } catch (InterruptedException e) {
                log.warn("Render thread interrupted - playback will stop");
            }
        }

        private void processLatencyStats() {
            bufferArrivalTimes.add(System.currentTimeMillis());
            if (bufferArrivalTimes.size() > ARRIVAL_METRIC_PERIOD) {
                bufferArrivalTimes.remove(0);
                float avgWait = (bufferArrivalTimes.get(ARRIVAL_METRIC_PERIOD - 1) - bufferArrivalTimes.get(0)) / (float)(ARRIVAL_METRIC_PERIOD - 1);
                float maxPermittedWait = 1000 / EXPECTED_BUFFERS_PER_SEC;
                float performanceRatio = maxPermittedWait / avgWait;

                if (performanceRatio < 0.95f) {
                    log.warn("Audio buffer latency detected; avg wait is {}ms, max permissible is {}ms. Performance ratio: {}", avgWait, maxPermittedWait, performanceRatio);
                } else {
                    log.debug("Current audio buffer performance ratio: {}", performanceRatio);
                }

                if (DYNAMIC_FREQ_ADJUST_ENABLED) {
                    double nudge = (performanceRatio > 1) ? (0.02 * (1 - performanceRatio)) : (0.01 * (1 - performanceRatio));
                    double freqRatio = 1 + nudge;
                    int oldFreq = downsampler.getOutputFrequency();
                    int newFreq = (int) (oldFreq * freqRatio);
                    downsampler.setOutputFrequency(newFreq);
                }
            }

            if (buffers.size() > 10) {
                log.warn("Audio playback backlog of {} seconds ({} buffers)", buffers.size() / EXPECTED_BUFFERS_PER_SEC, buffers.size());
            }
        }
    }
}
