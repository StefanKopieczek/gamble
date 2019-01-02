package com.kopieczek.gamble.hardware.audio;

public abstract class SquareWaveChannel extends Channel {
    private int tickCtr = 0;

    public short[] tick() {
        int stepLengthInTicks = getStepLengthInTicks();
        boolean duty[] = getDutyCycle();
        short volume = getVolume();
        int periodLength = stepLengthInTicks * duty.length;
        int dutyOffset = (tickCtr % periodLength) / stepLengthInTicks;
        boolean isHigh = duty[dutyOffset];
        short amplitude = isHigh ? volume : 0;
        tickCtr = (tickCtr + 1) % 32768;
//        if (tickCtr == 0) {
//            System.out.println(volume + " " + getFrequency());
//        }
        return new short[] {amplitude, amplitude};
    }

    private int getStepLengthInTicks() {
        int frequencyHz = getFrequency();
        float frequencyInTicks = (float)frequencyHz / Apu.MASTER_FREQUENCY_HZ;
        float stepFrequencyInTicks = frequencyInTicks * 8;
        return (int)(1 / stepFrequencyInTicks);
    }

    protected abstract int getFrequency();
    protected abstract short getVolume();
    protected abstract boolean[] getDutyCycle();
}
