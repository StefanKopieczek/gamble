package com.kopieczek.gamble.hardware.audio;

public class ButterworthFilter implements Filter {
    private int frequency;
    private double[] xv = new double[3];
    private double[] yv = new double[3];

    // TODO derive these on the fly
    // Currently using http://www-users.cs.york.ac.uk/~fisher/cgi-bin/mkfscript
    double gain = 1.161559968e+03;
    double h0 = -0.9187499897;
    double h1 = -0.9187499897;

    @Override
    public void setFrequency(int newValue) {
        frequency = newValue;
    }

    @Override
    public short filter(short sample) {
        double input = normalize(sample);
        xv[0] = xv[1];
        xv[1] = xv[2];
        xv[2] = input / gain;
        yv[0] = yv[1];
        yv[1] = yv[2];
        yv[2] = (xv[0] + xv[2]) + 2 * xv[1] + (h0 * yv[0]) + (h1 * yv[1]);
        return (short)(denormalize(yv[2]) * 100);
    }
}
