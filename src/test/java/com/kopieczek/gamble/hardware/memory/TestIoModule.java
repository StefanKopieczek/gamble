package com.kopieczek.gamble.hardware.memory;

import com.google.common.collect.ImmutableMap;
import com.kopieczek.gamble.hardware.cpu.Interrupt;
import org.junit.Test;

import java.awt.*;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

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
    public void test_background_display_is_enabled_if_0xff00_bit_0_is_high() {
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

    @Test
    public void test_write_to_0xff50_disables_bios() {
        doRangeTest(0xff50, mmu ->
                assertFalse(mmu.isBiosEnabled())
        );
    }

    @Test
    public void test_write_of_00_to_0xff46_when_source_memory_is_zeroed_leaves_oam_zeroed() {
        Mmu mmu = Mmu.build();
        setRange(mmu, 0x0000, 0xa0, idx -> 0x00);
        mmu.setByte(0xff46, 0x00);
        mmu.stepAhead(1000); // Much more time than we need - but will test precise tick timings later.
        assertMemoryValues(mmu, 0xfe00, 0xa0, idx -> 0x00);
    }

    @Test
    public void test_write_of_0x00_to_0xff46_copies_range_0x0000_0x009f_to_0xfe00() {
        Mmu mmu = Mmu.build();
        setRange(mmu, 0x0000, 0xa0, idx -> (idx * 2) % 0x0100); // Set source memory to a regular pattern
        mmu.setByte(0xff46, 0x00);
        mmu.stepAhead(1000); // More time than we need - precise timings tested elsewhere.
        assertMemoryValues(mmu, 0xfe00, 0xa0, idx -> (idx * 2) % 0x0100);
    }

    @Test
    public void test_write_of_0x01_to_0xff46_copies_range_0x0100_0x019f_to_0xfe00() {
        Mmu mmu = Mmu.build();
        setRange(mmu, 0x0100, 0xa0, idx -> (idx * 7) % 0x0100);
        mmu.setByte(0xff46, 0x01);
        mmu.stepAhead(1000);
        assertMemoryValues(mmu, 0xfe00, 0xa0, idx -> (idx * 7) % 0x0100);
    }

    @Test
    public void test_write_of_0xb7_to_0xff46_copies_range_0xb700_0xb79f_to_0xfe00() {
        Mmu mmu = Mmu.build();
        setRange(mmu, 0xb700, 0xa0, idx -> (idx * 6) % 0x0100);
        mmu.setByte(0xff46, 0xb7);
        mmu.stepAhead(1000);
        assertMemoryValues(mmu, 0xfe00, 0xa0, idx -> (idx * 6) % 0x0100);
    }

    @Test
    public void test_dma_oam_completes_in_162_cycles() {
        Mmu mmu = Mmu.build();
        setRange(mmu, 0x6b00, 0xa0, idx -> idx % 0x0100);
        mmu.setByte(0xff46, 0x6b);
        mmu.stepAhead(162);
        assertMemoryValues(mmu, 0xfe00, 0xa0, idx -> idx % 0x0100);
    }

    @Test
    public void test_dma_oam_not_completed_after_161_cycles() {
        Mmu mmu = Mmu.build();
        setRange(mmu, 0x0000, 0xa0, idx -> 0xde);
        mmu.setByte(0xff46, 0x00);
        mmu.stepAhead(161);
        assertNotEquals(0xde, mmu.readByte(0xfe9f));
    }

    @Test
    public void test_dma_oam_doesnt_write_outside_range() {
        Mmu mmu = Mmu.build();
        setRange(mmu, 0x0000, 0x2000, idx -> 0xde);
        mmu.setByte(0xff46, 0x01);
        mmu.stepAhead(1000);
        assertNotEquals(0xde, mmu.readByte(0xfdff));
        assertNotEquals(0xde, mmu.readByte(0xfea0));
    }

    @Test
    public void test_two_simultaneous_oams() {
        Mmu mmu = Mmu.build();
        setRange(mmu, 0x0000, 0xa0, idx->0xaa);
        setRange(mmu, 0x0100, 0xa0, idx->0xbb);
        mmu.setByte(0xff46, 0x00);
        mmu.stepAhead(80);
        mmu.setByte(0xff46, 0x01);
        mmu.stepAhead(82);
        assertEquals(0xaa, mmu.readByte(0xfe9f)); // First OAM has finished but second hasn't yet reached end
        assertEquals(0xbb, mmu.readByte(0xfe20)); // However second OAM should have covered some ground
        mmu.stepAhead(80);
        assertEquals(0xbb, mmu.readByte(0xfe9f)); // Second OAM should now be finished
    }

    @Test
    public void test_three_simultaneous_oams() {
        Mmu mmu = Mmu.build();
        setRange(mmu, 0x0000, 0xa0, idx->0xaa);
        setRange(mmu, 0x2000, 0xa0, idx->0xbb);
        setRange(mmu, 0x1700, 0xa0, idx->0xcd);
        mmu.setByte(0xff46, 0x00);
        mmu.stepAhead(1);
        mmu.setByte(0xff46, 0x20);
        mmu.stepAhead(1);
        mmu.setByte(0xff46, 0x17);
        mmu.stepAhead(1);
        assertEquals(0xaa, mmu.readByte(0xfe00));
        mmu.stepAhead(161);
        assertEquals(0xcd, mmu.readByte(0xfe9f));
    }

    @Test
    public void test_joypad_memory_is_initially_0x0f() {
        // "All buttons unpressed" looks like bits 0-5 inclusive brought high.
        // Bits 6 and 7 are unused and always low.
        Mmu mmu = Mmu.build();
        assertEquals(0x0f, mmu.readByte(0xff00));
    }

    @Test
    public void writes_to_joypad_affect_only_top_four_bits() {
        Mmu mmu = Mmu.build();
        int initialValue = mmu.readByte(0xff00);
        mmu.setByte(0xff00, 0xff & (~initialValue));
        assertEquals((~initialValue & 0xf0) + (initialValue & 0x0f), mmu.readByte(0xff00));
    }

    @Test
    public void test_all_joypad_inputs_initially_disabled() {
        Mmu mmu = Mmu.build();
        Io io = mmu.getIo();
        for (Io.Button button : Io.Button.values()) {
            assertFalse(button + " should be disabled initially", io.isButtonPressed(button));
        }
    }

    @Test
    public void test_setting_buttons_true_means_they_are_pressed() {
        for (Io.Button button : Io.Button.values()) {
            Mmu mmu = Mmu.build();
            mmu.getIo().setButtonPressed(button, true);
            assertTrue(button + " did not appear enabled after being set", mmu.getIo().isButtonPressed(button));
        }
    }

    @Test
    public void test_setting_buttons_false_means_they_are_not_pressed() {
        for (Io.Button button : Io.Button.values()) {
            Mmu mmu = Mmu.build();
            Io io = mmu.getIo();
            io.setButtonPressed(button, true);
            io.setButtonPressed(button, false);
            assertFalse(button + " was still enabled after being unset", io.isButtonPressed(button));
        }
    }

    @Test
    public void test_pressing_a_with_button_select_on_resets_0xff00_bit_0() {
        Mmu mmu = Mmu.build();
        mmu.setByte(0xff00, 0x20);
        mmu.getIo().setButtonPressed(Io.Button.A, true);
        assertEquals(0x00, 0x01 & mmu.readByte(0xff00));
        assertEquals(0x2e, 0xfe & mmu.readByte(0xff00));
    }

    @Test
    public void test_releasing_a_with_button_select_on_sets_0xff00_bit_0() {
        Mmu mmu = Mmu.build();
        mmu.setByte(0xff00, 0x20);
        mmu.getIo().setButtonPressed(Io.Button.A, true);
        mmu.getIo().setButtonPressed(Io.Button.A, false);
        assertEquals(0x01, 0x01 & mmu.readByte(0xff00));
        assertEquals(0x2e, 0xfe & mmu.readByte(0xff00));
    }

    @Test
    public void test_pressing_a_with_button_select_off_does_not_modify_memory() {
        Mmu mmu = Mmu.build();
        mmu.getIo().setButtonPressed(Io.Button.A, true);
        assertEquals(0x01, 0x01 & mmu.readByte(0xff00));
        assertEquals(0x0e, 0xfe & mmu.readByte(0xff00));
    }

    @Test
    public void test_enabling_button_select_after_pressing_a_resets_0xff00_bit_0() {
        Mmu mmu = Mmu.build();
        mmu.getIo().setButtonPressed(Io.Button.A, true);
        mmu.setByte(0xff00, 0x20);
        assertEquals(0x00, 0x01 & mmu.readByte(0xff00));
    }

    @Test
    public void test_disabling_button_select_after_pressing_a_sets_0xff00_bit_0() {
        Mmu mmu = Mmu.build();
        mmu.setByte(0xff00, 0x20);
        mmu.getIo().setButtonPressed(Io.Button.A, true);
        mmu.setByte(0xff00, 0x00);
        assertEquals(0x01, 0x01 & mmu.readByte(0xff00));
    }

    @Test
    public void test_enabling_pad_select_does_not_enable_button_a() {
        Mmu mmu = Mmu.build();
        mmu.getIo().setButtonPressed(Io.Button.A, true);
        mmu.setByte(0xff00, 0x10);
        assertEquals(0x01, 0x01 & mmu.readByte(0xff00));
    }

    @Test
    public void test_pressing_b_with_button_select_on_resets_0xff00_bit_1() {
        Mmu mmu = Mmu.build();
        mmu.setByte(0xff00, 0x20);
        mmu.getIo().setButtonPressed(Io.Button.B, true);
        assertEquals(0x00, 0x02 & mmu.readByte(0xff00));
        assertEquals(0x2d, 0xfd & mmu.readByte(0xff00));
    }

    @Test
    public void test_releasing_b_with_button_select_on_sets_0xff00_bit_1() {
        Mmu mmu = Mmu.build();
        mmu.setByte(0xff00, 0x20);
        mmu.getIo().setButtonPressed(Io.Button.B, true);
        mmu.getIo().setButtonPressed(Io.Button.B, false);
        assertEquals(0x02, 0x02 & mmu.readByte(0xff00));
        assertEquals(0x2d, 0xfd & mmu.readByte(0xff00));
    }

    @Test
    public void test_pressing_b_with_button_select_off_does_not_modify_memory() {
        Mmu mmu = Mmu.build();
        mmu.getIo().setButtonPressed(Io.Button.B, true);
        assertEquals(0x02, 0x02 & mmu.readByte(0xff00));
        assertEquals(0x0d, 0xfd & mmu.readByte(0xff00));
    }

    @Test
    public void test_enabling_button_select_after_pressing_b_resets_0xff00_bit_1() {
        Mmu mmu = Mmu.build();
        mmu.getIo().setButtonPressed(Io.Button.B, true);
        mmu.setByte(0xff00, 0x20);
        assertEquals(0x00, 0x02 & mmu.readByte(0xff00));
    }

    @Test
    public void test_disabling_button_select_after_pressing_b_sets_0xff00_bit_1() {
        Mmu mmu = Mmu.build();
        mmu.setByte(0xff00, 0x20);
        mmu.getIo().setButtonPressed(Io.Button.B, true);
        mmu.setByte(0xff00, 0x00);
        assertEquals(0x02, 0x02 & mmu.readByte(0xff00));
    }

    @Test
    public void test_enabling_pad_select_does_not_enable_button_b() {
        Mmu mmu = Mmu.build();
        mmu.getIo().setButtonPressed(Io.Button.B, true);
        mmu.setByte(0xff00, 0x10);
        assertEquals(0x02, 0x02 & mmu.readByte(0xff00));
    }

    @Test
    public void test_pressing_select_with_button_select_on_resets_0xff00_bit_2() {
        Mmu mmu = Mmu.build();
        mmu.setByte(0xff00, 0x20);
        mmu.getIo().setButtonPressed(Io.Button.SELECT, true);
        assertEquals(0x00, 0x04 & mmu.readByte(0xff00));
        assertEquals(0x2b, 0xfb & mmu.readByte(0xff00));
    }

    @Test
    public void test_releasing_select_with_button_select_on_sets_0xff00_bit_2() {
        Mmu mmu = Mmu.build();
        mmu.setByte(0xff00, 0x20);
        mmu.getIo().setButtonPressed(Io.Button.SELECT, true);
        mmu.getIo().setButtonPressed(Io.Button.SELECT, false);
        assertEquals(0x04, 0x04 & mmu.readByte(0xff00));
        assertEquals(0x2b, 0xfb & mmu.readByte(0xff00));
    }

    @Test
    public void test_pressing_select_with_button_select_off_does_not_modify_memory() {
        Mmu mmu = Mmu.build();
        mmu.getIo().setButtonPressed(Io.Button.SELECT, true);
        assertEquals(0x04, 0x04 & mmu.readByte(0xff00));
        assertEquals(0x0b, 0xfb & mmu.readByte(0xff00));
    }

    @Test
    public void test_enabling_button_select_after_pressing_select_resets_0xff00_bit_2() {
        Mmu mmu = Mmu.build();
        mmu.getIo().setButtonPressed(Io.Button.SELECT, true);
        mmu.setByte(0xff00, 0x20);
        assertEquals(0x00, 0x04 & mmu.readByte(0xff00));
    }

    @Test
    public void test_disabling_button_select_after_pressing_select_sets_0xff00_bit_2() {
        Mmu mmu = Mmu.build();
        mmu.setByte(0xff00, 0x20);
        mmu.getIo().setButtonPressed(Io.Button.SELECT, true);
        mmu.setByte(0xff00, 0x00);
        assertEquals(0x04, 0x04 & mmu.readByte(0xff00));
    }

    @Test
    public void test_enabling_pad_select_does_not_enable_select_button() {
        Mmu mmu = Mmu.build();
        mmu.getIo().setButtonPressed(Io.Button.SELECT, true);
        mmu.setByte(0xff00, 0x10);
        assertEquals(0x04, 0x04 & mmu.readByte(0xff00));
    }

    @Test
    public void test_pressing_start_with_button_select_on_resets_0xff00_bit_3() {
        Mmu mmu = Mmu.build();
        mmu.setByte(0xff00, 0x20);
        mmu.getIo().setButtonPressed(Io.Button.START, true);
        assertEquals(0x00, 0x08 & mmu.readByte(0xff00));
        assertEquals(0x27, 0xf7 & mmu.readByte(0xff00));
    }

    @Test
    public void test_releasing_start_with_button_select_on_sets_0xff00_bit_3() {
        Mmu mmu = Mmu.build();
        mmu.setByte(0xff00, 0x20);
        mmu.getIo().setButtonPressed(Io.Button.START, true);
        mmu.getIo().setButtonPressed(Io.Button.START, false);
        assertEquals(0x08, 0x08 & mmu.readByte(0xff00));
        assertEquals(0x27, 0xf7 & mmu.readByte(0xff00));
    }

    @Test
    public void test_pressing_start_with_button_select_off_does_not_modify_memory() {
        Mmu mmu = Mmu.build();
        mmu.getIo().setButtonPressed(Io.Button.START, true);
        assertEquals(0x08, 0x08 & mmu.readByte(0xff00));
        assertEquals(0x07, 0xf7 & mmu.readByte(0xff00));
    }

    @Test
    public void test_enabling_button_select_after_pressing_start_resets_0xff00_bit_3() {
        Mmu mmu = Mmu.build();
        mmu.getIo().setButtonPressed(Io.Button.START, true);
        mmu.setByte(0xff00, 0x20);
        assertEquals(0x00, 0x08 & mmu.readByte(0xff00));
    }

    @Test
    public void test_disabling_button_select_after_pressing_start_sets_0xff00_bit_3() {
        Mmu mmu = Mmu.build();
        mmu.setByte(0xff00, 0x20);
        mmu.getIo().setButtonPressed(Io.Button.START, true);
        mmu.setByte(0xff00, 0x00);
        assertEquals(0x08, 0x08 & mmu.readByte(0xff00));
    }

    @Test
    public void test_enabling_pad_select_does_not_enable_start_button() {
        Mmu mmu = Mmu.build();
        mmu.getIo().setButtonPressed(Io.Button.START, true);
        mmu.setByte(0xff00, 0x10);
        assertEquals(0x08, 0x08 & mmu.readByte(0xff00));
    }

    @Test
    public void test_pressing_right_with_pad_select_on_resets_0xff00_bit_0() {
        Mmu mmu = Mmu.build();
        mmu.setByte(0xff00, 0x10);
        mmu.getIo().setButtonPressed(Io.Button.RIGHT, true);
        assertEquals(0x00, 0x01 & mmu.readByte(0xff00));
        assertEquals(0x1e, 0xfe & mmu.readByte(0xff00));
    }

    @Test
    public void test_releasing_right_with_pad_select_on_sets_0xff00_bit_0() {
        Mmu mmu = Mmu.build();
        mmu.setByte(0xff00, 0x10);
        mmu.getIo().setButtonPressed(Io.Button.RIGHT, true);
        mmu.getIo().setButtonPressed(Io.Button.RIGHT, false);
        assertEquals(0x01, 0x01 & mmu.readByte(0xff00));
        assertEquals(0x1e, 0xfe & mmu.readByte(0xff00));
    }

    @Test
    public void test_pressing_right_with_pad_select_off_does_not_modify_memory() {
        Mmu mmu = Mmu.build();
        mmu.getIo().setButtonPressed(Io.Button.RIGHT, true);
        assertEquals(0x01, 0x01 & mmu.readByte(0xff00));
        assertEquals(0x0e, 0xfe & mmu.readByte(0xff00));
    }

    @Test
    public void test_enabling_pad_select_after_pressing_right_resets_0xff00_bit_0() {
        Mmu mmu = Mmu.build();
        mmu.getIo().setButtonPressed(Io.Button.RIGHT, true);
        mmu.setByte(0xff00, 0x10);
        assertEquals(0x00, 0x01 & mmu.readByte(0xff00));
    }

    @Test
    public void test_disabling_pad_select_after_pressing_right_sets_0xff00_bit_0() {
        Mmu mmu = Mmu.build();
        mmu.setByte(0xff00, 0x10);
        mmu.getIo().setButtonPressed(Io.Button.RIGHT, true);
        mmu.setByte(0xff00, 0x00);
        assertEquals(0x01, 0x01 & mmu.readByte(0xff00));
    }

    @Test
    public void test_enabling_button_select_does_not_enable_right_button() {
        Mmu mmu = Mmu.build();
        mmu.setByte(0xff00, 0x20);
        mmu.getIo().setButtonPressed(Io.Button.RIGHT, true);
        assertEquals(0x01, 0x01 & mmu.readByte(0xff00));
    }

    @Test
    public void test_pressing_left_with_pad_select_on_resets_0xff00_bit_1() {
        Mmu mmu = Mmu.build();
        mmu.setByte(0xff00, 0x10);
        mmu.getIo().setButtonPressed(Io.Button.LEFT, true);
        assertEquals(0x00, 0x02 & mmu.readByte(0xff00));
        assertEquals(0x1d, 0xfd & mmu.readByte(0xff00));
    }

    @Test
    public void test_releasing_left_with_pad_select_on_sets_0xff00_bit_1() {
        Mmu mmu = Mmu.build();
        mmu.setByte(0xff00, 0x10);
        mmu.getIo().setButtonPressed(Io.Button.LEFT, true);
        mmu.getIo().setButtonPressed(Io.Button.LEFT, false);
        assertEquals(0x02, 0x02 & mmu.readByte(0xff00));
        assertEquals(0x1d, 0xfd & mmu.readByte(0xff00));
    }

    @Test
    public void test_pressing_left_with_pad_select_off_does_not_modify_memory() {
        Mmu mmu = Mmu.build();
        mmu.getIo().setButtonPressed(Io.Button.LEFT, true);
        assertEquals(0x02, 0x02 & mmu.readByte(0xff00));
        assertEquals(0x0d, 0xfd & mmu.readByte(0xff00));
    }

    @Test
    public void test_enabling_pad_select_after_pressing_left_resets_0xff00_bit_1() {
        Mmu mmu = Mmu.build();
        mmu.getIo().setButtonPressed(Io.Button.LEFT, true);
        mmu.setByte(0xff00, 0x10);
        assertEquals(0x00, 0x02 & mmu.readByte(0xff00));
    }

    @Test
    public void test_disabling_pad_select_after_pressing_left_sets_0xff00_bit_1() {
        Mmu mmu = Mmu.build();
        mmu.setByte(0xff00, 0x10);
        mmu.getIo().setButtonPressed(Io.Button.LEFT, true);
        mmu.setByte(0xff00, 0x00);
        assertEquals(0x02, 0x02 & mmu.readByte(0xff00));
    }

    @Test
    public void test_enabling_button_select_does_not_enable_left_button() {
        Mmu mmu = Mmu.build();
        mmu.setByte(0xff00, 0x20);
        mmu.getIo().setButtonPressed(Io.Button.LEFT, true);
        assertEquals(0x02, 0x02 & mmu.readByte(0xff00));
    }

    @Test
    public void test_pressing_up_with_pad_select_on_resets_0xff00_bit_2() {
        Mmu mmu = Mmu.build();
        mmu.setByte(0xff00, 0x10);
        mmu.getIo().setButtonPressed(Io.Button.UP, true);
        assertEquals(0x00, 0x04 & mmu.readByte(0xff00));
        assertEquals(0x1b, 0xfb & mmu.readByte(0xff00));
    }

    @Test
    public void test_releasing_up_with_pad_select_on_sets_0xff00_bit_2() {
        Mmu mmu = Mmu.build();
        mmu.setByte(0xff00, 0x10);
        mmu.getIo().setButtonPressed(Io.Button.UP, true);
        mmu.getIo().setButtonPressed(Io.Button.UP, false);
        assertEquals(0x04, 0x04 & mmu.readByte(0xff00));
        assertEquals(0x1b, 0xfb & mmu.readByte(0xff00));
    }

    @Test
    public void test_pressing_up_with_pad_select_off_does_not_modify_memory() {
        Mmu mmu = Mmu.build();
        mmu.getIo().setButtonPressed(Io.Button.UP, true);
        assertEquals(0x04, 0x04 & mmu.readByte(0xff00));
        assertEquals(0x0b, 0xfb & mmu.readByte(0xff00));
    }

    @Test
    public void test_enabling_pad_select_after_pressing_up_resets_0xff00_bit_2() {
        Mmu mmu = Mmu.build();
        mmu.getIo().setButtonPressed(Io.Button.UP, true);
        mmu.setByte(0xff00, 0x10);
        assertEquals(0x00, 0x04 & mmu.readByte(0xff00));
    }

    @Test
    public void test_disabling_pad_select_after_pressing_up_sets_0xff00_bit_2() {
        Mmu mmu = Mmu.build();
        mmu.setByte(0xff00, 0x10);
        mmu.getIo().setButtonPressed(Io.Button.UP, true);
        mmu.setByte(0xff00, 0x00);
        assertEquals(0x04, 0x04 & mmu.readByte(0xff00));
    }

    @Test
    public void test_enabling_button_select_does_not_enable_up_button() {
        Mmu mmu = Mmu.build();
        mmu.setByte(0xff00, 0x20);
        mmu.getIo().setButtonPressed(Io.Button.UP, true);
        assertEquals(0x04, 0x04 & mmu.readByte(0xff00));
    }

    @Test
    public void test_pressing_down_with_pad_select_on_resets_0xff00_bit_3() {
        Mmu mmu = Mmu.build();
        mmu.setByte(0xff00, 0x10);
        mmu.getIo().setButtonPressed(Io.Button.DOWN, true);
        assertEquals(0x00, 0x08 & mmu.readByte(0xff00));
        assertEquals(0x17, 0xf7 & mmu.readByte(0xff00));
    }

    @Test
    public void test_releasing_down_with_pad_select_on_sets_0xff00_bit_3() {
        Mmu mmu = Mmu.build();
        mmu.setByte(0xff00, 0x10);
        mmu.getIo().setButtonPressed(Io.Button.DOWN, true);
        mmu.getIo().setButtonPressed(Io.Button.DOWN, false);
        assertEquals(0x08, 0x08 & mmu.readByte(0xff00));
        assertEquals(0x17, 0xf7 & mmu.readByte(0xff00));
    }

    @Test
    public void test_pressing_down_with_pad_select_off_does_not_modify_memory() {
        Mmu mmu = Mmu.build();
        mmu.getIo().setButtonPressed(Io.Button.DOWN, true);
        assertEquals(0x08, 0x08 & mmu.readByte(0xff00));
        assertEquals(0x07, 0xf7 & mmu.readByte(0xff00));
    }

    @Test
    public void test_enabling_pad_select_after_pressing_down_resets_0xff00_bit_3() {
        Mmu mmu = Mmu.build();
        mmu.getIo().setButtonPressed(Io.Button.DOWN, true);
        mmu.setByte(0xff00, 0x10);
        assertEquals(0x00, 0x08 & mmu.readByte(0xff00));
    }

    @Test
    public void test_disabling_pad_select_after_pressing_down_sets_0xff00_bit_3() {
        Mmu mmu = Mmu.build();
        mmu.setByte(0xff00, 0x10);
        mmu.getIo().setButtonPressed(Io.Button.DOWN, true);
        mmu.setByte(0xff00, 0x00);
        assertEquals(0x08, 0x08 & mmu.readByte(0xff00));
    }

    @Test
    public void test_enabling_button_select_does_not_enable_down_button() {
        Mmu mmu = Mmu.build();
        mmu.setByte(0xff00, 0x20);
        mmu.getIo().setButtonPressed(Io.Button.DOWN, true);
        assertEquals(0x08, 0x08 & mmu.readByte(0xff00));
    }

    @Test
    public void test_pressing_left_when_b_is_pressed_and_both_pad_and_buttons_are_selected_does_nothing() {
        Mmu mmu = Mmu.build();
        mmu.setByte(0xff00, 0x30);
        mmu.getIo().setButtonPressed(Io.Button.LEFT, true);
        assertEquals("Bit 1 should be low at this point", 0x00, 0x02 & mmu.readByte(0xff00));
        mmu.getIo().setButtonPressed(Io.Button.UP, true);
        assertEquals("Bit 1 should still be low", 0x00, 0x02 & mmu.readByte(0xff00));
    }

    @Test
    public void test_releasing_start_when_start_and_down_are_pressed_and_both_pad_and_buttons_are_selected_does_nothing() {
        Mmu mmu = Mmu.build();
        mmu.getIo().setButtonPressed(Io.Button.START, true);
        mmu.getIo().setButtonPressed(Io.Button.DOWN, true);
        mmu.setByte(0xff00, 0x30);
        assertEquals("At this point bit 4 should be low", 0x00, 0x08 & mmu.readByte(0xff00));
        mmu.getIo().setButtonPressed(Io.Button.START, false);
        assertEquals("Bit 4 should still be low", 0x00, 0x08 & mmu.readByte(0xff00));
    }

    @Test
    public void test_when_pad_and_buttons_are_selected_then_both_pad_and_buttons_can_fire() {
        Mmu mmu = Mmu.build();
        Io io = mmu.getIo();
        mmu.setByte(0xff00, 0x30);
        io.setButtonPressed(Io.Button.A, true);
        io.setButtonPressed(Io.Button.B, true);
        io.setButtonPressed(Io.Button.UP, true);
        io.setButtonPressed(Io.Button.DOWN, true);
        assertEquals(0x00, 0x0f & mmu.readByte(0xff00));
    }

    @Test
    public void test_switching_from_button_to_pad_select_and_back() {
        Mmu mmu = Mmu.build();
        Io io = mmu.getIo();
        io.setButtonPressed(Io.Button.START, true);
        io.setButtonPressed(Io.Button.SELECT, true);
        io.setButtonPressed(Io.Button.LEFT, true);
        io.setButtonPressed(Io.Button.RIGHT, true);
        assertEquals("Joypad bits should be high", 0x0f, 0x0f & mmu.readByte(0xff00));
        mmu.setByte(0xff00, 0x20); // Select buttons only
        assertEquals(0x03, 0x0f & mmu.readByte(0xff00));
        mmu.setByte(0xff00, 0x10); // Select pad only
        assertEquals(0x0c, 0x0f & mmu.readByte(0xff00));
        mmu.setByte(0xff00, 0x20); // Select buttons again
        assertEquals(0x03, 0x0f & mmu.readByte(0xff00));
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

    private static void setRange(Mmu mmu, int startAddr, int length,
                                 Function<Integer, Integer> valueGenerator) {

        for (int idx = 0 ; idx < length; idx++) {
            mmu.setByte(startAddr + idx, valueGenerator.apply(idx));
        }
    }

    private static void assertMemoryValues(Mmu mmu, int startAddr, int length,
            Function<Integer, Integer> getExpectedValue) {
        for (int idx = 0; idx < length; idx++) {
            final int address = startAddr + idx;
            assertEquals("Unexpected value at address 0x" + Integer.toHexString(address),
                (long)getExpectedValue.apply(address),
                mmu.readByte(address));
        }
    }
}
