package com.kopieczek.gamble.cpu;

import com.kopieczek.gamble.memory.MemoryManagementUnit;
import com.kopieczek.gamble.memory.SimpleMemoryModule;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Note the tests below intentionally don't use the Operation enum, and instead specify opcodes manually.
 * This gives us additional confidence that the Operation codes are correct.
 */
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
        assertFalse(cpu.isSet(Flag.OPERATION));
        assertFalse(cpu.isSet(Flag.NIBBLE));
        assertFalse(cpu.isSet(Flag.CARRY));
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

        // Apparently INC rollover never sets carry.
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    // All incs should set nibble flag but we'll use INC C as representative
    // for all 8 bit INCs.
    public void test_inc_c_sets_nibble_flag_after_16_incs() {
        Cpu cpu = runProgram(0x0c, 0x0c, 0x0c, 0x0c,
                             0x0c, 0x0c, 0x0c, 0x0c,
                             0x0c, 0x0c, 0x0c, 0x0c,
                             0x0c, 0x0c, 0x0c, 0x0c);
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_inc_c_unsets_nibble_flag_after_17_incs() {
        Cpu cpu = runProgram(0x0c, 0x0c, 0x0c, 0x0c,
                             0x0c, 0x0c, 0x0c, 0x0c,
                             0x0c, 0x0c, 0x0c, 0x0c,
                             0x0c, 0x0c, 0x0c, 0x0c,
                             0x0c);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_rollover_followed_by_increment_unsets_zero_flag() {
        Cpu cpu = runProgram(0x06, 0xff, 0x04, 0x04);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_nop_doesnt_clear_flags() {
        Cpu cpu = cpuWithProgram(0x00);
        cpu.flags = new boolean[] {true, true, true, true};
        runProgram(cpu, 1);
        assertTrue(cpu.isSet(Flag.ZERO));
        assertTrue(cpu.isSet(Flag.CARRY));
        assertTrue(cpu.isSet(Flag.NIBBLE));
        assertTrue(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_single_inc_hl_at_00() {
        Cpu cpu = runProgram(0x34);

        // 0x0000 held the 0x34 instruction initially, which should now
        // have been incremented to 0x35.
        assertEquals(0x35, cpu.readByte(0x0000));
    }

    @Test
    public void test_single_inc_hl_at_0xcafe() {
        Cpu cpu = runProgram(0x26, 0xca, 0x2e, 0xfe, 0x34);
        assertEquals(0x01, cpu.readByte(0xcafe));
    }

    @Test
    public void test_inc_hl_rollover() {
        int[] program = new int[260];

        // Set HL=0xeeee.
        program[0] = 0x26;
        program[1] = 0xee;
        program[2] = 0x2e;
        program[3] = 0xee;

        // 256x INC HL to trigger rollover.
        for (int i=4; i<260; i++) {
            program[i] = 0x34;
        }

        Cpu cpu = runProgram(program);
        assertEquals(0x00, cpu.readByte(0xeeee));
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_inc_hl_uses_12_cycles() {
        Cpu cpu = runProgram(0x34);
        assertEquals(12, cpu.getCycles());
    }

    @Test
    public void test_inc_hl_sets_nibble_flag_after_16_incs() {
        Cpu cpu = runProgram(0x26, 0x12, 0x2e, 0x34, // HL=0x1234
                             0x34, 0x34, 0x34, 0x34,
                             0x34, 0x34, 0x34, 0x34,
                             0x34, 0x34, 0x34, 0x34,
                             0x34, 0x34, 0x34, 0x34);
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_byte_inc_clears_operation_flag() {
        Cpu cpu = cpuWithProgram(0x1c);
        cpu.flags[Flag.OPERATION.ordinal()] = true;
        runProgram(cpu, 1);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_inc_hl_clears_operation_flag() {
        Cpu cpu = cpuWithProgram(0x34);
        cpu.flags[Flag.OPERATION.ordinal()] = true;
        runProgram(cpu, 1);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_direct_load_0x01_to_b() {
        Cpu cpu = runProgram(0x06, 0x01);
        assertEquals(0x01, cpu.readByte(Register.B));
    }

    @Test
    public void test_direct_load_0xff_to_b() {
        Cpu cpu = runProgram(0x06, 0xff);
        assertEquals(0xff, cpu.readByte(Register.B));
    }

    @Test
    public void test_direct_load_is_idempotent() {
        Cpu cpu = runProgram(0x06, 0x1f, 0x06, 0x1f);
        assertEquals(0x1f, cpu.readByte(Register.B));
    }

    @Test
    public void test_direct_load_to_b_uses_8_cycles() {
        Cpu cpu = runProgram(0x06, 0x80);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_direct_load_to_c() {
        Cpu cpu = runProgram(0x0e, 0xab);
        assertEquals(0xab, cpu.readByte(Register.C));
    }

    @Test
    public void test_direct_load_doesnt_affect_other_registers() {
        Cpu cpu = runProgram(0x0e, 0x1f);
        assertEquals(0x1f, cpu.readByte(Register.C));
        assertEquals(0x00, cpu.readByte(Register.A));
        assertEquals(0x00, cpu.readByte(Register.B));
        assertEquals(0x00, cpu.readByte(Register.D));
        assertEquals(0x00, cpu.readByte(Register.E));
        assertEquals(0x00, cpu.readByte(Register.H));
        assertEquals(0x00, cpu.readByte(Register.L));
    }

    @Test
    public void test_direct_load_to_d() {
        Cpu cpu = runProgram(0x16, 0x10);
        assertEquals(0x10, cpu.readByte(Register.D));
    }

    @Test
    public void test_direct_load_to_e() {
        Cpu cpu = runProgram(0x1e, 0x17);
        assertEquals(0x17, cpu.readByte(Register.E));
    }

    @Test
    public void test_direct_load_to_h() {
        Cpu cpu = runProgram(0x26, 0x44);
        assertEquals(0x44, cpu.readByte(Register.H));
    }

    @Test
    public void test_direct_load_to_l() {
        Cpu cpu = runProgram(0x2e, 0x37);
        assertEquals(0x37, cpu.readByte(Register.L));
    }

    @Test
    public void test_direct_load_leaves_empty_flags_alone() {
        Cpu cpu = runProgram(0x2e, 0x00);
        assertFalse(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.OPERATION));
        assertFalse(cpu.isSet(Flag.ZERO));
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_direct_load_leaves_set_flags_alone() {
        Cpu cpu = cpuWithProgram(0x2e, 0x01);
        cpu.flags = new boolean[]{true, true, true, true};
        runProgram(cpu, 2);
        assertTrue(cpu.isSet(Flag.CARRY));
        assertTrue(cpu.isSet(Flag.OPERATION));
        assertTrue(cpu.isSet(Flag.ZERO));
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_load_a_to_a() {
        Cpu cpu = runProgram(0x3c, 0x7f); // INC A; LD A, A
        assertArrayEquals(cpu.flags, new boolean[]{false, false, false, false});
        assertEquals(8, cpu.getCycles()); // 4 for the INC A, 4 for the LD A, A
        assertEquals(0x01, cpu.readByte(Register.A));
    }

    @Test
    public void test_load_b_to_a() {
        Cpu cpu = runProgram(0x06, 0xaa, // LD B, 0xaa
                             0x78);      // LD A, B
        assertArrayEquals(cpu.flags, new boolean[]{false, false, false, false});
        assertEquals(12, cpu.getCycles()); // 8 for the LD B, 4 for the LD A, C
        assertEquals(0xaa, cpu.readByte(Register.A));
        assertEquals(0xaa, cpu.readByte(Register.B));
    }

    @Test
    public void test_load_c_to_a() {
        Cpu cpu = runProgram(0x0e, 0x10, // LD C, 0x10
                             0x79);      // LD A, C
        assertArrayEquals(cpu.flags, new boolean[]{false, false, false, false});
        assertEquals(12, cpu.getCycles()); // 8 for the LD C, 4 for the LD A, C
        assertEquals(0x10, cpu.readByte(Register.A));
        assertEquals(0x10, cpu.readByte(Register.C));
    }

    @Test
    public void test_load_d_to_a() {
        Cpu cpu = runProgram(0x16, 0xff, // LD D, 0xff
                             0x7a);      // LD A, D
        assertArrayEquals(cpu.flags, new boolean[]{false, false, false, false});
        assertEquals(12, cpu.getCycles()); // 8 for the LD D, 4 for the LD A, D
        assertEquals(0xff, cpu.readByte(Register.A));
        assertEquals(0xff, cpu.readByte(Register.D));
    }

    @Test
    public void test_load_e_to_a_when_e_is_nonzero() {
        Cpu cpu = runProgram(0x1c,       // INC E
                             0x1e, 0x00, // LD E, 0x00
                             0x7b);      // LD A, E
        assertArrayEquals(cpu.flags, new boolean[]{false, false, false, false});
        assertEquals(4 + 8 + 4, cpu.getCycles());
        assertEquals(0x00, cpu.readByte(Register.A));
        assertEquals(0x00, cpu.readByte(Register.E));
    }

    @Test
    public void test_load_h_to_a() {
        Cpu cpu = runProgram(0x26, 0xca, // LD H, 0xca
                             0x7c);      // LD A, H
        assertArrayEquals(cpu.flags, new boolean[]{false, false, false, false});
        assertEquals(12, cpu.getCycles()); // 8 for the LD B, 4 for the LD A, H
        assertEquals(0xca, cpu.readByte(Register.A));
        assertEquals(0xca, cpu.readByte(Register.H));
    }

    @Test
    public void test_load_l_to_a() {
        Cpu cpu = runProgram(0x2e, 0xfe, // LD L, 0xfe
                             0x7d);      // LD A, L
        assertArrayEquals(cpu.flags, new boolean[]{false, false, false, false});
        assertEquals(12, cpu.getCycles()); // 8 for the LD L, 4 for the LD A, L
        assertEquals(0xfe, cpu.readByte(Register.A));
        assertEquals(0xfe, cpu.readByte(Register.L));
    }

    private static Cpu cpuWithProgram(int... program) {
        MemoryManagementUnit mmu = buildMmu();
        mmu.setBiosEnabled(false);
        for (int idx = 0; idx < program.length; idx++) {
            mmu.setByte(idx, program[idx]);
        }

        return new Cpu(mmu);
    }

    private static Cpu runProgram(int... program) {
        Cpu cpu = cpuWithProgram(program);
        runProgram(cpu, program.length);
        return cpu;
    }

    private static void runProgram(Cpu cpu, int programLength) {
        int ticks = 0;
        while (cpu.getProgramCounter() < programLength) {
            cpu.tick();
            ticks++;
            if (ticks > 1000) {
                throw new RuntimeException("Program failed to terminate");
            }
        }
    }
}
