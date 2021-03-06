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
    private static final boolean USE_BLOCKING_AUDIO = true;
    private static final int NUM_CHANNELS = 2;
    private static final int SAMPLE_WIDTH_BYTES = 2;
    private static final int FRAME_WIDTH_BYTES = NUM_CHANNELS * SAMPLE_WIDTH_BYTES;
    private static final int SAMPLE_RATE = 44000;
    private static final int BUFFER_SIZE = SAMPLE_RATE / 20;
    private static final float EXPECTED_BUFFERS_PER_SEC = SAMPLE_RATE / ((float)BUFFER_SIZE / FRAME_WIDTH_BYTES);
    private static final int MAX_BUFFERS = (int)(EXPECTED_BUFFERS_PER_SEC * 0.2);

    private final Downsampler downsampler;
    private SourceDataLine lineOut;

    private final BlockingQueue<byte[]> buffers = buildBufferQueue();
    private byte[] buffer = new byte[BUFFER_SIZE];
    private int bufPtr = 0;


    private static final AudioFormat FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
        SAMPLE_RATE,
        SAMPLE_WIDTH_BYTES * 8,
         NUM_CHANNELS,
        4,
        SAMPLE_RATE,
        false);

    public StereoRenderer() {
        downsampler = new FilteringDecimator(ButterworthFilter.class);
        downsampler.setInputFrequency(Governor.FREQUENCY_HZ);
        downsampler.setOutputFrequency(SAMPLE_RATE);
    }

    public void init() throws LineUnavailableException {
        lineOut = AudioSystem.getSourceDataLine(FORMAT);
        lineOut.open(FORMAT, BUFFER_SIZE);
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
        try {
            System.arraycopy(bytes, 0, buffer, bufPtr, bytes.length);
            bufPtr += bytes.length;
            if (bufPtr == buffer.length) {
                buffers.put(buffer);
                buffer = new byte[BUFFER_SIZE];
                bufPtr = 0;
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted while blocking on audio buffer", e);
            Thread.currentThread().interrupt();
        }
    }

    private static byte[] convert(short[] sample) {
        byte[] result = new byte[FRAME_WIDTH_BYTES];
        ByteBuffer bb = ByteBuffer.wrap(result);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.asShortBuffer().put(sample);
        return result;
    }

    private static LinkedBlockingQueue<byte[]> buildBufferQueue() {
        if (USE_BLOCKING_AUDIO) {
            return new LinkedBlockingQueue<>(MAX_BUFFERS);
        } else {
            return new LinkedBlockingQueue<>();
        }
    }

    private class RenderThread extends Thread {
        private static final int ARRIVAL_METRIC_PERIOD = 20;
        LinkedList<Long> bufferArrivalTimes = new LinkedList<>();

        @Override
        public void run() {
            try {
                while (true) {
                    // Drop excess buffers if we have have a backlog
                    int dropped = 0;
                    if(buffers.size() > MAX_BUFFERS) {
                        while(buffers.size() > MAX_BUFFERS / 2) {
                            buffers.take();
                            dropped += 1;
                        }
                    }
                    if (dropped > 0) {
                        log.debug("Dropped {} buffers to reduce backlog", dropped);
                    }

                    // Take and render one buffer
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

                if (performanceRatio < 0.8f) {
                    log.debug("Audio buffer latency detected; avg wait is {}ms, max permissible is {}ms. Performance ratio: {}", avgWait, maxPermittedWait, performanceRatio);
                } else {
                    log.debug("Current audio buffer performance ratio: {}", performanceRatio);
                }
            }
        }
    }
}
