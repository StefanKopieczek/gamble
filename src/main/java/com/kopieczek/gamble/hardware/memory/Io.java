package com.kopieczek.gamble.hardware.memory;

import java.awt.*;

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
    int getLcdCurrentLine();
    void setLcdCurrentLine(int i);
    int getLyCompare();
    int getWindowY();
    int getWindowX();
    Color getShadeForBackgroundColor(int colorId);
    Color getShadeForPalette0Color(int colorId);
    Color getShadeForPalette1Color(int colorId);
    boolean areTileMapEntriesSigned();
    boolean isButtonPressed(Button button);
    void setButtonPressed(Button button, boolean isPressed);
    boolean isTimerEnabled();

    enum LcdControllerMode {
        HBLANK,
        VBLANK,
        OAM_READ,
        DATA_TRANSFER;
    }

    enum Button {
        A(false),
        B(false),
        START(false),
        SELECT(false),
        UP(true),
        DOWN(true),
        LEFT(true),
        RIGHT(true);

        final boolean isDirectional;

        Button(boolean isDirectional) {
            this.isDirectional = isDirectional;
        }

        public boolean isDirectional() {
            return isDirectional;
        }
    }
}
