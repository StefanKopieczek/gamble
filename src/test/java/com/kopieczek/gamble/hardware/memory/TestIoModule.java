package com.kopieczek.gamble.hardware.memory;

import com.google.common.collect.ImmutableMap;
import com.kopieczek.gamble.hardware.cpu.Interrupt;
import org.junit.Test;

import java.awt.*;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class TestIoModule {
    private static final ImmutableMap<Integer, Color> paletteMappings = ImmutableMap.of(
            0, Color.WHITE,
            1, Color.LIGHT_GRAY,
            2, Color.DARK_GRAY,
            3, Color.BLACK
    );

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
    public void test_are_tile_map_entries_signed_returns_false_when_0xff40_bit_4_is_high() {
        doRangedBitCheckTest(0xff40, 4, (mmu, isBit4High) ->
            assertEquals(!isBit4High, mmu.getIo().areTileMapEntriesSigned())
        );
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

    @Test
    public void test_get_ly_compare_returns_0xff45() {
        doRangeTest(0xff45, mmu ->
                assertEquals(mmu.readByte(0xff45), mmu.getIo().getLyCompare())
        );
    }

    @Test
    public void test_setting_ly_compare_equal_to_lcd_current_line_sets_coincidence_flag() {
        Mmu mmu = Mmu.build();
        for (int lcdLine = 0; lcdLine < 154; lcdLine++) {
            mmu.getIo().setLcdCurrentLine(lcdLine);
            for (int lyCompare = 0x00; lyCompare < 0xff; lyCompare++) {
                mmu.setByte(0xff45, lyCompare);
                boolean coincidenceFlag = (mmu.readByte(0xff41)  & 0x04) > 0;
                assertEquals(lcdLine == lyCompare, coincidenceFlag);
            }
        }
    }

    @Test
    public void test_setting_lcd_current_line_equal_to_ly_compare_sets_coincidence_flag() {
        Mmu mmu = Mmu.build();
        for (int lyCompare = 0x00; lyCompare < 0xff; lyCompare++) {
            mmu.setByte(0xff45, lyCompare);
            for (int lcdLine = 0; lcdLine < 154; lcdLine++) {
                mmu.getIo().setLcdCurrentLine(lcdLine);
                boolean coincidenceFlag = (mmu.readByte(0xff41) & 0x04) > 0;
                assertEquals(lcdLine == lyCompare, coincidenceFlag);
            }
        }
    }

    @Test
    public void test_setting_ly_compare_equal_to_lcd_current_line_fires_stat_interrupt() {
        Mmu mmu = Mmu.build();
        for (int lcdLine = 0; lcdLine < 154; lcdLine++) {
            mmu.getIo().setLcdCurrentLine(lcdLine);
            for (int lyCompare = 0x00; lyCompare < 0xff; lyCompare++) {
                mmu.resetInterrupt(Interrupt.LCD_STAT);
                mmu.setByte(0xff45, lyCompare);
                assertEquals(lcdLine == lyCompare, mmu.checkInterrupt(Interrupt.LCD_STAT));
            }
        }
    }

    @Test
    public void test_setting_lcd_current_line_equal_to_ly_compare_fires_stat_interrupt() {
        Mmu mmu = Mmu.build();
        for (int lyCompare = 0x00; lyCompare < 0xff; lyCompare++) {
            mmu.setByte(0xff45, lyCompare);
            for (int lcdLine = 0; lcdLine < 154; lcdLine++) {
                mmu.resetInterrupt(Interrupt.LCD_STAT);
                mmu.getIo().setLcdCurrentLine(lcdLine);
                assertEquals(lcdLine == lyCompare, mmu.checkInterrupt(Interrupt.LCD_STAT));
            }
        }
    }

    @Test
    public void test_get_window_y_position_returns_0xff4a() {
        doRangeTest(0xff4a, mmu ->
                assertEquals(mmu.readByte(0xff4a), mmu.getIo().getWindowY())
        );
    }

    @Test
    public void test_get_window_x_position_returns_0xff4b_plus_7() {
        doRangeTest(0xff4b, mmu ->
                assertEquals(mmu.readByte(0xff4b) + 7, mmu.getIo().getWindowX())
        );
    }

    @Test
    public void test_get_background_shade_for_color_0_returns_bits_0_and_1_of_0xff47() {
        doRangeTest(0xff47, mmu -> {
            Color expected = paletteMappings.get(mmu.readByte(0xff47) & 0x03);
            assertEquals(expected, mmu.getIo().getShadeForBackgroundColor(0));
        });
    }

    @Test
    public void test_get_background_shade_for_color_1_returns_bits_2_and_3_of_0xff47_as_color() {
        doRangeTest(0xff47, mmu -> {
            Color expected = paletteMappings.get(mmu.readByte(0xff47) & 0x0c);
            assertEquals(expected, mmu.getIo().getShadeForBackgroundColor(1));
        });
    }

    @Test
    public void test_get_background_shade_for_color_2_returns_bits_4_and_5_of_0xff47_as_color() {
        doRangeTest(0xff47, mmu -> {
            Color expected = paletteMappings.get(mmu.readByte(0xff47) & 0x30);
            assertEquals(expected, mmu.getIo().getShadeForBackgroundColor(2));
        });
    }

    @Test
    public void test_get_background_shade_for_color_3_returns_bits_6_and_7_of_0xff47_as_color() {
        doRangeTest(0xff47, mmu -> {
            Color expected = paletteMappings.get(mmu.readByte(0xff47) & 0xc0);
            assertEquals(expected, mmu.getIo().getShadeForBackgroundColor(3));
        });
    }

    @Test
    public void test_get_sprite_palette_0_shade_for_color_0_returns_transparency() {
        doRangeTest(0xff48, mmu ->
        assertEquals(0xff, mmu.getIo().getShadeForBackgroundColor(0).getAlpha()));
    }

    @Test
    public void test_get_sprite_palette_0_shade_for_color_1_returns_bits_2_and_3_of_0xff48_as_color() {
        doRangeTest(0xff48, mmu -> {
            Color expected = paletteMappings.get(mmu.readByte(0xff48) & 0x0c);
            assertEquals(expected, mmu.getIo().getShadeForPalette0Color(1));
        });
    }

    @Test
    public void test_get_sprite_palette_0_shade_for_color_2_returns_bits_4_and_5_of_0xff48_as_color() {
        doRangeTest(0xff48, mmu -> {
            Color expected = paletteMappings.get(mmu.readByte(0xff48) & 0x30);
            assertEquals(expected, mmu.getIo().getShadeForPalette0Color(2));
        });
    }

    @Test
    public void test_get_sprite_palette_0_shade_for_color_3_returns_bits_6_and_7_of_0xff48_as_color() {
        doRangeTest(0xff48, mmu -> {
            Color expected = paletteMappings.get(mmu.readByte(0xff48) & 0xc0);
            assertEquals(expected, mmu.getIo().getShadeForPalette0Color(3));
        });
    }
    @Test
    public void test_get_sprite_palette_1_shade_for_color_0_returns_transparency() {
        doRangeTest(0xff49, mmu ->
        assertEquals(0xff, mmu.getIo().getShadeForBackgroundColor(0).getAlpha()));
    }

    @Test
    public void test_get_sprite_palette_1_shade_for_color_1_returns_bits_2_and_3_of_0xff48_as_color() {
        doRangeTest(0xff49, mmu -> {
            Color expected = paletteMappings.get(mmu.readByte(0xff49) & 0x0c);
            assertEquals(expected, mmu.getIo().getShadeForPalette1Color(1));
        });
    }

    @Test
    public void test_get_sprite_palette_1_shade_for_color_2_returns_bits_4_and_5_of_0xff48_as_color() {
        doRangeTest(0xff49, mmu -> {
            Color expected = paletteMappings.get(mmu.readByte(0xff49) & 0x30);
            assertEquals(expected, mmu.getIo().getShadeForPalette1Color(2));
        });
    }

    @Test
    public void test_get_sprite_palette_1_shade_for_color_3_returns_bits_6_and_7_of_0xff48_as_color() {
        doRangeTest(0xff49, mmu -> {
            Color expected = paletteMappings.get(mmu.readByte(0xff49) & 0xc0);
            assertEquals(expected, mmu.getIo().getShadeForPalette1Color(3));
        });
    }

    @Test(expected=UnsupportedOperationException.class)
    public void test_write_to_0xff46_throws_unsupported_operation_exception() {
        Mmu mmu = Mmu.build();
        mmu.setByte(0xff46, 0x00);
    }

    @Test
    public void test_write_to_0xff50_disables_bios() {
        doRangeTest(0xff50, mmu ->
                assertFalse(mmu.isBiosEnabled())
        );
    }

    private static void doRangeTest(int address, Consumer<Mmu> test) {
        for (int value = 0x00; value < 0xff; value++) {
            Mmu mmu = Mmu.build();
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
