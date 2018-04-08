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
}
