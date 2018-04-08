package com.kopieczek.gamble.memory;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class TestIoModule {

    @Test
    public void test_lcd_is_disabled_when_0xff40_bit_7_is_low() {
        doRangedBitCheckTest(0xff40, 7, (mmu, isBit7High) ->
            assertEquals(isBit7High, mmu.getIo().isLcdDisplayEnabled())
        );
    }

    @Test
    public void test_get_window_tile_map_start_address_returns_0x9800_if_0xff40_bit_6_is_low_else_0x9c00() {
        doRangedBitCheckTest(0xff40, 6, (mmu, isBit6High) -> {
            final int expectedAddr = isBit6High ? 0x9c00 : 0x9800;
            assertEquals(expectedAddr, mmu.getIo().getWindowTileMapStartAddress());
        });
    }

    @Test
    public void test_get_window_tile_map_end_address_returns_0x9800_if_0xff40_bit_6_is_low_else_0x9c00() {
        doRangedBitCheckTest(0xff40, 6, (mmu, isBit6High) -> {
            final int expectedAddr = isBit6High ? 0x9fff : 0x9bff;
            assertEquals(expectedAddr, mmu.getIo().getWindowTileMapEndAddress());
        });
    }

    @Test
    public void test_window_display_is_enabled_when_0xff40_bit_5_is_high() {
        doRangedBitCheckTest(0xff40, 5, (mmu, isBit5High) ->
            assertEquals(isBit5High, mmu.getIo().isWindowDisplayEnabled())
        );
    }

    @Test
    public void test_tile_data_start_address_is_0x8000_when_0xff40_bit_4_is_high_else_0x8800() {
        doRangedBitCheckTest(0xff40, 4, (mmu, isBit4High) -> {
            final int expectedAddr = isBit4High ? 0x8000 : 0x8800;
            assertEquals(expectedAddr, mmu.getIo().getTileDataStartAddress());
        });
    }

    @Test
    public void test_tile_data_end_address_is_0x8fff_when_0xff40_bit_4_is_high_else_0x97ff() {
        doRangedBitCheckTest(0xff40, 4, (mmu, isBit4High) -> {
            final int expectedAddr = isBit4High ? 0x8fff : 0x97ff;
            assertEquals(expectedAddr, mmu.getIo().getTileDataEndAddress());
        });
    }

    @Test
    public void test_background_tile_map_start_address_is_0x9c00_when_0xff40_bit_3_is_high_else_0x9800() {
        doRangedBitCheckTest(0xff40, 3, (mmu, isBit3High) -> {
            final int expectedAddr = isBit3High ? 0x9c00 : 0x9800;
            assertEquals(expectedAddr, mmu.getIo().getBackgroundTileMapStartAddress());
        });
    }

    @Test
    public void test_background_tile_map_end_address_is_0x9fff_when_0xff40_bit_3_is_high_else_0x9bff() {
        doRangedBitCheckTest(0xff40, 3, (mmu, isBit3High) -> {
            final int expectedAddr = isBit3High ? 0x9fff : 0x9bff;
            assertEquals(expectedAddr, mmu.getIo().getBackgroundTileMapEndAddress());
        });
    }

    @Test
    public void test_sprite_width_is_always_8_regardless_of_0xff40_value() {
        doRangeTest(0xff40, mmu ->
            assertEquals(8, mmu.getIo().getSpriteWidth())
        );
    }

    @Test
    public void test_sprite_height_is_16_if_0xff40_bit_2_is_high_else_8() {
        doRangedBitCheckTest(0xff40, 2, (mmu, isBit2High) -> {
            final int expected = isBit2High ? 16 : 8;
            assertEquals(expected, mmu.getIo().getSpriteHeight());
        });
    }

    @Test
    public void test_sprite_display_is_enabled_if_0xff40_bit_1_is_high() {
        doRangedBitCheckTest(0xff40, 1, (mmu, isBit1High) ->
                assertEquals(isBit1High, mmu.getIo().isSpriteDisplayEnabled())
        );
    }

    @Test
    public void test_background_display_is_enabled_if_0xff_bit_0_is_high() {
        doRangedBitCheckTest(0xff40, 0, (mmu, isBit0High) ->
                assertEquals(isBit0High, mmu.getIo().isBackgroundDisplayEnabled())
        );
    }

    @Test
    public void test_set_oam_interrupt_changes_bit_5_of_0xff41() {
        doRangedBitSetTest(0xff41, 5, true, mmu ->
                mmu.getIo().setOamInterrupt(true)
        );
        doRangedBitSetTest(0xff41, 5, false, mmu ->
                mmu.getIo().setOamInterrupt(false)
        );
    }

    @Test
    public void test_set_v_blank_interrupt_changes_bit_4_of_0xff41() {
        doRangedBitSetTest(0xff41, 4, true, mmu ->
                mmu.getIo().setVBlankInterrupt(true)
        );
        doRangedBitSetTest(0xff41, 4, false, mmu ->
                mmu.getIo().setVBlankInterrupt(false)
        );
    }

    @Test
    public void test_set_v_blank_interrupt_changes_bit_3_of_0xff41() {
        doRangedBitSetTest(0xff41, 3, true, mmu ->
                mmu.getIo().setHBlankInterrupt(true)
        );
        doRangedBitSetTest(0xff41, 3, false, mmu ->
                mmu.getIo().setHBlankInterrupt(false)
        );
    }

    // *** TODO *** - test coincidence flag (0xff41 bit 2)

    @Test
    public void test_set_lcd_controller_mode_changes_bits_0_and_1_of_0xff41() {
        final Map<Io.LcdControllerMode, Integer> expectedBits = ImmutableMap.of(
            Io.LcdControllerMode.HBLANK, 0x00,
            Io.LcdControllerMode.VBLANK, 0x01,
            Io.LcdControllerMode.OAM_READ, 0x02,
            Io.LcdControllerMode.DATA_TRANSFER, 0x03
        );

        for (Io.LcdControllerMode mode : Io.LcdControllerMode.values()) {
            doRangeTest(0xff41, mmu -> {
                int oldValue = mmu.readByte(0xff41);
                mmu.getIo().setLcdControllerMode(mode);
                int newValue = mmu.readByte(0xff41);
                int modeBits = newValue & 0x03;
                assertEquals("Unexpected mode bits", (Object)expectedBits.get(mode), modeBits);
                boolean otherBitsChanged = (0xfc & (oldValue ^ newValue)) > 0;
                assertFalse(otherBitsChanged);
            });
        }
    }

    @Test
    public void test_get_scroll_y_returns_0xff42() {
        doRangeTest(0xff42, mmu ->
                assertEquals(mmu.readByte(0xff42), mmu.getIo().getScrollY())
        );
    }

    @Test
    public void test_get_scroll_x_returns_0xff43() {
        doRangeTest(0xff43, mmu ->
                assertEquals(mmu.readByte(0xff43), mmu.getIo().getScrollX())
        );
    }

    @Test
    public void test_set_lcd_current_line_sets_0xff44() {
        Mmu mmu = Mmu.build();
        for (int line = 0; line < 153; line++) {
            mmu.getIo().setLcdCurrentLine(line);
            assertEquals(line, mmu.readByte(0xff44));
        }
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_setting_lcd_current_line_to_154_throws_illegal_argument_exception() {
        Mmu mmu = Mmu.build();
        mmu.getIo().setLcdCurrentLine(154);
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_setting_lcd_current_line_to_200_throws_illegal_argument_exception() {
        Mmu mmu = Mmu.build();
        mmu.getIo().setLcdCurrentLine(200);
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_setting_lcd_current_line_to_255_throws_illegal_argument_exception() {
        Mmu mmu = Mmu.build();
        mmu.getIo().setLcdCurrentLine(255);
    }

    @Test
    public void test_get_lcd_current_line_returns_0xff44() {
        doRangeTest(0xff44, mmu ->
                assertEquals(mmu.readByte(0xff44), mmu.getIo().getLcdCurrentLine())
        );
    }

    private static void doRangeTest(int address, Consumer<Mmu> test) {
        Mmu mmu = Mmu.build();
        for (int value = 0x00; value < 0xff; value++) {
            mmu.setByte(address, value);
            test.accept(mmu);
        }
    }

    private static void doRangedBitCheckTest(int address, int bitIdx, final BiConsumer<Mmu, Boolean> test) {
        final int mask = 0x01 << bitIdx;
        doRangeTest(address, mmu -> {
           boolean bitState = (mmu.readByte(address) & mask) > 0;
           test.accept(mmu, bitState);
        });
    }

    private static void doRangedBitSetTest(int address, int bitIdx, boolean expected, Consumer<Mmu> setup) {
        final int mask = 0x01 << bitIdx;
        doRangeTest(address, mmu -> {
            int oldValue = mmu.readByte(address);
            setup.accept(mmu);
            int newValue = mmu.readByte(address);
            boolean isTargetBitHigh = (newValue & mask) > 0;
            assertEquals("Target bit had unexpected value", expected, isTargetBitHigh);
            boolean otherBitsChanged = ((oldValue ^ newValue) & ~mask) > 0;
            assertFalse(otherBitsChanged);
        });
    }
}
