package com.kopieczek.gamble.memory;

public interface Io {
    boolean isLcdDisplayEnabled();
    int getWindowTileMapStartAddress();
    int getWindowTileMapEndAddress();
    boolean isWindowDisplayEnabled();
    int getTileDataStartAddress();
    int getTileDataEndAddress();
    int getBackgroundTileMapStartAddress();
    int getBackgroundTileMapEndAddress();
    int getSpriteWidth();
    int getSpriteHeight();
    boolean isSpriteDisplayEnabled();
    boolean isBackgroundDisplayEnabled();
    void setOamInterrupt(boolean isInterrupted);
    void setVBlankInterrupt(boolean isInterrupted);
    void setHBlankInterrupt(boolean isInterrupted);
    void setLcdControllerMode(LcdControllerMode mode);
    int getScrollY();
    int getScrollX();

    enum LcdControllerMode {
        HBLANK,
        VBLANK,
        OAM_READ,
        DATA_TRANSFER;
    }
}
