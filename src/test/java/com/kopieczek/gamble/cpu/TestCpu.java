package com.kopieczek.gamble.cpu;

import com.kopieczek.gamble.memory.MemoryManagementUnit;
import com.kopieczek.gamble.memory.SimpleMemoryModule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestCpu {
    @Test
    public void test_simple_read() {
        MemoryManagementUnit mmu = buildMmu();
        mmu.setByte(0xdead, 0xf0);
        Cpu cpu = new Cpu(mmu);
        assertEquals(0xf0, cpu.readByte(0xdead));
    }

    @Test
    public void test_simple_write() {
        MemoryManagementUnit mmu = buildMmu();
        Cpu cpu = new Cpu(mmu);
        cpu.setByte(0xdead, 0xf0);
        assertEquals(0xf0, mmu.readByte(0xdead));
    }

    private static MemoryManagementUnit buildMmu() {
        return new MemoryManagementUnit(
                new SimpleMemoryModule(MemoryManagementUnit.BIOS_SIZE),
                new SimpleMemoryModule(MemoryManagementUnit.ROM_0_SIZE),
                new SimpleMemoryModule(MemoryManagementUnit.ROM_1_SIZE),
                new SimpleMemoryModule(MemoryManagementUnit.VRAM_SIZE),
                new SimpleMemoryModule(MemoryManagementUnit.EXT_RAM_SIZE),
                new SimpleMemoryModule(MemoryManagementUnit.RAM_SIZE),
                new SimpleMemoryModule(MemoryManagementUnit.SPRITES_SIZE),
                new SimpleMemoryModule(MemoryManagementUnit.IO_AREA_SIZE),
                new SimpleMemoryModule(MemoryManagementUnit.ZRAM_SIZE)
        );
    }

    @Test
    public void test_initial_state() {
        Cpu cpu = new Cpu(buildMmu());
        assertEquals(0, cpu.getCycles());
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_single_nop() {
        Cpu cpu = runProgram(0x00);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_nop_doesnt_set_zero_flag() {
        Cpu cpu = runProgram(0x00);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_multiple_nops() {
        Cpu cpu = runProgram(0x00, 0x00, 0x00);
        assertEquals(12, cpu.getCycles());
    }

    @Test
    public void test_increment_a() {
        Cpu cpu = runProgram(0x3c);
        assertEquals(0x01, cpu.readByte(Register.A));
    }

    @Test
    public void test_single_increment_doesnt_set_zero_flag() {
        Cpu cpu = runProgram(0x3c);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_increment_a_twice() {
        Cpu cpu = runProgram(0x3c, 0x3c);
        assertEquals(0x02, cpu.readByte(Register.A));
    }

    @Test
    public void test_increment_b() {
        Cpu cpu = runProgram(0x04, 0x04, 0x04);
        assertEquals(0x03, cpu.readByte(Register.B));
    }

    @Test
    public void test_increment_c() {
        Cpu cpu = runProgram(0x0c, 0x0c);
        assertEquals(0x02, cpu.readByte(Register.C));
    }

    @Test
    public void test_increment_d() {
        Cpu cpu = runProgram(0x14, 0x14, 0x14, 0x14, 0x14);
        assertEquals(0x05, cpu.readByte(Register.D));
    }

    @Test
    public void test_increment_e() {
        Cpu cpu = runProgram(0x1c, 0x1c);
        assertEquals(0x02, cpu.readByte(Register.E));
    }

    @Test
    public void test_increment_h() {
        Cpu cpu = runProgram(0x24, 0x24, 0x24);
        assertEquals(0x03, cpu.readByte(Register.H));
    }

    @Test
    public void test_increment_l() {
        Cpu cpu = runProgram(0x2c, 0x2c, 0x2c, 0x2c);
        assertEquals(0x04, cpu.readByte(Register.L));
    }

    @Test
    public void test_rollover() {
        int[] program = new int[256];
        for (int idx = 0; idx < program.length; idx++) {
            program[idx] = 0x04;
        }

        // One-byte registers should overflow to 0 when they reach 0x100.
        Cpu cpu = runProgram(program);
        assertEquals(0x00, cpu.readByte(Register.B));

        // The result of the operation was 0, so the zero flag should be set.
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_rollover_followed_by_increment_unsets_zero_flag() {
        int[] program = new int[257];
        for (int idx = 0; idx < program.length; idx++) {
            program[idx] = 0x04;
        }

        Cpu cpu = runProgram(program);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_nop_unsets_zero_flag() {
        int[] program = new int[257];
        for (int idx = 0; idx < 256; idx++) {
            program[idx] = 0x04;
        }
        program[256] = 0x00;

        Cpu cpu = runProgram(program);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_single_inc_hl() {
        Cpu cpu = runProgram(0x34);
        assertEquals(0x01, cpu.readByte(Register.L));
    }

    @Test
    public void test_inc_hl_when_register_l_is_full() {
        int[] program = new int[256];
        for (int idx = 0; idx < 255; idx++) {
            program[idx] = 0x2c; // INC L
        }
        program[255] = 0x34; // INC HL

        Cpu cpu = runProgram(program);
        assertEquals(0x00, cpu.readByte(Register.L));
        assertEquals(0x01, cpu.readByte(Register.H));
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_inc_hl_rollover_at_0x10000() {
        int[] program = new int[2 * 0xff + 1];
        for (int idx = 0; idx < 0xff; idx++) {
            program[idx] = 0x2c; // INC L
            program[0xff + idx] = 0x24; // INC H
        }

        // After running the program so far, HF=0xffff.
        // Now do one more INC HL to trigger rollover.
        program[2 * 0xff] = 0x34; // INC HL

        Cpu cpu = runProgram(program);
        assertEquals(0x00, cpu.readByte(Register.L));
        assertEquals(0x00, cpu.readByte(Register.H));
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    private static Cpu runProgram(int... program) {
        MemoryManagementUnit mmu = buildMmu();
        mmu.setBiosEnabled(false);
        for (int idx = 0; idx < program.length; idx++) {
            mmu.setByte(idx, program[idx]);
        }

        Cpu cpu = new Cpu(mmu);
        int ticks = 0;
        while (cpu.getProgramCounter() < program.length) {
            cpu.tick();
            ticks++;
            if (ticks > 1000) {
                throw new RuntimeException("Program failed to terminate");
            }
        }

        return cpu;
    }
}
