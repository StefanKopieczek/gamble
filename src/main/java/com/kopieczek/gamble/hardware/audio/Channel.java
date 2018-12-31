package com.kopieczek.gamble.hardware.audio;

abstract class Channel {
    public abstract short[][][] stepAhead(int ticks);
}
