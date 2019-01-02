package com.kopieczek.gamble.hardware.audio;

public class SineWaveTestChannel extends Channel {
    private final float frequencyHz;
    int tick = 0;

    SineWaveTestChannel(float frequencyHz) {
        this.frequencyHz = frequencyHz;
    }

    @Override
    public short[] tick() {
        short amplitude = amplitudeAt(tick);
        tick++;
        return new short[]{amplitude, amplitude};
    }

    private short amplitudeAt(int tick) {
        double seconds = tick / (double)Apu.MASTER_FREQUENCY_HZ;
        double frequencyAdjust = 2 * Math.PI * frequencyHz;
        double val = Math.sin(frequencyAdjust * seconds);
        return (short)(val * Short.MAX_VALUE);
    }

    public static void main(String[] args) {
        System.out.println(new SineWaveTestChannel(440).amplitudeAt(0));
        System.out.println(new SineWaveTestChannel(440).amplitudeAt(Apu.MASTER_FREQUENCY_HZ));
        System.out.println(new SineWaveTestChannel(440).amplitudeAt(Apu.MASTER_FREQUENCY_HZ / 1760));
    }
}
