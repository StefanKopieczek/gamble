package com.kopieczek.gamble.hardware.memory;

import com.google.common.collect.ImmutableMap;
import com.kopieczek.gamble.hardware.cpu.Interrupt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

class IoModule extends RamModule implements Io {
    private static final Logger log = LogManager.getLogger(IoModule.class);

    private static final int JOYPAD_ADDR = 0x0000;
    private static final int TIMER_DIV_ADDR = 0x0004;
    private static final int TIMER_COUNTER_ADDR = 0x0005;
    private static final int TIMER_MODULO_ADDR = 0x0006;
    private static final int TIMER_CONTROL_ADDR = 0x0007;
    private static final int LCD_CONTROL_ADDR = 0x0040;
    private static final int LCD_STATUS_ADDR = 0x0041;
    private static final int SCROLL_Y_ADDR = 0x0042;
    private static final int SCROLL_X_ADDR = 0x0043;
    private static final int LCD_CURRENT_LINE_ADDR = 0x0044;
    private static final int LCD_LY_COMPARE_ADDR = 0x0045;
    private static final int DMA_TRANSFER_ADDR = 0x0046;
    private static final int BACKGROUND_PALETTE_ADDR = 0x0047;
    private static final int SPRITE_PALETTE_0_ADDR = 0x0048;
    private static final int SPRITE_PALETTE_1_ADDR = 0x0049;
    private static final int WINDOW_Y_POSITION_ADDR = 0x004a;
    private static final int WINDOW_X_POSITION_ADDR = 0x004b;
    private static final int BIOS_DISABLE_ADDR = 0x0050;
    private static final int LCD_MAX_LINE = 153;

    private static final Map<Io.LcdControllerMode, Integer> lcdControllerModeBits = ImmutableMap.of(
            LcdControllerMode.HBLANK, 0x00,
            LcdControllerMode.VBLANK, 0x01,
            LcdControllerMode.OAM_READ, 0x02,
            LcdControllerMode.DATA_TRANSFER, 0x03
    );

    private static final Map<Integer, Color> shadeMap = ImmutableMap.of(
            0, Color.WHITE,
            1, Color.LIGHT_GRAY,
            2, Color.DARK_GRAY,
            3, Color.BLACK
    );

    private static final Map<Button, Integer> buttonMasks = ImmutableMap.<Button, Integer>builder()
            .put(Button.A, 0x01)
            .put(Button.B, 0x02)
            .put(Button.SELECT, 0x04)
            .put(Button.START, 0x08)
            .put(Button.RIGHT, 0x01)
            .put(Button.LEFT, 0x02)
            .put(Button.UP, 0x04)
            .put(Button.DOWN, 0x08)
            .build();

    private final boolean[] buttonStates = new boolean[Button.values().length];
    private final java.util.List<SpriteChangeListener> spriteListeners = new ArrayList<>();
    private boolean areTallSpritesEnabled = false;

    // Used for OAM DMA copy and for setting interrupts
    private Mmu globalMemory;

    IoModule() {
        super(Mmu.IO_AREA_SIZE);
        initTriggersAndFilters();

        // Initialize key registers to hardware-standard values
        setByteDirect(JOYPAD_ADDR, 0x0f);
        setByteDirect(LCD_STATUS_ADDR, 0x86);
    }

    private void initTriggersAndFilters() {
        addFilter(JOYPAD_ADDR, Filter.readOnlyFilter(this, JOYPAD_ADDR, 0b00001111));
        addFilter(LCD_STATUS_ADDR, Filter.readOnlyFilter(this, LCD_STATUS_ADDR, 0b00000011));
        addTrigger(JOYPAD_ADDR, this::recalculateJoypadRegister);
        addTrigger(LCD_LY_COMPARE_ADDR, this::updateCoincidenceFlag);
        addTrigger(LCD_CURRENT_LINE_ADDR, this::updateCoincidenceFlag);
        addTrigger(LCD_CONTROL_ADDR, this::maybeFireSpriteHeightChange);
        addTrigger(SPRITE_PALETTE_0_ADDR, this::fireSpritePaletteChange);
        addTrigger(SPRITE_PALETTE_1_ADDR, this::fireSpritePaletteChange);
        addTrigger(DMA_TRANSFER_ADDR, this::doDmaTransfer);
        addTrigger(BIOS_DISABLE_ADDR, this::disableBios);
        addTrigger(TIMER_DIV_ADDR, () -> setByteDirect(TIMER_DIV_ADDR, 0x00));
    }

    void linkGlobalMemory(Mmu mmu) {
        globalMemory = mmu;
    }

