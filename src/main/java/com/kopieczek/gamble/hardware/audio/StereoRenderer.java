package com.kopieczek.gamble.hardware.audio;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class StereoRenderer implements Renderer {
    private static final Logger log = LogManager.getLogger(StereoRenderer.class);
    private static final int NUM_CHANNELS = 2;
    private static final int SAMPLE_WIDTH_BYTES = 2;
    private static final int FRAME_WIDTH_BYTES = NUM_CHANNELS * SAMPLE_WIDTH_BYTES;

    private SourceDataLine lineOut;
    private BlockingQueue<short[]> samples = new LinkedBlockingQueue<>();

    private static final AudioFormat FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_UNSIGNED,
        16000,
        SAMPLE_WIDTH_BYTES * 8,
         NUM_CHANNELS,
        2,
        16000,
        false);


    public void init() throws LineUnavailableException {
        lineOut = AudioSystem.getSourceDataLine(FORMAT);
        new RenderThread().start();
    }

    @Override
    public void render(short[] newSample) {
        samples.add(newSample);
    }

    private class RenderThread extends Thread {
        @Override
        public void run() {
            try {
                while (true) {
                    short[] sample = samples.take();
                    byte[] frameBytes = convert(sample);
                    lineOut.write(frameBytes, 0, FRAME_WIDTH_BYTES);
                }
            } catch (InterruptedException e) {
                log.warn("Render thread interrupted - playback will stop");
            }
        }

        private byte[] convert(short[] sample) {
            byte[] result = new byte[FRAME_WIDTH_BYTES];
            ByteBuffer bb = ByteBuffer.wrap(result);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            bb.putShort(sample[0]);
            bb.putShort(sample[1]);

            return result;
        }
    }
}
