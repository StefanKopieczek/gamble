package com.kopieczek.gamble.hardware.audio;

import java.lang.reflect.Constructor;

class FilteringDecimator implements Downsampler {
    private final Downsampler decimatorDelegate;
    private final Filter leftFilter;
    private final Filter rightFilter;

    <T extends Filter> FilteringDecimator(Class<T> filterClass) {
        decimatorDelegate = new SimpleDecimator();
        this.leftFilter = makeFilter(filterClass);
        this.rightFilter = makeFilter(filterClass);
    }

    <T extends Filter> T makeFilter(Class<T> filterClass) {
        try {
            Constructor<T> constructor = filterClass.getConstructor();
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setInputFrequency(int newValue) {
        decimatorDelegate.setInputFrequency(newValue);
        leftFilter.setFrequency(newValue);
        rightFilter.setFrequency(newValue);
    }

    @Override
    public void setOutputFrequency(int newValue) {
        decimatorDelegate.setOutputFrequency(newValue);
    }

    @Override
    public int getInputFrequency() {
        return decimatorDelegate.getInputFrequency();
    }

    @Override
    public int getOutputFrequency() {
        return decimatorDelegate.getOutputFrequency();
    }

    @Override
    public short[] accept(short[] sample) {
        short[] filtered = new short[] { leftFilter.filter(sample[0]), rightFilter.filter(sample[1]) };
        return decimatorDelegate.accept(filtered);
    }
}
