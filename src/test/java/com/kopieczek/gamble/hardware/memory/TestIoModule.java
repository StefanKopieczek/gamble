package com.kopieczek.gamble.hardware.memory;

import com.google.common.collect.ImmutableMap;
import com.kopieczek.gamble.hardware.audio.AudioOutputMode;
import com.kopieczek.gamble.hardware.audio.MasterAudioListener;
import com.kopieczek.gamble.hardware.cpu.Interrupt;
import com.kopieczek.gamble.hardware.memory.cartridge.RamBackedTestCartridge;
import org.junit.Test;

import java.awt.*;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

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
        Mmu mmu = getTestMmu();
        for (int line = 0; line < 153; line++) {
            mmu.getIo().setLcdCurrentLine(line);
            assertEquals(line, mmu.readByte(0xff44));
        }
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_setting_lcd_current_line_to_154_throws_illegal_argument_exception() {
        Mmu mmu = getTestMmu();
        mmu.getIo().setLcdCurrentLine(154);
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_setting_lcd_current_line_to_200_throws_illegal_argument_exception() {
        Mmu mmu = getTestMmu();
        mmu.getIo().setLcdCurrentLine(200);
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_setting_lcd_current_line_to_255_throws_illegal_argument_exception() {
        Mmu mmu = getTestMmu();
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
    public void test_should_stat_interrupt_for_coincidence_checks_bit_6_of_0xff41() {
        doRangedBitCheckTest(0xff41, 6, (mmu, isSet) -> {
            assertEquals(isSet, mmu.getIo().shouldStatInterruptFor(Io.LcdStatEvent.COINCIDENCE));
        });
    }

    @Test
    public void test_should_stat_interrupt_for_oam_checks_bit_5_of_0xff41() {
        doRangedBitCheckTest(0xff41, 5, (mmu, isSet) -> {
            assertEquals(isSet, mmu.getIo().shouldStatInterruptFor(Io.LcdStatEvent.OAM));
        });
    }

    @Test
    public void test_should_stat_interrupt_for_vblank_checks_bit_4_of_0xff41() {
        doRangedBitCheckTest(0xff41, 4, (mmu, isSet) -> {
            assertEquals(isSet, mmu.getIo().shouldStatInterruptFor(Io.LcdStatEvent.VBLANK));
        });
    }

    @Test
    public void test_should_stat_interrupt_for_hblank_checks_bit_3_of_0xff41() {
        doRangedBitCheckTest(0xff41, 3, (mmu, isSet) -> {
            assertEquals(isSet, mmu.getIo().shouldStatInterruptFor(Io.LcdStatEvent.HBLANK));
        });
    }

    @Test
    public void test_setting_ly_compare_equal_to_lcd_current_line_sets_coincidence_flag() {
        Mmu mmu = getTestMmu();
        for (int lcdLine = 0; lcdLine < 154; lcdLine++) {
            mmu.getIo().setLcdCurrentLine(lcdLine);
            for (int lyCompare = 0x00; lyCompare < 0xff; lyCompare++) {
                mmu.setByte(0xff45, lyCompare);
                boolean coincidenceFlag = (mmu.readByte(0xff41)  & 0x04) > 0;
                String msg = String.format("LYC=%02x, LY=%02x", lyCompare, lcdLine);
                assertEquals(msg, lcdLine == lyCompare, coincidenceFlag);
            }
        }
    }

    @Test
    public void test_setting_lcd_current_line_equal_to_ly_compare_sets_coincidence_flag() {
        Mmu mmu = getTestMmu();
        for (int lyCompare = 0x00; lyCompare < 0xff; lyCompare++) {
            mmu.setByte(0xff45, lyCompare);
            for (int lcdLine = 0; lcdLine < 154; lcdLine++) {
                mmu.getIo().setLcdCurrentLine(lcdLine);
                boolean coincidenceFlag = (mmu.readByte(0xff41) & 0x04) > 0;
                String msg = String.format("LYC=%02x, LY=%02x", lyCompare, lcdLine);
                assertEquals(msg, lcdLine == lyCompare, coincidenceFlag);
            }
        }
    }

    @Test
    public void test_setting_ly_compare_equal_to_lcd_current_line_fires_stat_interrupt_when_enabled() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff41, 0x40);

        // Iterate backwards to avoid false negative at (0,0) due to default values
        for (int lcdLine = 153; lcdLine >= 0; lcdLine--) {
            mmu.getIo().setLcdCurrentLine(lcdLine);
            for (int lyCompare = 0x00; lyCompare <= 0xff; lyCompare++) {
                mmu.resetInterrupt(Interrupt.LCD_STAT);
                mmu.setByte(0xff45, lyCompare);
                String msg = String.format("LYC=%02x, LY=%02x", lyCompare, lcdLine);
                assertEquals(msg, lcdLine == lyCompare, mmu.checkInterrupt(Interrupt.LCD_STAT));
            }
        }
    }

    @Test
    public void test_setting_lcd_current_line_equal_to_ly_compare_fires_stat_interrupt_when_enabled() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff41, 0x40);

        // Iterate backwards to avoid false negative at (0,0) due to default values
        for (int lyCompare = 0xff; lyCompare >= 0; lyCompare--) {
            mmu.setByte(0xff45, lyCompare);
            for (int lcdLine = 0; lcdLine < 154; lcdLine++) {
                mmu.resetInterrupt(Interrupt.LCD_STAT);
                mmu.getIo().setLcdCurrentLine(lcdLine);
                String msg = String.format("LYC=%02x, LY=%02x", lyCompare, lcdLine);
                assertEquals(msg, lcdLine == lyCompare, mmu.checkInterrupt(Interrupt.LCD_STAT));
            }
        }
    }

    @Test
    public void test_coincidence_stat_interrupt_does_not_fire_if_not_enabled() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff41, 0x38); // Select all stat interrupts *except* coincidence

        // Iterate backwards to avoid false negative at (0,0) due to default values
        for (int lyCompare = 0xff; lyCompare >= 0x00; lyCompare--) {
            mmu.setByte(0xff45, lyCompare);
            for (int lcdLine = 0; lcdLine < 154; lcdLine++) {
                mmu.getIo().setLcdCurrentLine(lcdLine);
                String msg = String.format("LYC=%02x, LY=%02x", lyCompare, lcdLine);
                assertFalse(msg, mmu.checkInterrupt(Interrupt.LCD_STAT));
            }
        }
    }

    @Test
    public void test_coincidence_stat_interrupt_does_not_double_fire_when_current_line_is_set() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff41, 0x40);

        mmu.setByte(0xff45, 12);
        mmu.getIo().setLcdCurrentLine(12);
        assertTrue("Interrupt should fire on first coincidence", mmu.checkInterrupt(Interrupt.LCD_STAT));
        mmu.resetInterrupt(Interrupt.LCD_STAT);

        mmu.getIo().setLcdCurrentLine(12);
        assertFalse("Interrupt should not re-fire if the line is re-stored", mmu.checkInterrupt(Interrupt.LCD_STAT));
    }

    @Test
    public void test_coincidence_stat_interrupt_does_not_double_fire_when_line_selector_is_set() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff41, 0x40);

        mmu.setByte(0xff45, 12);
        mmu.getIo().setLcdCurrentLine(12);
        assertTrue("Interrupt should fire on first coincidence", mmu.checkInterrupt(Interrupt.LCD_STAT));
        mmu.resetInterrupt(Interrupt.LCD_STAT);

        mmu.setByte(0xff45, 12);
        assertFalse("Interrupt should not re-fire if the selector is re-stored", mmu.checkInterrupt(Interrupt.LCD_STAT));
    }

    @Test
    public void test_handle_oam_fires_interrupt_if_enabled() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff41, 0x20);
        mmu.getIo().handleOam();
        assertTrue(mmu.checkInterrupt(Interrupt.LCD_STAT));
    }

    @Test
    public void test_handle_oam_does_not_fire_interrupt_when_not_selected() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff41, 0x58); // Select all stat interrupts *except* oam
        mmu.getIo().handleOam();
        assertFalse(mmu.checkInterrupt(Interrupt.LCD_STAT));
    }

    @Test
    public void test_handle_vblank_fires_interrupt_if_enabled() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff41, 0x10);
        mmu.getIo().handleVBlank();
        assertTrue(mmu.checkInterrupt(Interrupt.LCD_STAT));
    }

    @Test
    public void test_handle_vblank_does_not_fire_interrupt_when_not_selected() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff41, 0x68); // Select all stat interrupts *except* vblank
        mmu.getIo().handleVBlank();
        assertFalse(mmu.checkInterrupt(Interrupt.LCD_STAT));
    }

    @Test
    public void test_handle_hblank_fires_interrupt_if_enabled() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff41, 0x08);
        mmu.getIo().handleHBlank();
        assertTrue(mmu.checkInterrupt(Interrupt.LCD_STAT));
    }

    @Test
    public void test_handle_hblank_does_not_fire_interrupt_when_not_selected() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff41, 0x70); // Select all stat interrupts *except* hblank
        mmu.getIo().handleHBlank();
        assertFalse(mmu.checkInterrupt(Interrupt.LCD_STAT));
    }

    @Test
    public void test_get_window_y_position_returns_0xff4a() {
        doRangeTest(0xff4a, mmu ->
                assertEquals(mmu.readByte(0xff4a), mmu.getIo().getWindowY())
        );
    }

    @Test
    public void test_get_window_x_position_returns_0xff4b_minus_7() {
        doRangeTest(0xff4b, mmu ->
                assertEquals(mmu.readByte(0xff4b) - 7, mmu.getIo().getWindowX())
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
            Color expected = paletteMappings.get((mmu.readByte(0xff47) & 0x0c) >> 2);
            assertEquals(expected, mmu.getIo().getShadeForBackgroundColor(1));
        });
    }

    @Test
    public void test_get_background_shade_for_color_2_returns_bits_4_and_5_of_0xff47_as_color() {
        doRangeTest(0xff47, mmu -> {
            Color expected = paletteMappings.get((mmu.readByte(0xff47) & 0x30) >> 4);
            assertEquals(expected, mmu.getIo().getShadeForBackgroundColor(2));
        });
    }

    @Test
    public void test_get_background_shade_for_color_3_returns_bits_6_and_7_of_0xff47_as_color() {
        doRangeTest(0xff47, mmu -> {
            Color expected = paletteMappings.get((mmu.readByte(0xff47) & 0xc0) >> 6);
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
            Color expected = paletteMappings.get((mmu.readByte(0xff48) & 0x0c) >> 2);
            assertEquals(expected, mmu.getIo().loadPalette0()[1]);
        });
    }

    @Test
    public void test_get_sprite_palette_0_shade_for_color_2_returns_bits_4_and_5_of_0xff48_as_color() {
        doRangeTest(0xff48, mmu -> {
            Color expected = paletteMappings.get((mmu.readByte(0xff48) & 0x30) >> 4);
            assertEquals(expected, mmu.getIo().loadPalette0()[2]);
        });
    }

    @Test
    public void test_get_sprite_palette_0_shade_for_color_3_returns_bits_6_and_7_of_0xff48_as_color() {
        doRangeTest(0xff48, mmu -> {
            Color expected = paletteMappings.get((mmu.readByte(0xff48) & 0xc0) >> 6);
            assertEquals(expected, mmu.getIo().loadPalette0()[3]);
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
            Color expected = paletteMappings.get((mmu.readByte(0xff49) & 0x0c) >> 2);
            assertEquals(expected, mmu.getIo().loadPalette1()[1]);
        });
    }

    @Test
    public void test_get_sprite_palette_1_shade_for_color_2_returns_bits_4_and_5_of_0xff48_as_color() {
        doRangeTest(0xff49, mmu -> {
            Color expected = paletteMappings.get((mmu.readByte(0xff49) & 0x30) >> 4);
            assertEquals(expected, mmu.getIo().loadPalette1()[2]);
        });
    }

    @Test
    public void test_get_sprite_palette_1_shade_for_color_3_returns_bits_6_and_7_of_0xff48_as_color() {
        doRangeTest(0xff49, mmu -> {
            Color expected = paletteMappings.get((mmu.readByte(0xff49) & 0xc0) >> 6);
            assertEquals(expected, mmu.getIo().loadPalette1()[3]);
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
        Mmu mmu = getTestMmu();
        setRange(mmu, 0x0000, 0xa0, idx -> 0x00);
        mmu.setByte(0xff46, 0x00);
        mmu.stepAhead(1000); // Much more time than we need - but will test precise tick timings later.
        assertMemoryValues(mmu, 0xfe00, 0xa0, idx -> 0x00);
    }

    @Test
    public void test_write_of_0x00_to_0xff46_copies_range_0x0000_0x009f_to_0xfe00() {
        Mmu mmu = getTestMmu();
        setRange(mmu, 0x0000, 0xa0, idx -> (idx * 2) % 0x0100); // Set source memory to a regular pattern
        mmu.setByte(0xff46, 0x00);
        mmu.stepAhead(1000); // More time than we need - precise timings tested elsewhere.
        assertMemoryValues(mmu, 0xfe00, 0xa0, idx -> (idx * 2) % 0x0100);
    }

    @Test
    public void test_write_of_0x01_to_0xff46_copies_range_0x0100_0x019f_to_0xfe00() {
        Mmu mmu = getTestMmu();
        setRange(mmu, 0x0100, 0xa0, idx -> (idx * 7) % 0x0100);
        mmu.setByte(0xff46, 0x01);
        mmu.stepAhead(1000);
        assertMemoryValues(mmu, 0xfe00, 0xa0, idx -> (idx * 7) % 0x0100);
    }

    @Test
    public void test_write_of_0xb7_to_0xff46_copies_range_0xb700_0xb79f_to_0xfe00() {
        Mmu mmu = getTestMmu();
        setRange(mmu, 0xb700, 0xa0, idx -> (idx * 6) % 0x0100);
        mmu.setByte(0xff46, 0xb7);
        mmu.stepAhead(1000);
        assertMemoryValues(mmu, 0xfe00, 0xa0, idx -> (idx * 6) % 0x0100);
    }

    @Test
    public void test_dma_oam_completes_in_162_cycles() {
        Mmu mmu = getTestMmu();
        setRange(mmu, 0x6b00, 0xa0, idx -> idx % 0x0100);
        mmu.setByte(0xff46, 0x6b);
        mmu.stepAhead(162);
        assertMemoryValues(mmu, 0xfe00, 0xa0, idx -> idx % 0x0100);
    }

    @Test
    public void test_dma_oam_not_completed_after_161_cycles() {
        Mmu mmu = getTestMmu();
        setRange(mmu, 0x0000, 0xa0, idx -> 0xde);
        mmu.setByte(0xff46, 0x00);
        mmu.stepAhead(161);
        assertNotEquals(0xde, mmu.readByte(0xfe9f));
    }

    @Test
    public void test_dma_oam_doesnt_write_outside_range() {
        Mmu mmu = getTestMmu();
        setRange(mmu, 0x0000, 0x2000, idx -> 0xde);
        mmu.setByte(0xff46, 0x01);
        mmu.stepAhead(1000);
        assertNotEquals(0xde, mmu.readByte(0xfdff));
        assertNotEquals(0xde, mmu.readByte(0xfea0));
    }

    @Test
    public void test_two_simultaneous_oams() {
        Mmu mmu = getTestMmu();
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
        Mmu mmu = getTestMmu();
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
        Mmu mmu = getTestMmu();
        assertEquals(0x0f, mmu.readByte(0xff00));
    }

    @Test
    public void writes_to_joypad_affect_only_top_four_bits() {
        Mmu mmu = getTestMmu();
        int initialValue = mmu.readByte(0xff00);
        mmu.setByte(0xff00, 0xff & (~initialValue));
        assertEquals((~initialValue & 0xf0) + (initialValue & 0x0f), mmu.readByte(0xff00));
    }

    @Test
    public void test_all_joypad_inputs_initially_disabled() {
        Mmu mmu = getTestMmu();
        Io io = mmu.getIo();
        for (Io.Button button : Io.Button.values()) {
            assertFalse(button + " should be disabled initially", io.isButtonPressed(button));
        }
    }

    @Test
    public void test_setting_buttons_true_means_they_are_pressed() {
        for (Io.Button button : Io.Button.values()) {
            Mmu mmu = getTestMmu();
            mmu.getIo().setButtonPressed(button, true);
            assertTrue(button + " did not appear enabled after being set", mmu.getIo().isButtonPressed(button));
        }
    }

    @Test
    public void test_setting_buttons_false_means_they_are_not_pressed() {
        for (Io.Button button : Io.Button.values()) {
            Mmu mmu = getTestMmu();
            Io io = mmu.getIo();
            io.setButtonPressed(button, true);
            io.setButtonPressed(button, false);
            assertFalse(button + " was still enabled after being unset", io.isButtonPressed(button));
        }
    }

    @Test
    public void test_pressing_a_with_button_select_on_resets_0xff00_bit_0() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x10);
        mmu.getIo().setButtonPressed(Io.Button.A, true);
        assertEquals(0x00, 0x01 & mmu.readByte(0xff00));
        assertEquals(0x1e, 0xfe & mmu.readByte(0xff00));
    }

    @Test
    public void test_releasing_a_with_button_select_on_sets_0xff00_bit_0() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x10);
        mmu.getIo().setButtonPressed(Io.Button.A, true);
        mmu.getIo().setButtonPressed(Io.Button.A, false);
        assertEquals(0x01, 0x01 & mmu.readByte(0xff00));
        assertEquals(0x1e, 0xfe & mmu.readByte(0xff00));
    }

    @Test
    public void test_pressing_a_with_button_select_off_does_not_modify_memory() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x20);
        mmu.getIo().setButtonPressed(Io.Button.A, true);
        assertEquals(0x01, 0x01 & mmu.readByte(0xff00));
        assertEquals(0x2e, 0xfe & mmu.readByte(0xff00));
    }

    @Test
    public void test_enabling_button_select_after_pressing_a_resets_0xff00_bit_0() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x30);
        mmu.getIo().setButtonPressed(Io.Button.A, true);
        mmu.setByte(0xff00, 0x10);
        assertEquals(0x00, 0x01 & mmu.readByte(0xff00));
    }

    @Test
    public void test_disabling_button_select_after_pressing_a_sets_0xff00_bit_0() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x00);
        mmu.getIo().setButtonPressed(Io.Button.A, true);
        mmu.setByte(0xff00, 0x20);
        assertEquals(0x01, 0x01 & mmu.readByte(0xff00));
    }

    @Test
    public void test_enabling_pad_select_does_not_enable_button_a() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x30);
        mmu.getIo().setButtonPressed(Io.Button.A, true);
        mmu.setByte(0xff00, 0x20);
        assertEquals(0x01, 0x01 & mmu.readByte(0xff00));
    }

    @Test
    public void test_pressing_b_with_button_select_on_resets_0xff00_bit_1() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x10);
        mmu.getIo().setButtonPressed(Io.Button.B, true);
        assertEquals(0x00, 0x02 & mmu.readByte(0xff00));
        assertEquals(0x1d, 0xfd & mmu.readByte(0xff00));
    }

    @Test
    public void test_releasing_b_with_button_select_on_sets_0xff00_bit_1() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x10);
        mmu.getIo().setButtonPressed(Io.Button.B, true);
        mmu.getIo().setButtonPressed(Io.Button.B, false);
        assertEquals(0x02, 0x02 & mmu.readByte(0xff00));
        assertEquals(0x1d, 0xfd & mmu.readByte(0xff00));
    }

    @Test
    public void test_pressing_b_with_button_select_off_does_not_modify_memory() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x20);
        mmu.getIo().setButtonPressed(Io.Button.B, true);
        assertEquals(0x02, 0x02 & mmu.readByte(0xff00));
        assertEquals(0x2d, 0xfd & mmu.readByte(0xff00));
    }

    @Test
    public void test_enabling_button_select_after_pressing_b_resets_0xff00_bit_1() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x20);
        mmu.getIo().setButtonPressed(Io.Button.B, true);
        mmu.setByte(0xff00, 0x00);
        assertEquals(0x00, 0x02 & mmu.readByte(0xff00));
    }

    @Test
    public void test_disabling_button_select_after_pressing_b_sets_0xff00_bit_1() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x00);
        mmu.getIo().setButtonPressed(Io.Button.B, true);
        mmu.setByte(0xff00, 0x20);
        assertEquals(0x02, 0x02 & mmu.readByte(0xff00));
    }

    @Test
    public void test_enabling_pad_select_does_not_enable_button_b() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x30);
        mmu.getIo().setButtonPressed(Io.Button.B, true);
        mmu.setByte(0xff00, 0x20);
        assertEquals(0x02, 0x02 & mmu.readByte(0xff00));
    }

    @Test
    public void test_pressing_select_with_button_select_on_resets_0xff00_bit_2() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x10);
        mmu.getIo().setButtonPressed(Io.Button.SELECT, true);
        assertEquals(0x00, 0x04 & mmu.readByte(0xff00));
        assertEquals(0x1b, 0xfb & mmu.readByte(0xff00));
    }

    @Test
    public void test_releasing_select_with_button_select_on_sets_0xff00_bit_2() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x10);
        mmu.getIo().setButtonPressed(Io.Button.SELECT, true);
        mmu.getIo().setButtonPressed(Io.Button.SELECT, false);
        assertEquals(0x04, 0x04 & mmu.readByte(0xff00));
        assertEquals(0x1b, 0xfb & mmu.readByte(0xff00));
    }

    @Test
    public void test_pressing_select_with_button_select_off_does_not_modify_memory() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x20);
        mmu.getIo().setButtonPressed(Io.Button.SELECT, true);
        assertEquals(0x04, 0x04 & mmu.readByte(0xff00));
        assertEquals(0x2b, 0xfb & mmu.readByte(0xff00));
    }

    @Test
    public void test_enabling_button_select_after_pressing_select_resets_0xff00_bit_2() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x20);
        mmu.getIo().setButtonPressed(Io.Button.SELECT, true);
        mmu.setByte(0xff00, 0x10);
        assertEquals(0x00, 0x04 & mmu.readByte(0xff00));
    }

    @Test
    public void test_disabling_button_select_after_pressing_select_sets_0xff00_bit_2() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x10);
        mmu.getIo().setButtonPressed(Io.Button.SELECT, true);
        mmu.setByte(0xff00, 0x20);
        assertEquals(0x04, 0x04 & mmu.readByte(0xff00));
    }

    @Test
    public void test_enabling_pad_select_does_not_enable_select_button() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x20);
        mmu.getIo().setButtonPressed(Io.Button.SELECT, true);
        assertEquals(0x04, 0x04 & mmu.readByte(0xff00));
    }

    @Test
    public void test_pressing_start_with_button_select_on_resets_0xff00_bit_3() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x10);
        mmu.getIo().setButtonPressed(Io.Button.START, true);
        assertEquals(0x00, 0x08 & mmu.readByte(0xff00));
        assertEquals(0x17, 0xf7 & mmu.readByte(0xff00));
    }

    @Test
    public void test_releasing_start_with_button_select_on_sets_0xff00_bit_3() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x00);
        mmu.getIo().setButtonPressed(Io.Button.START, true);
        mmu.getIo().setButtonPressed(Io.Button.START, false);
        assertEquals(0x08, 0x08 & mmu.readByte(0xff00));
        assertEquals(0x07, 0xf7 & mmu.readByte(0xff00));
    }

    @Test
    public void test_pressing_start_with_button_select_off_does_not_modify_memory() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x30);
        mmu.getIo().setButtonPressed(Io.Button.START, true);
        assertEquals(0x08, 0x08 & mmu.readByte(0xff00));
        assertEquals(0x37, 0xf7 & mmu.readByte(0xff00));
    }

    @Test
    public void test_enabling_button_select_after_pressing_start_resets_0xff00_bit_3() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x30);
        mmu.getIo().setButtonPressed(Io.Button.START, true);
        mmu.setByte(0xff00, 0x10);
        assertEquals(0x00, 0x08 & mmu.readByte(0xff00));
    }

    @Test
    public void test_disabling_button_select_after_pressing_start_sets_0xff00_bit_3() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x10);
        mmu.getIo().setButtonPressed(Io.Button.START, true);
        mmu.setByte(0xff00, 0x30);
        assertEquals(0x08, 0x08 & mmu.readByte(0xff00));
    }

    @Test
    public void test_enabling_pad_select_does_not_enable_start_button() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x30);
        mmu.getIo().setButtonPressed(Io.Button.START, true);
        mmu.setByte(0xff00, 0x20);
        assertEquals(0x08, 0x08 & mmu.readByte(0xff00));
    }

    @Test
    public void test_pressing_right_with_pad_select_on_resets_0xff00_bit_0() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x20);
        mmu.getIo().setButtonPressed(Io.Button.RIGHT, true);
        assertEquals(0x00, 0x01 & mmu.readByte(0xff00));
        assertEquals(0x2e, 0xfe & mmu.readByte(0xff00));
    }

    @Test
    public void test_releasing_right_with_pad_select_on_sets_0xff00_bit_0() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x10);
        mmu.getIo().setButtonPressed(Io.Button.RIGHT, true);
        mmu.getIo().setButtonPressed(Io.Button.RIGHT, false);
        assertEquals(0x01, 0x01 & mmu.readByte(0xff00));
        assertEquals(0x1e, 0xfe & mmu.readByte(0xff00));
    }

    @Test
    public void test_pressing_right_with_pad_select_off_does_not_modify_memory() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x10);
        mmu.getIo().setButtonPressed(Io.Button.RIGHT, true);
        assertEquals(0x01, 0x01 & mmu.readByte(0xff00));
        assertEquals(0x1e, 0xfe & mmu.readByte(0xff00));
    }

    @Test
    public void test_enabling_pad_select_after_pressing_right_resets_0xff00_bit_0() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x10);
        mmu.getIo().setButtonPressed(Io.Button.RIGHT, true);
        mmu.setByte(0xff00, 0x00);
        assertEquals(0x00, 0x01 & mmu.readByte(0xff00));
    }

    @Test
    public void test_disabling_pad_select_after_pressing_right_sets_0xff00_bit_0() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x00);
        mmu.getIo().setButtonPressed(Io.Button.RIGHT, true);
        mmu.setByte(0xff00, 0x10);
        assertEquals(0x01, 0x01 & mmu.readByte(0xff00));
    }

    @Test
    public void test_enabling_button_select_does_not_enable_right_button() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x10);
        mmu.getIo().setButtonPressed(Io.Button.RIGHT, true);
        assertEquals(0x01, 0x01 & mmu.readByte(0xff00));
    }

    @Test
    public void test_pressing_left_with_pad_select_on_resets_0xff00_bit_1() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x20);
        mmu.getIo().setButtonPressed(Io.Button.LEFT, true);
        assertEquals(0x00, 0x02 & mmu.readByte(0xff00));
        assertEquals(0x2d, 0xfd & mmu.readByte(0xff00));
    }

    @Test
    public void test_releasing_left_with_pad_select_on_sets_0xff00_bit_1() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x20);
        mmu.getIo().setButtonPressed(Io.Button.LEFT, true);
        mmu.getIo().setButtonPressed(Io.Button.LEFT, false);
        assertEquals(0x02, 0x02 & mmu.readByte(0xff00));
        assertEquals(0x2d, 0xfd & mmu.readByte(0xff00));
    }

    @Test
    public void test_pressing_left_with_pad_select_off_does_not_modify_memory() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x10);
        mmu.getIo().setButtonPressed(Io.Button.LEFT, true);
        assertEquals(0x02, 0x02 & mmu.readByte(0xff00));
        assertEquals(0x1d, 0xfd & mmu.readByte(0xff00));
    }

    @Test
    public void test_enabling_pad_select_after_pressing_left_resets_0xff00_bit_1() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x10);
        mmu.getIo().setButtonPressed(Io.Button.LEFT, true);
        mmu.setByte(0xff00, 0x00);
        assertEquals(0x00, 0x02 & mmu.readByte(0xff00));
    }

    @Test
    public void test_disabling_pad_select_after_pressing_left_sets_0xff00_bit_1() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x00);
        mmu.getIo().setButtonPressed(Io.Button.LEFT, true);
        mmu.setByte(0xff00, 0x10);
        assertEquals(0x02, 0x02 & mmu.readByte(0xff00));
    }

    @Test
    public void test_enabling_button_select_does_not_enable_left_button() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x10);
        mmu.getIo().setButtonPressed(Io.Button.LEFT, true);
        assertEquals(0x02, 0x02 & mmu.readByte(0xff00));
    }

    @Test
    public void test_pressing_up_with_pad_select_on_resets_0xff00_bit_2() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x20);
        mmu.getIo().setButtonPressed(Io.Button.UP, true);
        assertEquals(0x00, 0x04 & mmu.readByte(0xff00));
        assertEquals(0x2b, 0xfb & mmu.readByte(0xff00));
    }

    @Test
    public void test_releasing_up_with_pad_select_on_sets_0xff00_bit_2() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x10);
        mmu.getIo().setButtonPressed(Io.Button.UP, true);
        mmu.getIo().setButtonPressed(Io.Button.UP, false);
        assertEquals(0x04, 0x04 & mmu.readByte(0xff00));
        assertEquals(0x1b, 0xfb & mmu.readByte(0xff00));
    }

    @Test
    public void test_pressing_up_with_pad_select_off_does_not_modify_memory() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x10);
        mmu.getIo().setButtonPressed(Io.Button.UP, true);
        assertEquals(0x04, 0x04 & mmu.readByte(0xff00));
        assertEquals(0x1b, 0xfb & mmu.readByte(0xff00));
    }

    @Test
    public void test_enabling_pad_select_after_pressing_up_resets_0xff00_bit_2() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x30);
        mmu.getIo().setButtonPressed(Io.Button.UP, true);
        mmu.setByte(0xff00, 0x20);
        assertEquals(0x00, 0x04 & mmu.readByte(0xff00));
    }

    @Test
    public void test_disabling_pad_select_after_pressing_up_sets_0xff00_bit_2() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x00);
        mmu.getIo().setButtonPressed(Io.Button.UP, true);
        mmu.setByte(0xff00, 0x10);
        assertEquals(0x04, 0x04 & mmu.readByte(0xff00));
    }

    @Test
    public void test_enabling_button_select_does_not_enable_up_button() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x30);
        mmu.getIo().setButtonPressed(Io.Button.UP, true);
        mmu.setByte(0xff00, 0x10);
        assertEquals(0x04, 0x04 & mmu.readByte(0xff00));
    }

    @Test
    public void test_pressing_down_with_pad_select_on_resets_0xff00_bit_3() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x20);
        mmu.getIo().setButtonPressed(Io.Button.DOWN, true);
        assertEquals(0x00, 0x08 & mmu.readByte(0xff00));
        assertEquals(0x27, 0xf7 & mmu.readByte(0xff00));
    }

    @Test
    public void test_releasing_down_with_pad_select_on_sets_0xff00_bit_3() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x10);
        mmu.getIo().setButtonPressed(Io.Button.DOWN, true);
        mmu.getIo().setButtonPressed(Io.Button.DOWN, false);
        assertEquals(0x08, 0x08 & mmu.readByte(0xff00));
        assertEquals(0x17, 0xf7 & mmu.readByte(0xff00));
    }

    @Test
    public void test_pressing_down_with_pad_select_off_does_not_modify_memory() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x10);
        mmu.getIo().setButtonPressed(Io.Button.DOWN, true);
        assertEquals(0x08, 0x08 & mmu.readByte(0xff00));
        assertEquals(0x17, 0xf7 & mmu.readByte(0xff00));
    }

    @Test
    public void test_enabling_pad_select_after_pressing_down_resets_0xff00_bit_3() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x30);
        mmu.getIo().setButtonPressed(Io.Button.DOWN, true);
        mmu.setByte(0xff00, 0x20);
        assertEquals(0x00, 0x08 & mmu.readByte(0xff00));
    }

    @Test
    public void test_disabling_pad_select_after_pressing_down_sets_0xff00_bit_3() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x00);
        mmu.getIo().setButtonPressed(Io.Button.DOWN, true);
        mmu.setByte(0xff00, 0x10);
        assertEquals(0x08, 0x08 & mmu.readByte(0xff00));
    }

    @Test
    public void test_enabling_button_select_does_not_enable_down_button() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x10);
        mmu.getIo().setButtonPressed(Io.Button.DOWN, true);
        assertEquals(0x08, 0x08 & mmu.readByte(0xff00));
    }

    @Test
    public void test_pressing_left_when_b_is_pressed_and_both_pad_and_buttons_are_selected_does_nothing() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x00);
        mmu.getIo().setButtonPressed(Io.Button.LEFT, true);
        assertEquals("Bit 1 should be low at this point", 0x00, 0x02 & mmu.readByte(0xff00));
        mmu.getIo().setButtonPressed(Io.Button.UP, true);
        assertEquals("Bit 1 should still be low", 0x00, 0x02 & mmu.readByte(0xff00));
    }

    @Test
    public void test_releasing_start_when_start_and_down_are_pressed_and_both_pad_and_buttons_are_selected_does_nothing() {
        Mmu mmu = getTestMmu();
        mmu.getIo().setButtonPressed(Io.Button.START, true);
        mmu.getIo().setButtonPressed(Io.Button.DOWN, true);
        mmu.setByte(0xff00, 0x00);
        assertEquals("At this point bit 4 should be low", 0x00, 0x08 & mmu.readByte(0xff00));
        mmu.getIo().setButtonPressed(Io.Button.START, false);
        assertEquals("Bit 4 should still be low", 0x00, 0x08 & mmu.readByte(0xff00));
    }

    @Test
    public void test_when_pad_and_buttons_are_selected_then_both_pad_and_buttons_can_fire() {
        Mmu mmu = getTestMmu();
        Io io = mmu.getIo();
        mmu.setByte(0xff00, 0x00);
        io.setButtonPressed(Io.Button.A, true);
        io.setButtonPressed(Io.Button.B, true);
        io.setButtonPressed(Io.Button.UP, true);
        io.setButtonPressed(Io.Button.DOWN, true);
        assertEquals(0x00, 0x0f & mmu.readByte(0xff00));
    }

    @Test
    public void test_switching_from_button_to_pad_select_and_back() {
        Mmu mmu = getTestMmu();
        Io io = mmu.getIo();
        mmu.setByte(0xff00, 0x30); // Start with both lines disabled
        io.setButtonPressed(Io.Button.START, true);
        io.setButtonPressed(Io.Button.SELECT, true);
        io.setButtonPressed(Io.Button.LEFT, true);
        io.setButtonPressed(Io.Button.RIGHT, true);
        assertEquals("Joypad bits should be high", 0x0f, 0x0f & mmu.readByte(0xff00));
        mmu.setByte(0xff00, 0x10); // Select buttons only
        assertEquals(0x03, 0x0f & mmu.readByte(0xff00));
        mmu.setByte(0xff00, 0x20); // Select pad only
        assertEquals(0x0c, 0x0f & mmu.readByte(0xff00));
        mmu.setByte(0xff00, 0x10); // Select buttons again
        assertEquals(0x03, 0x0f & mmu.readByte(0xff00));
    }

    @Test
    public void test_pressing_a_selected_button_fires_joypad_interrupt() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x10);
        assertFalse("No interrupt should be fired at the start", mmu.checkInterrupt(Interrupt.JOYPAD));
        mmu.getIo().setButtonPressed(Io.Button.START, true);
        assertTrue("Joypad interrupt should now have fired", mmu.checkInterrupt(Interrupt.JOYPAD));
    }

    @Test
    public void test_pressing_an_unselected_button_does_not_fire_joypad_interrupt() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x20);
        mmu.getIo().setButtonPressed(Io.Button.B, true);
        assertFalse(mmu.checkInterrupt(Interrupt.JOYPAD));
    }

    @Test
    public void test_resetting_a_selected_button_does_not_fire_an_interrupt() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x10);
        mmu.getIo().setButtonPressed(Io.Button.LEFT, true);
        mmu.resetInterrupt(Interrupt.JOYPAD);
        mmu.getIo().setButtonPressed(Io.Button.LEFT, false);
        assertFalse(mmu.checkInterrupt(Interrupt.JOYPAD));
    }

    @Test
    public void test_resetting_a_selected_button_does_not_clear_the_existing_interrupt() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x20);
        mmu.getIo().setButtonPressed(Io.Button.DOWN, true);
        mmu.getIo().setButtonPressed(Io.Button.DOWN, false);
        assertTrue(mmu.checkInterrupt(Interrupt.JOYPAD));
    }

    @Test
    public void test_selecting_a_pressed_button_fires_an_interrupt() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff00, 0x30);
        mmu.getIo().setButtonPressed(Io.Button.SELECT, true);
        assertFalse("At this point no interrupt should have fired", mmu.checkInterrupt(Interrupt.JOYPAD));
        mmu.setByte(0xff00, 0x10);
        assertTrue(mmu.checkInterrupt(Interrupt.JOYPAD));
    }

    @Test
    public void test_timer_enabled_if_and_only_if_ff07_bit_2_is_high() {
        doRangedBitCheckTest(0xff07,2, (mmu, isBit2High) -> {
            final boolean timerShouldBeEnabled = isBit2High;
            assertEquals("Unexpected timer enable state when 0xff07 is 0x" + Integer.toString(mmu.readByte(0xff07)),
                timerShouldBeEnabled, mmu.getIo().isTimerEnabled());
        });
    }

    @Test
    public void test_timer_counter_ticks_per_cycle() {
        doRangeTest(0xff07, mmu -> {
            int timerControlByte = mmu.readByte(0xff07);
            int speedBits = (timerControlByte & 0x03);
            int expected;
            if (speedBits == 0b00) {
                expected = 1024;
            } else if (speedBits == 0b01) {
                expected = 16;
            } else if (speedBits == 0b10) {
                expected = 64;
            } else {
                expected = 256;
            }

            assertEquals("Unexpected timer speed when control byte was 0x" + Integer.toHexString(mmu.readByte(0xff07)),
                    expected,
                    mmu.getIo().getCyclesPerTimerCounterTick());
        });
    }

    @Test
    public void test_get_timer_div_returns_0xff04() {
        doRangeTest(0xff04, mmu -> {
            assertEquals(mmu.readByte(0xff04), mmu.getIo().getTimerDiv());
        });
    }

    @Test
    public void test_writing_anything_to_timer_div_resets_it() {
        for (int toWrite = 0x00; toWrite <= 0xff; toWrite++) {
            final int toWrite2 = toWrite;
            doRangeTest(0xff04, mmu -> {
                int oldValue = mmu.readByte(0xff04);
                mmu.setByte(0xff04, toWrite2);
                assertEquals("Writing 0x" + Integer.toHexString(toWrite2) + " to 0xff04 should reset it",
                        0x00,
                        mmu.readByte(0xff04));
            });
        }
    }

    @Test
    public void test_setting_timer_div_writes_to_0xff04() {
        for (int val = 0x00; val <= 0xff; val++) {
            Mmu mmu = getTestMmu();
            mmu.getIo().setTimerDiv(val);
            assertEquals(val, mmu.readByte(0xff04));
        }
    }

    @Test
    public void test_get_timer_counter() {
        doRangeTest(0xff05, mmu -> {
            assertEquals(mmu.readByte(0xff05), mmu.getIo().getTimerCounter());
        });
    }

    @Test
    public void test_set_timer_counter() {
        for (int val = 0x00; val < 0xff; val++) {
            Mmu mmu = getTestMmu();
            mmu.getIo().setTimerCounter(val);
            assertEquals(val, mmu.readByte(0xff05));
        }
    }

    @Test
    public void test_reset_timer_counter() {
        doRangeTest(0xff06, mmu -> {
            mmu.getIo().resetTimerCounter();
            assertEquals(mmu.readByte(0xff06), mmu.getIo().getTimerCounter());
        });
    }

    @Test
    public void test_get_sprite_data_start_address_returns_0x8000() {
        assertEquals(0x8000, getTestMmu().getIo().getSpriteDataStartAddress());
    }

    @Test
    public void test_get_sprite_pattern_start_address_returns_0xfe00() {
        assertEquals(0xfe00, getTestMmu().getIo().getSpritePatternStartAddress());
    }

    @Test
    public void test_listeners_fired_on_sprite_height_change_from_8_to_16() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff40, 0x00);
        TestHeightListener listener = new TestHeightListener();
        mmu.getIo().register(listener);
        mmu.setByte(0xff40, 0x04);
        assertEquals(true, listener.tallSpritesEnabledOnLastCall);
    }

    @Test
    public void test_listeners_fired_on_sprite_height_change_from_16_to_8() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff40, 0x04);
        TestHeightListener listener = new TestHeightListener();
        mmu.getIo().register(listener);
        mmu.setByte(0xff40, 0x00);
        assertEquals(false, listener.tallSpritesEnabledOnLastCall);
    }

    @Test
    public void test_repeated_high_writes_to_control_address_ignored_for_height_change_event() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff40, 0x00);
        AtomicBoolean alreadyFired = new AtomicBoolean(false);
        TestHeightListener listener = new TestHeightListener() {
            @Override
            public void onSpriteHeightChanged(boolean useTallSprites) {
                assertFalse("Unexpectedly fired a second time", alreadyFired.getAndSet(true));
            }
        };
        mmu.getIo().register(listener);
        mmu.setByte(0xff40, 0x04);
        mmu.setByte(0xff40, 0xff);
    }

    @Test
    public void test_repeated_low_writes_to_control_address_ignored_for_height_change_event() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff40, 0x04);
        AtomicBoolean alreadyFired = new AtomicBoolean(false);
        TestHeightListener listener = new TestHeightListener() {
            @Override
            public void onSpriteHeightChanged(boolean useTallSprites) {
                assertFalse("Unexpectedly fired a second time", alreadyFired.getAndSet(true));
            }
        };
        mmu.getIo().register(listener);
        mmu.setByte(0xff40, 0x00);
        mmu.setByte(0xff40, 0xfb);
    }

    @Test
    public void test_multiple_sprite_height_changes() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff40, 0x00);
        AtomicInteger timesFired = new AtomicInteger(0);
        TestHeightListener listener = new TestHeightListener() {
            @Override
            public void onSpriteHeightChanged(boolean useTallSprites) {
                timesFired.incrementAndGet();
            }
        };
        mmu.getIo().register(listener);
        mmu.setByte(0xff40, 0x04);
        mmu.setByte(0xff40, 0x00);
        mmu.setByte(0xff40, 0x04);
        mmu.setByte(0xff40, 0x00);
        mmu.setByte(0xff40, 0x04);
        mmu.setByte(0xff40, 0x00);
        assertEquals(6, timesFired.get());
    }

    @Test
    public void test_sprite_height_change_not_fired_after_write_if_height_unchanged_even_with_new_listener() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff40, 0xff);
        TestHeightListener listener = new TestHeightListener() {
            @Override
            public void onSpriteHeightChanged(boolean useTallSprites) {
                fail();
            }
        };
        mmu.getIo().register(listener);
        mmu.setByte(0xff40, 0xff);
    }

    @Test
    public void test_square_1_sweep_period_is_0x00_when_ff10_is_0x00() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff10, 0x00);
        assertEquals(0x00, mmu.getIo().getSquare1SweepPeriod());
    }

    @Test
    public void test_square_1_sweep_period_is_0x00_when_ff10_is_0x8f() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff10, 0x8f);
        assertEquals(0x00, mmu.getIo().getSquare1SweepPeriod());
    }

    @Test
    public void test_square_1_sweep_period_is_0x01_when_ff10_is_0x10() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff10, 0x10);
        assertEquals(0x01, mmu.getIo().getSquare1SweepPeriod());
    }

    @Test
    public void test_square_1_sweep_period_is_0x05_when_ff10_is_0x50() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff10, 0x50);
        assertEquals(0x05, mmu.getIo().getSquare1SweepPeriod());
    }

    @Test
    public void test_square_1_sweep_period_is_0x05_when_ff10_is_0xdf() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff10, 0xdf);
        assertEquals(0x05, mmu.getIo().getSquare1SweepPeriod());
    }

    @Test
    public void test_square_1_sweep_period_is_0x07_when_ff10_is_0x70() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff10, 0x70);
        assertEquals(0x07, mmu.getIo().getSquare1SweepPeriod());
    }

    @Test
    public void test_square_1_sweep_sign_is_positive_iff_ff10_bit_3_is_high() {
        doRangedBitCheckTest(0xff10, 3, (mmu, isHigh) -> {
            int expected = isHigh ? -1 : +1;
            assertEquals(expected, mmu.getIo().getSquare1SweepSign());
        });
    }

    @Test
    public void test_square_1_sweep_shift_is_0x00_when_ff10_is_0x00() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff10, 0x00);
        assertEquals(0x00, mmu.getIo().getSquare1SweepShift());
    }

    @Test
    public void test_square_1_sweep_shift_is_0x01_when_ff10_is_0x01() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff10, 0x01);
        assertEquals(0x01, mmu.getIo().getSquare1SweepShift());
    }

    @Test
    public void test_square_1_sweep_shift_is_0x00_when_ff10_is_0xf8() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff10, 0xf8);
        assertEquals(0x00, mmu.getIo().getSquare1SweepShift());
    }

    @Test
    public void test_square_1_sweep_shift_is_0x06_when_ff10_is_0x06() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff10, 0x06);
        assertEquals(0x06, mmu.getIo().getSquare1SweepShift());
    }

    @Test
    public void test_square_1_sweep_shift_is_0x06_when_ff10_is_0xfe() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff10, 0xfe);
        assertEquals(0x06, mmu.getIo().getSquare1SweepShift());
    }

    @Test
    public void test_square_1_sweep_shift_is_0x07_when_ff10_is_0x07() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff10, 0x07);
        assertEquals(0x07, mmu.getIo().getSquare1SweepShift());
    }

    @Test
    public void test_square_1_duty() {
        boolean[][] duties = new boolean[][] {
                new boolean[] {false, false, false, false, false, false, false, true},
                new boolean[] {true, false, false, false, false, false, false, true},
                new boolean[] {true, false, false, false, false, true, true, true},
                new boolean[] {false, true, true, true, true, true, true, false}
        };

        doRangeTest(0xff11, mmu -> {
            int nr11 = mmu.readByte(0xff11);
            int dutyHigh = (nr11 & 0x80) > 0 ? 2 : 0;
            int dutyLow = (nr11 & 0x40) > 0 ? 1 : 0;
            int dutyIdx = dutyHigh + dutyLow;
            assertArrayEquals(duties[dutyIdx], mmu.getIo().getSquare1DutyCycle());
        });
    }

    @Test
    public void test_square_1_length_counter_set_to_0x40_when_0xff11_is_0x00() {
        final AtomicInteger seenLength = new AtomicInteger(-1);
        Mmu mmu = getTestMmu();
        mmu.getIo().register(new Square1RegisterAdapter() {
            public void onLengthCounterUpdated(int newValue) {
                seenLength.set(newValue);
            }
        });
        mmu.setByte(0xff11, 0x00);
        assertEquals(0x40, seenLength.get());
    }

    @Test
    public void test_square_1_length_counter_set_to_0x3f_when_0xff11_is_0x01() {
        final AtomicInteger seenLength = new AtomicInteger(-1);
        Mmu mmu = getTestMmu();
        mmu.getIo().register(new Square1RegisterAdapter() {
            public void onLengthCounterUpdated(int newValue) {
                seenLength.set(newValue);
            }
        });
        mmu.setByte(0xff11, 0x01);
        assertEquals(0x3f, seenLength.get());
    }

    @Test
    public void test_square_1_length_counter_set_to_0x3f_when_0xff11_is_0xc1() {
        final AtomicInteger seenLength = new AtomicInteger(-1);
        Mmu mmu = getTestMmu();
        mmu.getIo().register(new Square1RegisterAdapter() {
            public void onLengthCounterUpdated(int newValue) {
                seenLength.set(newValue);
            }
        });
        mmu.setByte(0xff11, 0xc1);
        assertEquals(0x3f, seenLength.get());
    }

    @Test
    public void test_square_1_length_counter_set_to_0x14_when_0xff11_is_0x2c() {
        final AtomicInteger seenLength = new AtomicInteger(-1);
        Mmu mmu = getTestMmu();
        mmu.getIo().register(new Square1RegisterAdapter() {
            public void onLengthCounterUpdated(int newValue) {
                seenLength.set(newValue);
            }
        });
        mmu.setByte(0xff11, 0x2c);
        assertEquals(0x14, seenLength.get());
    }

    @Test
    public void test_square_1_length_counter_set_to_0x14_when_0xff11_is_0xec() {
        final AtomicInteger seenLength = new AtomicInteger(-1);
        Mmu mmu = getTestMmu();
        mmu.getIo().register(new Square1RegisterAdapter() {
            public void onLengthCounterUpdated(int newValue) {
                seenLength.set(newValue);
            }
        });
        mmu.setByte(0xff11, 0xec);
        assertEquals(0x14, seenLength.get());
    }

    @Test
    public void test_square_1_length_counter_set_to_0x01_when_0xff11_is_0x3f() {
        final AtomicInteger seenLength = new AtomicInteger(-1);
        Mmu mmu = getTestMmu();
        mmu.getIo().register(new Square1RegisterAdapter() {
            public void onLengthCounterUpdated(int newValue) {
                seenLength.set(newValue);
            }
        });
        mmu.setByte(0xff11, 0xec);
        assertEquals(0x14, seenLength.get());
    }

    @Test
    public void test_square_1_length_counter_set_to_0x40_when_0xff11_is_0x40() {
        // The period is contained in the lower 6 bits of NR11.
        // 0x40 is 0b0100_0000 so should be treated the same as 0x00
        // and so should have the same period as if 0xff11 were 0x00.
        final AtomicInteger seenLength = new AtomicInteger(-1);
        Mmu mmu = getTestMmu();
        mmu.getIo().register(new Square1RegisterAdapter() {
            public void onLengthCounterUpdated(int newValue) {
                seenLength.set(newValue);
            }
        });
        mmu.setByte(0xff11, 0xec);
        assertEquals(0x14, seenLength.get());
    }

    @Test
    public void test_square_1_starting_volume_is_0x00_when_0xff12_is_0x00() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff12, 0x00);
        assertEquals(0x00, mmu.getIo().getSquare1StartingVolume());
    }

    @Test
    public void test_square_1_starting_volume_is_0x01_when_0xff12_is_0x10() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff12, 0x10);
        assertEquals(0x01, mmu.getIo().getSquare1StartingVolume());
    }

    @Test
    public void test_square_1_starting_volume_is_0x0e_when_ff12_is_0xe0() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff12, 0xe0);
        assertEquals(0x0e, mmu.getIo().getSquare1StartingVolume());
    }

    @Test
    public void test_square_1_starting_volume_is_0x0f_when_ff12_is_0xff() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff12, 0xff);
        assertEquals(0x0f, mmu.getIo().getSquare1StartingVolume());
    }

    @Test
    public void test_square_1_starting_volume_is_0x00_when_ff12_is_0x0f() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff12, 0x0f);
        assertEquals(0x00, mmu.getIo().getSquare1StartingVolume());
    }

    @Test
    public void test_square_1_envelope_sign() {
        doRangedBitCheckTest(0xff12, 3, (mmu, isBitHigh) -> {
            int expectedSign = isBitHigh ? +1 : -1;
            assertEquals(expectedSign, mmu.getIo().getSquare1EnvelopeSign());
        });
    }

    @Test
    public void test_get_square_1_envelope_step_length_is_0x00_when_0xff12_is_0x00() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff12, 0x00);
        assertEquals(0x00, mmu.getIo().getSquare1EnvelopeStepLength());
    }

    @Test
    public void test_get_square_1_envelope_step_length_is_0x01_when_0xff12_is_0x01() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff12, 0x01);
        assertEquals(0x01, mmu.getIo().getSquare1EnvelopeStepLength());
    }

    @Test
    public void test_get_square_1_envelope_step_length_is_0x01_when_0xff12_is_0xf9() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff12, 0xf9);
        assertEquals(0x01, mmu.getIo().getSquare1EnvelopeStepLength());
    }

    @Test
    public void test_get_square_1_envelope_step_length_is_0x05_when_0xff12_is_0x05() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff12, 0x05);
        assertEquals(0x05, mmu.getIo().getSquare1EnvelopeStepLength());
    }

    @Test
    public void test_get_square_1_envelope_step_length_is_0x07_when_0xff12_is_0x07() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff12, 0x07);
        assertEquals(0x07, mmu.getIo().getSquare1EnvelopeStepLength());
    }

    @Test
    public void test_get_square_1_envelope_step_length_is_0x00_when_0xff12_is_0x08() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff12, 0x08);
        assertEquals(0x00, mmu.getIo().getSquare1EnvelopeStepLength());
    }

    @Test
    public void test_get_square_1_freq_ctr_returns_0x000_when_0xff13_is_0x00_and_0xff14_is_00() {
        doSquare1FrequencyTest(0x00, 0x00, 0x000);
    }

    @Test
    public void test_get_square_1_freq_ctr_returns_0x001_when_0xff13_is_0x01_and_0xff14_is_0x00() {
        doSquare1FrequencyTest(0x01, 0x00, 0x001);
    }

    @Test
    public void test_get_square_1_freq_ctr_returns_0x0ab_when_0xff13_is_0xab_and_0xff14_is_0x00() {
        doSquare1FrequencyTest(0xab, 0x00, 0x0ab);
    }

    @Test
    public void test_get_square_1_freq_ctr_returns_0x0ff_when_0xff13_is_0xff_and_0xff14_is_0x00() {
        doSquare1FrequencyTest(0xff, 0x00, 0x0ff);
    }

    @Test
    public void test_get_square_1_freq_ctr_returns_0x149_when_0xff13_is_0x49_and_0xff14_is_0x01() {
        doSquare1FrequencyTest(0x49, 0x01, 0x149);
    }

    @Test
    public void test_get_square_1_freq_ctr_returns_0x5bf_when_0xff13_is_0xbf_and_0xff14_is_0x05() {
        doSquare1FrequencyTest(0xbf, 0x05, 0x5bf);
    }

    @Test
    public void test_get_square_1_freq_ctr_returns_0x790_when_0xff13_is_0x90_and_0xff14_is_0x07() {
        doSquare1FrequencyTest(0x90, 0x07, 0x790);
    }

    @Test
    public void test_get_square_1_freq_ctr_returns_0x090_when_0xff13_is_0x90_and_0xff14_is_0xf8() {
        doSquare1FrequencyTest(0x90, 0xf8, 0x090);
    }

    @Test
    public void test_get_square_1_freq_ctr_returns_0x790_when_0xff13_is_0x90_and_0xff14_is_0xff() {
        doSquare1FrequencyTest(0x90, 0xff, 0x790);
    }

    @Test
    public void test_get_square_1_freq_ctr_returns_0x000_when_0xff13_is_0x00_and_0xff14_is_0xf8() {
        doSquare1FrequencyTest(0x00, 0xf8, 0x000);
    }

    @Test
    public void test_get_square_1_freq_ctr_returns_0x543_when_0xff13_is_0x43_and_0xff14_is_0x2d() {
        doSquare1FrequencyTest(0x43, 0x2d, 0x543);
    }

    @Test
    public void test_is_square_1_continuous_mode_enabled_is_true_iff_0xff14_bit_6_is_low() {
        doRangedBitCheckTest(0xff14, 6, (mmu, bitIsHigh) -> {
            assertEquals(!bitIsHigh, mmu.getIo().isSquare1ContinuousModeEnabled());
        });
    }

    @Test
    public void test_square_1_trigger() {
        for (int val = 0; val <= 0xff; val++) {
            AtomicBoolean triggered = new AtomicBoolean(false);
            Mmu mmu = getTestMmu();
            mmu.getIo().register(new Square1RegisterAdapter() {
                public void onTrigger() {
                    triggered.set(true);
                }
            });
            mmu.setByte(0xff14, val);
            assertEquals((val & 0x80) > 0, triggered.get());
        }
    }

    @Test
    public void test_clear_square_1_restart_flag() {
        doRangedBitSetTest(0xff14, 7, false, mmu -> mmu.getIo().clearSquare1RestartFlag());
    }

    @Test
    public void test_set_square_1_frequency_counter() {
        Mmu mmu = getTestMmu();
        for (int nr13 = 0x00; nr13 <= 0xff; nr13++) {
            for (int nr14 = 0x00; nr14 <= 0xff; nr14++) {
                for (int newFreq = 0x000; newFreq <= 0x7ff; newFreq++) {
                    mmu.setByte(0xff13, nr13);
                    mmu.setByte(0xff14, nr14);
                    mmu.getIo().setSquare1FrequencyCounter(newFreq);
                    assertEquals("Incorrect value for LSB", newFreq & 0xff, mmu.readByte(0xff13));
                    assertEquals("Incorrect value for MSB", (newFreq & 0x07) >> 8, mmu.readByte(0xff14) & 0x07);
                    assertEquals("Unrelated NR14 bytes changed unexpectedly", nr14 & 0xf8, mmu.readByte(0xff14) & 0xf8);
                }
            }
        }
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_set_square_1_frequency_counter_fails_on_negative_input() {
        getTestMmu().getIo().setSquare1FrequencyCounter(-1);
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_set_square_1_frequency_counter_fails_on_overlarge_value() {
        getTestMmu().getIo().setSquare1FrequencyCounter(0x800);
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_set_square_1_frequency_counter_fails_on_overlarge_value_2() {
        getTestMmu().getIo().setSquare1FrequencyCounter(0x9ab);
    }

    @Test
    public void test_square_2_duty() {
        boolean[][] duties = new boolean[][] {
                new boolean[] {false, false, false, false, false, false, false, true},
                new boolean[] {true, false, false, false, false, false, false, true},
                new boolean[] {true, false, false, false, false, true, true, true},
                new boolean[] {false, true, true, true, true, true, true, false}
        };

        doRangeTest(0xff16, mmu -> {
            int nr21 = mmu.readByte(0xff16);
            int dutyHigh = (nr21 & 0x80) > 0 ? 2 : 0;
            int dutyLow = (nr21 & 0x40) > 0 ? 1 : 0;
            int dutyIdx = dutyHigh + dutyLow;
            assertArrayEquals(duties[dutyIdx], mmu.getIo().getSquare2DutyCycle());
        });
    }

    @Test
    public void test_square_2_length_counter_set_to_0x40_when_0xff16_is_0x00() {
        final AtomicInteger seenLength = new AtomicInteger(-1);
        Mmu mmu = getTestMmu();
        mmu.getIo().register(new Square2RegisterAdapter() {
            public void onLengthCounterUpdated(int newValue) {
                seenLength.set(newValue);
            }
        });
        mmu.setByte(0xff16, 0x00);
        assertEquals(0x40, seenLength.get());
    }

    @Test
    public void test_square_2_length_counter_set_to_0x3f_when_0xff16_is_0x01() {
        final AtomicInteger seenLength = new AtomicInteger(-1);
        Mmu mmu = getTestMmu();
        mmu.getIo().register(new Square2RegisterAdapter() {
            public void onLengthCounterUpdated(int newValue) {
                seenLength.set(newValue);
            }
        });
        mmu.setByte(0xff16, 0x01);
        assertEquals(0x3f, seenLength.get());
    }

    @Test
    public void test_square_2_length_counter_set_to_0x3f_when_0xff16_is_0xc1() {
        final AtomicInteger seenLength = new AtomicInteger(-1);
        Mmu mmu = getTestMmu();
        mmu.getIo().register(new Square2RegisterAdapter() {
            public void onLengthCounterUpdated(int newValue) {
                seenLength.set(newValue);
            }
        });
        mmu.setByte(0xff16, 0xc1);
        assertEquals(0x3f, seenLength.get());
    }

    @Test
    public void test_square_2_length_counter_set_to_0x14_when_0xff16_is_0x2c() {
        final AtomicInteger seenLength = new AtomicInteger(-1);
        Mmu mmu = getTestMmu();
        mmu.getIo().register(new Square2RegisterAdapter() {
            public void onLengthCounterUpdated(int newValue) {
                seenLength.set(newValue);
            }
        });
        mmu.setByte(0xff16, 0x2c);
        assertEquals(0x14, seenLength.get());
    }

    @Test
    public void test_square_2_length_counter_set_to_0x14_when_0xff16_is_0xec() {
        final AtomicInteger seenLength = new AtomicInteger(-1);
        Mmu mmu = getTestMmu();
        mmu.getIo().register(new Square2RegisterAdapter() {
            public void onLengthCounterUpdated(int newValue) {
                seenLength.set(newValue);
            }
        });
        mmu.setByte(0xff16, 0xec);
        assertEquals(0x14, seenLength.get());
    }

    @Test
    public void test_square_2_length_counter_set_to_0x01_when_0xff16_is_0x3f() {
        final AtomicInteger seenLength = new AtomicInteger(-1);
        Mmu mmu = getTestMmu();
        mmu.getIo().register(new Square2RegisterAdapter() {
            public void onLengthCounterUpdated(int newValue) {
                seenLength.set(newValue);
            }
        });
        mmu.setByte(0xff16, 0x3f);
        assertEquals(0x01, seenLength.get());
    }

    @Test
    public void test_square_2_length_counter_set_to_0x40_when_0xff16_is_0x40() {
        // The period is contained in the lower 6 bits of NR21.
        // 0x40 is 0b0100_0000 so should be treated the same as 0x00
        // and so should have the same period as if 0xff16 were 0x00.
        final AtomicInteger seenLength = new AtomicInteger(-1);
        Mmu mmu = getTestMmu();
        mmu.getIo().register(new Square2RegisterAdapter() {
            public void onLengthCounterUpdated(int newValue) {
                seenLength.set(newValue);
            }
        });
        mmu.setByte(0xff16, 0x40);
        assertEquals(0x40, seenLength.get());
    }

    @Test
    public void test_square_2_starting_volume_is_0x00_when_0xff17_is_0x00() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff17, 0x00);
        assertEquals(0x00, mmu.getIo().getSquare2StartingVolume());
    }

    @Test
    public void test_square_2_starting_volume_is_0x01_when_0xff17_is_0x10() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff17, 0x10);
        assertEquals(0x01, mmu.getIo().getSquare2StartingVolume());
    }

    @Test
    public void test_square_2_starting_volume_is_0x0e_when_ff17_is_0xe0() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff17, 0xe0);
        assertEquals(0x0e, mmu.getIo().getSquare2StartingVolume());
    }

    @Test
    public void test_square_2_starting_volume_is_0x0f_when_ff17_is_0xff() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff17, 0xff);
        assertEquals(0x0f, mmu.getIo().getSquare2StartingVolume());
    }

    @Test
    public void test_square_2_starting_volume_is_0x00_when_ff17_is_0x0f() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff17, 0x0f);
        assertEquals(0x00, mmu.getIo().getSquare2StartingVolume());
    }

    @Test
    public void test_square_2_envelope_sign() {
        doRangedBitCheckTest(0xff17, 3, (mmu, isBitHigh) -> {
            int expectedSign = isBitHigh ? +1 : -1;
            assertEquals(expectedSign, mmu.getIo().getSquare2EnvelopeSign());
        });
    }

    @Test
    public void test_get_square_2_envelope_step_length_is_0x00_when_0xff17_is_0x00() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff17, 0x00);
        assertEquals(0x00, mmu.getIo().getSquare2EnvelopeStepLength());
    }

    @Test
    public void test_get_square_2_envelope_step_length_is_0x01_when_0xff17_is_0x01() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff17, 0x01);
        assertEquals(0x01, mmu.getIo().getSquare2EnvelopeStepLength());
    }

    @Test
    public void test_get_square_2_envelope_step_length_is_0x01_when_0xff17_is_0xf9() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff17, 0xf9);
        assertEquals(0x01, mmu.getIo().getSquare2EnvelopeStepLength());
    }

    @Test
    public void test_get_square_2_envelope_step_length_is_0x05_when_0xff17() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff17, 0x05);
        assertEquals(0x05, mmu.getIo().getSquare2EnvelopeStepLength());
    }

    @Test
    public void test_get_square_2_envelope_step_length_is_0x07_when_0xff17_is_0x07() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff17, 0x07);
        assertEquals(0x07, mmu.getIo().getSquare2EnvelopeStepLength());
    }

    @Test
    public void test_get_square_2_envelope_step_length_is_0x00_when_0xff17_is_0x08() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff17, 0x08);
        assertEquals(0x00, mmu.getIo().getSquare2EnvelopeStepLength());
    }

    @Test
    public void test_get_square_2_freq_ctr_returns_0x000_when_0xff18_is_0x00_and_0xff19_is_00() {
        doSquare2FrequencyTest(0x00, 0x00, 0x000);
    }

    @Test
    public void test_get_square_2_freq_ctr_returns_0x001_when_0xff18_is_0x01_and_0xff19_is_0x00() {
        doSquare2FrequencyTest(0x01, 0x00, 0x001);
    }

    @Test
    public void test_get_square_2_freq_ctr_returns_0x0ab_when_0xff18_is_0xab_and_0xff19_is_0x00() {
        doSquare2FrequencyTest(0xab, 0x00, 0x0ab);
    }

    @Test
    public void test_get_square_2_freq_ctr_returns_0x0ff_when_0xff18_is_0xff_and_0xff19_is_0x00() {
        doSquare2FrequencyTest(0xff, 0x00, 0x0ff);
    }

    @Test
    public void test_get_square_2_freq_ctr_returns_0x149_when_0xff18_is_0x49_and_0xff19_is_0x01() {
        doSquare2FrequencyTest(0x49, 0x01, 0x149);
    }

    @Test
    public void test_get_square_2_freq_ctr_returns_0x5bf_when_0xff18_is_0xbf_and_0xff19_is_0x05() {
        doSquare2FrequencyTest(0xbf, 0x05, 0x5bf);
    }

    @Test
    public void test_get_square_2_freq_ctr_returns_0x790_when_0xff18_is_0x90_and_0xff19_is_0x07() {
        doSquare2FrequencyTest(0x90, 0x07, 0x790);
    }

    @Test
    public void test_get_square_2_freq_ctr_returns_0x090_when_0xff18_is_0x90_and_0xff19_is_0xf8() {
        doSquare2FrequencyTest(0x90, 0xf8, 0x090);
    }

    @Test
    public void test_get_square_2_freq_ctr_returns_0x790_when_0xff18_is_0x90_and_0xff19_is_0xff() {
        doSquare2FrequencyTest(0x90, 0xff, 0x790);
    }

    @Test
    public void test_get_square_2_freq_ctr_returns_0x000_when_0xff18_is_0x00_and_0xff19_is_0xf8() {
        doSquare2FrequencyTest(0x00, 0xf8, 0x000);
    }

    @Test
    public void test_get_square_2_freq_ctr_returns_0x543_when_0xff18_is_0x43_and_0xff19_is_0x2d() {
        doSquare2FrequencyTest(0x43, 0x2d, 0x543);
    }

    @Test
    public void test_is_square_2_continuous_mode_enabled_is_true_iff_0xff19_bit_6_is_low() {
        doRangedBitCheckTest(0xff19, 6, (mmu, bitIsHigh) -> {
            assertEquals(!bitIsHigh, mmu.getIo().isSquare2ContinuousModeEnabled());
        });
    }

    @Test
    public void test_square_2_trigger() {
        for (int val = 0; val <= 0xff; val++) {
            AtomicBoolean triggered = new AtomicBoolean(false);
            Mmu mmu = getTestMmu();
            mmu.getIo().register(new Square2RegisterAdapter() {
                public void onTrigger() {
                    triggered.set(true);
                }
            });
            mmu.setByte(0xff19, val);
            assertEquals((val & 0x80) > 0, triggered.get());
        }
    }

    @Test
    public void test_clear_square_2_restart_flag() {
        doRangedBitSetTest(0xff19, 7, false, mmu -> mmu.getIo().clearSquare2RestartFlag());
    }

    @Test
    public void test_set_square_2_frequency_counter() {
        Mmu mmu = getTestMmu();
        for (int nr23 = 0x00; nr23 <= 0xff; nr23++) {
            for (int inr24 = 0x00; inr24 <= 0xff; inr24++) {
                for (int newFreq = 0x000; newFreq <= 0x7ff; newFreq++) {
                    mmu.setByte(0xff18, nr23);
                    mmu.setByte(0xff19, inr24);
                    mmu.getIo().setSquare2FrequencyCounter(newFreq);
                    assertEquals("Incorrect value for LSB", newFreq & 0xff, mmu.readByte(0xff18));
                    assertEquals("Incorrect value for MSB", (newFreq & 0x07) >> 8, mmu.readByte(0xff19) & 0x07);
                    assertEquals("Unrelated NR24 bytes changed unexpectedly", inr24 & 0xf8, mmu.readByte(0xff19) & 0xf8);
                }
            }
        }
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_set_square_2_frequency_counter_fails_on_negative_input() {
        getTestMmu().getIo().setSquare2FrequencyCounter(-1);
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_set_square_2_frequency_counter_fails_on_overlarge_value() {
        getTestMmu().getIo().setSquare2FrequencyCounter(0x800);
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_set_square_2_frequency_counter_fails_on_overlarge_value_2() {
        getTestMmu().getIo().setSquare2FrequencyCounter(0x9ab);
    }

    @Test
    public void test_is_wave_dac_enabled_returns_false_if_0xff1a_is_0x00() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff1a, 0x00);
        assertFalse(mmu.getIo().isWaveDacEnabled());
    }

    @Test
    public void test_is_wave_dac_enabled_returns_true_if_0ff1a_is_0x80() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff1a, 0x80);
        assertTrue(mmu.getIo().isWaveDacEnabled());
    }

    @Test
    public void test_is_wave_dac_enabled_returns_false_if_0xff1a_is_0x7f() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff1a, 0x7f);
        assertFalse(mmu.getIo().isWaveDacEnabled());
    }

    @Test
    public void test_is_wave_dac_enabled_returns_true_if_0xff1a_is_0xff() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff1a, 0xff);
        assertTrue(mmu.getIo().isWaveDacEnabled());
    }

    @Test
    public void test_wave_length_counter_set_to_0x100_when_0xff1b_is_0x00() {
        final AtomicInteger seenLength = new AtomicInteger(-1);
        Mmu mmu = getTestMmu();
        mmu.getIo().register(new WaveRegisterAdapter() {
            public void onLengthCounterUpdated(int newValue) {
                seenLength.set(newValue);
            }
        });
        mmu.setByte(0xff1b, 0x00);
        assertEquals(0x100, seenLength.get());
    }

    @Test
    public void test_wave_length_counter_set_to_0xff_when_0xff1b_is_0x01() {
        final AtomicInteger seenLength = new AtomicInteger(-1);
        Mmu mmu = getTestMmu();
        mmu.getIo().register(new WaveRegisterAdapter() {
            public void onLengthCounterUpdated(int newValue) {
                seenLength.set(newValue);
            }
        });
        mmu.setByte(0xff1b, 0x01);
        assertEquals(0xff, seenLength.get());
    }

    @Test
    public void test_wave_length_counter_set_to_0x4c_when_0xff1b_is_0xb4() {
        final AtomicInteger seenLength = new AtomicInteger(-1);
        Mmu mmu = getTestMmu();
        mmu.getIo().register(new WaveRegisterAdapter() {
            public void onLengthCounterUpdated(int newValue) {
                seenLength.set(newValue);
            }
        });
        mmu.setByte(0xff1b, 0xb4);
        assertEquals(0x4c, seenLength.get());
    }

    @Test
    public void test_wave_length_counter_set_to_0x01_when_0xff1b_is_0xff() {
        final AtomicInteger seenLength = new AtomicInteger(-1);
        Mmu mmu = getTestMmu();
        mmu.getIo().register(new WaveRegisterAdapter() {
            public void onLengthCounterUpdated(int newValue) {
                seenLength.set(newValue);
            }
        });
        mmu.setByte(0xff1b, 0xff);
        assertEquals(0x01, seenLength.get());
    }

    @Test
    public void get_wave_volume_percent_returns_0_when_0xff1c_is_0x00() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff1c, 0x00);
        assertEquals(0, mmu.getIo().getWaveVolumePercent());
    }

    @Test
    public void get_wave_volume_percent_returns_100_when_0xff1c_is_0x20() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff1c, 0x20);
        assertEquals(100, mmu.getIo().getWaveVolumePercent());
    }

    @Test
    public void get_wave_volume_percent_returns_0_when_0xff1c_is_0x8f() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff1c, 0x8f);
        assertEquals(0, mmu.getIo().getWaveVolumePercent());
    }

    @Test
    public void get_wave_volume_percent_returns_50_when_0xff1c_is_0x40() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff1c, 0x40);
        assertEquals(50, mmu.getIo().getWaveVolumePercent());
    }

    @Test
    public void get_wave_volume_percent_returns_50_when_0xff1c_is_0xc0() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff1c, 0xc0);
        assertEquals(50, mmu.getIo().getWaveVolumePercent());
    }

    @Test
    public void get_wave_volume_percent_returns_50_when_0xff1c_is_0x4f() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff1c, 0x4f);
        assertEquals(50, mmu.getIo().getWaveVolumePercent());
    }

    @Test
    public void get_wave_volume_percent_returns_50_when_0xff1c_is_0xcf() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff1c, 0xcf);
        assertEquals(50, mmu.getIo().getWaveVolumePercent());
    }

    @Test
    public void get_wave_volume_percent_returns_25_when_0xff1c_is_0x60() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff1c, 0x60);
        assertEquals(25, mmu.getIo().getWaveVolumePercent());
    }

    @Test
    public void test_get_wave_freq_ctr_returns_0x000_when_0xff1d_is_0x00_and_0xff1e_is_00() {
        doWaveFrequencyTest(0x00, 0x00, 0x000);
    }

    @Test
    public void test_get_wave_freq_ctr_returns_0x001_when_0xff1d_is_0x01_and_0xff1e_is_0x00() {
        doWaveFrequencyTest(0x01, 0x00, 0x001);
    }

    @Test
    public void test_get_wave_freq_ctr_returns_0x0ab_when_0xff1d_is_0xab_and_0xff1e_is_0x00() {
        doWaveFrequencyTest(0xab, 0x00, 0x0ab);
    }

    @Test
    public void test_get_wave_freq_ctr_returns_0x0ff_when_0xff1d_is_0xff_and_0xff1e_is_0x00() {
        doWaveFrequencyTest(0xff, 0x00, 0x0ff);
    }

    @Test
    public void test_get_wave_freq_ctr_returns_0x149_when_0xff1d_is_0x49_and_0xff1e_is_0x01() {
        doWaveFrequencyTest(0x49, 0x01, 0x149);
    }

    @Test
    public void test_get_wave_freq_ctr_returns_0x5bf_when_0xff1d_is_0xbf_and_0xff1e_is_0x05() {
        doWaveFrequencyTest(0xbf, 0x05, 0x5bf);
    }

    @Test
    public void test_get_wave_freq_ctr_returns_0x790_when_0xff1d_is_0x90_and_0xff1e_is_0x07() {
        doWaveFrequencyTest(0x90, 0x07, 0x790);
    }

    @Test
    public void test_get_wave_freq_ctr_returns_0x090_when_0xff1d_is_0x90_and_0xff1e_is_0xf8() {
        doWaveFrequencyTest(0x90, 0xf8, 0x090);
    }

    @Test
    public void test_get_wave_freq_ctr_returns_0x790_when_0xff1d_is_0x90_and_0xff1e_is_0xff() {
        doWaveFrequencyTest(0x90, 0xff, 0x790);
    }

    @Test
    public void test_get_wave_freq_ctr_returns_0x000_when_0xff1d_is_0x00_and_0xff1e_is_0xf8() {
        doWaveFrequencyTest(0x00, 0xf8, 0x000);
    }

    @Test
    public void test_get_wave_freq_ctr_returns_0x543_when_0xff1d_is_0x43_and_0xff1e_is_0x2d() {
        doWaveFrequencyTest(0x43, 0x2d, 0x543);
    }

    @Test
    public void test_is_wave_continuous_mode_enabled_is_true_iff_0xff1e_bit_6_is_low() {
        doRangedBitCheckTest(0xff1e, 6, (mmu, bitIsHigh) -> {
            assertEquals(!bitIsHigh, mmu.getIo().isWaveContinuousModeEnabled());
        });
    }

    @Test
    public void test_wave_trigger() {
        for (int val = 0; val <= 0xff; val++) {
            AtomicBoolean triggered = new AtomicBoolean(false);
            Mmu mmu = getTestMmu();
            mmu.getIo().register(new WaveRegisterAdapter() {
                public void onTrigger() {
                    triggered.set(true);
                }
            });
            mmu.setByte(0xff1e, val);
            assertEquals((val & 0x80) > 0, triggered.get());
        }
    }

    @Test
    public void test_clear_wave_restart_flag() {
        doRangedBitSetTest(0xff1e, 7, false, mmu -> mmu.getIo().clearWaveRestartFlag());
    }

    @Test
    public void test_set_wave_frequency_counter() {
        Mmu mmu = getTestMmu();
        for (int nr33 = 0x00; nr33 <= 0xff; nr33++) {
            for (int nr34 = 0x00; nr34 <= 0xff; nr34++) {
                for (int newFreq = 0x000; newFreq <= 0x7ff; newFreq++) {
                    mmu.setByte(0xff1d, nr33);
                    mmu.setByte(0xff1e, nr34);
                    mmu.getIo().setWaveFrequencyCounter(newFreq);
                    assertEquals("Incorrect value for LSB", newFreq & 0xff, mmu.readByte(0xff1d));
                    assertEquals("Incorrect value for MSB", (newFreq & 0x07) >> 8, mmu.readByte(0xff1e) & 0x07);
                    assertEquals("Unrelated NR34 bytes changed unexpectedly", nr34 & 0xf8, mmu.readByte(0xff1e) & 0xf8);
                }
            }
        }
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_set_wave_frequency_counter_fails_on_negative_input() {
        getTestMmu().getIo().setWaveFrequencyCounter(-1);
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_set_wave_frequency_counter_fails_on_overlarge_value() {
        getTestMmu().getIo().setWaveFrequencyCounter(0x800);
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_set_wave_frequency_counter_fails_on_overlarge_value_2() {
        getTestMmu().getIo().setWaveFrequencyCounter(0x9ab);
    }

    @Test
    public void test_noise_length_counter_set_to_0x40_when_0xff20_is_0x00() {
        final AtomicInteger seenLength = new AtomicInteger(-1);
        Mmu mmu = getTestMmu();
        mmu.getIo().register(new NoiseRegisterAdapter() {
            public void onLengthCounterUpdated(int newValue) {
                seenLength.set(newValue);
            }
        });
        mmu.setByte(0xff20, 0x00);
        assertEquals(0x40, seenLength.get());
    }

    @Test
    public void test_noise_length_counter_set_to_0x3f_when_0xff20_is_0x01() {
        final AtomicInteger seenLength = new AtomicInteger(-1);
        Mmu mmu = getTestMmu();
        mmu.getIo().register(new NoiseRegisterAdapter() {
            public void onLengthCounterUpdated(int newValue) {
                seenLength.set(newValue);
            }
        });
        mmu.setByte(0xff20, 0x01);
        assertEquals(0x3f, seenLength.get());
    }

    @Test
    public void test_noise_length_counter_set_to_0x3f_when_0xff20_is_0xc1() {
        final AtomicInteger seenLength = new AtomicInteger(-1);
        Mmu mmu = getTestMmu();
        mmu.getIo().register(new NoiseRegisterAdapter() {
            public void onLengthCounterUpdated(int newValue) {
                seenLength.set(newValue);
            }
        });
        mmu.setByte(0xff20, 0xc1);
        assertEquals(0x3f, seenLength.get());
    }

    @Test
    public void test_noise_length_counter_set_to_0x14_when_0xff20_is_0x2c() {
        final AtomicInteger seenLength = new AtomicInteger(-1);
        Mmu mmu = getTestMmu();
        mmu.getIo().register(new NoiseRegisterAdapter() {
            public void onLengthCounterUpdated(int newValue) {
                seenLength.set(newValue);
            }
        });
        mmu.setByte(0xff20, 0x2c);
        assertEquals(0x14, seenLength.get());
    }

    @Test
    public void test_noise_length_counter_set_to_0x14_when_0xff20_is_0xec() {
        final AtomicInteger seenLength = new AtomicInteger(-1);
        Mmu mmu = getTestMmu();
        mmu.getIo().register(new NoiseRegisterAdapter() {
            public void onLengthCounterUpdated(int newValue) {
                seenLength.set(newValue);
            }
        });
        mmu.setByte(0xff20, 0xec);
        assertEquals(0x14, seenLength.get());
    }

    @Test
    public void test_noise_length_counter_set_to_0x01_when_0xff20_is_0x3f() {
        final AtomicInteger seenLength = new AtomicInteger(-1);
        Mmu mmu = getTestMmu();
        mmu.getIo().register(new NoiseRegisterAdapter() {
            public void onLengthCounterUpdated(int newValue) {
                seenLength.set(newValue);
            }
        });
        mmu.setByte(0xff20, 0x3f);
        assertEquals(0x01, seenLength.get());
    }

    @Test
    public void test_noise_length_counter_set_to_0x40_when_0xff20_is_0x40() {
        // The period is contained in the lower 6 bits of NR41.
        // 0x40 is 0b0100_0000 so should be treated the same as 0x00
        // and so should have the same period as if 0xff20 were 0x00.
        final AtomicInteger seenLength = new AtomicInteger(-1);
        Mmu mmu = getTestMmu();
        mmu.getIo().register(new NoiseRegisterAdapter() {
            public void onLengthCounterUpdated(int newValue) {
                seenLength.set(newValue);
            }
        });
        mmu.setByte(0xff20, 0x40);
        assertEquals(0x40, seenLength.get());
    }

    @Test
    public void test_noise_starting_volume_is_0x00_when_0xff21_is_0x00() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff21, 0x00);
        assertEquals(0x00, mmu.getIo().getNoiseStartingVolume());
    }

    @Test
    public void test_noise_starting_volume_is_0x01_when_0xff21_is_0x10() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff21, 0x10);
        assertEquals(0x01, mmu.getIo().getNoiseStartingVolume());
    }

    @Test
    public void test_noise_starting_volume_is_0x0e_when_ff21_is_0xe0() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff21, 0xe0);
        assertEquals(0x0e, mmu.getIo().getNoiseStartingVolume());
    }

    @Test
    public void test_noise_starting_volume_is_0x0f_when_ff21_is_0xff() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff21, 0xff);
        assertEquals(0x0f, mmu.getIo().getNoiseStartingVolume());
    }

    @Test
    public void test_noise_starting_volume_is_0x00_when_ff21_is_0x0f() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff21, 0x0f);
        assertEquals(0x00, mmu.getIo().getNoiseStartingVolume());
    }

    @Test
    public void test_noise_envelope_sign() {
        doRangedBitCheckTest(0xff21, 3, (mmu, isBitHigh) -> {
            int expectedSign = isBitHigh ? +1 : -1;
            assertEquals(expectedSign, mmu.getIo().getNoiseEnvelopeSign());
        });
    }

    @Test
    public void test_get_noise_envelope_step_length_is_0x00_when_0xff21_is_0x00() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff21, 0x00);
        assertEquals(0x00, mmu.getIo().getNoiseEnvelopeStepLength());
    }

    @Test
    public void test_get_noise_envelope_step_length_is_0x01_when_0xff21_is_0x01() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff21, 0x01);
        assertEquals(0x01, mmu.getIo().getNoiseEnvelopeStepLength());
    }

    @Test
    public void test_get_noise_envelope_step_length_is_0x01_when_0xff21_is_0xf9() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff21, 0xf9);
        assertEquals(0x01, mmu.getIo().getNoiseEnvelopeStepLength());
    }

    @Test
    public void test_get_noise_envelope_step_length_is_0x05_when_0xff21() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff21, 0x05);
        assertEquals(0x05, mmu.getIo().getNoiseEnvelopeStepLength());
    }

    @Test
    public void test_get_noise_envelope_step_length_is_0x07_when_0xff21_is_0x07() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff21, 0x07);
        assertEquals(0x07, mmu.getIo().getNoiseEnvelopeStepLength());
    }

    @Test
    public void test_get_noise_envelope_step_length_is_0x00_when_0xff21_is_0x08() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff21, 0x08);
        assertEquals(0x00, mmu.getIo().getNoiseEnvelopeStepLength());
    }

    @Test
    public void test_get_noise_frequency_shift_is_0x00_when_0xff22_is_0x00() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff22, 0x00);
        assertEquals(0x00, mmu.getIo().getNoiseFrequencyShift());
    }

    @Test
    public void test_get_noise_frequency_shift_is_0x01_when_0xff22_is_0x10() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff22, 0x10);
        assertEquals(0x01, mmu.getIo().getNoiseFrequencyShift());
    }

    @Test
    public void test_get_noise_frequency_shift_is_0x07_when_0xff22_is_0x70() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff22, 0x70);
        assertEquals(0x07, mmu.getIo().getNoiseFrequencyShift());
    }

    @Test
    public void test_get_noise_frequency_shift_is_0x07_when_0xff22_is_0x7f() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff22, 0x70);
        assertEquals(0x07, mmu.getIo().getNoiseFrequencyShift());
    }

    @Test
    public void test_get_noise_frequency_shift_is_0x0f_when_0xff22_is_0xf0() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff22, 0xf0);
        assertEquals(0x0f, mmu.getIo().getNoiseFrequencyShift());
    }

    @Test
    public void test_is_noise_wide_mode_enabled() {
        doRangedBitCheckTest(0xff22, 3, (mmu, isBitHigh) -> {
            assertEquals(!isBitHigh, mmu.getIo().isNoiseWideModeEnabled());
        });
    }

    @Test
    public void test_get_noise_frequency_divisor_returns_a_half_when_0xff22_is_0x00() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff22, 0x00);
        assertEquals(0.5f, mmu.getIo().getNoiseFrequencyDivisor(), 0.01f);
    }

    @Test
    public void test_get_noise_frequency_divisor_returns_1_when_0xff22_is_0x01() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff22, 0x01);
        assertEquals(1, mmu.getIo().getNoiseFrequencyDivisor(), 0.01f);
    }

    @Test
    public void test_get_noise_frequency_divisor_returns_2_when_0xff22_is_0x02() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff22, 0x02);
        assertEquals(2, mmu.getIo().getNoiseFrequencyDivisor(), 0.01f);
    }

    @Test
    public void test_get_noise_frequency_divisor_returns_5_when_0xff22_is_0x05() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff22, 0x05);
        assertEquals(5, mmu.getIo().getNoiseFrequencyDivisor(), 0.01f);
    }

    @Test
    public void test_get_noise_frequency_divisor_returns_7_when_0xff22_is_0x07() {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff22, 0x07);
        assertEquals(7, mmu.getIo().getNoiseFrequencyDivisor(), 0.01f);
    }

    private static void doSquare1FrequencyTest(int nr13, int nr14, int expectedFrequencyCounter) {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff13, nr13);
        mmu.setByte(0xff14, nr14);
        assertEquals(expectedFrequencyCounter, mmu.getIo().getSquare1FrequencyCounter());
    }

    private static void doSquare2FrequencyTest(int nr23, int nr24, int expectedFrequencyCounter) {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff18, nr23);
        mmu.setByte(0xff19, nr24);
        assertEquals(expectedFrequencyCounter, mmu.getIo().getSquare2FrequencyCounter());
    }

    private static void doWaveFrequencyTest(int nr33, int nr34, int expectedFrequencyCounter) {
        Mmu mmu = getTestMmu();
        mmu.setByte(0xff1d, nr33);
        mmu.setByte(0xff1e, nr34);
        assertEquals(expectedFrequencyCounter, mmu.getIo().getWaveFrequencyCounter());
    }

    @Test
    public void test_is_noise_continuous_mode_enabled_is_true_iff_0xff23_bit_6_is_low() {
        doRangedBitCheckTest(0xff23, 6, (mmu, bitIsHigh) -> {
            assertEquals(!bitIsHigh, mmu.getIo().isNoiseContinuousModeEnabled());
        });
    }

    @Test
    public void test_noise_trigger() {
        for (int val = 0; val <= 0xff; val++) {
            AtomicBoolean triggered = new AtomicBoolean(false);
            Mmu mmu = getTestMmu();
            mmu.getIo().register(new NoiseRegisterAdapter() {
                public void onTrigger() {
                    triggered.set(true);
                }
            });
            mmu.setByte(0xff23, val);
            assertEquals((val & 0x80) > 0, triggered.get());
        }
    }

    @Test
    public void test_clear_noise_restart_flag() {
        doRangedBitSetTest(0xff23, 7, false, mmu -> mmu.getIo().clearNoiseRestartFlag());
    }

    @Test
    public void test_square_1_output_mode_change_fires_with_none_if_0xff25_is_0x00() {
        Mmu mmu = getTestMmu();
        AtomicBoolean listenerFired = new AtomicBoolean(false);
        mmu.getIo().register(new Square1RegisterAdapter() {
            @Override
            public void onOutputModeChange(AudioOutputMode newMode) {
                assertEquals(AudioOutputMode.NONE, newMode);
                listenerFired.set(true);
            }
        });
        mmu.setByte(0xff25, 0x00);
        assertTrue("Listener should have fired", listenerFired.get());
    }

    @Test
    public void test_square_1_output_mode_change_fires_with_left_only_if_0xff25_is_0x10() {
        Mmu mmu = getTestMmu();
        AtomicBoolean listenerFired = new AtomicBoolean(false);
        mmu.getIo().register(new Square1RegisterAdapter() {
            @Override
            public void onOutputModeChange(AudioOutputMode newMode) {
                assertEquals(AudioOutputMode.LEFT_ONLY, newMode);
                listenerFired.set(true);
            }
        });
        mmu.setByte(0xff25, 0x10);
        assertTrue("Listener should have fired", listenerFired.get());
    }

    @Test
    public void test_square_1_output_mode_change_fires_with_right_only_if_0xff25_is_0x01() {
        Mmu mmu = getTestMmu();
        AtomicBoolean listenerFired = new AtomicBoolean(false);
        mmu.getIo().register(new Square1RegisterAdapter() {
            @Override
            public void onOutputModeChange(AudioOutputMode newMode) {
                assertEquals(AudioOutputMode.RIGHT_ONLY, newMode);
                listenerFired.set(true);
            }
        });
        mmu.setByte(0xff25, 0x01);
        assertTrue("Listener should have fired", listenerFired.get());
    }

    @Test
    public void test_square_1_output_mode_change_fires_with_right_only_if_0xff25_is_0xe1() {
        Mmu mmu = getTestMmu();
        AtomicBoolean listenerFired = new AtomicBoolean(false);
        mmu.getIo().register(new Square1RegisterAdapter() {
            @Override
            public void onOutputModeChange(AudioOutputMode newMode) {
                assertEquals(AudioOutputMode.RIGHT_ONLY, newMode);
                listenerFired.set(true);
            }
        });
        mmu.setByte(0xff25, 0xe1);
        assertTrue("Listener should have fired", listenerFired.get());
    }

    @Test
    public void test_square_1_output_mode_change_fires_with_right_only_if_0xff25_is_0x0f() {
        Mmu mmu = getTestMmu();
        AtomicBoolean listenerFired = new AtomicBoolean(false);
        mmu.getIo().register(new Square1RegisterAdapter() {
            @Override
            public void onOutputModeChange(AudioOutputMode newMode) {
                assertEquals(AudioOutputMode.RIGHT_ONLY, newMode);
                listenerFired.set(true);
            }
        });
        mmu.setByte(0xff25, 0x0f);
        assertTrue("Listener should have fired", listenerFired.get());
    }

    @Test
    public void test_square_1_output_mode_change_fires_with_right_only_if_0xff25_is_0xef() {
        Mmu mmu = getTestMmu();
        AtomicBoolean listenerFired = new AtomicBoolean(false);
        mmu.getIo().register(new Square1RegisterAdapter() {
            @Override
            public void onOutputModeChange(AudioOutputMode newMode) {
                assertEquals(AudioOutputMode.RIGHT_ONLY, newMode);
                listenerFired.set(true);
            }
        });
        mmu.setByte(0xff25, 0xef);
        assertTrue("Listener should have fired", listenerFired.get());
    }

    @Test
    public void test_square_1_output_mode_change_fires_with_left_only_if_0xff25_is_0xfe() {
        Mmu mmu = getTestMmu();
        AtomicBoolean listenerFired = new AtomicBoolean(false);
        mmu.getIo().register(new Square1RegisterAdapter() {
            @Override
            public void onOutputModeChange(AudioOutputMode newMode) {
                assertEquals(AudioOutputMode.LEFT_ONLY, newMode);
                listenerFired.set(true);
            }
        });
        mmu.setByte(0xff25, 0xfe);
        assertTrue("Listener should have fired", listenerFired.get());
    }

    @Test
    public void test_square_1_output_mode_change_fires_with_stereo_if_0xff25_is_0x11() {
        Mmu mmu = getTestMmu();
        AtomicBoolean listenerFired = new AtomicBoolean(false);
        mmu.getIo().register(new Square1RegisterAdapter() {
            @Override
            public void onOutputModeChange(AudioOutputMode newMode) {
                assertEquals(AudioOutputMode.STEREO, newMode);
                listenerFired.set(true);
            }
        });
        mmu.setByte(0xff25, 0x11);
        assertTrue("Listener should have fired", listenerFired.get());
    }

    @Test
    public void test_square_1_output_mode_change_fires_with_stereo_if_0xff25_is_0xff() {
        Mmu mmu = getTestMmu();
        AtomicBoolean listenerFired = new AtomicBoolean(false);
        mmu.getIo().register(new Square1RegisterAdapter() {
            @Override
            public void onOutputModeChange(AudioOutputMode newMode) {
                assertEquals(AudioOutputMode.STEREO, newMode);
                listenerFired.set(true);
            }
        });
        mmu.setByte(0xff25, 0xff);
        assertTrue("Listener should have fired", listenerFired.get());
    }

    @Test
    public void test_square_2_output_mode_change_fires_with_none_if_0xff25_is_0x00() {
        Mmu mmu = getTestMmu();
        AtomicBoolean listenerFired = new AtomicBoolean(false);
        mmu.getIo().register(new Square2RegisterAdapter() {
            @Override
            public void onOutputModeChange(AudioOutputMode newMode) {
                assertEquals(AudioOutputMode.NONE, newMode);
                listenerFired.set(true);
            }
        });
        mmu.setByte(0xff25, 0x00);
        assertTrue("Listener should have fired", listenerFired.get());
    }

    @Test
    public void test_square_2_output_mode_change_fires_with_right_only_if_0xff25_is_0x02() {
        Mmu mmu = getTestMmu();
        AtomicBoolean listenerFired = new AtomicBoolean(false);
        mmu.getIo().register(new Square2RegisterAdapter() {
            @Override
            public void onOutputModeChange(AudioOutputMode newMode) {
                assertEquals(AudioOutputMode.RIGHT_ONLY, newMode);
                listenerFired.set(true);
            }
        });
        mmu.setByte(0xff25, 0x02);
        assertTrue("Listener should have fired", listenerFired.get());
    }

    @Test
    public void test_square_2_output_mode_change_fires_with_right_only_if_0xff25_is_0xd2() {
        Mmu mmu = getTestMmu();
        AtomicBoolean listenerFired = new AtomicBoolean(false);
        mmu.getIo().register(new Square2RegisterAdapter() {
            @Override
            public void onOutputModeChange(AudioOutputMode newMode) {
                assertEquals(AudioOutputMode.RIGHT_ONLY, newMode);
                listenerFired.set(true);
            }
        });
        mmu.setByte(0xff25, 0xd2);
        assertTrue("Listener should have fired", listenerFired.get());
    }

    @Test
    public void test_square_2_output_mode_change_fires_with_right_only_if_0xff25_is_0x0f() {
        Mmu mmu = getTestMmu();
        AtomicBoolean listenerFired = new AtomicBoolean(false);
        mmu.getIo().register(new Square2RegisterAdapter() {
            @Override
            public void onOutputModeChange(AudioOutputMode newMode) {
                assertEquals(AudioOutputMode.RIGHT_ONLY, newMode);
                listenerFired.set(true);
            }
        });
        mmu.setByte(0xff25, 0x0f);
        assertTrue("Listener should have fired", listenerFired.get());
    }

    @Test
    public void test_square_2_output_mode_change_fires_with_right_only_if_0xff25_is_0xdf() {
        Mmu mmu = getTestMmu();
        AtomicBoolean listenerFired = new AtomicBoolean(false);
        mmu.getIo().register(new Square2RegisterAdapter() {
            @Override
            public void onOutputModeChange(AudioOutputMode newMode) {
                assertEquals(AudioOutputMode.RIGHT_ONLY, newMode);
                listenerFired.set(true);
            }
        });
        mmu.setByte(0xff25, 0xdf);
        assertTrue("Listener should have fired", listenerFired.get());
    }

    @Test
    public void test_square_2_output_mode_change_fires_with_left_only_if_0xff25_is_0xfd() {
        Mmu mmu = getTestMmu();
        AtomicBoolean listenerFired = new AtomicBoolean(false);
        mmu.getIo().register(new Square2RegisterAdapter() {
            @Override
            public void onOutputModeChange(AudioOutputMode newMode) {
                assertEquals(AudioOutputMode.LEFT_ONLY, newMode);
                listenerFired.set(true);
            }
        });
        mmu.setByte(0xff25, 0xfd);
        assertTrue("Listener should have fired", listenerFired.get());
    }

    @Test
    public void test_square_2_output_mode_change_fires_with_stereo_if_0xff25_is_0x22() {
        Mmu mmu = getTestMmu();
        AtomicBoolean listenerFired = new AtomicBoolean(false);
        mmu.getIo().register(new Square2RegisterAdapter() {
            @Override
            public void onOutputModeChange(AudioOutputMode newMode) {
                assertEquals(AudioOutputMode.STEREO, newMode);
                listenerFired.set(true);
            }
        });
        mmu.setByte(0xff25, 0x22);
        assertTrue("Listener should have fired", listenerFired.get());
    }

    @Test
    public void test_square_2_output_mode_change_fires_with_stereo_if_0xff25_is_0xff() {
        Mmu mmu = getTestMmu();
        AtomicBoolean listenerFired = new AtomicBoolean(false);
        mmu.getIo().register(new Square2RegisterAdapter() {
            @Override
            public void onOutputModeChange(AudioOutputMode newMode) {
                assertEquals(AudioOutputMode.STEREO, newMode);
                listenerFired.set(true);
            }
        });
        mmu.setByte(0xff25, 0xff);
        assertTrue("Listener should have fired", listenerFired.get());
    }

    @Test
    public void test_wave_output_mode_change_fires_with_none_if_0xff25_is_0x00() {
        Mmu mmu = getTestMmu();
        AtomicBoolean listenerFired = new AtomicBoolean(false);
        mmu.getIo().register(new WaveRegisterAdapter() {
            @Override
            public void onOutputModeChange(AudioOutputMode newMode) {
                assertEquals(AudioOutputMode.NONE, newMode);
                listenerFired.set(true);
            }
        });
        mmu.setByte(0xff25, 0x00);
        assertTrue("Listener should have fired", listenerFired.get());
    }

    @Test
    public void test_wave_output_mode_change_fires_with_left_only_if_0xff25_is_0x40() {
        Mmu mmu = getTestMmu();
        AtomicBoolean listenerFired = new AtomicBoolean(false);
        mmu.getIo().register(new WaveRegisterAdapter() {
            @Override
            public void onOutputModeChange(AudioOutputMode newMode) {
                assertEquals(AudioOutputMode.LEFT_ONLY, newMode);
                listenerFired.set(true);
            }
        });
        mmu.setByte(0xff25, 0x40);
        assertTrue("Listener should have fired", listenerFired.get());
    }

    @Test
    public void test_wave_output_mode_change_fires_with_right_only_if_0xff25_is_0x04() {
        Mmu mmu = getTestMmu();
        AtomicBoolean listenerFired = new AtomicBoolean(false);
        mmu.getIo().register(new WaveRegisterAdapter() {
            @Override
            public void onOutputModeChange(AudioOutputMode newMode) {
                assertEquals(AudioOutputMode.RIGHT_ONLY, newMode);
                listenerFired.set(true);
            }
        });
        mmu.setByte(0xff25, 0x04);
        assertTrue("Listener should have fired", listenerFired.get());
    }

    @Test
    public void test_wave_output_mode_change_fires_with_right_only_if_0xff25_is_0xb4() {
        Mmu mmu = getTestMmu();
        AtomicBoolean listenerFired = new AtomicBoolean(false);
        mmu.getIo().register(new WaveRegisterAdapter() {
            @Override
            public void onOutputModeChange(AudioOutputMode newMode) {
                assertEquals(AudioOutputMode.RIGHT_ONLY, newMode);
                listenerFired.set(true);
            }
        });
        mmu.setByte(0xff25, 0xb4);
        assertTrue("Listener should have fired", listenerFired.get());
    }

    @Test
    public void test_wave_output_mode_change_fires_with_right_only_if_0xff25_is_0x0f() {
        Mmu mmu = getTestMmu();
        AtomicBoolean listenerFired = new AtomicBoolean(false);
        mmu.getIo().register(new WaveRegisterAdapter() {
            @Override
            public void onOutputModeChange(AudioOutputMode newMode) {
                assertEquals(AudioOutputMode.RIGHT_ONLY, newMode);
                listenerFired.set(true);
            }
        });
        mmu.setByte(0xff25, 0x0f);
        assertTrue("Listener should have fired", listenerFired.get());
    }

    @Test
    public void test_wave_output_mode_change_fires_with_right_only_if_0xff25_is_0xbf() {
        Mmu mmu = getTestMmu();
        AtomicBoolean listenerFired = new AtomicBoolean(false);
        mmu.getIo().register(new WaveRegisterAdapter() {
            @Override
            public void onOutputModeChange(AudioOutputMode newMode) {
                assertEquals(AudioOutputMode.RIGHT_ONLY, newMode);
                listenerFired.set(true);
            }
        });
        mmu.setByte(0xff25, 0xbf);
        assertTrue("Listener should have fired", listenerFired.get());
    }

    @Test
    public void test_wave_output_mode_change_fires_with_left_only_if_0xff25_is_0xfb() {
        Mmu mmu = getTestMmu();
        AtomicBoolean listenerFired = new AtomicBoolean(false);
        mmu.getIo().register(new WaveRegisterAdapter() {
            @Override
            public void onOutputModeChange(AudioOutputMode newMode) {
                assertEquals(AudioOutputMode.LEFT_ONLY, newMode);
                listenerFired.set(true);
            }
        });
        mmu.setByte(0xff25, 0xfb);
        assertTrue("Listener should have fired", listenerFired.get());
    }

    @Test
    public void test_wave_output_mode_change_fires_with_stereo_only_if_0xff25_is_0x44() {
        Mmu mmu = getTestMmu();
        AtomicBoolean listenerFired = new AtomicBoolean(false);
        mmu.getIo().register(new WaveRegisterAdapter() {
            @Override
            public void onOutputModeChange(AudioOutputMode newMode) {
                assertEquals(AudioOutputMode.STEREO, newMode);
                listenerFired.set(true);
            }
        });
        mmu.setByte(0xff25, 0x44);
        assertTrue("Listener should have fired", listenerFired.get());
    }

    @Test
    public void test_wave_output_mode_change_fires_with_stereo_if_0xff25_is_0xfb() {
        Mmu mmu = getTestMmu();
        AtomicBoolean listenerFired = new AtomicBoolean(false);
        mmu.getIo().register(new WaveRegisterAdapter() {
            @Override
            public void onOutputModeChange(AudioOutputMode newMode) {
                assertEquals(AudioOutputMode.STEREO, newMode);
                listenerFired.set(true);
            }
        });
        mmu.setByte(0xff25, 0xff);
        assertTrue("Listener should have fired", listenerFired.get());
    }

    @Test
    public void test_noise_output_mode_change_fires_with_none_if_0xff25_is_0x00() {
        Mmu mmu = getTestMmu();
        AtomicBoolean listenerFired = new AtomicBoolean(false);
        mmu.getIo().register(new NoiseRegisterAdapter() {
            @Override
            public void onOutputModeChange(AudioOutputMode newMode) {
                assertEquals(AudioOutputMode.NONE, newMode);
                listenerFired.set(true);
            }
        });
        mmu.setByte(0xff25, 0x00);
        assertTrue("Listener should have fired", listenerFired.get());
    }

    @Test
    public void test_noise_output_mode_change_fires_with_left_only_if_0xff25_is_0x80() {
        Mmu mmu = getTestMmu();
        AtomicBoolean listenerFired = new AtomicBoolean(false);
        mmu.getIo().register(new NoiseRegisterAdapter() {
            @Override
            public void onOutputModeChange(AudioOutputMode newMode) {
                assertEquals(AudioOutputMode.LEFT_ONLY, newMode);
                listenerFired.set(true);
            }
        });
        mmu.setByte(0xff25, 0x80);
        assertTrue("Listener should have fired", listenerFired.get());
    }

    @Test
    public void test_noise_output_mode_change_fires_with_right_only_if_0xff25_is_0x08() {
        Mmu mmu = getTestMmu();
        AtomicBoolean listenerFired = new AtomicBoolean(false);
        mmu.getIo().register(new NoiseRegisterAdapter() {
            @Override
            public void onOutputModeChange(AudioOutputMode newMode) {
                assertEquals(AudioOutputMode.RIGHT_ONLY, newMode);
                listenerFired.set(true);
            }
        });
        mmu.setByte(0xff25, 0x08);
        assertTrue("Listener should have fired", listenerFired.get());
    }

    @Test
    public void test_noise_output_mode_change_fires_with_right_only_if_0xff25_is_0x78() {
        Mmu mmu = getTestMmu();
        AtomicBoolean listenerFired = new AtomicBoolean(false);
        mmu.getIo().register(new NoiseRegisterAdapter() {
            @Override
            public void onOutputModeChange(AudioOutputMode newMode) {
                assertEquals(AudioOutputMode.RIGHT_ONLY, newMode);
                listenerFired.set(true);
            }
        });
        mmu.setByte(0xff25, 0x78);
        assertTrue("Listener should have fired", listenerFired.get());
    }

    @Test
    public void test_noise_output_mode_change_fires_with_right_only_if_0xff25_is_0x0f() {
        Mmu mmu = getTestMmu();
        AtomicBoolean listenerFired = new AtomicBoolean(false);
        mmu.getIo().register(new NoiseRegisterAdapter() {
            @Override
            public void onOutputModeChange(AudioOutputMode newMode) {
                assertEquals(AudioOutputMode.RIGHT_ONLY, newMode);
                listenerFired.set(true);
            }
        });
        mmu.setByte(0xff25, 0x0f);
        assertTrue("Listener should have fired", listenerFired.get());
    }

    @Test
    public void test_noise_output_mode_change_fires_with_right_only_if_0xff25_is_0x7f() {
        Mmu mmu = getTestMmu();
        AtomicBoolean listenerFired = new AtomicBoolean(false);
        mmu.getIo().register(new NoiseRegisterAdapter() {
            @Override
            public void onOutputModeChange(AudioOutputMode newMode) {
                assertEquals(AudioOutputMode.RIGHT_ONLY, newMode);
                listenerFired.set(true);
            }
        });
        mmu.setByte(0xff25, 0x7f);
        assertTrue("Listener should have fired", listenerFired.get());
    }

    @Test
    public void test_noise_output_mode_change_fires_with_left_only_if_0xff25_is_0xf7() {
        Mmu mmu = getTestMmu();
        AtomicBoolean listenerFired = new AtomicBoolean(false);
        mmu.getIo().register(new NoiseRegisterAdapter() {
            @Override
            public void onOutputModeChange(AudioOutputMode newMode) {
                assertEquals(AudioOutputMode.LEFT_ONLY, newMode);
                listenerFired.set(true);
            }
        });
        mmu.setByte(0xff25, 0xf7);
        assertTrue("Listener should have fired", listenerFired.get());
    }

    @Test
    public void test_noise_output_mode_change_fires_with_stereo_if_0xff25_is_0x88() {
        Mmu mmu = getTestMmu();
        AtomicBoolean listenerFired = new AtomicBoolean(false);
        mmu.getIo().register(new NoiseRegisterAdapter() {
            @Override
            public void onOutputModeChange(AudioOutputMode newMode) {
                assertEquals(AudioOutputMode.STEREO, newMode);
                listenerFired.set(true);
            }
        });
        mmu.setByte(0xff25, 0x88);
        assertTrue("Listener should have fired", listenerFired.get());
    }

    @Test
    public void test_noise_output_mode_change_fires_with_stereo_if_0xff25_is_0xff() {
        Mmu mmu = getTestMmu();
        AtomicBoolean listenerFired = new AtomicBoolean(false);
        mmu.getIo().register(new NoiseRegisterAdapter() {
            @Override
            public void onOutputModeChange(AudioOutputMode newMode) {
                assertEquals(AudioOutputMode.STEREO, newMode);
                listenerFired.set(true);
            }
        });
        mmu.setByte(0xff25, 0xff);
        assertTrue("Listener should have fired", listenerFired.get());
    }

    @Test
    public void test_master_sound_enable_listener() {
        for (int val = 0x00; val <= 0xff; val++) {
            Mmu mmu = getTestMmu();
            boolean expectedEnabled = (val & 0x80) > 0;
            AtomicBoolean wasListenerFired = new AtomicBoolean(false);
            MasterAudioListener listener = new MasterAudioListener() {
                @Override
                public void onAudioEnableChanged(boolean newValue) {
                     wasListenerFired.set(true);
                     assertEquals(expectedEnabled, newValue);
                }
            };

            mmu.getIo().register(listener);
            mmu.setByte(0xff26, val);
            assertTrue("Listener should have fired", wasListenerFired.get());
        }
    }

    @Test
    public void test_set_square_1_is_playing_flag_to_false() {
        doRangedBitSetTest(0xff26, 0, false, mmu -> mmu.getIo().setSquare1PlayingFlag(false));
    }

    @Test
    public void test_set_square_1_is_playing_flag_to_true() {
        doRangedBitSetTest(0xff26, 0, true, mmu -> mmu.getIo().setSquare1PlayingFlag(true));
    }

    @Test
    public void test_set_square_2_is_playing_flag_to_false() {
        doRangedBitSetTest(0xff26, 1, false, mmu -> mmu.getIo().setSquare2PlayingFlag(false));
    }

    @Test
    public void test_set_square_2_is_playing_flag_to_true() {
        doRangedBitSetTest(0xff26, 1, true, mmu -> mmu.getIo().setSquare2PlayingFlag(true));
    }

    @Test
    public void test_set_wave_is_playing_flag_to_false() {
        doRangedBitSetTest(0xff26, 2, false, mmu -> mmu.getIo().setWavePlayingFlag(false));
    }

    @Test
    public void test_set_wave_is_playing_flag_to_true() {
        doRangedBitSetTest(0xff26, 2, true, mmu -> mmu.getIo().setWavePlayingFlag(true));
    }

    @Test
    public void test_wave_load_samples() {
        Mmu mmu = getTestMmu();
        int[] range = {0x12, 0x34, 0x56, 0x78, 0x9a, 0xbc, 0xde, 0xf0, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88};
        short[] expected = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f,
                0x00, 0x01, 0x01, 0x02, 0x02, 0x03, 0x03, 0x04, 0x04, 0x05, 0x05, 0x06, 0x06, 0x07, 0x07, 0x08, 0x08};

        for (int idx = 0; idx < 16; idx++) {
            mmu.setByte(0xff30 + idx, range[idx]);
        }

        assertArrayEquals(expected, mmu.getIo().getWaveData());
    }

    @Test
    public void test_set_noise_is_playing_flag_to_false() {
        doRangedBitSetTest(0xff26, 3, false, mmu -> mmu.getIo().setNoisePlayingFlag(false));
    }

    @Test
    public void test_set_noise_is_playing_flag_to_true() {
        doRangedBitSetTest(0xff26, 3, true, mmu -> mmu.getIo().setNoisePlayingFlag(true));
    }

    private static void doRangeTest(int address, Consumer<Mmu> test) {
        for (int value = 0x00; value < 0xff; value++) {
            Mmu mmu = getTestMmu();
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

    private static class TestHeightListener extends SpriteChangeAdapter {
        Boolean tallSpritesEnabledOnLastCall = null;
        @Override
        public void onSpriteHeightChanged(boolean areTallSpritesEnabled) {
            tallSpritesEnabledOnLastCall = areTallSpritesEnabled;
        }
    }

    private static Mmu getTestMmu() {
        Mmu mmu = Mmu.build(false);
        mmu.setBiosEnabled(false);

        // Cartridge ROM usually isn't writeable.
        // For simplicity of testing, replace it with RAM banks.
        mmu.loadCartridge(new RamBackedTestCartridge());

        return mmu;
    }
}
