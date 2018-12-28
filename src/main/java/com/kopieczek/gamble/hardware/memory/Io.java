package com.kopieczek.gamble.hardware.memory;

import java.awt.*;

public interface Io extends TimerRegisters {
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
    void handleOam();
    void handleHBlank();
    void handleVBlank();
    void setLcdControllerMode(LcdControllerMode mode);
    boolean shouldStatInterruptFor(LcdStatEvent coincidence);
    int getScrollY();
    int getScrollX();
    int getLcdCurrentLine();
    void setLcdCurrentLine(int i);
    int getLyCompare();
    int getWindowY();
    int getWindowX();
    Color getShadeForBackgroundColor(int colorId);
    Color[] loadPalette0();
    Color[] loadPalette1();
    boolean areTileMapEntriesSigned();
    boolean isButtonPressed(Button button);
    void setButtonPressed(Button button, boolean isPressed);
    int getSpriteDataStartAddress();
    int getSpritePatternStartAddress();
    void register(SpriteChangeListener listener);
    int getSquare1SweepPeriod();
    int getSquare1SweepSign();
    int getSquare1SweepShift();
    boolean[] getSquare1DutyCycle();
    int getSquare1RemainingTime();
    int getSquare1StartingVolume();
    int getSquare1EnvelopeSign();
    int getSquare1EnvelopeStepLength();
    int getSquare1FrequencyCounter();
    boolean isSquare1ContinuousModeEnabled();
    boolean isSquare1Restarted();

    enum LcdControllerMode {
        HBLANK,
        VBLANK,
        OAM_READ,
        DATA_TRANSFER;
    }

    enum LcdStatEvent {
        COINCIDENCE,
        OAM,
        HBLANK,
        VBLANK
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
