package com.kopieczek.gamble.hardware.audio;

abstract class Channel {
    public abstract byte[][][] stepAhead(int ticks);
}
