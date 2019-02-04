package com.kopieczek.gamble.hardware.memory.cartridge;

import com.kopieczek.gamble.hardware.memory.*;
import org.junit.Test;
import sun.misc.IOUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class TestMbcType3Cartridge {
    private static final int BYTES_IN_MB = 1048576;
    private static final int BYTES_IN_KB = 1024;
    private static final int CARTRIDGE_SIZE = 2 * BYTES_IN_MB + 64 * BYTES_IN_KB;
    private static final long SEED = 1234L;

    private final Random random = new Random(SEED);
    private final int[] cartridge1 = buildTestData(random);

    private static Mmu getMmuForCartridge(Cartridge cartridge) {
        Mmu mmu = Mmu.build(true);
        mmu.setBiosEnabled(false);
        mmu.loadCartridge(cartridge);
        return mmu;
    }

    @Test
    public void test_rom_0_is_never_switched() {
        MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
        Mmu mmu = getMmuForCartridge(cartridge);
        long rom0Sig = getDigest(cartridge.getRom0());
        IntStream.range(0x4000, 0xffff).forEach(addr -> mmu.setByte(addr, 0x00));
        assertEquals(rom0Sig, getDigest(cartridge.getRom0()));
        IntStream.range(0x4000, 0xffff).forEach(addr -> mmu.setByte(addr, 0xff));
        assertEquals(rom0Sig, getDigest(cartridge.getRom0()));
        IntStream.range(0x4000, 0xffff).forEach(addr -> mmu.setByte(addr, 0xc7));
        assertEquals(rom0Sig, getDigest(cartridge.getRom0()));
    }

    @Test
    public void test_switch_rom_from_bank_1_to_bank_2_via_0x2000() {
        MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
        Mmu mmu = getMmuForCartridge(cartridge);
        mmu.setByte(0x2000, 0x01);
        long rom1Sig = getDigest(cartridge.getRom1());
        mmu.setByte(0x2000, 0x02);
        assertNotEquals(rom1Sig, getDigest(cartridge.getRom1()));
    }

    @Test
    public void test_switch_rom_from_bank_1_to_bank_2_and_back_again() {
        MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
        Mmu mmu = getMmuForCartridge(cartridge);
        mmu.setByte(0x2000, 0x01);
        long rom1Sig = getDigest(cartridge.getRom1());
        mmu.setByte(0x2000, 0x02);
        mmu.setByte(0x2000, 0x01);
        assertEquals(rom1Sig, getDigest(cartridge.getRom1()));
    }

    @Test
    public void test_request_rom_bank_1_when_already_on_bank_1() {
        MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
        Mmu mmu = getMmuForCartridge(cartridge);
        mmu.setByte(0x2000, 0x01);
        long rom1Sig = getDigest(cartridge.getRom1());
        mmu.setByte(0x2000, 0x01);
        assertEquals("Rom should have stayed on bank 1", rom1Sig, getDigest(cartridge.getRom1()));
    }

    @Test
    public void test_initial_rom_bank_is_bank_1() {
        MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
        Mmu mmu = getMmuForCartridge(cartridge);
        long initialBankSig = getDigest(cartridge.getRom1());
        mmu.setByte(0x2000, 0x01);
        long rom1Sig = getDigest(cartridge.getRom1());
        assertEquals(rom1Sig, initialBankSig);
    }

    @Test
    public void test_all_extension_rom_banks_are_different() {
        final int totalNumberOfBanks = 127;
        MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
        Mmu mmu = getMmuForCartridge(cartridge);
        Set<Long> bankSignaturesSeen = new HashSet<>();
        for (int bank = 1; bank <= totalNumberOfBanks; bank++) {
            mmu.setByte(0x2000, bank);
            bankSignaturesSeen.add(getDigest(cartridge.getRom1()));
        }
        assertEquals(totalNumberOfBanks, bankSignaturesSeen.size());
    }

    @Test
    public void test_high_bit_on_rom_request_is_ignored() {
        final int totalNumberOfBanks = 127;
        MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
        Mmu mmu = getMmuForCartridge(cartridge);
        for (int bank = 1; bank <= totalNumberOfBanks; bank++) {
            int highBank = bank | 0x80;
            mmu.setByte(0x2000, bank);
            long lowBitSig = getDigest(cartridge.getRom1());
            mmu.setByte(0x2000, highBank);
            long highBitSig = getDigest(cartridge.getRom1());
            String msg = String.format("Expected equal bytes when requesting banks %d and %d", bank, highBank);
            assertEquals(msg, lowBitSig, highBitSig);
        }
    }

    @Test
    public void test_rom_bank_1_is_cartridge_bytes_0x4000_to_0x8000() {
        MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
        Mmu mmu = getMmuForCartridge(cartridge);
        mmu.setByte(0x2000, 0x01);
        int[] expected = Arrays.copyOfRange(cartridge1, 0x4000, 0x8000);
        assertArrayEquals(expected, getBytes(cartridge.getRom1()));
    }

    @Test
    public void test_rom_bank_64_is_cartridge_bytes_0x100000_to_0x104000() {
        MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
        Mmu mmu = getMmuForCartridge(cartridge);
        mmu.setByte(0x2000, 64);
        int[] expected = Arrays.copyOfRange(cartridge1, 0x100000, 0x104000);
        assertArrayEquals(expected, getBytes(cartridge.getRom1()));
    }

    @Test
    public void test_rom_bank_127_is_cartridge_bytes_0x1fc000_to_0x200000() {
        MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
        Mmu mmu = getMmuForCartridge(cartridge);
        mmu.setByte(0x2000, 127);
        int[] expected = Arrays.copyOfRange(cartridge1, 0x1fc000, 0x200000);
        assertArrayEquals(expected, getBytes(cartridge.getRom1()));
    }

    @Test
    public void test_requesting_rom_bank_0_in_extension_ram_yields_bank_1() {
        MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
        Mmu mmu = getMmuForCartridge(cartridge);
        mmu.setByte(0x2000, 1);
        long bank1Sig = getDigest(cartridge.getRom1());
        mmu.setByte(0x2000, 0);
        assertEquals(bank1Sig, getDigest(cartridge.getRom1()));
    }

    @Test
    public void test_requesting_rom_bank_128_in_extension_ram_yields_bank_1() {
        MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
        Mmu mmu = getMmuForCartridge(cartridge);
        mmu.setByte(0x2000, 1);
        long bank1Sig = getDigest(cartridge.getRom1());
        mmu.setByte(0x2000, 128);
        assertEquals(bank1Sig, getDigest(cartridge.getRom1()));
    }

    @Test
    public void test_any_address_in_0x2000_to_0x4000_can_request_rom_bank_2() {
        long rom2Sig = getDigest(new RomModule(Arrays.copyOfRange(cartridge1, 0x8000, 0xc000)));
        for (int addr = 0x2000; addr < 0x4000; addr++) {
            MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
            Mmu mmu = getMmuForCartridge(cartridge);
            mmu.setByte(addr, 2);
            String msg = String.format("Write to %x failed to update rom bank", addr);
            assertEquals(msg, rom2Sig, getDigest(cartridge.getRom1()));
        }
    }

    @Test
    public void test_any_address_in_0x2000_to_0x4000_can_request_rom_bank_84() {
        long rom84Sig = getDigest(new RomModule(Arrays.copyOfRange(cartridge1, 0x150000, 0x154000)));
        for (int addr = 0x2000; addr < 0x4000; addr++) {
            MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
            Mmu mmu = getMmuForCartridge(cartridge);
            mmu.setByte(addr, 84);
            String msg = String.format("Write to %x failed to update rom bank", addr);
            assertEquals(msg, rom84Sig, getDigest(cartridge.getRom1()));
        }
    }

    @Test
    public void test_writing_to_0x1fff_does_not_modify_rom_bank() {
        MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
        Mmu mmu = getMmuForCartridge(cartridge);
        mmu.setByte(0x2000, 120);
        long currentRom = getDigest(cartridge.getRom1());
        mmu.setByte(0x1fff, 1);
        assertEquals("ROM bank should not have changed", currentRom, getDigest(cartridge.getRom1()));
    }

    @Test
    public void test_writing_to_0x4000_does_not_modify_rom_bank() {
        MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
        Mmu mmu = getMmuForCartridge(cartridge);
        mmu.setByte(0x2000, 120);
        long currentRom = getDigest(cartridge.getRom1());
        mmu.setByte(0x4000, 1);
        assertEquals("ROM bank should not have changed", currentRom, getDigest(cartridge.getRom1()));
    }

    @Test
    public void test_rom_0_is_the_first_0x4000_bytes_of_the_cartridge() {
        MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
        int[] expected = Arrays.copyOfRange(cartridge1, 0x0000, 0x4000);
        assertArrayEquals(expected, getBytes(cartridge.getRom0()));
    }

    @Test
    public void test_rom_0_never_banks() {
        MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
        Mmu mmu = getMmuForCartridge(cartridge);
        int[] expected = Arrays.copyOfRange(cartridge1, 0x0000, 0x4000);
        for (int addr = 0x0000; addr <= 0xffff; addr++) {
            mmu.setByte(addr, 0x00);
            assertArrayEquals(expected, getBytes(cartridge.getRom0()));
            mmu.setByte(addr, 0x46);
            assertArrayEquals(expected, getBytes(cartridge.getRom0()));
            mmu.setByte(addr, 0xff);
            assertArrayEquals(expected, getBytes(cartridge.getRom0()));
        }
    }

    @Test
    public void test_rom_0_is_not_writeable() {
        final int addr = 0x1234;
        MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
        Mmu mmu = getMmuForCartridge(cartridge);
        int datum = mmu.readByte(addr);
        mmu.setByte(addr, (datum + 1) % 256);
        assertEquals(datum, mmu.readByte(addr));
    }

    @Test
    public void test_rom_1_is_not_writeable() {
        final int addr = 0x4567;
        MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
        Mmu mmu = getMmuForCartridge(cartridge);
        int datum = mmu.readByte(addr);
        mmu.setByte(addr, (datum + 1) % 256);
        assertEquals(datum, mmu.readByte(addr));
    }

    @Test
    public void test_ram_0_is_writeable_when_enabled() {
        MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
        Mmu mmu = getMmuForCartridge(cartridge);
        mmu.setByte(0x0000, 0x0a); // Enable RAM
        mmu.setByte(0x4000, 0x00); // Select RAM bank 0
        for (int addr = 0xa000; addr < 0xc000; addr++) {
            mmu.setByte(addr, addr % 256);
        }
        for (int addr = 0xa000; addr < 0xc000; addr++) {
            int datum = mmu.readByte(addr);
            int expected = addr % 256;
            assertEquals("Unexpected byte at addr 0x" + Integer.toHexString(addr), expected, datum);
        }
    }

    @Test
    public void test_ram_0_can_be_overwritten() {
        MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
        Mmu mmu = getMmuForCartridge(cartridge);
        mmu.setByte(0x0000, 0x0a); // Enable RAM
        mmu.setByte(0x4000, 0x00); // Select RAM bank 0
        randomize(cartridge.getRam());
        long initialSig = getDigest(cartridge.getRam());
        randomize(cartridge.getRam());
        long newSig = getDigest(cartridge.getRam());
        assertNotEquals("RAM contents should have been modified", initialSig, newSig);
    }

    @Test
    public void test_ram_0_cannot_be_written_to_when_disabled() {
        MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
        Mmu mmu = getMmuForCartridge(cartridge);
        mmu.setByte(0x0000, 0x0a); // Initially enable RAM
        mmu.setByte(0x4000, 0x00); // Select RAM bank 0
        randomize(cartridge.getRam());
        long initialSig = getDigest(cartridge.getRam());
        mmu.setByte(0x0000, 0x00); // Disable RAM
        randomize(cartridge.getRam()); // This should have no effect
        mmu.setByte(0x0000, 0x0a); // Re-enable RAM so it can be read
        long newSig = getDigest(cartridge.getRam());
        assertEquals("RAM contents should have been unchanged by second write", initialSig, newSig);
    }

    @Test
    public void test_ram_reads_return_0xff_when_disabled() {
        MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
        Mmu mmu = getMmuForCartridge(cartridge);
        mmu.setByte(0x0000, 0x00); // Disable RAM
        mmu.setByte(0x4000, 0x00); // Select RAM bank 0 (though this doesn't really matter)
        for (int addr = 0xa000; addr < 0xc000; addr++) {
            assertEquals(0xff, mmu.readByte(addr));
        }
    }

    @Test
    public void test_switching_banks_changes_ram() {
        MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
        Mmu mmu = getMmuForCartridge(cartridge);
        mmu.setByte(0x0000, 0x0a); // Enable RAM
        mmu.setByte(0x4000, 0x00); // Initially select RAM bank 0
        randomize(cartridge.getRam());
        long ram0Sig = getDigest(cartridge.getRam());
        mmu.setByte(0x4000, 0x01); // Select RAM bank 1
        long initialRam1Sig = getDigest(cartridge.getRam());
        assertNotEquals(ram0Sig, initialRam1Sig);
    }

    @Test
    public void test_switching_ram_to_bank_0_then_bank_1_and_then_bank_0_is_a_no_op() {
        MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
        Mmu mmu = getMmuForCartridge(cartridge);
        mmu.setByte(0x0000, 0x0a); // Enable RAM
        mmu.setByte(0x4000, 0x00); // Initially select RAM bank 0
        randomize(cartridge.getRam());
        long ram0Sig = getDigest(cartridge.getRam());
        mmu.setByte(0x4000, 0x01); // Select RAM bank 1
        mmu.setByte(0x4000, 0x00); // ...and immediately bank back to 0
        assertEquals("Ram 0 should not have been modified", ram0Sig, getDigest(cartridge.getRam()));
    }

    @Test
    public void test_changes_to_ram_1_do_not_affect_ram_0() {
        MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
        Mmu mmu = getMmuForCartridge(cartridge);
        mmu.setByte(0x0000, 0x0a); // Enable RAM
        mmu.setByte(0x4000, 0x00); // Initially select RAM bank 0
        randomize(cartridge.getRam());
        long initialRam0Sig = getDigest(cartridge.getRam());
        mmu.setByte(0x4000, 0x01); // Select RAM bank 1
        randomize(cartridge.getRam());
        mmu.setByte(0x4000, 0x00); // Re-select RAM bank 0
        assertEquals("Ram 0 should not have been modified", initialRam0Sig, getDigest(cartridge.getRam()));
    }

    @Test
    public void test_changes_to_ram_0_do_not_affect_ram_1() {
        MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
        Mmu mmu = getMmuForCartridge(cartridge);
        mmu.setByte(0x0000, 0x0a); // Enable RAM
        mmu.setByte(0x4000, 0x01); // Initially select RAM bank 0
        randomize(cartridge.getRam());
        long initialRam1Sig = getDigest(cartridge.getRam());
        mmu.setByte(0x4000, 0x00); // Select RAM bank 1
        randomize(cartridge.getRam());
        mmu.setByte(0x4000, 0x01); // Re-select RAM bank 0
        assertEquals("Ram 1 should not have been modified", initialRam1Sig, getDigest(cartridge.getRam()));
    }

    @Test
    public void test_that_there_are_at_least_eight_ram_banks() {
        MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
        Mmu mmu = getMmuForCartridge(cartridge);
        mmu.setByte(0x0000, 0x0a); // Enable RAM
        for (int bankIdx = 0; bankIdx < 8; bankIdx++) {
            mmu.setByte(0x4000, bankIdx);
            randomize(cartridge.getRam());
        }
        Set<Long> seenBanks = new HashSet<>();
        for (int bankIdx = 0; bankIdx < 8; bankIdx++) {
            mmu.setByte(0x4000, bankIdx);
            seenBanks.add(getDigest(cartridge.getRam()));
        }
        assertTrue("Expected >=8 RAM banks; actually saw " + seenBanks.size(), seenBanks.size() >= 8);
    }

    @Test
    public void test_ram_can_bank_when_disabled() {
        MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
        Mmu mmu = getMmuForCartridge(cartridge);
        mmu.setByte(0x0000, 0x0a); // Initially activate RAM
        mmu.setByte(0x4000, 3); // Select RAM bank 3
        randomize(cartridge.getRam());
        long ram3Sig = getDigest(cartridge.getRam());
        mmu.setByte(0x4000, 1); // Select RAM bank 1
        mmu.setByte(0x0000, 0x00); // Disable RAM
        mmu.setByte(0x4000, 3); // Bank back to RAM 3
        mmu.setByte(0x0000, 0x0a); // Re-enable RAM
        assertEquals("Ram bank 3 should be selected", ram3Sig, getDigest(cartridge.getRam()));
    }

    @Test
    public void test_any_address_in_0x4000_to_0x6000_can_request_ram_bank_2() {
        MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
        Mmu mmu = getMmuForCartridge(cartridge);
        mmu.setByte(0x0000, 0x0a); // Activate RAM

        // Init RAM bank 2 and record signature
        mmu.setByte(0x4000, 2);
        randomize(cartridge.getRam());
        long ram2Sig = getDigest(cartridge.getRam());
        mmu.setByte(0x4000, 0);

        for (int addr = 0x4000; addr < 0x6000; addr++) {
            mmu.setByte(addr, 2);
            String msg = String.format("Write to %x failed to update ram bank", addr);
            assertEquals(msg, ram2Sig, getDigest(cartridge.getRam()));
        }
    }

    @Test
    public void test_any_address_in_0x4000_to_0x6000_can_request_ram_bank_6() {
        MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
        Mmu mmu = getMmuForCartridge(cartridge);
        mmu.setByte(0x0000, 0x0a); // Activate RAM

        // Init RAM bank 2 and record signature
        mmu.setByte(0x4000, 6);
        randomize(cartridge.getRam());
        long ram2Sig = getDigest(cartridge.getRam());
        mmu.setByte(0x4000, 0);

        for (int addr = 0x4000; addr < 0x6000; addr++) {
            mmu.setByte(addr, 6);
            String msg = String.format("Write to %x failed to update ram bank", addr);
            assertEquals(msg, ram2Sig, getDigest(cartridge.getRam()));
        }
    }

    @Test
    public void test_writing_to_any_address_in_0x0000_to_0x2000_can_enable_and_disable_ram() {
        MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
        Mmu mmu = getMmuForCartridge(cartridge);
        mmu.setByte(0x0000, 0x0a); // Activate RAM
        mmu.setByte(0xa000, 0xde);
        mmu.setByte(0x0000, 0x00); // Disable RAM

        for (int addr = 0x0000; addr < 0x2000; addr++) {
            assertEquals("RAM should be initially disabled", 0xff, mmu.readByte(0xa000));
            mmu.setByte(addr, 0x0a);
            assertEquals("RAM should now be enabled", 0xde, mmu.readByte(0xa000));
            mmu.setByte(addr, 0x00);
            assertEquals("RAM should now be disabled", 0xff, mmu.readByte(0xa000));
        }
    }

    @Test
    public void test_writing_0x05_does_not_enable_ram() {
        MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
        Mmu mmu = getMmuForCartridge(cartridge);
        mmu.setByte(0x0000, 0x0a); // Activate RAM
        mmu.setByte(0xa000, 0xde);
        mmu.setByte(0x0000, 0x00); // Disable RAM
        mmu.setByte(0x0000, 0x05);
        assertEquals("RAM should still be disabled", 0xff, mmu.readByte(0xa000));
    }

    @Test
    public void test_writing_0x05_disables_ram() {
        MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
        Mmu mmu = getMmuForCartridge(cartridge);
        mmu.setByte(0x0000, 0x0a); // Activate RAM
        mmu.setByte(0xa000, 0xde);
        mmu.setByte(0x0000, 0x05);
        assertEquals("RAM should be disabled", 0xff, mmu.readByte(0xa000));
    }

    @Test
    public void test_writing_0x1a_enables_ram() {
        MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
        Mmu mmu = getMmuForCartridge(cartridge);
        mmu.setByte(0x0000, 0x0a); // Activate RAM
        mmu.setByte(0xa000, 0xde);
        mmu.setByte(0x0000, 0x00);
        mmu.setByte(0x0000, 0x1a);
        assertEquals("RAM should be enabled", 0xde, mmu.readByte(0xa000));
    }

    @Test
    public void test_writing_0xfa_enables_ram() {
        MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
        Mmu mmu = getMmuForCartridge(cartridge);
        mmu.setByte(0x0000, 0x0a); // Activate RAM
        mmu.setByte(0xa000, 0xde);
        mmu.setByte(0x0000, 0x00);
        mmu.setByte(0x0000, 0xfa);
        assertEquals("RAM should be enabled", 0xde, mmu.readByte(0xa000));
    }

    @Test
    public void test_writing_0x8a_enables_ram() {
        MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
        Mmu mmu = getMmuForCartridge(cartridge);
        mmu.setByte(0x0000, 0x0a); // Activate RAM
        mmu.setByte(0xa000, 0xde);
        mmu.setByte(0x0000, 0x00);
        mmu.setByte(0x0000, 0x8a);
        assertEquals("RAM should be enabled", 0xde, mmu.readByte(0xa000));
    }

    @Test
    public void test_ram_is_initially_disabled() {
        MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
        Mmu mmu = getMmuForCartridge(cartridge);
        mmu.setByte(0xa000, 0xde);
        assertEquals("RAM should be disabled", 0xff, mmu.readByte(0xa000));
    }

    @Test
    public void test_ram_is_initially_at_bank_0() {
        MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
        Mmu mmu = getMmuForCartridge(cartridge);
        mmu.setByte(0x0000, 0x0a); // Enable RAM
        randomize(cartridge.getRam());
        long signature = getDigest(cartridge.getRam());
        mmu.setByte(0x4000, 0x00);
        assertEquals("RAM should be at bank 0", signature, getDigest(cartridge.getRam()));
    }

    @Test
    public void test_export_and_import_ram_data_are_inverse() throws Exception {
        MbcType3Cartridge cartridge = buildTestCartridge(cartridge1);
        Mmu mmu = getMmuForCartridge(cartridge);
        mmu.setByte(0x0000, 0x0a); // Enable RAM

        // Seed RAM, randomly, and record checksums
        long[] signatures = new long[8];
        for (int bankIdx = 0; bankIdx < 8; bankIdx++) {
            mmu.setByte(0x4000, bankIdx);
            randomize(cartridge.getRam());
            signatures[bankIdx] = getDigest(cartridge.getRam());
        }

        // Export save state and load into a new cartridge; assert checksums are unchanged
        byte[] ramBytes = cartridge.exportRamData();
        cartridge = buildTestCartridge(cartridge1);
        cartridge.importRamData(ramBytes);
        mmu = getMmuForCartridge(cartridge);
        mmu.setByte(0x0000, 0x0a); // Enable RAM
        for (int bankIdx = 0; bankIdx < 8; bankIdx++) {
            mmu.setByte(0x4000, bankIdx);
            assertEquals("Unexpected data at index " + bankIdx, signatures[bankIdx], getDigest(cartridge.getRam()));
        }
    }

    private static int[] buildTestData(Random random) {
        return IntStream.range(0, CARTRIDGE_SIZE).map(idx -> random.nextInt(256)).toArray();
    }

    private static MbcType3Cartridge buildTestCartridge(int[] data) {
        int[] dataCopy = Arrays.copyOf(data, data.length);
        return new MbcType3Cartridge(dataCopy);
    }

    private static long getDigest(MemoryModule memory) {
        final int prime = 31;
        long digest = 0;
        for (int addr = 0; addr < memory.getSizeInBytes(); addr++) {
            digest = ((digest * prime) + memory.readByte(addr)) % Long.MAX_VALUE;
        }
        return digest;
    }

    private static int[] getBytes(MemoryModule memory) {
        int[] bytes = new int[memory.getSizeInBytes()];
        for (int idx = 0; idx < bytes.length; idx++) {
            bytes[idx] = memory.readByte(idx);
        }
        return bytes;
    }

    private void randomize(MemoryModule module) {
        // Note - this randomization uses the seeded randomizer at this.random,
        // so results will be consistent between tests.
        for (int addr = 0x0000; addr < module.getSizeInBytes(); addr++) {
            module.setByte(addr, random.nextInt(256));
        }
    }
}