    private void disableBios() {
        globalMemory.setBiosEnabled(false);
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
    public boolean areTileMapEntriesSigned() {
        return !isHigh(LCD_CONTROL_ADDR, 4);
    }

    @Override
    public boolean isButtonPressed(Button button) {
        return buttonStates[button.ordinal()];
    }

    @Override
    public void setButtonPressed(Button button, boolean isPressed) {
        boolean wasPressed = buttonStates[button.ordinal()];
        if (wasPressed != isPressed) {
            log.info("Button {} was {}", button, isPressed ? "pressed" : "released");
            buttonStates[button.ordinal()] = isPressed;
            recalculateJoypadRegister();
        }
    }

    @Override
    public boolean isTimerEnabled() {
        return isHigh(TIMER_CONTROL_ADDR, 2);
    }

    @Override
    public int getCyclesPerTimerCounterTick() {
        int controlBits = readByte(TIMER_CONTROL_ADDR) & 0b11;
        switch (controlBits) {
            case 0b00:
                return 1024;
            case 0b01:
                return 16;
            case 0b10:
                return 64;
            default:
                return 256;
        }
    }

    @Override
    public int getTimerDiv() {
        return readByte(TIMER_DIV_ADDR);
    }

    @Override
    public void setTimerDiv(int newValue) {
        setByteDirect(TIMER_DIV_ADDR, newValue);
    }

    @Override
    public int getTimerCounter() {
        return readByte(TIMER_COUNTER_ADDR);
    }

    @Override
    public void setTimerCounter(int newValue) {
        setByteDirect(TIMER_COUNTER_ADDR, newValue);
    }

    @Override
    public void resetTimerCounter() {
        setByteDirect(TIMER_COUNTER_ADDR, readByte(TIMER_MODULO_ADDR));
    }

    private void recalculateJoypadRegister() {
        int oldJoypadValue = readByte(JOYPAD_ADDR);
        boolean shouldSelectButtons = (oldJoypadValue & 0x20) == 0;
        boolean shouldSelectDirections = (oldJoypadValue & 0x10) == 0;

        // Bring low any bit corresponding to a pressed button that we're
        // currently selecting on.
        int newJoypadValue = Arrays.stream(Button.values())
                .filter(this::isButtonPressed)
                .filter(button ->
                    (shouldSelectDirections && button.isDirectional()) ||
                    (shouldSelectButtons && !button.isDirectional())
                )
                .mapToInt(buttonMasks::get)
                .reduce(0x0f, (a, b) -> a & ~b);

        // Leave selectButtons / selectDirections control bits unchanged.
        newJoypadValue += (0xf0 & oldJoypadValue);

        boolean newButtonsPressed = ((oldJoypadValue ^ newJoypadValue) & oldJoypadValue) > 0;
        if (newButtonsPressed) {
            globalMemory.setInterrupt(Interrupt.JOYPAD);
        }

        setByteDirect(JOYPAD_ADDR, newJoypadValue);
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

    @Override
    public void handleOam() {
        if (shouldStatInterruptFor(LcdStatEvent.OAM)) {
            globalMemory.setInterrupt(Interrupt.LCD_STAT);
        }
    }

    @Override
    public void handleHBlank() {
        if (shouldStatInterruptFor(LcdStatEvent.HBLANK)) {
            globalMemory.setInterrupt(Interrupt.LCD_STAT);
        }
    }

    @Override
    public void handleVBlank() {
        if (shouldStatInterruptFor(LcdStatEvent.VBLANK)) {
            globalMemory.setInterrupt(Interrupt.LCD_STAT);
        }
    }

    @Override
    public void setLcdControllerMode(LcdControllerMode mode) {
        final int oldValue = readByte(LCD_STATUS_ADDR);
        final int modeBits = lcdControllerModeBits.get(mode);
        final int newValue = (oldValue & 0xfc) + modeBits;
        setByteDirect(LCD_STATUS_ADDR, newValue);
    }

    @Override
    public boolean shouldStatInterruptFor(LcdStatEvent event) {
        int statusByte = readByte(LCD_STATUS_ADDR);
        switch (event) {
            case COINCIDENCE:
                return (statusByte & 0x40) > 0;
            case OAM:
                return (statusByte & 0x20) > 0;
            case VBLANK:
                return (statusByte & 0x10) > 0;
            case HBLANK:
                return (statusByte & 0x08) > 0;
            default:
                return false;
        }
    }

    @Override
    public int getScrollY() {
        return readByte(SCROLL_Y_ADDR);
    }

    @Override
    public int getScrollX() {
        return readByte(SCROLL_X_ADDR);
    }

    @Override
    public int getLcdCurrentLine() {
        return readByte(LCD_CURRENT_LINE_ADDR);
    }

    @Override
    public void setLcdCurrentLine(int value) {
        if (value <= LCD_MAX_LINE) {
            setByte(LCD_CURRENT_LINE_ADDR, value);
        } else {
            throw new IllegalArgumentException("LCD current line cannot be greater than 153 (is " +
                    value + ")");
        }
    }

    @Override
    public int getLyCompare() {
        return readByte(LCD_LY_COMPARE_ADDR);
    }

    @Override
    public int getWindowY() {
        return readByte(WINDOW_Y_POSITION_ADDR);
    }

    @Override
    public int getWindowX() {
        // 0xff4b holds the x position minus 7 pixels
        return readByte(WINDOW_X_POSITION_ADDR) + 7;
    }

    @Override
    public Color getShadeForBackgroundColor(int colorId) {
        // Bits 1-0 are color 0; bits 3-2 are color 1; ... ; bits 7-6 are color 3
        int shift = colorId * 2;
        int colorMask = 0x03 << shift;
        int shadeId = (readByte(BACKGROUND_PALETTE_ADDR) & colorMask) >> shift;
        return shadeMap.get(shadeId);
    }

    @Override
    public Color[] loadPalette0() {
        return loadPalette(0);
    }

    @Override
    public Color[] loadPalette1() {
        return loadPalette(1);
    }

    private Color[] loadPalette(int paletteIdx) {
        Color[] palette = new Color[4];
        for (int idx = 0; idx < 4; idx++) {
            palette[idx] = getShadeForPaletteColor(paletteIdx, idx);
        }
        return palette;
    }

    private Color getShadeForPalette0Color(int colorId) {
        return getShadeForPaletteColor(0, colorId);
    }

    private Color getShadeForPalette1Color(int colorId) {
        return getShadeForPaletteColor(1, colorId);
    }

    @Override
    public int getSpriteDataStartAddress() {
        return 0x8000;
    }

    @Override
    public int getSpritePatternStartAddress() {
        return 0xfe00;
    }

    @Override
    public void register(SpriteChangeListener listener) {
        spriteListeners.add(listener);
    }

    private Color getShadeForPaletteColor(int paletteId, int colorId) {
        if (colorId == 0) {
            // Color 0 is always full transparency regardless of palette
            return new Color(255, 255, 255, 0);
        } else {
            // Bits 1-0 are unused; bits 3-2 are color 1; ... ; bits 7-6 are color 3
            int shift = (colorId * 2);
            int colorMask = 0x03 << shift;
            int paletteAddress = (paletteId == 0) ? SPRITE_PALETTE_0_ADDR : SPRITE_PALETTE_1_ADDR;
            int shadeId = (readByte(paletteAddress) & colorMask) >> shift;
            return shadeMap.get(shadeId);
        }
    }

    private void updateCoincidenceFlag() {
        final boolean wasEnabled = (readByte(LCD_STATUS_ADDR) & 0x04) > 0;
        final boolean isEnabled = (getLcdCurrentLine() == getLyCompare());
        setBit(LCD_STATUS_ADDR, 2, isEnabled);

        boolean hasChanged = (wasEnabled != isEnabled);
        boolean isSelected = shouldStatInterruptFor(LcdStatEvent.COINCIDENCE);

        if (hasChanged && isEnabled && isSelected) {
            globalMemory.setInterrupt(Interrupt.LCD_STAT);
        }
    }

    private void doDmaTransfer() {
        globalMemory.doDmaTransfer(readByte(DMA_TRANSFER_ADDR));
    }

    private void maybeFireSpriteHeightChange() {
        boolean oldValue = this.areTallSpritesEnabled;
        boolean newValue = getSpriteHeight() == 16;
        this.areTallSpritesEnabled = newValue;
        if (oldValue != newValue) {
            spriteListeners.forEach(l -> l.onSpriteHeightChanged(newValue));
        }
    }

    private void fireSpritePaletteChange() {
        spriteListeners.forEach(l -> l.onSpritePaletteChanged());
    }

    private boolean isHigh(int address, int bitIdx) {
        return (readByte(address) & (0x01 << bitIdx)) > 0;
    }

    private void setBit(int address, int bitIdx, boolean isSet) {
        final int oldValue = readByte(address);
        final int newValue;
        if (isSet) {
            newValue = oldValue | (0x01 << bitIdx);
        } else {
            newValue = oldValue & ~(0x01 << bitIdx);
        }
        setByte(address, newValue);
    }
}
