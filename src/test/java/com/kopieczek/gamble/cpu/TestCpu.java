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
        assertEquals(0xf0, cpu.readFrom(Pointer.of(Word.literal(0xdead))));
    }

    @Test
    public void test_simple_write() {
        MemoryManagementUnit mmu = buildMmu();
        Cpu cpu = new Cpu(mmu);
        cpu.writeTo(Pointer.of(Word.literal(0xdead)), Byte.literal(0xf0));
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
        assertEquals(0x01, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_single_increment_doesnt_set_zero_flag() {
        Cpu cpu = runProgram(0x3c);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_increment_a_twice() {
        Cpu cpu = runProgram(0x3c, 0x3c);
        assertEquals(0x02, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_increment_b() {
        Cpu cpu = runProgram(0x04, 0x04, 0x04);
        assertEquals(0x03, cpu.read(Byte.Register.B));
    }

    @Test
    public void test_increment_c() {
        Cpu cpu = runProgram(0x0c, 0x0c);
        assertEquals(0x02, cpu.read(Byte.Register.C));
    }

    @Test
    public void test_increment_d() {
        Cpu cpu = runProgram(0x14, 0x14, 0x14, 0x14, 0x14);
        assertEquals(0x05, cpu.read(Byte.Register.D));
    }

    @Test
    public void test_increment_e() {
        Cpu cpu = runProgram(0x1c, 0x1c);
        assertEquals(0x02, cpu.read(Byte.Register.E));
    }

    @Test
    public void test_increment_h() {
        Cpu cpu = runProgram(0x24, 0x24, 0x24);
        assertEquals(0x03, cpu.read(Byte.Register.H));
    }

    @Test
    public void test_increment_l() {
        Cpu cpu = runProgram(0x2c, 0x2c, 0x2c, 0x2c);
        assertEquals(0x04, cpu.read(Byte.Register.L));
    }

    @Test
    public void test_rollover() {
        int[] program = new int[256];
        for (int idx = 0; idx < program.length; idx++) {
            program[idx] = 0x04;
        }

        // One-byte registers should overflow to 0 when they reach 0x100.
        Cpu cpu = runProgram(program);
        assertEquals(0x00, cpu.read(Byte.Register.B));

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
        cpu.set(Byte.Register.F, Byte.literal(0xf0));
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
        assertEquals(0x35, cpu.unsafeRead(0x0000));
    }

    @Test
    public void test_single_inc_hl_at_0xcafe() {
        Cpu cpu = runProgram(0x26, 0xca, 0x2e, 0xfe, 0x34);
        assertEquals(0x01, cpu.unsafeRead(0xcafe));
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
        assertEquals(0x00, cpu.unsafeRead(0xeeee));
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
        cpu.set(Flag.OPERATION, true);
        runProgram(cpu, 1);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_inc_hl_clears_operation_flag() {
        Cpu cpu = cpuWithProgram(0x34);
        cpu.set(Flag.OPERATION, true);
        runProgram(cpu, 1);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_direct_load_0x01_to_b() {
        Cpu cpu = runProgram(0x06, 0x01);
        assertEquals(0x01, cpu.read(Byte.Register.B));
    }

    @Test
    public void test_direct_load_0xff_to_b() {
        Cpu cpu = runProgram(0x06, 0xff);
        assertEquals(0xff, cpu.read(Byte.Register.B));
    }

    @Test
    public void test_direct_load_is_idempotent() {
        Cpu cpu = runProgram(0x06, 0x1f, 0x06, 0x1f);
        assertEquals(0x1f, cpu.read(Byte.Register.B));
    }

    @Test
    public void test_direct_load_to_b_uses_8_cycles() {
        Cpu cpu = runProgram(0x06, 0x80);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_direct_load_to_c() {
        Cpu cpu = runProgram(0x0e, 0xab);
        assertEquals(0xab, cpu.read(Byte.Register.C));
    }

    @Test
    public void test_direct_load_doesnt_affect_other_registers() {
        Cpu cpu = runProgram(0x0e, 0x1f);
        assertEquals(0x1f, cpu.read(Byte.Register.C));
        assertEquals(0x00, cpu.read(Byte.Register.A));
        assertEquals(0x00, cpu.read(Byte.Register.B));
        assertEquals(0x00, cpu.read(Byte.Register.D));
        assertEquals(0x00, cpu.read(Byte.Register.E));
        assertEquals(0x00, cpu.read(Byte.Register.H));
        assertEquals(0x00, cpu.read(Byte.Register.L));
    }

    @Test
    public void test_direct_load_to_d() {
        Cpu cpu = runProgram(0x16, 0x10);
        assertEquals(0x10, cpu.read(Byte.Register.D));
    }

    @Test
    public void test_direct_load_to_e() {
        Cpu cpu = runProgram(0x1e, 0x17);
        assertEquals(0x17, cpu.read(Byte.Register.E));
    }

    @Test
    public void test_direct_load_to_h() {
        Cpu cpu = runProgram(0x26, 0x44);
        assertEquals(0x44, cpu.read(Byte.Register.H));
    }

    @Test
    public void test_direct_load_to_l() {
        Cpu cpu = runProgram(0x2e, 0x37);
        assertEquals(0x37, cpu.read(Byte.Register.L));
    }

    @Test
    public void test_direct_load_to_a() {
        Cpu cpu = runProgram(0x3e, 0x91);
        assertEquals(0x91, cpu.read(Byte.Register.A));
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
        cpu.set(Byte.Register.F, Byte.literal(0xf0));
        runProgram(cpu, 2);
        assertTrue(cpu.isSet(Flag.CARRY));
        assertTrue(cpu.isSet(Flag.OPERATION));
        assertTrue(cpu.isSet(Flag.ZERO));
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_load_a_to_a() {
        Cpu cpu = runProgram(0x3c, 0x7f); // INC A; LD A, A
        cpu.set(Byte.Register.F, Byte.literal(0x00));
        assertEquals(8, cpu.getCycles()); // 4 for the INC A, 4 for the LD A, A
        assertEquals(0x01, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_load_b_to_a() {
        Cpu cpu = runProgram(0x06, 0xaa, // LD B, 0xaa
                             0x78);      // LD A, B
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(12, cpu.getCycles()); // 8 for the LD B, 4 for the LD A, B
        assertEquals(0xaa, cpu.read(Byte.Register.A));
        assertEquals(0xaa, cpu.read(Byte.Register.B));
    }

    @Test
    public void test_load_c_to_a() {
        Cpu cpu = runProgram(0x0e, 0x10, // LD C, 0x10
                             0x79);      // LD A, C
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(12, cpu.getCycles()); // 8 for the LD C, 4 for the LD A, C
        assertEquals(0x10, cpu.read(Byte.Register.A));
        assertEquals(0x10, cpu.read(Byte.Register.C));
    }

    @Test
    public void test_load_d_to_a() {
        Cpu cpu = runProgram(0x16, 0xff, // LD D, 0xff
                             0x7a);      // LD A, D
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(12, cpu.getCycles()); // 8 for the LD D, 4 for the LD A, D
        assertEquals(0xff, cpu.read(Byte.Register.A));
        assertEquals(0xff, cpu.read(Byte.Register.D));
    }

    @Test
    public void test_load_e_to_a_when_e_is_nonzero() {
        Cpu cpu = runProgram(0x1c,       // INC E
                             0x1e, 0x00, // LD E, 0x00
                             0x7b);      // LD A, E
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(4 + 8 + 4, cpu.getCycles());
        assertEquals(0x00, cpu.read(Byte.Register.A));
        assertEquals(0x00, cpu.read(Byte.Register.E));
    }

    @Test
    public void test_load_h_to_a() {
        Cpu cpu = runProgram(0x26, 0xca, // LD H, 0xca
                             0x7c);      // LD A, H
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(12, cpu.getCycles()); // 8 for the LD H, 4 for the LD A, H
        assertEquals(0xca, cpu.read(Byte.Register.A));
        assertEquals(0xca, cpu.read(Byte.Register.H));
    }

    @Test
    public void test_load_l_to_a() {
        Cpu cpu = runProgram(0x2e, 0xfe, // LD L, 0xfe
                             0x7d);      // LD A, L
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(12, cpu.getCycles()); // 8 for the LD L, 4 for the LD A, L
        assertEquals(0xfe, cpu.read(Byte.Register.A));
        assertEquals(0xfe, cpu.read(Byte.Register.L));
    }

    @Test
    public void test_load_indirect_hl_to_a() {
        Cpu cpu = runProgram(
                0x26, 0xca,              // Set H = 0xca
                0x2e, 0xfe,              // Set L = 0xfe
                0x34, 0x34, 0x34, 0x34,  // INC_HLx4, so (0xcafe) holds 0x04.
                0x7e                     // LD A, (HL).
        );

        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(0x04, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_load_b_to_b() {
        Cpu cpu = runProgram(0x04, 0x40); // INC B; LD B, B
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(8, cpu.getCycles()); // 4 for the INC B, 8 for the LD B, B
        assertEquals(0x01, cpu.read(Byte.Register.B));
    }

    @Test
    public void test_load_c_to_b() {
        Cpu cpu = runProgram(0x0e, 0x10, // LD C, 0x10
                             0x41);      // LD B, C
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(12, cpu.getCycles()); // 8 for the LD C, 4 for the LD B, C
        assertEquals(0x10, cpu.read(Byte.Register.B));
        assertEquals(0x10, cpu.read(Byte.Register.C));
    }

    @Test
    public void test_load_d_to_b() {
        Cpu cpu = runProgram(0x16, 0xff, // LD D, 0xff
                             0x42);      // LD A, D
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(12, cpu.getCycles()); // 8 for the LD D, 4 for the LD B, D
        assertEquals(0xff, cpu.read(Byte.Register.B));
        assertEquals(0xff, cpu.read(Byte.Register.D));
    }

    @Test
    public void test_load_e_to_b() {
        Cpu cpu = runProgram(0x1e, 0x01, // LD E, 0x01
                             0x43);      // LD B, E
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(12, cpu.getCycles()); // 8 for the LD E, 4 for the LD B, E
        assertEquals(0x01, cpu.read(Byte.Register.B));
        assertEquals(0x01, cpu.read(Byte.Register.E));
    }

    @Test
    public void test_load_h_to_b() {
        Cpu cpu = runProgram(0x26, 0xca, // LD H, 0xca
                             0x44);      // LD B, H
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(12, cpu.getCycles()); // 8 for the LD H, 4 for the LD B, H
        assertEquals(0xca, cpu.read(Byte.Register.B));
        assertEquals(0xca, cpu.read(Byte.Register.H));
    }

    @Test
    public void test_load_l_to_b() {
        Cpu cpu = runProgram(0x2e, 0xfe, // LD L, 0xfe
                             0x45);      // LD B, L
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(12, cpu.getCycles()); // 8 for the LD L, 4 for the LD B, L
        assertEquals(0xfe, cpu.read(Byte.Register.B));
        assertEquals(0xfe, cpu.read(Byte.Register.L));
    }

    @Test
    public void test_load_indirect_hl_to_b() {
        Cpu cpu = runProgram(
                0x26, 0xaa,              // Set H = 0xaa
                0x2e, 0xbb,              // Set L = 0xbb
                0x34, 0x34, 0x34,        // INC_HLx3, so (0xaabb) holds 0x03.
                0x46                     // LD B, (HL).
        );

        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(0x03, cpu.read(Byte.Register.B));
    }

    @Test
    public void test_load_indirect_hl_to_b_uses_8_cycles() {
        Cpu cpu = runProgram(0x46);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_load_b_to_c() {
        Cpu cpu = runProgram(0x06, 0x10, // LD B, 0x10
                             0x48);      // LD C, B
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(12, cpu.getCycles()); // 8 for the LD B, 4 for the LD C, B
        assertEquals(0x10, cpu.read(Byte.Register.C));
        assertEquals(0x10, cpu.read(Byte.Register.B));
    }

    @Test
    public void test_load_c_to_c() {
        Cpu cpu = runProgram(0x0c, 0x49); // INC C; LD C, C
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(8, cpu.getCycles()); // 4 for the INC C, 8 for the LD C, C
        assertEquals(0x01, cpu.read(Byte.Register.C));
    }

    @Test
    public void test_load_d_to_c() {
        Cpu cpu = runProgram(0x16, 0xff, // LD D, 0xff
                             0x4a);      // LD A, D
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(12, cpu.getCycles()); // 8 for the LD D, 4 for the LD C, D
        assertEquals(0xff, cpu.read(Byte.Register.C));
        assertEquals(0xff, cpu.read(Byte.Register.D));
    }

    @Test
    public void test_load_e_to_c() {
        Cpu cpu = runProgram(0x1e, 0x01, // LD E, 0x01
                             0x4b);      // LD C, E
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(12, cpu.getCycles()); // 8 for the LD E, 4 for the LD C, E
        assertEquals(0x01, cpu.read(Byte.Register.C));
        assertEquals(0x01, cpu.read(Byte.Register.E));
    }

    @Test
    public void test_load_h_to_c() {
        Cpu cpu = runProgram(0x26, 0xca, // LD H, 0xca
                             0x4c);      // LD C, H
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(12, cpu.getCycles()); // 8 for the LD H, 4 for the LD C, H
        assertEquals(0xca, cpu.read(Byte.Register.C));
        assertEquals(0xca, cpu.read(Byte.Register.H));
    }

    @Test
    public void test_load_l_to_c() {
        Cpu cpu = runProgram(0x2e, 0xfe, // LD L, 0xfe
                             0x4d);      // LD C, L
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(12, cpu.getCycles()); // 8 for the LD L, 4 for the LD B, L
        assertEquals(0xfe, cpu.read(Byte.Register.C));
        assertEquals(0xfe, cpu.read(Byte.Register.L));
    }

    @Test
    public void test_load_indirect_hl_to_c() {
        Cpu cpu = runProgram(
                0x26, 0xa1,              // Set H = 0xa1
                0x2e, 0x2b,              // Set L = 0x2b
                0x34, 0x34,              // INC_HLx2, so (0xa12b) holds 0x02.
                0x4e                     // LD C, (HL).
        );

        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(0x02, cpu.read(Byte.Register.C));
    }

    @Test
    public void test_load_indirect_hl_to_c_uses_8_cycles() {
        Cpu cpu = runProgram(0x4e);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_load_b_to_d() {
        Cpu cpu = runProgram(0x06, 0x10, // LD B, 0x10
                             0x50);      // LD D, B
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(12, cpu.getCycles()); // 8 for the LD B, 4 for the LD D, B
        assertEquals(0x10, cpu.read(Byte.Register.D));
        assertEquals(0x10, cpu.read(Byte.Register.B));
    }

    @Test
    public void test_load_c_to_d() {
        Cpu cpu = runProgram(0x0e, 0xff, // LD C, 0xff
                             0x51);      // LD D, C
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(12, cpu.getCycles()); // 8 for the LD C, 4 for the LD D, C
        assertEquals(0xff, cpu.read(Byte.Register.D));
        assertEquals(0xff, cpu.read(Byte.Register.C));
    }

    @Test
    public void test_load_d_to_d() {
        Cpu cpu = runProgram(0x14, 0x52); // INC D; LD D, D
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(8, cpu.getCycles()); // 4 for the INC D, 8 for the LD D, D
        assertEquals(0x01, cpu.read(Byte.Register.D));
    }

    @Test
    public void test_load_e_to_d() {
        Cpu cpu = runProgram(0x1e, 0x01, // LD E, 0x01
                             0x53);      // LD D, E
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(12, cpu.getCycles()); // 8 for the LD E, 4 for the LD D, E
        assertEquals(0x01, cpu.read(Byte.Register.D));
        assertEquals(0x01, cpu.read(Byte.Register.E));
    }

    @Test
    public void test_load_h_to_d() {
        Cpu cpu = runProgram(0x26, 0xca, // LD H, 0xca
                             0x54);      // LD D, H
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(12, cpu.getCycles()); // 8 for the LD H, 4 for the LD D, H
        assertEquals(0xca, cpu.read(Byte.Register.D));
        assertEquals(0xca, cpu.read(Byte.Register.H));
    }

    @Test
    public void test_load_l_to_d() {
        Cpu cpu = runProgram(0x2e, 0xfe, // LD L, 0xfe
                             0x55);      // LD D, L
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(12, cpu.getCycles()); // 8 for the LD L, 4 for the LD D, L
        assertEquals(0xfe, cpu.read(Byte.Register.D));
        assertEquals(0xfe, cpu.read(Byte.Register.L));
    }

    @Test
    public void test_load_indirect_hl_to_d() {
        Cpu cpu = runProgram(
                0x26, 0xa1,              // Set H = 0xa1
                0x2e, 0x2b,              // Set L = 0x2b
                0x34, 0x34,              // INC_HLx2, so (0xa12b) holds 0x02.
                0x56                     // LD D, (HL).
        );

        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(0x02, cpu.read(Byte.Register.D));
    }

    @Test
    public void test_load_indirect_hl_to_d_uses_8_cycles() {
        Cpu cpu = runProgram(0x56);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_load_b_to_e() {
        Cpu cpu = runProgram(0x06, 0x10, // LD B, 0x10
                             0x58);      // LD E, B
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(12, cpu.getCycles()); // 8 for the LD B, 4 for the LD B, E
        assertEquals(0x10, cpu.read(Byte.Register.E));
        assertEquals(0x10, cpu.read(Byte.Register.B));
    }

    @Test
    public void test_load_c_to_e() {
        Cpu cpu = runProgram(0x0e, 0xff, // LD C, 0xff
                             0x59);      // LD E, C
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(12, cpu.getCycles()); // 8 for the LD C, 4 for the LD E, C
        assertEquals(0xff, cpu.read(Byte.Register.E));
        assertEquals(0xff, cpu.read(Byte.Register.C));
    }

    @Test
    public void test_load_d_to_e() {
        Cpu cpu = runProgram(0x16, 0x01, // LD D, 0x01
                             0x5a);      // LD E, D
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(12, cpu.getCycles()); // 8 for the LD D, 4 for the LD E, D
        assertEquals(0x01, cpu.read(Byte.Register.E));
        assertEquals(0x01, cpu.read(Byte.Register.D));
    }

    @Test
    public void test_load_e_to_e() {
        Cpu cpu = runProgram(0x1c, 0x5b); // INC E; LD E, E
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(8, cpu.getCycles()); // 4 for the INC E, 8 for the LD E, E
        assertEquals(0x01, cpu.read(Byte.Register.E));
    }

    @Test
    public void test_load_h_to_e() {
        Cpu cpu = runProgram(0x26, 0xca, // LD H, 0xca
                             0x5c);      // LD E, H
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(12, cpu.getCycles()); // 8 for the LD H, 4 for the LD E, H
        assertEquals(0xca, cpu.read(Byte.Register.E));
        assertEquals(0xca, cpu.read(Byte.Register.H));
    }

    @Test
    public void test_load_l_to_e() {
        Cpu cpu = runProgram(0x2e, 0xfe, // LD L, 0xfe
                             0x5d);      // LD E, L
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(12, cpu.getCycles()); // 8 for the LD L, 4 for the LD E, L
        assertEquals(0xfe, cpu.read(Byte.Register.E));
        assertEquals(0xfe, cpu.read(Byte.Register.L));
    }

    @Test
    public void test_load_indirect_hl_to_e() {
        Cpu cpu = runProgram(
                0x26, 0x1a,              // Set H = 0x1a
                0x2e, 0xb2,              // Set L = 0xb2
                0x34, 0x34, 0x34, 0x34,  // INC_HLx4, so (0xa12b) holds 0x04.
                0x5e                     // LD E, (HL).
        );

        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(0x04, cpu.read(Byte.Register.E));
    }

    @Test
    public void test_load_indirect_hl_to_e_uses_8_cycles() {
        Cpu cpu = runProgram(0x5e);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_load_b_to_h() {
        Cpu cpu = runProgram(0x06, 0x10, // LD B, 0x10
                             0x60);      // LD H, B
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(12, cpu.getCycles()); // 8 for the LD B, 4 for the LD H, B
        assertEquals(0x10, cpu.read(Byte.Register.H));
        assertEquals(0x10, cpu.read(Byte.Register.B));
    }

    @Test
    public void test_load_c_to_h() {
        Cpu cpu = runProgram(0x0e, 0xff, // LD C, 0xff
                             0x61);      // LD H, C
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(12, cpu.getCycles()); // 8 for the LD C, 4 for the LD H, C
        assertEquals(0xff, cpu.read(Byte.Register.H));
        assertEquals(0xff, cpu.read(Byte.Register.C));
    }

    @Test
    public void test_load_d_to_h() {
        Cpu cpu = runProgram(0x16, 0x01, // LD D, 0x01
                             0x62);      // LD H, D
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(12, cpu.getCycles()); // 8 for the LD D, 4 for the LD H, D
        assertEquals(0x01, cpu.read(Byte.Register.H));
        assertEquals(0x01, cpu.read(Byte.Register.D));
    }

    @Test
    public void test_load_e_to_h() {
        Cpu cpu = runProgram(0x1e, 0xca, // LD E, 0xca
                             0x63);      // LD H, E
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(12, cpu.getCycles()); // 8 for the LD H, 4 for the LD H, E
        assertEquals(0xca, cpu.read(Byte.Register.E));
        assertEquals(0xca, cpu.read(Byte.Register.H));
    }

    @Test
    public void test_load_h_to_h() {
        Cpu cpu = runProgram(0x24, 0x64); // INC H; LD H, H
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(8, cpu.getCycles()); // 4 for the INC H, 8 for the LD H, H
        assertEquals(0x01, cpu.read(Byte.Register.H));
    }

    @Test
    public void test_load_l_to_h() {
        Cpu cpu = runProgram(0x2e, 0xfe, // LD L, 0xfe
                             0x65);      // LD H, L
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(12, cpu.getCycles()); // 8 for the LD L, 4 for the LD H, L
        assertEquals(0xfe, cpu.read(Byte.Register.H));
        assertEquals(0xfe, cpu.read(Byte.Register.L));
    }

    @Test
    public void test_load_indirect_hl_to_h() {
        Cpu cpu = runProgram(
                0x26, 0x1a,              // Set H = 0x1a
                0x2e, 0xb2,              // Set L = 0xb2
                0x34, 0x34, 0x34, 0x34,  // INC_HLx4, so (0xa12b) holds 0x04.
                0x66                     // LD H, (HL).
        );

        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(0x04, cpu.read(Byte.Register.H));
    }

    @Test
    public void test_load_indirect_hl_to_h_uses_8_cycles() {
        Cpu cpu = runProgram(0x66);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_load_b_to_l() {
        Cpu cpu = runProgram(0x06, 0x10, // LD B, 0x10
                             0x68);      // LD L, B
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(12, cpu.getCycles()); // 8 for the LD B, 4 for the LD L, B
        assertEquals(0x10, cpu.read(Byte.Register.L));
        assertEquals(0x10, cpu.read(Byte.Register.B));
    }

    @Test
    public void test_load_c_to_l() {
        Cpu cpu = runProgram(0x0e, 0xff, // LD C, 0xff
                             0x69);      // LD L, C
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(12, cpu.getCycles()); // 8 for the LD C, 4 for the LD L, C
        assertEquals(0xff, cpu.read(Byte.Register.L));
        assertEquals(0xff, cpu.read(Byte.Register.C));
    }

    @Test
    public void test_load_d_to_l() {
        Cpu cpu = runProgram(0x16, 0x01, // LD D, 0x01
                             0x6a);      // LD L, D
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(12, cpu.getCycles()); // 8 for the LD D, 4 for the LD L, D
        assertEquals(0x01, cpu.read(Byte.Register.L));
        assertEquals(0x01, cpu.read(Byte.Register.D));
    }

    @Test
    public void test_load_e_to_l() {
        Cpu cpu = runProgram(0x1e, 0xca, // LD E, 0xca
                             0x6b);      // LD L, E
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(12, cpu.getCycles()); // 8 for the LD H, 4 for the LD E, H
        assertEquals(0xca, cpu.read(Byte.Register.L));
        assertEquals(0xca, cpu.read(Byte.Register.E));
    }

    @Test
    public void test_load_h_to_l() {
        Cpu cpu = runProgram(0x26, 0xfe, // LD H, 0xfe
                             0x6c);      // LD L, H
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(12, cpu.getCycles()); // 8 for the LD H, 4 for the LD L, H
        assertEquals(0xfe, cpu.read(Byte.Register.L));
        assertEquals(0xfe, cpu.read(Byte.Register.H));
    }

    @Test
    public void test_load_l_to_l() {
        Cpu cpu = runProgram(0x2c, 0x6d); // INC H; LD H, H
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(8, cpu.getCycles()); // 4 for the INC L, 8 for the LD L, L
        assertEquals(0x01, cpu.read(Byte.Register.L));
    }

    @Test
    public void test_load_indirect_hl_to_L() {
        Cpu cpu = runProgram(
                0x26, 0xee,              // Set H = 0xee
                0x2e, 0xff,              // Set L = 0xff
                0x34, 0x34, 0x34, 0x34,  // INC_HLx4, so (0xeeff) holds 0x04.
                0x6e                     // LD L, (HL).
        );

        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
        assertEquals(0x04, cpu.read(Byte.Register.L));
    }

    @Test
    public void test_load_indirect_hl_to_l_uses_8_cycles() {
        Cpu cpu = runProgram(0x6e);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_load_b_to_indirect_hl() {
        Cpu cpu = runProgram(
            0x06, 0x56, // LD B, 0x56
            0x26, 0x10, // LD H, 0x10
            0x2e, 0xff, // LD L, 0xff (now HL=0x10ff).
            0x70        // LD (HL), B
        );

        assertEquals(0x56, cpu.unsafeRead(0x10ff));
    }

    @Test
    public void test_load_b_to_indirect_hl_uses_8_cycles() {
        Cpu cpu = runProgram(0x70);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_load_c_to_indirect_hl() {
        Cpu cpu = runProgram(
                0x0e, 0xaa, // LD C, 0xaa
                0x26, 0x4f, // LD H, 0x4f
                0x2e, 0xfe, // LD L, 0xfe (now HL=0x4ffe).
                0x71        // LD (HL), C
        );

        assertEquals(0xaa, cpu.unsafeRead(0x4ffe));
    }

    @Test
    public void test_load_d_to_indirect_hl() {
        Cpu cpu = runProgram(
                0x16, 0x14, // LD D, 0x14
                0x26, 0x20, // LD H, 0x20
                0x2e, 0xf3, // LD L, 0xf3 (now HL=0x20f3).
                0x72        // LD (HL), D
        );

        assertEquals(0x14, cpu.unsafeRead(0x20f3));
    }

    @Test
    public void test_load_e_to_indirect_hl() {
        Cpu cpu = runProgram(
                0x1e, 0x77, // LD E, 0x77
                0x26, 0x00, // LD H, 0x00
                0x2e, 0x01, // LD L, 0x01 (now HL=0x0001).
                0x73        // LD (HL), E
        );

        assertEquals(0x77, cpu.unsafeRead(0x0001));
    }

    @Test
    public void test_load_h_to_indirect_hl() {
        Cpu cpu = runProgram(
                0x26, 0x19, // LD H, 0x19
                0x2e, 0x91, // LD L, 0x01 (now HL=0x1991).
                0x74        // LD (HL), H
        );

        assertEquals(0x19, cpu.unsafeRead(0x1991));
    }

    @Test
    public void test_load_l_to_indirect_hl() {
        Cpu cpu = runProgram(
                0x26, 0x10, // LD H, 0x10
                0x2e, 0x66, // LD L, 0x66 (now HL=0x1066).
                0x75        // LD (HL), L
        );

        assertEquals(0x66, cpu.unsafeRead(0x1066));
    }

    @Test
    public void test_load_value_to_indirect_hl() {
        Cpu cpu = runProgram(
                0x26, 0x5f, // LD H, 0x5f
                0x2e, 0x6c, // LD L, 0x6c (now HL=0x5f6c).
                0x36, 0xbc  // LD (HL), 0xbc
        );

        assertEquals(0xbc, cpu.unsafeRead(0x5f6c));
    }

    @Test
    public void test_load_value_to_indirect_hl_uses_12_cycles() {
        Cpu cpu = runProgram(0x36, 0xff);
        assertEquals(12, cpu.getCycles());
    }

    @Test
    public void test_load_indirect_bc_to_a() {
        Cpu cpu = runProgram(
                0x26, 0x43,
                0x2e, 0x77,
                0x36, 0x3e,  // Load 3e to (0x4377)
                0x44, 0x4d,  // Set (BC) = 0x4377.
                0x26, 0x00,
                0x2e, 0x00,  // Clear (HL).
                0x0a         // LD A, (BC)
        );

        assertEquals(0x3e, cpu.read(Byte.Register.A));
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
    }

    @Test
    public void test_load_indirect_bc_to_a_when_a_is_nonzero() {
        Cpu cpu = runProgram(
                0x3c,        // INC A
                0x26, 0x43,
                0x2e, 0x77,
                0x36, 0x3e,  // Load 3e to (0x4377)
                0x44, 0x4d,  // Set (BC) = 0x4377.
                0x26, 0x00,
                0x2e, 0x00,  // Clear (HL).
                0x0a         // LD A, (BC)
        );

        assertEquals(0x3e, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_load_indirect_bc_to_a_with_0_overwrites_a() {
        Cpu cpu = runProgram(0x06, 0xff, 0x78, 0x0a);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_load_indirect_bc_to_a_uses_8_cycles() {
        Cpu cpu = runProgram(0x0a);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_load_indirect_de_to_a() {
        Cpu cpu = runProgram(
                0x26, 0x99,
                0x2e, 0xab,
                0x36, 0xb4,  // Load b4 to (0x99ab)
                0x54, 0x5d,  // Set (DE) = 0x4377.
                0x26, 0x00,
                0x2e, 0x00,  // Clear (HL).
                0x1a         // LD A, (DE)
        );

        assertEquals(0xb4, cpu.read(Byte.Register.A));
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
    }

    @Test
    public void test_load_indirect_address_to_a() {
        Cpu cpu = runProgram(
                0x26, 0x99,
                0x2e, 0xab,
                0x36, 0xb4,       // Load b4 to (0x99ab)
                0x26, 0x00,
                0x2e, 0x00,       // Clear (HL)
                0xfa, 0xab, 0x99  // LD A (0x99ab) - note the arguments are little endian.
        );

        assertEquals(0xb4, cpu.read(Byte.Register.A));
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
    }

    @Test
    public void test_load_indirect_address_to_a_uses_16_cycles() {
        Cpu cpu = runProgram(0xfa, 0x00, 0x00);
        assertEquals(16, cpu.getCycles());
    }

    @Test
    public void test_indirect_load_c_to_a() {
        Cpu cpu = runProgram(
            0x26, 0xff,
            0x2e, 0x54,
            0x36, 0xcd,       // Load 0xcd to (0xff54)
            0x26, 0x00,
            0x2e, 0x00,       // Clear (HL)
            0x0e, 0x54,       // LD C, 0x54
            0xf2              // LD A, 0xff(C)
        );
        assertEquals(0xcd, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_indirect_load_c_to_a_uses_8_cycles() {
        Cpu cpu = runProgram(0xf2);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_load_c_to_a_indirect() {
        Cpu cpu = runProgram(
            0x3e, 0x3b, // LD A, 0x3b
            0x0e, 0xab, // LD C, 0xab
            0xe2        // LD 0xff(C), A
        );
        assertEquals(0x3b, cpu.unsafeRead(0xff0ab));
    }

    @Test
    public void test_load_c_to_a_indirect_uses_8_cycles() {
        Cpu cpu = runProgram(0xe2);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_ldd_a_hl() {
        Cpu cpu = runProgram(
            0x26, 0xff,
            0x2e, 0x54,       // Set HL = 0xff54.
            0x36, 0xcd,       // Store 0xcd at 0xff54.
            0x3a              // Move 0xcd to A and dec HL.
        );
        assertEquals(0xcd, cpu.read(Byte.Register.A));
        assertEquals(0xff, cpu.read(Byte.Register.H));
        assertEquals(0x53, cpu.read(Byte.Register.L));
    }

    @Test
    public void test_ldd_a_hl_uses_8_cycles() {
        Cpu cpu = runProgram(0x3a);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_ldd_a_hl_doesnt_set_flags() {
        Cpu cpu = runProgram(
                0x2e, 0x01,       // Set HL = 0x0001.
                0x3a              // LDD A, (HL)
        );
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
    }

    @Test
    public void test_ldd_a_hl_byte_rollover() {
        Cpu cpu = runProgram(
                0x26, 0xff,
                0x2e, 0x00,       // Set HL = 0xff00.
                0x3a              // LDD A, (HL)
        );
        assertEquals(0xfe, cpu.read(Byte.Register.H));
        assertEquals(0xff, cpu.read(Byte.Register.L));
    }

    @Test
    public void test_ldd_a_hl_word_rollover() {
        Cpu cpu = runProgram(
                0x26, 0x00,
                0x2e, 0x00,       // Set HL = 0x0000.
                0x3a              // LDD A, (HL)
        );
        assertEquals(0xff, cpu.read(Byte.Register.H));
        assertEquals(0xff, cpu.read(Byte.Register.L));
    }

    @Test
    public void test_ldd_hl_a() {
        Cpu cpu = runProgram(
                0x3c,       // INC A
                0x26, 0xab, // LD H, 0xab
                0x2e, 0xcd, // LD L, 0xcd
                0x32        // LDD (HL), A
        );

        assertEquals(0x01, cpu.unsafeRead(0xabcd));
        assertEquals(0xab, cpu.read(Byte.Register.H));
        assertEquals(0xcc, cpu.read(Byte.Register.L));
    }

    @Test
    public void test_ldd_hl_a_uses_8_cycles() {
        Cpu cpu = runProgram(0x32);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_ldd_hl_a_byte_rollover() {
        Cpu cpu = runProgram(
                0x26, 0xff,
                0x2e, 0x00,       // Set HL = 0xff00.
                0x32              // LDD (HL), A
        );
        assertEquals(0xfe, cpu.read(Byte.Register.H));
        assertEquals(0xff, cpu.read(Byte.Register.L));
    }

    @Test
    public void test_ldd_hl_a_word_rollover() {
        Cpu cpu = runProgram(
                0x26, 0x00,
                0x2e, 0x00,       // Set HL = 0x0000.
                0x32              // LDD (HL), A
        );
        assertEquals(0xff, cpu.read(Byte.Register.H));
        assertEquals(0xff, cpu.read(Byte.Register.L));
    }

    @Test
    public void test_ldd_hl_a_doesnt_set_flags() {
        Cpu cpu = runProgram(
                0x2e, 0x01,       // Set HL = 0x0001.
                0x32              // LDD A, (HL)
        );
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
    }

    @Test
    public void test_ldi_a_hl() {
        Cpu cpu = runProgram(
                0x26, 0xef,
                0x2e, 0x56,       // Set HL = 0xef56.
                0x36, 0xcd,       // Store 0xcd at 0xef56.
                0x2a              // Move 0xcd to A and inc HL.
        );

        assertEquals(0xcd, cpu.read(Byte.Register.A));
        assertEquals(0xef, cpu.read(Byte.Register.H));
        assertEquals(0x57, cpu.read(Byte.Register.L));
    }

    @Test
    public void test_ldi_a_hl_uses_8_cycles() {
        Cpu cpu = runProgram(0x2a);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_ldi_a_hl_doesnt_set_flags() {
        Cpu cpu = runProgram(
                0x26, 0xff,       // S
                0x2e, 0xff,       // Set HL = 0xffff.
                0x2a              // LDI A, (HL)
        );
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
    }

    @Test
    public void test_ldi_a_hl_byte_rollover() {
        Cpu cpu = runProgram(
                0x26, 0x00,
                0x2e, 0xff,       // Set HL = 0x00ff.
                0x2a              // LDI A, (HL)
        );
        assertEquals(0x01, cpu.read(Byte.Register.H));
        assertEquals(0x00, cpu.read(Byte.Register.L));
    }

    @Test
    public void test_ldi_a_hl_word_rollover() {
        Cpu cpu = runProgram(
                0x26, 0xff,
                0x2e, 0xff,       // Set HL = 0xffff.
                0x2a              // LDI A, (HL)
        );
        assertEquals(0x00, cpu.read(Byte.Register.H));
        assertEquals(0x00, cpu.read(Byte.Register.L));
    }

    @Test
    public void test_ldi_hl_a() {
        Cpu cpu = runProgram(
                0x3c,       // INC A
                0x26, 0xab, // LD H, 0xab
                0x2e, 0xcd, // LD L, 0xcd
                0x22        // LDI (HL), A
        );

        assertEquals(0x01, cpu.unsafeRead(0xabcd));
        assertEquals(0xab, cpu.read(Byte.Register.H));
        assertEquals(0xce, cpu.read(Byte.Register.L));
    }

    @Test
    public void test_ldi_hl_a_uses_8_cycles() {
        Cpu cpu = runProgram(0x22);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_ldi_hl_a_byte_rollover() {
        Cpu cpu = runProgram(
                0x26, 0x00,
                0x2e, 0xff,       // Set HL = 0x00ff.
                0x22              // LDI (HL), A
        );
        assertEquals(0x01, cpu.read(Byte.Register.H));
        assertEquals(0x00, cpu.read(Byte.Register.L));
    }

    @Test
    public void test_ldi_hl_a_word_rollover() {
        Cpu cpu = runProgram(
                0x26, 0xff,
                0x2e, 0xff,       // Set HL = 0xffff.
                0x22              // LDI (HL), A
        );
        assertEquals(0x00, cpu.read(Byte.Register.H));
        assertEquals(0x00, cpu.read(Byte.Register.L));
    }

    @Test
    public void test_ldi_hl_a_doesnt_set_flags() {
        Cpu cpu = runProgram(
                0x26, 0xff,
                0x2e, 0xff,       // Set HL = 0xffff.
                0x22              // LDI A, (HL)
        );
        assertEquals(0x00, cpu.read(Byte.Register.F)); // Assert flags all clear.
    }

    @Test
    public void test_load_a_to_address() {
        Cpu cpu = runProgram(
                0x3e, 0x64, // LD A, 0x64
                0xe0, 0x34  // LD (0xff34), 0x64
        );
        assertEquals(0x64, cpu.unsafeRead(0xff34));
    }

    @Test
    public void test_load_a_to_address_uses_12_cycles() {
        Cpu cpu = runProgram(0xe0, 0xff);
        assertEquals(12, cpu.getCycles());
    }

    @Test
    public void test_load_address_to_a() {
        Cpu cpu = runProgram(
                0x0e, 0x46, // LD C, 0x46
                0x26, 0xff, // LD H, 0xff
                0x2e, 0x34, // LD L, 0x34
                0x71,       // LD (HL), C
                0xf0, 0x34  // LD A, (0xff34)
        );

        assertEquals(0x46, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_load_address_to_a_uses_12_cycles() {
        Cpu cpu = runProgram(0xf0, 0x01);
        assertEquals(12, cpu.getCycles());
    }

    @Test
    public void test_direct_load_to_bc() {
        Cpu cpu = runProgram(0x01, 0x34, 0x56);
        assertEquals(0x56, cpu.read(Byte.Register.B));
        assertEquals(0x34, cpu.read(Byte.Register.C));
        assertEquals(12, cpu.getCycles());
    }

    @Test
    public void test_direct_load_to_de() {
        Cpu cpu = runProgram(0x11, 0x10, 0x20);
        assertEquals(0x20, cpu.read(Byte.Register.D));
        assertEquals(0x10, cpu.read(Byte.Register.E));
        assertEquals(12, cpu.getCycles());
    }

    @Test
    public void test_direct_load_to_hl() {
        Cpu cpu = runProgram(0x21, 0x66, 0x77);
        assertEquals(0x77, cpu.read(Byte.Register.H));
        assertEquals(0x66, cpu.read(Byte.Register.L));
        assertEquals(12, cpu.getCycles());
    }

    @Test
    public void test_direct_load_to_sp() {
        Cpu cpu = runProgram(0x31, 0x12, 0x6e);
        assertEquals(0x6e12, cpu.read(Word.Register.SP));
        assertEquals(12, cpu.getCycles());
    }

    @Test
    public void test_copy_hl_to_sp() {
        Cpu cpu = runProgram(
                0x21, 0x76, 0x98, // Set HL=0x9876
                0xf9              // LD SP, HL
        );
        assertEquals(0x9876, cpu.read(Word.Register.SP));
    }

    @Test
    public void test_copy_hl_to_sp_uses_8_cycles() {
        Cpu cpu = runProgram(0xf9);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_ldhl_sp_with_no_offset() {
        Cpu cpu = runProgram(
                0x31, 0x55, 0x34, // LD SP, 0x3455
                0xf8, 0x00        // LD HL, SP + 0x00
        );

        assertEquals(0x3455, cpu.read(Word.Register.HL));
    }

    @Test
    public void test_ldhl_sp_n_uses_12_cycles() {
        Cpu cpu = runProgram(0xf8);
        assertEquals(12, cpu.getCycles());
    }

    @Test
    public void test_ldhl_sp_with_offset() {
        Cpu cpu = runProgram(
                0x31, 0x55, 0x34, // LD SP, 0x3455
                0xf8, 0x77        // LD HL, SP + 0x00
        );
        assertEquals(0x34cc, cpu.read(Word.Register.HL));
    }

    @Test
    public void test_ldhl_sp_resets_zero() {
        Cpu cpu = runProgram(
                0x21, 0xff, 0xff, // LD L, 0xffff
                0x2c,             // INC L (triggers ZERO flag)
                0xf8, 0x00        // LD HL, SP+0x00
        );
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_ldhl_sp_never_sets_zero() {
        Cpu cpu = runProgram(
                0xf8, 0x00        // LD HL, SP+0x00
        );
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_ldhl_sp_sets_carry_flag_on_carry() {
        Cpu cpu = runProgram(
                0x31, 0xff, 0x00, // LD SP, 0x00ff
                0xf8, 0x01        // LD HL, SP + 0x01
        );
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_ldhl_sp_sets_carry_flag_when_bit_8_is_already_high() {
        Cpu cpu = runProgram(
                0x31, 0xff, 0x01, // LD SP, 0x01ff
                0xf8, 0x01        // LD HL, SP + 0x01
        );
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_ldhl_sp_doesnt_set_carry_flag_for_every_nonzero_offset() {
        Cpu cpu = runProgram(0xf8, 0xff);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_ldhl_sp_doesnt_set_carry_flag_when_change_is_due_to_sp_bits() {
        Cpu cpu = runProgram(
                0x31, 0x00, 0x01, // LD SP, 0x0100
                0xf8, 0x01        // LD HL, SP + 0x01
        );
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_ldhl_sp_sets_nibble_flag_on_nibble_carry() {
        Cpu cpu = runProgram(
                0x31, 0x0f, 0x00, // LD SP, 0x000f
                0xf8, 0x01        // LD HL, SP + 0x01
        );
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_ldhl_sp_doesnt_just_set_nibble_flag_whenever_bit_4_is_high() {
        Cpu cpu = runProgram(
                0x31, 0x37, 0x00, // LD SP, 0x0037
                0xf8, 0x44        // LD HL, SP + 0x44
        );
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_write_sp_to_nn() {
        Cpu cpu = runProgram(
                0x31, 0xbb, 0xaa, // LD SP, 0xaabb
                0x08, 0xdd, 0xcc  // LD (0xccdd), SP
        );
        assertEquals(0xbb, cpu.unsafeRead(0xccdd));
        assertEquals(0xaa, cpu.unsafeRead(0xccde));
    }

    @Test
    public void test_write_sp_to_nn_uses_20_cycles() {
        Cpu cpu = runProgram(0x08, 0x00, 0x01);
        assertEquals(20, cpu.getCycles());
    }

    @Test
    public void test_flag_register_is_00_when_empty() {
        Cpu cpu = cpuWithProgram();
        cpu.set(Flag.ZERO, false);
        cpu.set(Flag.OPERATION, false);
        cpu.set(Flag.NIBBLE, false);
        cpu.set(Flag.CARRY, false);
        assertEquals(0x00, cpu.read(Byte.Register.F));
    }

    @Test
    public void test_flag_register_incorporates_zero_flag() {
        Cpu cpu = cpuWithProgram();
        cpu.set(Flag.ZERO, true);
        cpu.set(Flag.OPERATION, false);
        cpu.set(Flag.NIBBLE, false);
        cpu.set(Flag.CARRY, false);
        assertEquals(0x80, cpu.read(Byte.Register.F));
    }

    @Test
    public void test_flag_register_incorporates_nibble_flag() {
        Cpu cpu = cpuWithProgram();
        cpu.set(Flag.ZERO, false);
        cpu.set(Flag.OPERATION, false);
        cpu.set(Flag.NIBBLE, true);
        cpu.set(Flag.CARRY, false);
        assertEquals(0x20, cpu.read(Byte.Register.F));
    }

    @Test
    public void test_flag_register_incorporates_operation_flag() {
        Cpu cpu = cpuWithProgram();
        cpu.set(Flag.ZERO, false);
        cpu.set(Flag.OPERATION, true);
        cpu.set(Flag.NIBBLE, false);
        cpu.set(Flag.CARRY, false);
        assertEquals(0x40, cpu.read(Byte.Register.F));
    }

    @Test
    public void test_flag_register_incorporates_carry_flag() {
        Cpu cpu = cpuWithProgram();
        cpu.set(Flag.ZERO, false);
        cpu.set(Flag.OPERATION, false);
        cpu.set(Flag.NIBBLE, false);
        cpu.set(Flag.CARRY, true);
        assertEquals(0x10, cpu.read(Byte.Register.F));
    }

    @Test
    public void test_flag_register_with_two_flags() {
        Cpu cpu = cpuWithProgram();
        cpu.set(Flag.ZERO, true);
        cpu.set(Flag.OPERATION, true);
        cpu.set(Flag.NIBBLE, false);
        cpu.set(Flag.CARRY, false);
        assertEquals(0xc0, cpu.read(Byte.Register.F));
    }

    @Test
    public void test_flag_register_with_all_flags() {
        Cpu cpu = cpuWithProgram();
        cpu.set(Flag.ZERO, true);
        cpu.set(Flag.OPERATION, true);
        cpu.set(Flag.NIBBLE, true);
        cpu.set(Flag.CARRY, true);
        assertEquals(0xf0, cpu.read(Byte.Register.F));
    }

    @Test
    public void test_push_af() {
        Cpu cpu = cpuWithProgram(
                0x31, 0x34, 0x12, // LD SP, 0x1234
                0x3e, 0xab,       // LD A, 0xab
                0xf5              // PUSH AF
        );
        cpu.set(Flag.NIBBLE, true);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 6);
        assertEquals(0xab, cpu.unsafeRead(0x1233));
        assertEquals(0x30, cpu.unsafeRead(0x1232));
        assertEquals(0x1232, cpu.read(Word.Register.SP));
    }

    @Test
    public void test_push_af_uses_16_cycles() {
        Cpu cpu = runProgram(0xf5);
        assertEquals(16, cpu.getCycles());
    }

    @Test
    public void test_push_bc() {
        Cpu cpu = runProgram(
                0x01, 0x98, 0xa7, // LD BC, 0xa798
                0x31, 0x17, 0x43, // LD SP, 0x4317
                0xc5              // PUSH BC
        );
        assertEquals(0xa7, cpu.unsafeRead(0x4316));
        assertEquals(0x98, cpu.unsafeRead(0x4315));
        assertEquals(0x4315, cpu.read(Word.Register.SP));
    }

    @Test
    public void test_push_bc_uses_16_cycles() {
        Cpu cpu = runProgram(0xc5);
        assertEquals(16, cpu.getCycles());
    }

    @Test
    public void test_push_de() {
        Cpu cpu = runProgram(
                0x11, 0x5d, 0x39, // LD DE, 0x395d
                0x31, 0x3f, 0x78, // LD SP, 0x783f
                0xd5              // PUSH DE
        );
        assertEquals(0x39, cpu.unsafeRead(0x783e));
        assertEquals(0x5d, cpu.unsafeRead(0x783d));
        assertEquals(0x783d, cpu.read(Word.Register.SP));
    }

    @Test
    public void test_push_de_uses_16_cycles() {
        Cpu cpu = runProgram(0xd5);
        assertEquals(16, cpu.getCycles());
    }

    @Test
    public void test_push_hl() {
        Cpu cpu = runProgram(
                0x21, 0xaf, 0x64, // LD HL, 0x64af
                0x31, 0x9f, 0xa8, // LD SP, 0xa89f
                0xe5              // PUSH HL
        );
        assertEquals(0x64, cpu.unsafeRead(0xa89e));
        assertEquals(0xaf, cpu.unsafeRead(0xa89d));
        assertEquals(0xa89d, cpu.read(Word.Register.SP));
    }

    @Test
    public void test_push_hl_uses_16_cycles() {
        Cpu cpu = runProgram(0xe5);
        assertEquals(16, cpu.getCycles());
    }

    @Test
    public void test_pop_af() {
        Cpu cpu = runProgram(
            0x31, 0xc2, 0xb8, // LD SP, 0xb8c2
            0x08, 0xb4, 0x3d, // LD (0x3db4), SP
            0x31, 0xb4, 0x3d, // LD SP, 0x3db4
            0xf1              // POP AF
        );
       assertEquals(0xb8, cpu.read(Byte.Register.A));
       assertEquals(0xc2, cpu.read(Byte.Register.F));
    }

    @Test
    public void test_pop_af_uses_12_cycles() {
        Cpu cpu = runProgram(0xf1);
        assertEquals(12, cpu.getCycles());
    }

    @Test
    public void test_pop_bc() {
        Cpu cpu = runProgram(
                0x31, 0x38, 0xdc, // LD SP, 0xdc38
                0x08, 0x80, 0x18, // LD (0x1880), SP
                0x31, 0x80, 0x18, // LD SP, 0x1880
                0xc1              // POP BC
        );
        assertEquals(0xdc, cpu.read(Byte.Register.B));
        assertEquals(0x38, cpu.read(Byte.Register.C));
    }

    @Test
    public void test_pop_bc_uses_12_cycles() {
        Cpu cpu = runProgram(0xc1);
        assertEquals(12, cpu.getCycles());
    }

    @Test
    public void test_pop_de() {
        Cpu cpu = runProgram(
                0x31, 0xd8, 0x8a, // LD SP, 0x8ad8
                0x08, 0xac, 0x5f, // LD (0x5fac), SP
                0x31, 0xac, 0x5f, // LD SP, 0x5fac
                0xd1              // POP DE
        );
        assertEquals(0x8a, cpu.read(Byte.Register.D));
        assertEquals(0xd8, cpu.read(Byte.Register.E));
    }

    @Test
    public void test_pop_de_uses_12_cycles() {
        Cpu cpu = runProgram(0xd1);
        assertEquals(12, cpu.getCycles());
    }

    @Test
    public void test_pop_hl() {
        Cpu cpu = runProgram(
                0x31, 0x91, 0x45, // LD SP, 0x4591
                0x08, 0x1b, 0x73, // LD (0x731b), SP
                0x31, 0x1b, 0x73, // LD SP, 0x731b
                0xe1              // POP HL
        );
        assertEquals(0x45, cpu.read(Byte.Register.H));
        assertEquals(0x91, cpu.read(Byte.Register.L));
    }

    @Test
    public void test_pop_hl_uses_12_cycles() {
        Cpu cpu = runProgram(0xe1);
        assertEquals(12, cpu.getCycles());
    }

    @Test
    public void test_add_a_to_a_when_empty_gives_0() {
        Cpu cpu = runProgram(0x87);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_add_a_to_a_when_a_is_1_gives_2() {
        Cpu cpu = runProgram(0x3e, 0x01, 0x87);
        assertEquals(0x02, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_add_a_to_a_when_a_is_0x37_gives_0x6e() {
        Cpu cpu = runProgram(0x3e, 0x37, 0x87);
        assertEquals(0x6e, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_add_a_to_a_uses_4_cycles() {
        Cpu cpu = runProgram(0x87);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_adding_a_to_a_rolls_over_on_byte_overflow() {
        Cpu cpu = runProgram(0x3e, 0x90, 0x87);
        assertEquals(0x20, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_adding_a_to_a_sets_zero_flag_if_a_is_zero() {
        Cpu cpu = runProgram(0x87);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_adding_a_to_a_sets_zero_flag_if_result_overflows_to_0x00() {
        Cpu cpu = runProgram(0x3e, 0x80, 0x87);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_adding_a_to_a_doesnt_always_set_zero_flag() {
        Cpu cpu = runProgram(0x3e, 0x81, 0x87);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_adding_a_to_a_resets_operation_flag() {
        Cpu cpu = cpuWithProgram(0x87);
        cpu.set(Flag.OPERATION, true);
        runProgram(cpu, 1);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_adding_a_to_a_sets_carry_flag_on_overflow_to_zero() {
        Cpu cpu = runProgram(0x3e, 0x80, 0x87);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_adding_a_to_a_sets_carry_flag_on_overflow_past_zero() {
        Cpu cpu = runProgram(0x3e, 0x81, 0x87);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_adding_a_to_a_doesnt_always_set_carry_flag() {
        Cpu cpu = runProgram(0x3e, 0x7f, 0x87);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_adding_a_to_a_sets_nibble_flag_in_simplest_case() {
        Cpu cpu = runProgram(0x3e, 0x08, 0x87);
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_adding_a_to_a_sets_nibble_flag_despite_byte_overflow() {
        Cpu cpu = runProgram(0x3e, 0xff, 0x87);
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_adding_a_to_a_doesnt_set_nibble_flag_on_every_byte_overflow() {
        Cpu cpu = runProgram(0x3e, 0x80, 0x87);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_add_b_to_a_when_both_empty_gives_0() {
        Cpu cpu = runProgram(0x80);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_add_b_to_a_when_a_is_1_and_b_is_2_gives_3() {
        Cpu cpu = runProgram(0x3e, 0x01, 0x06, 0x02, 0x80);
        assertEquals(0x03, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_adding_b_to_a_doesnt_change_b() {
        Cpu cpu = runProgram(0x3e, 0x01, 0x06, 0x02, 0x80);
        assertEquals(0x02, cpu.read(Byte.Register.B));
    }

    @Test
    public void test_add_b_to_a_when_a_is_0x37_and_b_is_0x42_gives_0x79() {
        Cpu cpu = runProgram(0x3e, 0x37, 0x06, 0x42, 0x80);
        assertEquals(0x79, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_add_b_to_a_uses_4_cycles() {
        Cpu cpu = runProgram(0x80);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_adding_b_to_a_rolls_over_on_byte_overflow() {
        Cpu cpu = runProgram(0x3e, 0x81, 0x06, 0x87, 0x80);
        assertEquals(0x08, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_adding_b_to_a_sets_zero_flag_if_both_are_zero() {
        Cpu cpu = runProgram(0x80);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_adding_b_to_a_sets_zero_flag_if_result_overflows_to_0x00() {
        Cpu cpu = runProgram(0x3e, 0xaf, 0x06, 0x51, 0x80);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_adding_b_to_a_doesnt_always_set_zero_flag() {
        Cpu cpu = runProgram(0x3e, 0x81, 0x06, 0x82, 0x80);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_adding_b_to_a_resets_operation_flag() {
        Cpu cpu = cpuWithProgram(0x80);
        cpu.set(Flag.OPERATION, true);
        runProgram(cpu, 1);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_adding_b_to_a_sets_carry_flag_on_overflow_to_zero() {
        Cpu cpu = runProgram(0x3e, 0xaf, 0x06, 0x51, 0x80);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_adding_b_to_a_sets_carry_flag_on_overflow_past_zero() {
        Cpu cpu = runProgram(0x3e, 0xaf, 0x06, 0x52, 0x80);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_adding_b_to_a_doesnt_always_set_carry_flag() {
        Cpu cpu = runProgram(0x3e, 0x7f, 0x06, 0x7f, 0x80);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_adding_b_to_a_sets_nibble_flag_in_simplest_case() {
        Cpu cpu = runProgram(0x3e, 0x08, 0x06, 0x08, 0x80);
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_adding_b_to_a_sets_nibble_flag_on_chained_carry() {
        Cpu cpu = runProgram(0x3e, 0x0f, 0x06, 0x01, 0x80);
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_adding_b_to_a_sets_nibble_flag_on_chained_carry_2() {
        Cpu cpu = runProgram(0x3e, 0x01, 0x06, 0x0f, 0x80);
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_adding_b_to_a_sets_nibble_flag_despite_byte_overflow() {
        Cpu cpu = runProgram(0x3e, 0xff, 0x06, 0x02, 0x80);
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_adding_b_to_a_doesnt_set_nibble_flag_on_every_byte_overflow() {
        Cpu cpu = runProgram(0x3e, 0x80, 0x06, 0x8f, 0x80);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_add_c_to_a() {
        Cpu cpu = runProgram(0x3e, 0x62, 0x0e, 0xc0, 0x81);
        assertEquals(0x22, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_add_d_to_a() {
        Cpu cpu = runProgram(0x3e, 0x2b, 0x16, 0xe3, 0x82);
        assertEquals(0x0e, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_add_e_to_a() {
        Cpu cpu = runProgram(0x3e, 0x78, 0x1e, 0xfb, 0x83);
        assertEquals(0x73, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_add_h_to_a() {
        Cpu cpu = runProgram(0x3e, 0x8d, 0x26, 0x64, 0x84);
        assertEquals(0xf1, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_add_l_to_a() {
        Cpu cpu = runProgram(0x3e, 0x22, 0x2e, 0x6d, 0x85);
        assertEquals(0x8f, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_add_indirect_hl_to_a_when_hl_points_to_0x00() {
        Cpu cpu = runProgram(
            0x21, 0xcd, 0xab, // LD HL, 0xabcd
            0x3e, 0xef,       // LD A, 0xef
            0x86              // ADD A, (HL)
        );
        assertEquals(0xef, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_add_indirect_hl_to_a_when_hl_points_to_small_value() {
        Cpu cpu = runProgram(
                0x21, 0x71, 0x86, // LD HL, 0x9671
                0x36, 0x19,       // LD (HL), 0x19
                0x3e, 0xb3,       // LD A, 0xb3
                0x86              // ADD A, (HL)
        );
        assertEquals(0xcc, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_adding_indirect_hl_to_a_doesnt_change_memory() {
        Cpu cpu = runProgram(
                0x21, 0x18, 0x7b, // LD HL, 0x7b18
                0x36, 0xcc,       // LD (HL), 0xcc
                0x3e, 0xff,       // LD A, 0xff
                0x86              // ADD A, (HL)
        );
        assertEquals(0xcc, cpu.unsafeRead(0x7b18));
    }

    @Test
    public void test_adding_indirect_hl_to_a_uses_8_cycles() {
        Cpu cpu = runProgram(0x86);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_adding_indirect_hl_to_a_rolls_over_on_byte_overflow() {
        Cpu cpu = runProgram(
                0x21, 0x05, 0x63, // LD HL, 0x6305
                0x36, 0x81,       // LD (HL), 0x81
                0x3e, 0x87,       // LD A, 0x87
                0x86              // ADD A, (HL)
        );
        assertEquals(0x08, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_adding_indirect_hl_to_a_sets_zero_flag_if_both_are_zero() {
        Cpu cpu = runProgram(0x21, 0xab, 0xcd, 0x86);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_adding_indirect_hl_to_a_sets_zero_flag_if_result_overflows_to_0x00() {
        Cpu cpu = runProgram(
                0x21, 0x79, 0xde, // LD HL, 0xde79
                0x36, 0xaf,       // LD (HL), 0xaf
                0x3e, 0x51,       // LD A, 0x51
                0x86              // ADD A, (HL)
        );
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_adding_indirect_hl_to_a_doesnt_always_set_zero_flag() {
        Cpu cpu = runProgram(
                0x21, 0xbd, 0x33, // LD HL, 0x33bd
                0x36, 0x81,       // LD (HL), 0x81
                0x3e, 0x82,       // LD A, 0x82
                0x86              // ADD A, (HL)
        );
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_adding_indirect_hl_to_a_resets_operation_flag() {
        Cpu cpu = cpuWithProgram(0x86);
        cpu.set(Flag.OPERATION, true);
        runProgram(cpu, 1);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_adding_indirect_hl_to_a_sets_carry_flag_on_overflow_to_zero() {
        Cpu cpu = runProgram(
                0x21, 0x57, 0x18, // LD HL, 0x1857
                0x36, 0xaf,       // LD (HL), 0xaf
                0x3e, 0x51,       // LD A, 0x51
                0x86              // ADD A, (HL)
        );
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_adding_indirect_hl_to_a_sets_carry_flag_on_overflow_past_zero() {
        Cpu cpu = runProgram(
                0x21, 0x66, 0xf1, // LD HL, 0xf166
                0x36, 0xaf,       // LD (HL), 0xaf
                0x3e, 0x52,       // LD A, 0x52
                0x86              // ADD A, (HL)
        );
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_adding_indirect_hl_to_a_doesnt_always_set_carry_flag() {
        Cpu cpu = runProgram(
                0x21, 0x87, 0xf9, // LD HL, 0xf987
                0x36, 0x7f,       // LD (HL), 0x7f
                0x3e, 0x7f,       // LD A, 0x7f
                0x86              // ADD A, (HL)
        );
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_adding_indirect_hl_to_a_sets_nibble_flag_in_simplest_case() {
        Cpu cpu = runProgram(
                0x21, 0x53, 0xfc, // LD HL, 0xfc53
                0x36, 0x08,       // LD (HL), 0x08
                0x3e, 0x08,       // LD A, 0x08
                0x86              // ADD A, (HL)
        );
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_adding_indirect_hl_to_a_sets_nibble_flag_on_chained_carry() {
        Cpu cpu = runProgram(
                0x21, 0xe0, 0x43, // LD HL, 0x43e0
                0x36, 0x0f,       // LD (HL), 0x0f
                0x3e, 0x01,       // LD A, 0x01
                0x86              // ADD A, (HL)
        );
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_adding_indirect_hl_to_a_sets_nibble_flag_on_chained_carry_2() {
        Cpu cpu = runProgram(
                0x21, 0xa9, 0x6e, // LD HL, 0x6ea9
                0x36, 0x01,       // LD (HL), 0x01
                0x3e, 0x0f,       // LD A, 0x0f
                0x86              // ADD A, (HL)
        );
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_adding_indirect_hl_to_a_sets_nibble_flag_despite_byte_overflow() {
        Cpu cpu = runProgram(
                0x21, 0xb1, 0x2b, // LD HL, 0x2bb1
                0x36, 0xff,       // LD (HL), 0xff
                0x3e, 0x02,       // LD A, 0x02
                0x86              // ADD A, (HL)
        );
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_adding_argument_to_a() {
        Cpu cpu = runProgram(
                0x3e, 0x69,
                0xc6, 0xd2
        );
        assertEquals(0x3b, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_adding_argument_to_a_uses_8_cycles() {
        Cpu cpu = runProgram(0xc6);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_adding_argument_a_sets_zero_flag_if_both_are_zero() {
        Cpu cpu = runProgram(0xc6, 0x00);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_adding_argument_to_a_sets_zero_flag_if_result_overflows_to_0x00() {
        Cpu cpu = runProgram(
                0x3e, 0xd1,
                0xc6, 0x2f
        );
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_adding_argument_to_a_doesnt_always_set_zero_flag() {
        Cpu cpu = runProgram(
                0x3e, 0x81,
                0xc6, 0x82
        );
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_adding_argument_to_a_resets_operation_flag() {
        Cpu cpu = cpuWithProgram(0xc6, 0x00);
        cpu.set(Flag.OPERATION, true);
        runProgram(cpu, 1);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_adding_argument_to_a_sets_carry_flag_on_overflow_to_zero() {
        Cpu cpu = runProgram(
                0x3e, 0x6e,
                0xc6, 0x92
        );
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_adding_argument_to_a_sets_carry_flag_on_overflow_past_zero() {
        Cpu cpu = runProgram(
               0x3e, 0x6e,
               0xc6, 0x93
        );
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_adding_argument_to_a_doesnt_always_set_carry_flag() {
        Cpu cpu = runProgram(
                0x3e, 0x7f,
                0xc6, 0x7f
        );
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_adding_argument_to_a_sets_nibble_flag_in_simplest_case() {
        Cpu cpu = runProgram(
                0x3e, 0x08,
                0xc6, 0x08
        );
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_adding_argument_to_a_sets_nibble_flag_on_chained_carry() {
        Cpu cpu = runProgram(
                0x3e, 0x01,
                0xc6, 0x0f
        );
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_adding_argument_to_a_sets_nibble_flag_on_chained_carry_2() {
        Cpu cpu = runProgram(
                0x3e, 0x0f,
                0xc6, 0x01
        );
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_adding_argument_to_a_sets_nibble_flag_despite_byte_overflow() {
        Cpu cpu = runProgram(
                0x3e, 0xff,
                0xc6, 0x02
        );
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_add_a_to_itself_with_carry_when_a_is_0x00_and_carry_is_unset() {
        Cpu cpu = runProgram(0x8f);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_add_a_to_itself_with_carry_when_a_is_0x01_and_carry_is_unset() {
        Cpu cpu = runProgram(0x3c, 0x8f);
        assertEquals(0x02, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_add_a_to_itself_with_carry_when_a_is_0x00_and_carry_is_set() {
        Cpu cpu = cpuWithProgram(0x8f);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 1);
        assertEquals(0x01, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_add_a_to_itself_with_carry_when_a_is_0x01_and_carry_is_set() {
        Cpu cpu = cpuWithProgram(0x3c, 0x8f);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 2);
        assertEquals(0x03, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_adding_a_to_itself_with_carry_uses_4_cycles() {
        Cpu cpu = runProgram(0x8f);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_rollover_adding_a_to_itself_with_carry_when_carry_is_not_set() {
        Cpu cpu = runProgram(0x3e, 0x80, 0x8f);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_rollover_adding_a_to_itself_with_carry_when_carry_is_set() {
        Cpu cpu = cpuWithProgram(0x3e, 0x80, 0x8f);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertEquals(0x01, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_adding_a_to_itself_with_carry_resets_operation_flag() {
        Cpu cpu = cpuWithProgram(0x8f);
        cpu.set(Flag.OPERATION, true);
        runProgram(cpu, 1);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_adding_a_to_itself_with_carry_when_a_is_zero_and_carry_is_not_set_sets_zero_flag() {
        Cpu cpu = runProgram(0x8f);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_adding_a_to_itself_with_carry_when_a_is_0x01_and_carry_is_not_set_resets_zero_flag() {
        Cpu cpu = runProgram(
                0x8f,       // ADC A, A    (Set zero flag)
                0x3e, 0x01, // LD  A, 0x01
                0x8f        // ADC A, A
        );
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_adding_a_to_itself_with_carry_when_a_is_zero_and_carry_is_set_resets_zero_flag() {
        Cpu cpu = runProgram(
                0x3e, 0x80, 0x87, // (Set carry and zero flags)
                0x3e, 0x01,       // LD  A, 0x01
                0x8f              // ADC A, A
        );
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_adding_a_to_itself_with_carry_sets_carry_flag_on_overflow_to_zero() {
        Cpu cpu = runProgram(0x3e, 0x80, 0x8f);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_adding_a_to_itself_with_carry_sets_carry_flag_on_overflow_past_zero_when_carry_is_not_set() {
        Cpu cpu = runProgram(0x3e, 0x81, 0x8f);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_adding_a_to_itself_with_carry_sets_carry_flag_on_overflow_past_zero_when_carry_is_set() {
        Cpu cpu = runProgram(
                0x3e, 0x80, 0x87, // (Set carry flag)
                0x3e, 0x80,       // LD  A, 0x80
                0x8f              // LDC A, A
        );
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_adding_a_to_itself_with_carry_resets_carry_flag_if_no_carry_occurs() {
        Cpu cpu = runProgram(
                0x3e, 0x80, 0x87, // (Set carry flag)
                0x3e, 0x4f,       // LD  A, 0x4f
                0x8f              // LDC A, A
        );
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_adding_a_to_itself_with_carry_sets_nibble_flag_in_simplest_case() {
        Cpu cpu = runProgram(0x3e, 0x08, 0x8f);
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_adding_a_to_itself_with_carry_resets_nibble_flag_when_no_nibble_carry_occurs() {
        Cpu cpu = runProgram(0x3e, 0x08, 0x87, 0x8f);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_adding_b_to_a_with_carry() {
        Cpu cpu = runProgram(
                0x3e, 0x80, 0x87, // (Set carry)
                0x3e, 0x12,       // LD A, 0x12
                0x06, 0x34,       // LD B, 0x34
                0x88              // ADC A, B
        );
        assertEquals(0x47, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_adding_b_to_a_with_carry_doesnt_modify_b() {
        Cpu cpu = runProgram(
                0x3e, 0x80, 0x87, // (Set carry)
                0x3e, 0xab,       // LD A, 0xab
                0x06, 0xcd,       // LD B, 0xcd
                0x88              // ADC A, B
        );
        assertEquals(0xcd, cpu.read(Byte.Register.B));
    }

    @Test
    public void test_adding_b_to_a_with_carry_can_trigger_carry_due_to_carry_bit() {
        Cpu cpu = runProgram(
                0x3e, 0x80, 0x87, // (Set carry)
                0x3e, 0xff,       // LD A, 0xff
                0x06, 0x00,       // LD B, 0x00
                0x88              // ADC A, B
        );
        assertEquals(0x00, cpu.read(Byte.Register.A));
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_adding_b_to_a_with_carry_can_trigger_nibble_carry_due_to_carry_bit() {
        Cpu cpu = runProgram(
                0x3e, 0x80, 0x87, // (Set carry)
                0x3e, 0x07,       // LD A, 0x07
                0x06, 0x08,       // LD B, 0x08
                0x88              // ADC A, B
        );
        assertEquals(0x10, cpu.read(Byte.Register.A));
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_adding_b_to_a_with_carry_uses_4_cycles() {
        Cpu cpu = runProgram(0x88);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_adding_c_to_a_with_carry() {
        Cpu cpu = runProgram(
                0x3e, 0x80, 0x87, // (Set carry)
                0x3e, 0x57,       // LD A, 0x57
                0x0e, 0xb6,       // LD C, 0xb6
                0x89              // ADC A, C
        );
        assertEquals(0x0e, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_adding_c_to_a_with_carry_uses_4_cycles() {
        Cpu cpu = runProgram(0x89);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_adding_d_to_a_with_carry() {
        Cpu cpu = runProgram(
                0x3e, 0x80, 0x87, // (Set carry)
                0x3e, 0x2f,       // LD A, 0x2f
                0x16, 0xeb,       // LD D, 0xeb
                0x8a              // ADC A, D
        );
        assertEquals(0x1b, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_adding_d_to_a_with_carry_uses_4_cycles() {
        Cpu cpu = runProgram(0x8a);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_adding_e_to_a_with_carry() {
        Cpu cpu = runProgram(
                0x3e, 0x80, 0x87, // (Set carry)
                0x3e, 0x73,       // LD A, 0x73
                0x1e, 0x9d,       // LD E, 0x9d
                0x8b              // ADC A, E
        );
        assertEquals(0x11, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_adding_e_to_a_with_carry_uses_4_cycles() {
        Cpu cpu = runProgram(0x8b);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_adding_h_to_a_with_carry() {
        Cpu cpu = runProgram(
                0x3e, 0x80, 0x87, // (Set carry)
                0x3e, 0xc3,       // LD A, 0xc3
                0x26, 0xc1,       // LD H, 0xc1
                0x8c              // ADC A, H
        );
        assertEquals(0x85, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_adding_h_to_a_with_carry_uses_4_cycles() {
        Cpu cpu = runProgram(0x8c);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_adding_l_to_a_with_carry() {
        Cpu cpu = runProgram(
                0x3e, 0x80, 0x87, // (Set carry)
                0x3e, 0x39,       // LD A, 0x39
                0x2e, 0x01,       // LD L, 0x01
                0x8d              // ADC A, L
        );
        assertEquals(0x3b, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_adding_l_to_a_with_carry_uses_4_cycles() {
        Cpu cpu = runProgram(0x8d);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_add_indirect_hl_to_a_with_carry_when_everything_is_zero() {
        Cpu cpu = runProgram(
                0x24, // INC H so that it's not pointing at any program instructions
                0x8e
        );
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_add_indirect_hl_to_a_with_carry_when_a_and_carry_are_zero() {
        Cpu cpu = runProgram(0x8e); // NB: We're using 0x8e as an instruction and value here.
        assertEquals(0x8e, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_add_indirect_hl_to_a_with_carry_when_a_is_zero_and_carry_is_set() {
        Cpu cpu = runProgram(
                0x3e, 0x80, 0x87, // (Set carry)
                0x8e              // ADC A, 0x3e
        );
        assertEquals(0x3f, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_add_indirect_hl_to_a_with_carry_when_a_is_nonzero_and_carry_is_set() {
        Cpu cpu = runProgram(
                0x3e, 0x80, 0x87, // (Set carry)
                0x3e, 0x27,       // LD  A, 0x4f
                0x8e              // ADC A, 0x3e
        );
        assertEquals(0x66, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_add_indirect_hl_to_a_with_carry_when_hl_points_somewhere_other_than_0x00() {
        Cpu cpu = runProgram(
                0x26, 0xab, 0x2e, 0xbc, // Set HL to 0xabcd
                0x36, 0xd1,             // Set (HL) to 0xd1
                0x3e, 0x12,             // Set A to 0x12
                0x8e                    // ADC A, (HL)
        );
        assertEquals(0xe3, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_add_indirect_hl_to_a_with_carry_can_set_carry() {
        Cpu cpu = runProgram(
                0x3e, 0xc2,       // LD  A, 0xc2
                0x8e              // ADC A, 0x3e
        );
        assertEquals(0x00, cpu.read(Byte.Register.A));
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_add_indirect_hl_to_a_with_carry_sets_nibble_on_bit_3_carry() {
        Cpu cpu = runProgram(
                0x3e, 0x80, 0x87,       // (Set carry)
                0x3e, 0x01,             // Set A to 0x01
                0x8e                    // ADC A, (HL)
        );
        assertEquals(0x40, cpu.read(Byte.Register.A));
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_add_indirect_hl_to_a_with_carry_unsets_nibble_if_no_bit_3_carry_occurs() {
        Cpu cpu = cpuWithProgram(0x8e);
        cpu.set(Flag.NIBBLE, true);
        runProgram(cpu, 1);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_add_indirect_hl_to_a_with_carry_unsets_operation_flag() {
        Cpu cpu = cpuWithProgram(0x8e);
        cpu.set(Flag.OPERATION, true);
        runProgram(cpu, 1);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_add_indirect_hl_to_a_sets_zero_flag_when_all_values_zero() {
        Cpu cpu = runProgram(0x36, 0x00, 0x8e);
        assertEquals(0x00, cpu.read(Byte.Register.A));
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_add_indirect_hl_to_a_sets_zero_flag_on_rollover_to_zero() {
        Cpu cpu = runProgram(0x36, 0x90, 0x3e, 0x70, 0x8e);
        assertEquals(0x00, cpu.read(Byte.Register.A));
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_add_indirect_hl_to_a_unsets_zero_flag_when_result_nonzero() {
        Cpu cpu = cpuWithProgram(0x36, 0x90, 0x3e, 0x71, 0x8e);
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 5);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_add_indirect_hl_to_a_with_carry_uses_8_cycles() {
        Cpu cpu = runProgram(0x8e);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_add_argument_to_a_with_carry_when_everything_is_zero() {
        Cpu cpu = runProgram(0xce, 0x00);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_add_argument_to_a_with_carry_when_a_and_carry_are_zero() {
        Cpu cpu = runProgram(0xce, 0x54);
        assertEquals(0x54, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_add_argument_to_a_with_carry_when_a_is_zero_and_carry_is_set() {
        Cpu cpu = runProgram(
                0x3e, 0x80, 0x87, // (Set carry)
                0xce, 0xfd        // ADC A, 0xfd
        );
        assertEquals(0xfe, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_add_argument_to_a_with_carry_when_a_is_nonzero_and_carry_is_set() {
        Cpu cpu = runProgram(
                0x3e, 0x80, 0x87, // (Set carry)
                0x3e, 0x3d,       // LD  A, 0x3d
                0xce, 0x91        // ADC A, 0x91
        );
        assertEquals(0xcf, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_add_argument_to_a_with_carry_can_set_carry() {
        Cpu cpu = runProgram(
                0x3e, 0xd9,       // LD  A, 0xd9
                0xce, 0x27        // ADC A, 0x27
        );
        assertEquals(0x00, cpu.read(Byte.Register.A));
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_add_argument_to_a_with_carry_sets_nibble_on_bit_3_carry() {
        Cpu cpu = runProgram(
                0x3e, 0x80, 0x87,       // (Set carry)
                0x3e, 0x0d,             // Set A to 0x0f
                0xce, 0x02              // ADC A, 0x02
        );
        assertEquals(0x10, cpu.read(Byte.Register.A));
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_add_argument_to_a_with_carry_unsets_nibble_if_no_bit_3_carry_occurs() {
        Cpu cpu = cpuWithProgram(0xce, 0x00);
        cpu.set(Flag.NIBBLE, true);
        runProgram(cpu, 1);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_add_argument_to_a_with_carry_unsets_operation_flag() {
        Cpu cpu = cpuWithProgram(0xce);
        cpu.set(Flag.OPERATION, true);
        runProgram(cpu, 1);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_add_argument_to_a_sets_zero_flag_when_all_values_zero() {
        Cpu cpu = runProgram(0xce, 0x00);
        assertEquals(0x00, cpu.read(Byte.Register.A));
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_add_argument_to_a_sets_zero_flag_on_rollover_to_zero() {
        Cpu cpu = runProgram(0x3e, 0x60, 0xce, 0xa0);
        assertEquals(0x00, cpu.read(Byte.Register.A));
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_add_argument_to_a_unsets_zero_flag_when_result_nonzero() {
        Cpu cpu = cpuWithProgram(0x3e, 0x60, 0xce, 0x91);
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 5);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_add_argument_to_a_with_carry_uses_8_cycles() {
        Cpu cpu = runProgram(0xce, 0x00);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_subtract_a_from_a_gives_zero_when_a_is_zero() {
        Cpu cpu = runProgram(0x97);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_subtract_a_from_a_gives_zero_when_a_is_nonzero() {
        Cpu cpu = runProgram(0x3e, 0x94, 0x97);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_subtract_a_from_a_uses_4_cycles() {
        Cpu cpu = runProgram(0x97);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_subtract_a_from_a_sets_operation_flag_when_a_is_zero() {
        Cpu cpu = runProgram(0x97);
        assertTrue(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_subtract_a_from_a_sets_operation_flag_when_a_is_nonzero() {
        Cpu cpu = runProgram(0x3e, 0x8f, 0x97);
        assertTrue(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_subtract_a_from_a_sets_zero_flag_when_a_is_zero() {
        Cpu cpu = runProgram(0x97);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_subtract_a_from_a_sets_zero_flag_when_a_is_nonzero() {
        Cpu cpu = runProgram(0x3e, 0xae, 0x97);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_subtract_a_from_a_resets_carry_flag() {
        Cpu cpu = cpuWithProgram(0x3e, 0xb5, 0x97);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_subtract_a_from_a_resets_nibble_flag() {
        Cpu cpu = cpuWithProgram(0x3e, 0x34, 0x97);
        cpu.set(Flag.NIBBLE, true);
        runProgram(cpu, 3);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_subtract_b_from_a_is_zero_when_both_are_zero() {
        Cpu cpu = runProgram(0x90);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_subtract_b_from_a_uses_4_cycles() {
        Cpu cpu = runProgram(0x90);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_subtract_b_from_a_is_zero_when_b_equals_a() {
        Cpu cpu = runProgram(0x3e, 0x82, 0x06, 0x82, 0x90);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_subtract_b_from_a_when_a_is_greater_than_b() {
        Cpu cpu = runProgram(0x3e, 0xcd, 0x06, 0x64, 0x90);
        assertEquals(0x69, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_subtract_b_from_a_when_a_is_less_than_b() {
        Cpu cpu = runProgram(0x3e, 0x57, 0x06, 0xbd, 0x90);
        assertEquals(0x9a, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_subtract_b_from_a_leaves_b_alone() {
        Cpu cpu = runProgram(0x3e, 0x94, 0x06, 0x77, 0x90);
        assertEquals(0x77, cpu.read(Byte.Register.B));
    }

    @Test
    public void test_subtract_b_from_a_sets_operation_flag() {
        Cpu cpu = runProgram(0x3e, 0xaa, 0x06, 0x81, 0x90);
        assertTrue(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_subtract_b_from_a_resets_zero_flag_if_result_is_nonzero() {
        Cpu cpu = cpuWithProgram(0x3e, 0x34, 0x06, 0x12, 0x90);
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 5);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_subtract_b_from_a_sets_zero_flag_if_a_equals_b() {
        Cpu cpu = runProgram(0x3e, 0x34, 0x06, 0x34, 0x90);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_subtract_b_from_a_sets_carry_flag_on_borrow_from_bit_1() {
        Cpu cpu = runProgram(0x3e, 0x02, 0x06, 0x01, 0x90);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_subtract_b_from_a_resets_carry_flag_if_no_borrows_occur() {
        Cpu cpu = cpuWithProgram(0x3e, 0xd7, 0x06, 0x93, 0x90);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 5);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_subtract_b_from_a_sets_carry_flag_on_borrow_from_bit_2() {
        Cpu cpu = runProgram(0x3e, 0x04, 0x06, 0x02, 0x90);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_subtract_b_from_a_sets_carry_flag_on_borrow_from_bit_3() {
        Cpu cpu = runProgram(0x3e, 0x08, 0x06, 0x04, 0x90);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_subtract_b_from_a_sets_carry_flag_on_borrow_from_bit_4() {
        Cpu cpu = runProgram(0x3e, 0x10, 0x06, 0x08, 0x90);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_subtract_b_from_a_sets_carry_flag_on_borrow_from_bit_5() {
        Cpu cpu = runProgram(0x3e, 0x20, 0x06, 0x10, 0x90);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_subtract_b_from_a_sets_carry_flag_on_borrow_from_bit_6() {
        Cpu cpu = runProgram(0x3e, 0x40, 0x06, 0x20, 0x90);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_subtract_b_from_a_sets_carry_flag_on_borrow_from_bit_7() {
        Cpu cpu = runProgram(0x3e, 0x80, 0x06, 0x40, 0x90);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_subtract_b_from_a_sets_carry_flag_on_borrow_from_virtual_bit_8() {
        Cpu cpu = runProgram(0x3e, 0x7f, 0x06, 0x80, 0x90);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_subtract_b_from_a_sets_nibble_flag_on_direct_bit_4_borrow() {
        Cpu cpu = runProgram(0x3e, 0x17, 0x06, 0x08, 0x90);
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_subtract_b_from_a_resets_nibble_flag_when_no_bit_4_borrow_occurs() {
        Cpu cpu = cpuWithProgram(0x3e, 0xf8, 0x06, 0x07, 0x90);
        cpu.set(Flag.NIBBLE, true);
        runProgram(cpu, 5);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_subtract_b_from_a_sets_nibble_flag_on_double_bit_4_borrow() {
        Cpu cpu = runProgram(0x3e, 0x27, 0x06, 0x08, 0x90);
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_subtract_b_from_a_sets_nibble_flag_on_bit_4_borrow_from_virtual_bit_8() {
        Cpu cpu = runProgram(0x3e, 0x07, 0x06, 0x08, 0x90);
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_subtract_b_from_a_sets_nibble_flag_on_indirect_bit_4_borrow() {
        Cpu cpu = runProgram(0x3e, 0x80, 0x06, 0x01, 0x90);
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_subtract_c_from_a() {
        Cpu cpu = runProgram(0x3e, 0x2c, 0x0e, 0x5b, 0x91);
        assertEquals(0xd1, cpu.read(Byte.Register.A));
    }

    @Test
    public void tst_subtract_c_from_a_uses_4_cycles() {
        Cpu cpu = runProgram(0x91);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_subtract_d_from_a() {
        Cpu cpu = runProgram(0x3e, 0x1d, 0x16, 0x64, 0x92);
        assertEquals(0xb9, cpu.read(Byte.Register.A));
    }

    @Test
    public void tst_subtract_d_from_a_uses_4_cycles() {
        Cpu cpu = runProgram(0x92);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_subtract_e_from_a() {
        Cpu cpu = runProgram(0x3e, 0xac, 0x1e, 0xf8, 0x93);
        assertEquals(0xb4, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_subtract_e_from_a_uses_4_cycles() {
        Cpu cpu = runProgram(0x93);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_subtract_h_from_a() {
        Cpu cpu = runProgram(0x3e, 0x82, 0x26, 0xa1, 0x94);
        assertEquals(0xe1, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_subtract_h_from_a_uses_4_cycles() {
        Cpu cpu = runProgram(0x94);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_subtract_l_from_a() {
        Cpu cpu = runProgram(0x3e, 0xc9, 0x2e, 0xf0, 0x95);
        assertEquals(0xd9, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_subtract_l_from_a_uses_4_cycles() {
        Cpu cpu = runProgram(0x95);
        assertEquals(4, cpu.getCycles());
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
