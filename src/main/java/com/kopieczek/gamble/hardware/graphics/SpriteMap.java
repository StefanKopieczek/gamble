package com.kopieczek.gamble.hardware.graphics;

import com.kopieczek.gamble.hardware.memory.Io;
import com.kopieczek.gamble.hardware.memory.Oam;
import com.kopieczek.gamble.hardware.memory.Vram;

public class SpriteMap {
    private final Oam oam;
    private final Vram vram;

    SpriteMap(Io io, Oam oam, Vram vram) {
        this.oam = oam;
        this.vram = vram;
    }
}
