package com.kopieczek.gamble.hardware.memory.cartridge;

import com.kopieczek.gamble.hardware.memory.*;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

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

    private static int[] buildTestData(Random random) {
        return IntStream.range(0, CARTRIDGE_SIZE).map(idx -> random.nextInt()).toArray();
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
}
