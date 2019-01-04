package com.kopieczek.gamble.hardware.memory;

import com.google.common.collect.ImmutableMap;
import com.kopieczek.gamble.hardware.audio.*;
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
    private static final int NR10_ADDR = 0x0010; // -PPP SNNN (holds Square 1 sweep's period, sign, and shift number)
    private static final int NR11_ADDR = 0x0011; // DDLL LLLL (holds Square 1 duty cycle and length counter (len = 64 - L)
    private static final int NR12_ADDR = 0x0012; // VVVV SLLL (holds Square 1's starting volume, envelope sign, and length of envelope steps)
    private static final int NR13_ADDR = 0x0013; // Bottom 8 bits of Square 1's frequency counter value
    private static final int NR14_ADDR = 0x0014; // IC-- -FFF (holds Square 1's Initialize and Continuous flags, as well as the top 3 bits of its frequency counter)
    private static final int NR21_ADDR = 0x0016; // DDLL LLLL (holds Square 2 duty cycle and length counter (len = 64 - L)
    private static final int NR22_ADDR = 0x0017; // VVVV SLLL (holds Square 2's starting volume, envelope sign, and length of envelope steps)
    private static final int NR23_ADDR = 0x0018; // Bottom 8 bits of Square 2's frequency counter value
    private static final int NR24_ADDR = 0x0019; // IC-- -FFF (holds Square 2's Initialize and Continuous flags, as well as the top 3 bits of its frequency counter)
    private static final int NR30_ADDR = 0x001a; // E--- ---- (holds the Wave channel's DAC enable bit; other bits unused)
    private static final int NR31_ADDR = 0x001b; // LLLL LLLL (holds the Wave channel's length counter (len = 256 - L)
    private static final int NR32_ADDR = 0x001c; // -VV- ---- (holds the Wave channel's volume: 00=0%, 01=100%, 10=50%, 11=25%)
    private static final int NR33_ADDR = 0x001d; // Bottom 8 bits of the Wave channel's frequency counter value
    private static final int NR34_ADDR = 0x001e; // IC-- -FFF (holds the Wave channel's Initialize and Continuous flags, as well as the top 3 bits of its frequency counter)
    private static final int NR41_ADDR = 0x0020; // --LL LLLL (holds the Noise channel's length counter (len = 64 - L)
    private static final int NR42_ADDR = 0x0021; // VVVV SLLL (holds then Noise channel's starting volume, envelope sign, and length of envelope steps)
    private static final int NR43_ADDR = 0x0022; // FFFF WCCC (holds the Noise channel's frequency counter value, LSFR width mode, and divisor code)
    private static final int NR44_ADDR = 0x0023; // IC-- ---- (holds the Noise channel's Initialize and Continuous flags)
    private static final int NR51_ADDR = 0x0025; // dcba DCBA (channel L/R enable; lowercase=left, uppercase=right, a = channel_1, b = channel_2, ...)
    private static final int NR52_ADDR = 0x0026; // M--- 4321 (sound master enable, plus R/O flags indicating whether sound is currently playing on each channel)
    private static final int WAVE_DATA_ADDR = 0x0030;
    private static final int WAVE_DATA_SIZE_BYTES = 16;
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

    private static final boolean[][] squareWaveDutyCycles = new boolean[][] {
            new boolean[] {false, false, false, false, false, false, false, true},
            new boolean[] {true, false, false, false, false, false, false, true},
            new boolean[] {true, false, false, false, false, true, true, true},
            new boolean[] {false, true, true, true, true, true, true, false}
    };

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
    private final java.util.List<Square1RegisterListener> square1Listeners = new ArrayList<>();
    private final java.util.List<Square2RegisterListener> square2Listeners = new ArrayList<>();
    private final java.util.List<WaveRegisterListener> waveListeners = new ArrayList<>();
    private final java.util.List<NoiseRegisterListener> noiseListeners = new ArrayList<>();
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
        addTrigger(NR11_ADDR, this::fireSquare1DutyAndLengthChange);
        addTrigger(NR14_ADDR, this::maybeFireSquare1Trigger);
        addTrigger(NR21_ADDR, this::fireSquare2DutyAndLengthChange);
        addTrigger(NR24_ADDR, this::maybeFireSquare2Trigger);
        addTrigger(NR31_ADDR, this::fireWaveLengthChange);
        addTrigger(NR34_ADDR, this::maybeFireWaveTrigger);
        addTrigger(NR41_ADDR, this::fireNoiseLengthChange);
        addTrigger(NR44_ADDR, this::maybeFireNoiseTrigger);
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
        // 0xff4b holds the x position plus 7 pixels
        return readByte(WINDOW_X_POSITION_ADDR) - 7;
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

    @Override
    public void register(Square1RegisterListener listener) {
        square1Listeners.add(listener);
    }

    @Override
    public void register(Square2RegisterListener listener) {
        square2Listeners.add(listener);
    }

    @Override
    public void register(WaveRegisterListener listener) {
        waveListeners.add(listener);
    }

    @Override
    public void register(NoiseRegisterListener listener) {
        noiseListeners.add(listener);
    }

    private void fireSquare1DutyAndLengthChange() {
        square1Listeners.forEach(l -> l.onLengthCounterUpdated(getSquare1RemainingTime()));
        // TODO duty
    }

    @Override
    public int getSquare1SweepPeriod() {
        return 0x07 & (readByte(NR10_ADDR) >> 4);
    }

    @Override
    public int getSquare1SweepSign() {
        boolean shouldNegate = (readByte(NR10_ADDR) & 0x08) > 0;
        return shouldNegate ? -1 : +1;
    }

    @Override
    public int getSquare1SweepShift() {
        return 0x07 & readByte(NR10_ADDR);
    }

    @Override
    public boolean[] getSquare1DutyCycle() {
        int dutyIdx = readByte(NR11_ADDR) >> 6;
        return squareWaveDutyCycles[dutyIdx];
    }

    private int getSquare1RemainingTime() {
        return 64 - (0x3f & readByte(NR11_ADDR));
    }

    @Override
    public int getSquare1StartingVolume() {
        return readByte(NR12_ADDR) >> 4;
    }

    @Override
    public int getSquare1EnvelopeSign() {
        boolean isBitHigh = (readByte(NR12_ADDR) & 0x08) > 0;
        return isBitHigh ? +1 : -1;
    }

    @Override
    public int getSquare1EnvelopeStepLength() {
        return readByte(NR12_ADDR) & 0x07;
    }

    @Override
    public int getSquare1FrequencyCounter() {
        int lsb = readByte(NR13_ADDR);
        int msb = readByte(NR14_ADDR) & 0x07;
        return (msb << 8) + lsb;
    }

    @Override
    public void setSquare1FrequencyCounter(int newValue) {
        if (newValue < 0 || newValue > 0x7ff) {
            throw new IllegalArgumentException("Frequency counter value " + newValue + " exceeds bounds 0<=fc<0x800");
        }

        int lsb = newValue & 0xff;
        int msb = (newValue & 0x07) >> 8;
        int newNr13 = lsb;
        int oldNr14 = readByte(NR14_ADDR);
        int newNr14 = (oldNr14 & 0xf8) + (msb & 0x07);
        setByte(NR13_ADDR, newNr13);
        setByte(NR14_ADDR, newNr14);
    }

    @Override
    public boolean isSquare1ContinuousModeEnabled() {
        return (readByte(NR14_ADDR) & 0x40) == 0;
    }

    private boolean isSquare1Restarted() {
        return (readByte(NR14_ADDR) & 0x80) > 0;
    }

    private void maybeFireSquare1Trigger() {
        if (isSquare1Restarted()) {
            square1Listeners.forEach(Square1RegisterListener::onTrigger);
        }
    }

    @Override
    public void clearSquare1RestartFlag() {
        int oldValue = readByte(NR14_ADDR);
        int newValue = oldValue & 0x7f;
        setByte(NR14_ADDR, newValue);
    }

    private void fireSquare2DutyAndLengthChange() {
        square2Listeners.forEach(l -> l.onLengthCounterUpdated(getSquare2RemainingTime()));
        // TODO duty
    }

    @Override
    public boolean[] getSquare2DutyCycle() {
        int dutyIdx = readByte(NR21_ADDR) >> 6;
        return squareWaveDutyCycles[dutyIdx];
    }

    private int getSquare2RemainingTime() {
        return 64 - (0x3f & readByte(NR21_ADDR));
    }

    @Override
    public int getSquare2StartingVolume() {
        return readByte(NR22_ADDR) >> 4;
    }

    @Override
    public int getSquare2EnvelopeSign() {
        boolean isBitHigh = (readByte(NR22_ADDR) & 0x08) > 0;
        return isBitHigh ? +1 : -1;
    }

    @Override
    public int getSquare2EnvelopeStepLength() {
        return readByte(NR22_ADDR) & 0x07;
    }

    @Override
    public int getSquare2FrequencyCounter() {
        int lsb = readByte(NR23_ADDR);
        int msb = readByte(NR24_ADDR) & 0x07;
        return (msb << 8) + lsb;
    }

    @Override
    public void setSquare2FrequencyCounter(int newValue) {
        if (newValue < 0 || newValue > 0x7ff) {
            throw new IllegalArgumentException("Frequency counter value " + newValue + " exceeds bounds 0<=fc<0x800");
        }

        int lsb = newValue & 0xff;
        int msb = (newValue & 0x07) >> 8;
        int newNr23 = lsb;
        int oldNr24 = readByte(NR24_ADDR);
        int newNr24 = (oldNr24 & 0xf8) + (msb & 0x07);
        setByte(NR23_ADDR, newNr23);
        setByte(NR24_ADDR, newNr24);
    }

    @Override
    public boolean isSquare2ContinuousModeEnabled() {
        return (readByte(NR24_ADDR) & 0x40) == 0;
    }

    private boolean isSquare2Restarted() {
        return (readByte(NR24_ADDR) & 0x80) > 0;
    }

    private void maybeFireSquare2Trigger() {
        if (isSquare2Restarted()) {
            square2Listeners.forEach(Square2RegisterListener::onTrigger);
        }
    }

    @Override
    public void clearSquare2RestartFlag() {
        int oldValue = readByte(NR24_ADDR);
        int newValue = oldValue & 0x7f;
        setByte(NR24_ADDR, newValue);
    }

    private void fireWaveLengthChange() {
        waveListeners.forEach(l -> l.onLengthCounterUpdated(getWaveRemainingTime()));
    }

    @Override
    public boolean isWaveDacEnabled() {
        return (readByte(NR30_ADDR) & 0x80) > 0;
    }

    private int getWaveRemainingTime() {
        return 256 - readByte(NR31_ADDR);
    }

    @Override
    public int getWaveVolumePercent() {
        int volumeBits = (readByte(NR32_ADDR) & 0x60);
        switch (volumeBits) {
            case 0x20:
                return 100;
            case 0x40:
                return 50;
            case 0x60:
                return 25;
            default:
                return 0;
        }
    }

    @Override
    public int getWaveFrequencyCounter() {
        int lsb = readByte(NR33_ADDR);
        int msb = readByte(NR34_ADDR) & 0x07;
        return (msb << 8) + lsb;
    }

    @Override
    public void setWaveFrequencyCounter(int newValue) {
        if (newValue < 0 || newValue > 0x7ff) {
            throw new IllegalArgumentException("Frequency counter value " + newValue + " exceeds bounds 0<=fc<0x800");
        }

        int lsb = newValue & 0xff;
        int msb = (newValue & 0x07) >> 8;
        int newNr33 = lsb;
        int oldNr34 = readByte(NR34_ADDR);
        int newNr34 = (oldNr34 & 0xf8) + (msb & 0x07);
        setByte(NR33_ADDR, newNr33);
        setByte(NR34_ADDR, newNr34);
    }

    @Override
    public boolean isWaveContinuousModeEnabled() {
        return (readByte(NR34_ADDR) & 0x40) == 0;
    }

    private boolean isWaveRestarted() {
        return (readByte(NR34_ADDR) & 0x80) > 0;
    }

    private void maybeFireWaveTrigger() {
        if (isWaveRestarted()) {
            waveListeners.forEach(WaveRegisterListener::onTrigger);
        }
    }

    @Override
    public void clearWaveRestartFlag() {
        int oldValue = readByte(NR34_ADDR);
        int newValue = oldValue & 0x7f;
        setByte(NR34_ADDR, newValue);
    }

    private void fireNoiseLengthChange() {
        noiseListeners.forEach(l -> l.onLengthCounterUpdated(getNoiseRemainingTime()));
    }

    private int getNoiseRemainingTime() {
        return 64 - (0x3f & readByte(NR41_ADDR));
    }

    @Override
    public int getNoiseStartingVolume() {
        return readByte(NR42_ADDR) >> 4;
    }

    @Override
    public int getNoiseEnvelopeSign() {
        boolean isBitHigh = (readByte(NR42_ADDR) & 0x08) > 0;
        return isBitHigh ? +1 : -1;
    }

    @Override
    public int getNoiseEnvelopeStepLength() {
        return readByte(NR42_ADDR) & 0x07;
    }

    @Override
    public int getNoiseFrequencyShift() {
        return readByte(NR43_ADDR) >> 4;
    }

    @Override
    public boolean isNoiseWideModeEnabled() {
        return (readByte(NR43_ADDR) & 0x08) == 0;
    }

    @Override
    public float getNoiseFrequencyDivisor() {
        int code = readByte(NR43_ADDR) & 0x07;
        if (code == 0) {
            return 0.5f;
        } else {
            return code;
        }
    }

    @Override
    public boolean isNoiseContinuousModeEnabled() {
        return (readByte(NR44_ADDR) & 0x40) == 0;
    }

    public boolean isNoiseRestarted() {
        return (readByte(NR44_ADDR) & 0x80) > 0;
    }

    private void maybeFireNoiseTrigger() {
        if (isNoiseRestarted()) {
            noiseListeners.forEach(NoiseRegisterListener::onTrigger);
        }
    }

    @Override
    public void clearNoiseRestartFlag() {
        int oldValue = readByte(NR44_ADDR);
        int newValue = oldValue & 0x7f;
        setByte(NR44_ADDR, newValue);
    }

    @Override
    public AudioOutputMode getSquare1OutputMode() {
        return getAudioOutputMode(1);
    }

    @Override
    public AudioOutputMode getSquare2OutputMode() {
        return getAudioOutputMode(2);
    }

    @Override
    public AudioOutputMode getWaveOutputMode() {
        return getAudioOutputMode(3);
    }

    @Override
    public AudioOutputMode getNoiseOutputMode() {
        return getAudioOutputMode(4);
    }

    private AudioOutputMode getAudioOutputMode(int channel) {
        int controlByte = readByte(NR51_ADDR);

        // For channels a=1, b=2, ..., NR51 is represented bitwise as:
        //      dcba DCBA     (lowercase = left output, uppercase = right output)
        // thus the masks for channel n are 0x01<<(n+3) and 0x01<<(n-1).
        int leftEnableMask = (0x01 << (channel + 3));
        int rightEnableMask = (0x01 << (channel - 1));

        boolean leftEnable = (controlByte & leftEnableMask) > 0;
        boolean rightEnable = (controlByte & rightEnableMask) > 0;

        if (leftEnable && rightEnable) {
            return AudioOutputMode.STEREO;
        } else if (leftEnable) {
            return AudioOutputMode.LEFT_ONLY;
        } else if (rightEnable) {
            return AudioOutputMode.RIGHT_ONLY;
        } else {
            return AudioOutputMode.NONE;
        }
    }

    @Override
    public boolean isAudioOutputEnabled() {
        return (readByte(NR52_ADDR) & 0x80) > 0;
    }

    @Override
    public void setSquare1PlayingFlag(boolean isPlaying) {
        setIsPlayingFlag(1, isPlaying);
    }

    @Override
    public void setSquare2PlayingFlag(boolean isPlaying) {
        setIsPlayingFlag(2, isPlaying);
    }

    @Override
    public void setWavePlayingFlag(boolean isPlaying) {
        setIsPlayingFlag(3, isPlaying);
    }

    @Override
    public short[] getWaveData() {
        short[] data = new short[WAVE_DATA_SIZE_BYTES * 2];
        for (int idx = 0; idx < WAVE_DATA_SIZE_BYTES; idx++) {
            int samplePair = readByte(WAVE_DATA_ADDR + idx);
            data[idx * 2] = (short)(samplePair >> 4);
            data[idx * 2 + 1] = (short)(samplePair & 0x0f);
        }

        return data;
    }

    @Override
    public void setNoisePlayingFlag(boolean isPlaying) {
        setIsPlayingFlag(4, isPlaying);
    }

    private void setIsPlayingFlag(int channel, boolean isPlaying) {
        int oldNr52 = readByte(NR52_ADDR);
        int affectedBit = channel - 1;

        int newNr52;
        if (isPlaying) {
            int mask = 0x01 << affectedBit;
            newNr52 = oldNr52 | mask;
        } else {
            int mask = ~(0x01 << affectedBit);
            newNr52 = oldNr52 & mask;
        }

        setByte(NR52_ADDR, newNr52);
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
