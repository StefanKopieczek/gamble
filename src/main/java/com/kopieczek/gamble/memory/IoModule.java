package com.kopieczek.gamble.memory;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

class IoModule extends SimpleMemoryModule implements Io {
    private static final int LCD_CONTROL_ADDR = 0x0040;

    IoModule() {
        super(Mmu.IO_AREA_SIZE);
    }

    @Override
    public boolean isLcdDisplayEnabled() {
        return isHigh(LCD_CONTROL_ADDR, 7);
    }

    @Override
    public int getWindowTileMapStartAddress() {
        return isHigh(LCD_CONTROL_ADDR, 6) ? 0x9c00 : 0x9800;
    }

    @Override
    public int getWindowTileMapEndAddress() {
        return isHigh(LCD_CONTROL_ADDR, 6) ? 0x9fff : 0x9bff;
    }

    @Override
    public boolean isWindowDisplayEnabled() {
        return isHigh(LCD_CONTROL_ADDR, 5);
    }

    @Override
    public int getTileDataStartAddress() {
        return isHigh(LCD_CONTROL_ADDR, 4) ? 0x8000 : 0x8800;
    }

    @Override
    public int getTileDataEndAddress() {
        return isHigh(LCD_CONTROL_ADDR, 4) ? 0x8fff : 0x97ff;
    }

    @Override
    public int getBackgroundTileMapStartAddress() {
        return isHigh(LCD_CONTROL_ADDR, 3) ? 0x9c00 : 0x9800;
    }

    @Override
    public int getBackgroundTileMapEndAddress() {
        return isHigh(LCD_CONTROL_ADDR, 3) ? 0x9fff : 0x9bff;
    }

    @Override
    public int getSpriteWidth() {
        return 8;
    }

    @Override
    public int getSpriteHeight() {
        return isHigh(LCD_CONTROL_ADDR, 2) ? 16 : 8;
    }

    @Override
    public boolean isSpriteDisplayEnabled() {
        return isHigh(LCD_CONTROL_ADDR, 1);
    }

    @Override
    public boolean isBackgroundDisplayEnabled() {
        return isHigh(LCD_CONTROL_ADDR, 0);
    }

    private boolean isHigh(int address, int bitIdx) {
        return (readByte(address) & (0x01 << bitIdx)) > 0;
    }
}
