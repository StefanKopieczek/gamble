package com.kopieczek.gamble.cpu;

import com.kopieczek.gamble.memory.MemoryManagementUnit;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Note the tests below intentionally don't use the Operation enum, and instead specify opcodes manually.
 * This gives us additional confidence that the Operation codes are correct.
 */
public class TestCpu {
    @Test
    public void test_simple_read() {
        MemoryManagementUnit mmu = MemoryManagementUnit.build();
        mmu.setByte(0xdead, 0xf0);
        Cpu cpu = new Cpu(mmu);
        assertEquals(0xf0, cpu.readFrom(Pointer.of(Word.literal(0xdead))));
    }

    @Test
    public void test_simple_write() {
        MemoryManagementUnit mmu = MemoryManagementUnit.build();
        Cpu cpu = new Cpu(mmu);
        cpu.writeTo(Pointer.of(Word.literal(0xdead)), Byte.literal(0xf0));
        assertEquals(0xf0, mmu.readByte(0xdead));
    }

    @Test
    public void test_initial_state() {
        Cpu cpu = new Cpu(MemoryManagementUnit.build());
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
    public void test_dec_b_on_0x01_is_0x00() {
        Cpu cpu = runProgram(0x06, 0x01, 0x05);
        assertEquals(0x00, cpu.read(Byte.Register.B));
    }

    @Test
    public void test_dec_b_on_0x17_is_0x16() {
        Cpu cpu = runProgram(0x06, 0x17, 0x05);
        assertEquals(0x16, cpu.read(Byte.Register.B));
    }

    @Test
    public void test_dec_b_on_0xff_is_0xfe() {
        Cpu cpu = runProgram(0x06, 0xff, 0x05);
        assertEquals(0xfe, cpu.read(Byte.Register.B));
    }

    @Test
    public void test_dec_b_uses_4_cycles() {
        Cpu cpu = runProgram(0x05);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_dec_b_on_0x00_is_0xff() {
        Cpu cpu = runProgram(0x05);
        assertEquals(0xff, cpu.read(Byte.Register.B));
    }

    @Test
    public void test_dec_b_on_0x01_sets_zero_flag() {
        Cpu cpu = runProgram(0x06, 0x01, 0x05);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_dec_b_on_0xff_does_not_set_zero_flag() {
        Cpu cpu = runProgram(0x06, 0xff, 0x05);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_dec_b_on_0x00_does_not_set_zero_flag() {
        Cpu cpu = runProgram(0x06, 0x00, 0x05);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_dec_b_on_0x01_does_not_reset_zero_flag() {
        Cpu cpu = cpuWithProgram(0x06, 0x01, 0x05);
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 3);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_dec_b_on_0xfe_resets_zero_flag() {
        Cpu cpu = cpuWithProgram(0x06, 0xfe, 0x05);
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 3);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_dec_b_on_0x56_sets_operation_flag() {
        Cpu cpu = runProgram(0x06, 0x56, 0x05);
        assertTrue(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_dec_b_on_0x00_sets_operation_flag() {
        Cpu cpu = runProgram(0x06, 0x00, 0x05);
        assertTrue(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_dec_b_on_0x64_does_not_reset_operation_flag() {
        Cpu cpu = cpuWithProgram(0x06, 0x64, 0x05);
        cpu.set(Flag.OPERATION, true);
        runProgram(cpu, 3);
        assertTrue(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_dec_b_on_0x78_does_not_set_carry_flag() {
        Cpu cpu = runProgram(0x06, 0x78, 0x05);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_dec_b_on_0x78_does_not_reset_carry_flag() {
        Cpu cpu = cpuWithProgram(0x06, 0x78, 0x05);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
    }

    @Test
    public void test_dec_b_on_0x01_does_not_set_nibble_flag() {
        Cpu cpu = runProgram(0x06, 0x01, 0x05);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_dec_b_on_0x01_resets_nibble_flag() {
        Cpu cpu = cpuWithProgram(0x06, 0x01, 0x05);
        cpu.set(Flag.NIBBLE, true);
        runProgram(cpu, 3);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_dec_b_on_0x10_sets_nibble_flag() {
        Cpu cpu = runProgram(0x06, 0x10, 0x05);
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_dec_b_on_0x10_does_not_reset_nibble_flag() {
        Cpu cpu = cpuWithProgram(0x06, 0x10, 0x05);
        cpu.set(Flag.NIBBLE, true);
        runProgram(cpu, 3);
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_dec_b_on_0x80_sets_nibble_flag() {
        Cpu cpu = runProgram(0x06, 0x80, 0x05);
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_dec_b_on_0x00_sets_nibble_flag() {
        Cpu cpu = runProgram(0x05);
        assertTrue(cpu.isSet(Flag.NIBBLE));
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
        for (int i = 4; i < 260; i++) {
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
    public void test_load_a_to_b() {
        Cpu cpu = runProgram(0x3e, 0x94, 0x47);
        assertEquals(0x94, cpu.read(Byte.Register.A));
        assertEquals(12, cpu.getCycles()); // 8 + 4
    }

    @Test
    public void test_load_a_to_c() {
        Cpu cpu = runProgram(0x3e, 0x4f, 0x4f);
        assertEquals(0x4f, cpu.read(Byte.Register.A));
        assertEquals(12, cpu.getCycles()); // 8 + 4
    }

    @Test
    public void test_load_a_to_d() {
        Cpu cpu = runProgram(0x3e, 0x4f, 0x57);
        assertEquals(0x4f, cpu.read(Byte.Register.A));
        assertEquals(12, cpu.getCycles()); // 8 + 4
    }

    @Test
    public void test_load_a_to_e() {
        Cpu cpu = runProgram(0x3e, 0x07, 0x5f);
        assertEquals(0x07, cpu.read(Byte.Register.A));
        assertEquals(12, cpu.getCycles()); // 8 + 4
    }

    @Test
    public void test_load_a_to_h() {
        Cpu cpu = runProgram(0x3e, 0x27, 0x67);
        assertEquals(0x27, cpu.read(Byte.Register.A));
        assertEquals(12, cpu.getCycles()); // 8 + 4
    }

    @Test
    public void test_load_a_to_l() {
        Cpu cpu = runProgram(0x3e, 0x44, 0x67);
        assertEquals(0x44, cpu.read(Byte.Register.A));
        assertEquals(12, cpu.getCycles()); // 8 + 4
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
    public void test_load_a_to_indirect_hl() {
        Cpu cpu = runProgram(
                0x3e, 0x8b, // LD A, 8b
                0x26, 0x10, // LD H, 0x10
                0x2e, 0x66, // LD L, 0x66 (now HL=0x1066).
                0x77        // LD (HL), A
        );

        assertEquals(0x8b, cpu.unsafeRead(0x1066));
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

    @Test
    public void test_subtract_indirect_hl_from_a_is_zero_when_both_are_zero() {
        Cpu cpu = runProgram(0x24, 0x96);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_subtract_indirect_hl_from_a_uses_8_cycles() {
        Cpu cpu = runProgram(0x96);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_subtract_indirect_hl_from_a_is_zero_when_hl_mem_equals_a() {
        Cpu cpu = runProgram(0x3e, 0x3e, 0x96);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_subtract_indirect_hl_from_a_when_a_is_greater_than_hl_mem() {
        Cpu cpu = runProgram(0x3e, 0xd5, 0x96);
        assertEquals(0x97, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_subtract_indirect_hl_from_a_when_a_is_less_than_hl_mem() {
        Cpu cpu = runProgram(0x3e, 0xa3, 0x96);
        assertEquals(0x65, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_subtract_indirect_hl_from_a_leaves_hl_mem_alone() {
        Cpu cpu = runProgram(0x3e, 0x94, 0x96);
        assertEquals(0x3e, cpu.readFrom(Pointer.of(Word.Register.HL)));
    }

    @Test
    public void test_subtract_indirect_hl_from_a_when_hl_is_nonzero() {
        Cpu cpu = runProgram(
                0x26, 0xca, // LD H,    0xca
                0x2e, 0xfe, // LD L,    0xfe
                0x36, 0x12, // LD (HL), 0x12
                0x3e, 0xda, // LD A,    0xda
                0x96        // SUB A,   (HL)
        );
        assertEquals(0xc8, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_subtract_indirect_hl_from_a_sets_operation_flag() {
        Cpu cpu = runProgram(0x3e, 0xaa, 0x96);
        assertTrue(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_subtract_indirect_hl_from_a_resets_zero_flag_if_result_is_nonzero() {
        Cpu cpu = cpuWithProgram(0x3e, 0x34, 0x96);
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 3);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_subtract_indirect_hl_from_a_sets_zero_flag_if_a_equals_b() {
        Cpu cpu = runProgram(0x3e, 0x3e, 0x96);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_subtract_indirect_hl_from_a_sets_carry_flag_on_borrow_from_bit_1() {
        Cpu cpu = runProgram(0x3e, 0xfe, 0x36, 0x01, 0x96);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_subtract_indirect_hl_from_a_resets_carry_flag_if_no_borrows_occur() {
        Cpu cpu = cpuWithProgram(0x3e, 0x7f, 0x96);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_subtract_indirect_hl_from_a_sets_carry_flag_on_borrow_from_bit_2() {
        Cpu cpu = runProgram(0x3e, 0x04, 0x36, 0x02, 0x96);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_subtract_indirect_hl_from_a_sets_carry_flag_on_borrow_from_bit_3() {
        Cpu cpu = runProgram(0x3e, 0x08, 0x36, 0x04, 0x96);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_subtract_indirect_hl_from_a_sets_carry_flag_on_borrow_from_bit_4() {
        Cpu cpu = runProgram(0x3e, 0x10, 0x36, 0x08, 0x96);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_subtract_indirect_hl_from_a_sets_carry_flag_on_borrow_from_bit_5() {
        Cpu cpu = runProgram(0x3e, 0x20, 0x36, 0x10, 0x96);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_subtract_indirect_hl_from_a_sets_carry_flag_on_borrow_from_bit_6() {
        Cpu cpu = runProgram(0x3e, 0x40, 0x36, 0x20, 0x96);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_subtract_indirect_hl_from_a_sets_carry_flag_on_borrow_from_bit_7() {
        Cpu cpu = runProgram(0x3e, 0x80, 0x36, 0x40, 0x96);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_subtract_indirect_hl_from_a_sets_carry_flag_on_borrow_from_virtual_bit_8() {
        Cpu cpu = runProgram(0x3e, 0x7f, 0x36, 0x80, 0x96);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_subtract_indirect_hl_from_a_sets_nibble_flag_on_direct_bit_4_borrow() {
        Cpu cpu = runProgram(0x3e, 0x17, 0x36, 0x08, 0x96);
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_subtract_indirect_hl_from_a_resets_nibble_flag_when_no_bit_4_borrow_occurs() {
        Cpu cpu = cpuWithProgram(0x3e, 0xf8, 0x36, 0x07, 0x96);
        cpu.set(Flag.NIBBLE, true);
        runProgram(cpu, 5);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_subtract_indirect_hl_from_a_sets_nibble_flag_on_double_bit_4_borrow() {
        Cpu cpu = runProgram(0x3e, 0x27, 0x36, 0x08, 0x96);
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_subtract_indirect_hl_from_a_sets_nibble_flag_on_bit_4_borrow_from_virtual_bit_8() {
        Cpu cpu = runProgram(0x3e, 0x07, 0x36, 0x08, 0x96);
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_subtract_indirect_hl_from_a_sets_nibble_flag_on_indirect_bit_4_borrow() {
        Cpu cpu = runProgram(0x3e, 0x80, 0x36, 0x01, 0x96);
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_subtract_arg_from_a_is_zero_when_both_are_zero() {
        Cpu cpu = runProgram(0xd6, 0x00);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_subtract_arg_from_a_uses_8_cycles() {
        Cpu cpu = runProgram(0xd6, 0x00);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_subtract_arg_from_a_is_zero_when_arg_equals_a() {
        Cpu cpu = runProgram(0x3e, 0x84, 0x90, 0xd6, 0x84);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_subtract_arg_from_a_when_a_is_greater_than_arg() {
        Cpu cpu = runProgram(0x3e, 0xdd, 0xd6, 0x86);
        assertEquals(0x57, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_subtract_arg_from_a_when_a_is_less_than_arg() {
        Cpu cpu = runProgram(0x3e, 0x4a, 0xd6, 0xdd);
        assertEquals(0x6d, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_subtract_arg_from_a_sets_operation_flag() {
        Cpu cpu = runProgram(0x3e, 0xc7, 0xd6, 0x9f);
        assertTrue(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_subtract_arg_from_a_resets_zero_flag_if_result_is_nonzero() {
        Cpu cpu = cpuWithProgram(0x3e, 0x65, 0xd6, 0xf8);
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 5);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_subtract_arg_from_a_sets_zero_flag_if_a_equals_arg() {
        Cpu cpu = runProgram(0x3e, 0xae, 0xd6, 0xae);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_subtract_arg_from_a_sets_carry_flag_on_borrow_from_bit_1() {
        Cpu cpu = runProgram(0x3e, 0x02, 0xd6, 0x01);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_subtract_arg_from_a_resets_carry_flag_if_no_borrows_occur() {
        Cpu cpu = cpuWithProgram(0x3e, 0xd7, 0xd6, 0x93);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 5);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_subtract_arg_from_a_sets_carry_flag_on_borrow_from_bit_2() {
        Cpu cpu = runProgram(0x3e, 0x04, 0xd6, 0x02);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_subtract_arg_from_a_sets_carry_flag_on_borrow_from_bit_3() {
        Cpu cpu = runProgram(0x3e, 0x08, 0xd6, 0x04);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_subtract_arg_from_a_sets_carry_flag_on_borrow_from_bit_4() {
        Cpu cpu = runProgram(0x3e, 0x10, 0xd6, 0x08);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_subtract_arg_from_a_sets_carry_flag_on_borrow_from_bit_5() {
        Cpu cpu = runProgram(0x3e, 0x20, 0xd6, 0x10);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_subtract_arg_from_a_sets_carry_flag_on_borrow_from_bit_6() {
        Cpu cpu = runProgram(0x3e, 0x40, 0xd6, 0x20);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_subtract_arg_from_a_sets_carry_flag_on_borrow_from_bit_7() {
        Cpu cpu = runProgram(0x3e, 0x80, 0xd6, 0x40);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_subtract_arg_from_a_sets_carry_flag_on_borrow_from_virtual_bit_8() {
        Cpu cpu = runProgram(0x3e, 0x7f, 0xd6, 0x80);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_subtract_arg_from_a_sets_nibble_flag_on_direct_bit_4_borrow() {
        Cpu cpu = runProgram(0x3e, 0x17, 0xd6, 0x08);
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_subtract_arg_from_a_resets_nibble_flag_when_no_bit_4_borrow_occurs() {
        Cpu cpu = cpuWithProgram(0x3e, 0xf8, 0xd6, 0x07);
        cpu.set(Flag.NIBBLE, true);
        runProgram(cpu, 5);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_subtract_arg_from_a_sets_nibble_flag_on_double_bit_4_borrow() {
        Cpu cpu = runProgram(0x3e, 0x27, 0xd6, 0x08);
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_subtract_arg_from_a_sets_nibble_flag_on_bit_4_borrow_from_virtual_bit_8() {
        Cpu cpu = runProgram(0x3e, 0x07, 0xd6, 0x08);
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_subtract_arg_from_a_sets_nibble_flag_on_indirect_bit_4_borrow() {
        Cpu cpu = runProgram(0x3e, 0x80, 0xd6, 0x01);
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_subtract_a_from_a_with_carry_when_everything_is_0() {
        Cpu cpu = runProgram(0x9f);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_subtract_a_from_a_with_carry_when_a_is_nonzero_but_carry_is_zero() {
        Cpu cpu = runProgram(0x3e, 0x8c, 0x9f);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_subtract_a_from_a_with_carry_uses_four_cycles() {
        Cpu cpu = runProgram(0x9f);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_subtract_a_from_a_with_carry_when_a_is_zero_and_carry_is_set_gives_0xff() {
        Cpu cpu = runProgram(
                0x3e, 0x80, 0x87, // Set carry flag and reset A to 0x00
                0x9f
        );
        assertEquals(0xff, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_subtract_a_from_a_when_a_with_carry_is_nonzero_and_carry_flag_is_set_gives_0xff() {
        Cpu cpu = runProgram(
                0x3e, 0x80, 0x87, // Set carry flag
                0x3e, 0xe3, // LD A, 0xe3
                0x9f
        );
        assertEquals(0xff, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_subtract_a_from_a_with_carry_sets_operation_flag() {
        Cpu cpu = runProgram(0x9f);
        assertTrue(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_subtract_a_from_a_with_carry_preserves_carry_flag_when_it_is_not_set() {
        Cpu cpu = runProgram(0x3e, 0xce, 0x9f);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_subtract_a_from_a_with_carry_preserves_carry_flag_when_it_is_set() {
        Cpu cpu = runProgram(
                0x3e, 0x80, 0x87, // Set carry flag
                0x3e, 0x2f, // LD A, 0x2f,
                0x9f);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_subtract_a_from_a_with_carry_resets_nibble_flag_when_carry_is_not_set() {
        Cpu cpu = runProgram(
                0x3e, 0x08, 0x87, // Set nibble flag
                0x3e, 0xfd,
                0x9f
        );
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_subtract_a_from_a_with_carry_sets_nibble_flag_if_carry_is_set() {
        Cpu cpu = runProgram(
                0x3e, 0x80, 0x87, // Set carry flag
                0x3e, 0x3a, // LD A, 0x3a,
                0x9f);
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_subtract_b_from_a_with_carry() {
        Cpu cpu = runProgram(
                0x3e, 0x36,
                0x06, 0x31,
                0x98
        );
        assertEquals(0x05, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_subtract_b_from_a_with_carry_sets_carry_flag_on_borrow_from_bit_1() {
        Cpu cpu = runProgram(0x3e, 0x02, 0x06, 0x01, 0x98);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_subtract_b_from_a_with_carry_resets_carry_flag_if_no_borrows_occur() {
        Cpu cpu = cpuWithProgram(0x3e, 0xd7, 0x06, 0x93, 0x98);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 5);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_subtract_b_from_a_with_carry_sets_carry_flag_on_borrow_from_bit_2() {
        Cpu cpu = runProgram(0x3e, 0x04, 0x06, 0x02, 0x98);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_subtract_b_from_a_with_carry_sets_carry_flag_on_borrow_from_bit_3() {
        Cpu cpu = runProgram(0x3e, 0x08, 0x06, 0x04, 0x98);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_subtract_b_from_a_with_carry_sets_carry_flag_on_borrow_from_bit_4() {
        Cpu cpu = runProgram(0x3e, 0x10, 0x06, 0x08, 0x98);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_subtract_b_from_a_with_carry_sets_carry_flag_on_borrow_from_bit_5() {
        Cpu cpu = runProgram(0x3e, 0x20, 0x06, 0x10, 0x98);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_subtract_b_from_a_with_carry_sets_carry_flag_on_borrow_from_bit_6() {
        Cpu cpu = runProgram(0x3e, 0x40, 0x06, 0x20, 0x98);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_subtract_b_from_a_with_carry_sets_carry_flag_on_borrow_from_bit_7() {
        Cpu cpu = runProgram(0x3e, 0x80, 0x06, 0x40, 0x98);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_subtract_b_from_a_with_carry_sets_carry_flag_on_borrow_from_virtual_bit_8() {
        Cpu cpu = runProgram(0x3e, 0x7f, 0x06, 0x80, 0x98);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_subtract_b_from_a_with_carry_sets_nibble_flag_on_direct_bit_4_borrow() {
        Cpu cpu = runProgram(0x3e, 0x17, 0x06, 0x08, 0x98);
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_subtract_b_from_a_with_carry_resets_nibble_flag_when_no_bit_4_borrow_occurs() {
        Cpu cpu = cpuWithProgram(0x3e, 0xf8, 0x06, 0x07, 0x98);
        cpu.set(Flag.NIBBLE, true);
        runProgram(cpu, 5);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_subtract_b_from_a_with_carry_sets_nibble_flag_on_double_bit_4_borrow() {
        Cpu cpu = runProgram(0x3e, 0x27, 0x06, 0x08, 0x98);
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_subtract_b_from_a_with_carry_sets_nibble_flag_on_bit_4_borrow_from_virtual_bit_8() {
        Cpu cpu = runProgram(0x3e, 0x07, 0x06, 0x08, 0x98);
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_subtract_b_from_a_with_carry_sets_nibble_flag_on_indirect_bit_4_borrow() {
        Cpu cpu = runProgram(0x3e, 0x80, 0x06, 0x01, 0x98);
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_subtract_c_from_a_with_carry() {
        Cpu cpu = runProgram(
                0x3e, 0x11,
                0x0e, 0xe5,
                0x99
        );
        assertEquals(0x2c, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_subtract_d_from_a_with_carry() {
        Cpu cpu = runProgram(
                0x3e, 0x99,
                0x16, 0x7a,
                0x9a
        );
        assertEquals(0x1f, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_subtract_d_from_a_with_carry_sets_zero_flag_if_result_is_zero() {
        Cpu cpu = runProgram(
                0x3e, 0x81, 0x87, // Set carry flag
                0x3e, 0xac,
                0x16, 0xab,
                0x9a
        );
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_subtract_d_from_a_with_carry_resets_zero_flag_if_result_is_nonzero() {
        Cpu cpu = runProgram(
                0x3e, 0x80, 0x87, // Set carry and zero flags
                0x3e, 0xad,
                0x16, 0xab,
                0x9a
        );
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_subtract_e_from_a_with_carry() {
        Cpu cpu = runProgram(
                0x3e, 0x23,
                0x1e, 0xbf,
                0x9b
        );
        assertEquals(0x64, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_subtract_e_from_a_with_carry_uses_4_cycles() {
        Cpu cpu = runProgram(0x9b);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_subtract_h_from_a_with_carry() {
        Cpu cpu = runProgram(
                0x3e, 0xf7,
                0x26, 0x7f,
                0x9c
        );
        assertEquals(0x78, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_subtract_l_from_a_with_carry() {
        Cpu cpu = runProgram(
                0x3e, 0xb4,
                0x2e, 0x5e,
                0x9d
        );
        assertEquals(0x56, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_subtract_indirect_hl_from_a_with_carry_when_carry_is_zero() {
        Cpu cpu = runProgram(0x3e, 0xff, 0x36, 0x9d, 0x9e);
        assertEquals(0x62, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_subtract_indirect_hl_from_a_with_carry_when_carry_is_nonzero() {
        Cpu cpu = runProgram(
                0x3e, 0x80, 0x87,
                0x3e, 0xa6, 0x36, 0xf8,
                0x9e);
        assertEquals(0xad, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_subtract_indirect_hl_from_a_with_carry_uses_8_cycles() {
        Cpu cpu = runProgram(0x9e);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_subtract_arg_from_a_with_carry_when_carry_is_zero() {
        Cpu cpu = runProgram(0x3e, 0xca, 0xde, 0x64);
        assertEquals(0x66, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_subtract_arg_from_a_with_carry_uses_8_cycles() {
        Cpu cpu = runProgram(0xde);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_a_and_a_when_a_is_zero_returns_zero() {
        Cpu cpu = runProgram(0xa7);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_and_a_uses_4_cycles() {
        Cpu cpu = runProgram(0xa7);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_a_and_a_when_a_is_0xff_returns_0xff() {
        Cpu cpu = runProgram(0x3e, 0xff, 0xa7);
        assertEquals(0xff, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_and_a_when_a_is_0x3d_returns_0x3d() {
        Cpu cpu = runProgram(0x3e, 0x3d, 0xa7);
        assertEquals(0x3d, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_and_a_sets_zero_flag_when_result_is_zero() {
        Cpu cpu = runProgram(0xa7);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_a_and_a_resets_zero_flag_when_result_is_nonzero() {
        Cpu cpu = runProgram(0x3e, 0x80, 0x87, 0x3e, 0xd9, 0xa7);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_a_and_a_sets_nibble_flag() {
        Cpu cpu = runProgram(0xa7);
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_a_and_a_keeps_nibble_flag_set_if_already_set() {
        Cpu cpu = runProgram(0x3e, 0x08, 0x87, 0x3e, 0x19, 0xa7);
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_a_and_a_resets_operation_flag() {
        Cpu cpu = runProgram(0x97, 0xa7);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_a_and_a_resets_carry_flag() {
        Cpu cpu = runProgram(0x3e, 0x80, 0x87, 0xa7);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_a_and_b_is_0x00_when_both_are_zero() {
        Cpu cpu = runProgram(0xa0);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_and_b_is_0x00_when_b_is_zero() {
        Cpu cpu = runProgram(0x3e, 0x3f, 0xa0);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_and_b_is_0x00_when_a_is_zero() {
        Cpu cpu = runProgram(0x06, 0x7f, 0xa0);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_and_b_1() {
        Cpu cpu = runProgram(0x3e, 0x39, 0x06, 0x99, 0xa0);
        assertEquals(0x19, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_and_b_2() {
        Cpu cpu = runProgram(0x3e, 0x5d, 0x06, 0xa3, 0xa0);
        assertEquals(0x01, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_and_b_uses_4_cycles() {
        Cpu cpu = runProgram(0xa0);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_a_and_c() {
        Cpu cpu = runProgram(0x3e, 0x2b, 0x0e, 0x16, 0xa1);
        assertEquals(0x02, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_and_d() {
        Cpu cpu = runProgram(0x3e, 0x8d, 0x16, 0xfe, 0xa2);
        assertEquals(0x8c, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_and_e() {
        Cpu cpu = runProgram(0x3e, 0x63, 0x1e, 0xc8, 0xa3);
        assertEquals(0x40, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_and_h() {
        Cpu cpu = runProgram(0x3e, 0x8d, 0x26, 0xac, 0xa4);
        assertEquals(0x8c, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_and_l() {
        Cpu cpu = runProgram(0x3e, 0xd6, 0x2e, 0x1a, 0xa5);
        assertEquals(0x12, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_and_l_sets_zero_flag_when_a_and_l_have_no_bits_in_common() {
        Cpu cpu = runProgram(
                0x3e, 0b01010101,
                0x2e, 0b10101010,
                0xa5);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_a_and_indirect_hl_gives_zero_when_both_are_zero() {
        Cpu cpu = runProgram(0x36, 0x00, 0xa6);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_and_indirect_hl_gives_zero_when_a_is_zero() {
        Cpu cpu = runProgram(0xa6);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_and_indirect_hl_gives_zero_when_hl_mem_is_zero() {
        Cpu cpu = runProgram(0x3e, 0xf9, 0x36, 0x00, 0xa6);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_and_indirect_hl_gives_zero_when_no_bits_are_in_common() {
        Cpu cpu = runProgram(0x3e, 0b01010101, 0x36, 0b10101010, 0xa6);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_and_indirect_hl_1() {
        Cpu cpu = runProgram(0x3e, 0x54, 0x36, 0x43, 0xa6);
        assertEquals(0x40, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_and_indirect_hl_2() {
        Cpu cpu = runProgram(0x3e, 0x49, 0x36, 0x21, 0xa6);
        assertEquals(0x01, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_and_indirect_hl_uses_8_cycles() {
        Cpu cpu = runProgram(0xa6);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_a_and_argument() {
        Cpu cpu = runProgram(0x3e, 0x47, 0xe6, 0x7d);
        assertEquals(0x45, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_and_argument_uses_8_cycles() {
        Cpu cpu = runProgram(0xe6);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_a_or_a_uses_4_cycles() {
        Cpu cpu = runProgram(0xb7);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_a_or_a_is_zero_when_a_is_zero() {
        Cpu cpu = runProgram(0xb7);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_or_a_does_not_change_a_when_a_is_0xff() {
        Cpu cpu = runProgram(0x3e, 0xff, 0xb7);
        assertEquals(0xff, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_or_a_does_not_change_a_when_a_is_0xc6() {
        Cpu cpu = runProgram(0x3e, 0xc6, 0xb7);
        assertEquals(0xc6, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_or_a_sets_zero_flag_when_a_is_zero() {
        Cpu cpu = runProgram(0xb7);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_a_or_a_resets_zero_flag_when_a_is_nonzero() {
        Cpu cpu = runProgram(0x3e, 0x80, 0x87, 0x3e, 0x11, 0xb7);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_a_or_a_resets_operation_flag() {
        Cpu cpu = runProgram(0x97, 0x3e, 0x73, 0xb7);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_a_or_a_resets_nibble_flag() {
        Cpu cpu = runProgram(0x3e, 0x08, 0x87, 0x3e, 0x8b, 0xb7);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_a_or_a_resets_carry_flag() {
        Cpu cpu = runProgram(0x3e, 0x80, 0x87, 0x3e, 0xf9, 0xb7);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_a_or_b_is_zero_when_both_are_zero() {
        Cpu cpu = runProgram(0xb0);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_or_b_equals_a_when_b_is_zero() {
        Cpu cpu = runProgram(0x3e, 0x9f, 0xb0);
        assertEquals(0x9f, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_or_b_equals_a_when_a_is_zero() {
        Cpu cpu = runProgram(0x06, 0x99, 0xb0);
        assertEquals(0x99, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_or_b_1() {
        Cpu cpu = runProgram(0x3e, 0xaa, 0x06, 0xcf, 0xb0);
        assertEquals(0xef, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_or_b_2() {
        Cpu cpu = runProgram(0x3e, 0xe0, 0x06, 0x44, 0xb0);
        assertEquals(0xe4, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_or_b_resets_nibble_flag() {
        Cpu cpu = runProgram(0x3e, 0x08, 0x87, 0xb0);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_a_or_b_resets_carry_flag() {
        Cpu cpu = runProgram(0x3e, 0x80, 0x87, 0xb0);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_a_or_b_resets_operation_flag() {
        Cpu cpu = runProgram(0x97, 0xb0);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_a_or_b_uses_4_cycles() {
        Cpu cpu = runProgram(0xb0);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_a_or_c() {
        Cpu cpu = runProgram(0x3e, 0xd6, 0x0e, 0x6f, 0xb1);
        assertEquals(0xff, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_or_c_uses_4_cycles() {
        Cpu cpu = runProgram(0xb1);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_a_or_d() {
        Cpu cpu = runProgram(0x3e, 0xc4, 0x16, 0x23, 0xb2);
        assertEquals(0xe7, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_or_d_uses_4_cycles() {
        Cpu cpu = runProgram(0xb2);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_a_or_e() {
        Cpu cpu = runProgram(0x3e, 0x0a, 0x1e, 0x0d, 0xb3);
        assertEquals(0x0f, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_or_e_uses_4_cycles() {
        Cpu cpu = runProgram(0xb3);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_a_or_h() {
        Cpu cpu = runProgram(0x3e, 0x13, 0x26, 0x9e, 0xb4);
        assertEquals(0x9f, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_or_h_uses_4_cycles() {
        Cpu cpu = runProgram(0xb4);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_a_or_l() {
        Cpu cpu = runProgram(0x3e, 0x68, 0x2e, 0xf7, 0xb5);
        assertEquals(0xff, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_or_l_uses_4_cycles() {
        Cpu cpu = runProgram(0xb5);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_a_or_hl_uses_8_cycles() {
        Cpu cpu = runProgram(0xb6);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_a_or_hl() {
        Cpu cpu = runProgram(
                0x21, 0x74, 0xaf, // LD HL, 0xaf74
                0x3e, 0xa7,       // LD A, 0xa7
                0x36, 0xf6,       // LD (HL), 0xf6
                0xb6              // OR A, (HL)
        );
        assertEquals(0xf7, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_or_argument_uses_8_cycles() {
        Cpu cpu = runProgram(0xf6, 0x00);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_a_or_argument() {
        Cpu cpu = runProgram(0x3e, 0x58, 0xf6, 0x74);
        assertEquals(0x7c, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_xor_b_is_zero_when_both_are_zero() {
        Cpu cpu = runProgram(0xa8);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_xor_b_uses_4_cycles() {
        Cpu cpu = runProgram(0xa8);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_a_xor_b_is_0xff_when_a_is_zero_and_b_is_0xff() {
        Cpu cpu = runProgram(0x06, 0xff, 0xa8);
        assertEquals(0xff, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_xor_b_is_0xff_when_b_is_zero_and_a_is_0xff() {
        Cpu cpu = runProgram(0x3e, 0xff, 0xa8);
        assertEquals(0xff, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_xor_b_is_0x00_when_a_and_b_are_0xff() {
        Cpu cpu = runProgram(0x3e, 0xff, 0x06, 0xff, 0xa8);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_xor_b_with_arbitrary_values() {
        Cpu cpu = runProgram(0x3e, 0x19, 0x06, 0xd7, 0xa8);
        assertEquals(0xce, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_xor_b_sets_zero_flag_if_a_equals_b() {
        Cpu cpu = runProgram(0x3e, 0x6c, 0x06, 0x6c, 0xa8);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_a_xor_b_resets_zero_flag_if_a_does_not_equal_b() {
        Cpu cpu = runProgram(0xa7, 0x3e, 0xef, 0x06, 0xff, 0xa8);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_a_xor_b_resets_carry_flag_when_a_equals_b() {
        Cpu cpu = runProgram(0x3e, 0x80, 0x87, 0x3e, 0x6b, 0x06, 0x6b, 0xa8);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_a_xor_b_resets_carry_flag_when_a_does_not_equal_b() {
        Cpu cpu = runProgram(0x3e, 0x80, 0x87, 0x3e, 0x6b, 0x06, 0x7f, 0xa8);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_a_xor_b_resets_nibble_flag_when_a_equals_b() {
        Cpu cpu = runProgram(0x3e, 0x08, 0x87, 0x3e, 0xa9, 0x06, 0xa9, 0xa8);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_a_xor_b_resets_nibble_flag_when_a_does_not_equal_b() {
        Cpu cpu = runProgram(0x3e, 0x08, 0x87, 0x3e, 0xa9, 0x06, 0x17, 0xa8);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_a_xor_b_resets_operation_flag_when_a_equals_b() {
        Cpu cpu = runProgram(0x97, 0x3e, 0xa9, 0x06, 0xa9, 0xa8);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_a_xor_b_resets_operation_flag_when_a_does_not_equal_b() {
        Cpu cpu = runProgram(0x97, 0x3e, 0xa9, 0x06, 0x17, 0xa8);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_a_xor_a() {
        Cpu cpu = runProgram(0x3e, 0xdf, 0xaf);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_xor_a_uses_4_cycles() {
        Cpu cpu = runProgram(0xaf);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_a_xor_c() {
        Cpu cpu = runProgram(0x3e, 0x44, 0x0e, 0x28, 0xa9);
        assertEquals(0x6c, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_xor_c_uses_4_cycles() {
        Cpu cpu = runProgram(0xa9);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_a_xor_d() {
        Cpu cpu = runProgram(0x3e, 0x67, 0x16, 0xd5, 0xaa);
        assertEquals(0xb2, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_xor_d_uses_4_cycles() {
        Cpu cpu = runProgram(0xaa);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_a_xor_e() {
        Cpu cpu = runProgram(0x3e, 0x4a, 0x1e, 0x4f, 0xab);
        assertEquals(0x05, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_xor_e_uses_4_cycles() {
        Cpu cpu = runProgram(0xab);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_a_xor_h() {
        Cpu cpu = runProgram(0x3e, 0x39, 0x26, 0x89, 0xac);
        assertEquals(0xb0, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_xor_h_uses_4_cycles() {
        Cpu cpu = runProgram(0xac);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_a_xor_l() {
        Cpu cpu = runProgram(0x3e, 0xc9, 0x2e, 0x0e, 0xad);
        assertEquals(0xc7, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_xor_l_uses_4_cycles() {
        Cpu cpu = runProgram(0xad);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_a_xor_indirect_hl_uses_8_cycles() {
        Cpu cpu = runProgram(0xae);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_a_xor_indirect_hl() {
        Cpu cpu = runProgram(0x21, 0x65, 0x9c, 0x36, 0xf8, 0x3e, 0x68, 0xae);
        assertEquals(0x90, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_xor_argument_uses_8_cycles() {
        Cpu cpu = runProgram(0xee, 0x00);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_a_xor_argument() {
        Cpu cpu = runProgram(0x3e, 0x87, 0xee, 0x8a);
        assertEquals(0x0d, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_a_cp_b_uses_4_cycles() {
        Cpu cpu = runProgram(0xb8);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_a_cp_b_sets_zero_flag_when_both_are_zero() {
        Cpu cpu = runProgram(0xb8);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_a_cp_b_resets_zero_flag_when_a_and_b_are_different() {
        Cpu cpu = runProgram(0xa7, 0x3e, 0xab, 0x06, 0xcd, 0xb8);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_a_cp_b_sets_zero_flag_when_a_and_b_are_equal_but_nonzero() {
        Cpu cpu = runProgram(0x3e, 0xab, 0x06, 0xab, 0xb8);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_a_cp_b_sets_operation_flag_when_a_and_b_are_zero() {
        Cpu cpu = runProgram(0xb8);
        assertTrue(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_a_cp_b_sets_operation_flag_when_a_and_b_are_nonzero() {
        Cpu cpu = runProgram(0x3e, 0x12, 0x06, 0x34, 0xb8);
        assertTrue(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_a_cp_b_sets_carry_flag_when_a_is_less_than_b() {
        Cpu cpu = runProgram(0x3e, 0x19, 0x06, 0x22, 0xb8);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_a_cp_b_resets_carry_flag_when_a_is_greater_than_b() {
        Cpu cpu = runProgram(0x3e, 0x80, 0x87, 0x3e, 0xf3, 0x06, 0xf1, 0xb8);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_a_cp_b_resets_carry_flag_when_a_equals_b() {
        Cpu cpu = runProgram(0x3e, 0x80, 0x87, 0x3e, 0xf3, 0x06, 0xf3, 0xb8);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_a_cp_b_doesnt_set_carry_flag_on_bit_2_borrow_when_a_greater_than_b() {
        Cpu cpu = runProgram(0x3e, 0x02, 0x06, 0x01, 0xb8);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_a_cp_b_doesnt_set_carry_flag_on_bit_3_borrow_when_a_greater_than_b() {
        Cpu cpu = runProgram(0x3e, 0x04, 0x06, 0x02, 0xb8);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_a_cp_b_doesnt_set_carry_flag_on_bit_4_borrow_when_a_greater_than_b() {
        Cpu cpu = runProgram(0x3e, 0x08, 0x06, 0x04, 0xb8);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_a_cp_b_doesnt_set_carry_flag_on_bit_5_borrow_when_a_greater_than_b() {
        Cpu cpu = runProgram(0x3e, 0x10, 0x06, 0x08, 0xb8);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_a_cp_b_doesnt_set_carry_flag_on_bit_6_borrow_when_a_greater_than_b() {
        Cpu cpu = runProgram(0x3e, 0x20, 0x06, 0x10, 0xb8);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_a_cp_b_doesnt_set_carry_flag_on_bit_7_borrow_when_a_greater_than_b() {
        Cpu cpu = runProgram(0x3e, 0x40, 0x06, 0x20, 0xb8);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_a_cp_b_doesnt_set_carry_flag_on_bit_8_borrow_when_a_greater_than_b() {
        Cpu cpu = runProgram(0x3e, 0x80, 0x06, 0x40, 0xb8);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_a_cp_b_sets_nibble_flag_when_b_nibble_greater_than_a_nibble_and_b_greater_than_a() {
        Cpu cpu = runProgram(0x3e, 0xab, 0x06, 0xcd, 0xb8);
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_a_cp_b_resets_nibble_flag_when_b_nibble_less_than_a_nibble_and_b_less_than_a() {
        Cpu cpu = runProgram(0x3e, 0x08, 0x87, 0x3e, 0x56, 0x06, 0x34, 0xb8);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_a_cp_b_sets_nibble_flag_when_b_nibble_greater_than_a_nibble_and_b_less_than_a() {
        Cpu cpu = runProgram(0x3e, 0xcb, 0x06, 0xad, 0xb8);
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_a_cp_b_resets_nibble_flag_when_b_nibble_less_than_a_nibble_and_b_greater_than_a() {
        Cpu cpu = runProgram(0x3e, 0x08, 0x87, 0x3e, 0x36, 0x06, 0x54, 0xb8);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_a_cp_b_resets_nibble_flag_when_a_equals_b() {
        Cpu cpu = runProgram(0x3e, 0x08, 0x87, 0x3e, 0xaf, 0x06, 0xaf, 0xb8);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_a_cp_b_leaves_registers_alone() {
        Cpu cpu = runProgram(0x3e, 0x97, 0x06, 0xab, 0xb8);
        assertEquals(0x97, cpu.read(Byte.Register.A));
        assertEquals(0xab, cpu.read(Byte.Register.B));
    }

    @Test
    public void test_a_cp_c_when_a_less_than_c() {
        Cpu cpu = runProgram(0x3e, 0x94, 0x0e, 0xd2, 0xb9);
        assertTrue(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_a_cp_c_when_a_greater_than_c() {
        Cpu cpu = runProgram(0x3e, 0x17, 0x0e, 0x08, 0xb9);
        assertFalse(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_a_cp_c_when_a_equal_to_c() {
        Cpu cpu = runProgram(0x3e, 0x44, 0x0e, 0x44, 0xb9);
        assertFalse(cpu.isSet(Flag.CARRY));
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_a_cp_d_when_a_less_than_d() {
        Cpu cpu = runProgram(0x3e, 0x42, 0x16, 0x57, 0xba);
        assertTrue(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_a_cp_d_when_a_greater_than_d() {
        Cpu cpu = runProgram(0x3e, 0xe0, 0x16, 0x94, 0xba);
        assertFalse(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_a_cp_d_when_a_equal_to_d() {
        Cpu cpu = runProgram(0x3e, 0x15, 0x16, 0x15, 0xba);
        assertFalse(cpu.isSet(Flag.CARRY));
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_a_cp_e_when_a_less_than_e() {
        Cpu cpu = runProgram(0x3e, 0x3c, 0x1e, 0xff, 0xbb);
        assertTrue(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_a_cp_e_when_a_greater_than_e() {
        Cpu cpu = runProgram(0x3e, 0x65, 0x1e, 0x3f, 0xbb);
        assertFalse(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_a_cp_e_when_a_equal_to_e() {
        Cpu cpu = runProgram(0x3e, 0x0a, 0x1e, 0x0a, 0xbb);
        assertFalse(cpu.isSet(Flag.CARRY));
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_a_cp_h_when_a_less_than_h() {
        Cpu cpu = runProgram(0x3e, 0xc9, 0x26, 0xeb, 0xbc);
        assertTrue(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_a_cp_h_when_a_greater_than_h() {
        Cpu cpu = runProgram(0x3e, 0xa0, 0x26, 0x1e, 0xbc);
        assertFalse(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_a_cp_h_when_a_equal_to_h() {
        Cpu cpu = runProgram(0x3e, 0xca, 0x26, 0xca, 0xbc);
        assertFalse(cpu.isSet(Flag.CARRY));
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_a_cp_l_when_a_less_than_l() {
        Cpu cpu = runProgram(0x3e, 0x2e, 0x2e, 0x37, 0xbd);
        assertTrue(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_a_cp_l_when_a_greater_than_l() {
        Cpu cpu = runProgram(0x3e, 0xda, 0x2e, 0x69, 0xbd);
        assertFalse(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_a_cp_l_when_a_equal_to_l() {
        Cpu cpu = runProgram(0x3e, 0x9d, 0x2e, 0x9d, 0xbd);
        assertFalse(cpu.isSet(Flag.CARRY));
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_a_cp_a() {
        Cpu cpu = runProgram(0x3e, 0x8f, 0xbf);
        assertFalse(cpu.isSet(Flag.CARRY));
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_a_cp_indirect_hl_when_a_less_than_hl_mem() {
        Cpu cpu = runProgram(0x21, 0x12, 0x34, 0x3e, 0xbc, 0x36, 0xc6, 0xbe);
        assertTrue(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_a_cp_indirect_hl_when_a_greater_than_hl_mem() {
        Cpu cpu = runProgram(0x21, 0xaa, 0xbb, 0x3e, 0xbf, 0x36, 0x19, 0xbe);
        assertFalse(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_a_cp_indirect_hl_when_a_equal_to_hl_mem() {
        Cpu cpu = runProgram(0x21, 0xab, 0xcd, 0x3e, 0x5f, 0x36, 0x5f, 0xbe);
        assertFalse(cpu.isSet(Flag.CARRY));
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_a_cp_indirect_hl_uses_8_cycles() {
        Cpu cpu = runProgram(0xbe);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_a_cp_argument_when_a_less_than_arg() {
        Cpu cpu = runProgram(0x3e, 0x27, 0xfe, 0x3d);
        assertTrue(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_a_cp_argument_when_a_greater_than_arg() {
        Cpu cpu = runProgram(0x3e, 0xef, 0xfe, 0xb4);
        assertFalse(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_a_cp_argument_when_a_equal_to_arg() {
        Cpu cpu = runProgram(0x3e, 0xbb, 0xfe, 0xbb);
        assertFalse(cpu.isSet(Flag.CARRY));
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_a_cp_argument_uses_8_cycles() {
        Cpu cpu = runProgram(0xfe, 0x00);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_hl_add_bc_equals_zero_when_hl_and_bc_are_zero() {
        Cpu cpu = runProgram(0x09);
        assertEquals(0x00, cpu.read(Word.Register.HL));
    }

    @Test
    public void test_hl_add_bc_uses_8_cycles() {
        Cpu cpu = runProgram(0x09);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_hl_add_bc_when_hl_is_zero() {
        Cpu cpu = runProgram(0x01, 0x17, 0x9f, 0x09);
        assertEquals(0x9f17, cpu.read(Word.Register.BC));
    }

    @Test
    public void test_hl_add_bc_when_bc_is_zero() {
        Cpu cpu = runProgram(0x21, 0xfc, 0x18, 0x09);
        assertEquals(0x18fc, cpu.read(Word.Register.HL));
    }

    @Test
    public void test_hl_add_bc_with_arbitrary_arguments() {
        Cpu cpu = runProgram(
                0x21, 0x0e, 0x02,
                0x01, 0x91, 0x9f,
                0x09
        );
        assertEquals(0xa19f, cpu.read(Word.Register.HL));
    }

    @Test
    public void test_hl_add_bc_leaves_bc_alone() {
        Cpu cpu = runProgram(
                0x21, 0x93, 0x4c,
                0x01, 0xbc, 0x2c,
                0x09
        );
        assertEquals(0x2cbc, cpu.read(Word.Register.BC));
    }

    @Test
    public void test_hl_add_bc_rolls_over_at_0x10000() {
        Cpu cpu = runProgram(
                0x21, 0xba, 0xe1,
                0x01, 0x3b, 0x88,
                0x09
        );
        assertEquals(0x69f5, cpu.read(Word.Register.HL));
    }

    @Test
    public void test_hl_add_bc_does_not_set_zero_flag() {
        // Even when the result of the operation is zero, the flag should never be set.
        Cpu cpu = runProgram(0x09);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_hl_add_bc_does_not_reset_zero_flag() {
        // Even when the result of the operation is nonzero, the zero flag should be left unchanged.
        Cpu cpu = runProgram(0x87, 0x21, 0xab, 0xcd, 0x09);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_hl_add_bc_resets_operation_flag() {
        Cpu cpu = runProgram(0x97, 0x09);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_hl_add_bc_resets_carry_flag_if_no_carry_occurs() {
        Cpu cpu = runProgram(
                0x3e, 0x80, 0x87, // Set carry flag
                0x21, 0xaa, 0xaa, // LD HL, "0b 1010 1010 1010 1010"
                0x01, 0x55, 0x55, // LD BC, "0b 0101 0101 0101 0101"
                0x09              // ADD HL, BC
        );
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_hl_add_bc_sets_carry_flag_if_simple_bit_15_carry_occurs() {
        Cpu cpu = runProgram(
                0x21, 0x00, 0x80, // LD HL, 0x8000
                0x01, 0x00, 0x80, // LD BC, 0x8000
                0x09              // ADD HL, BC
        );
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_hl_add_bc_doesnt_set_carry_flag_for_carry_on_lower_bits() {
        Cpu cpu = runProgram(
                0x21, 0xff, 0x7f, // LD HL, 0x7fff
                0x01, 0xff, 0x7f, // LD BC, 0x7fff
                0x09
        );
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_hl_add_bc_sets_carry_flag_on_chained_carry_from_bit_15() {
        Cpu cpu = runProgram(
                0x21, 0xff, 0xff, // LD HL, 0xffff
                0x01, 0x01, 0x00, // LD BC, 0x0001
                0x09
        );
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_hl_add_bc_sets_nibble_flag_on_simple_carry_from_bit_11() {
        Cpu cpu = runProgram(
                0x21, 0x00, 0x08, // LD HL, 0x0800
                0x01, 0x00, 0x08, // LD BC, 0x0800
                0x09
        );
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_hl_add_bc_resets_nibble_flag_when_no_carry_from_bit_11_occurs() {
        Cpu cpu = runProgram(
                0x3e, 0x08, 0x87, // Set nibble flag
                0x21, 0xff, 0xf7, // LD HL, 0xf7ff
                0x01, 0xff, 0xf7, // LD BC, 0xf7ff
                0x09
        );
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_hl_add_bc_sets_nibble_flag_on_chained_carry_from_bit_11() {
        Cpu cpu = runProgram(
                0x01, 0xff, 0x0f, // LD BC, 0x0fff
                0x21, 0x01, 0x00, // LD HL, 0x0001
                0x09
        );
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_hl_add_de_with_arbitrary_arguments() {
        Cpu cpu = runProgram(
                0x21, 0x47, 0x09,
                0x11, 0x72, 0x4c,
                0x19
        );
        assertEquals(0x55b9, cpu.read(Word.Register.HL));
    }

    @Test
    public void test_hl_add_hl_with_arbitrary_arguments() {
        Cpu cpu = runProgram(
                0x21, 0xc9, 0xec,
                0x29
        );
        assertEquals(0xd992, cpu.read(Word.Register.HL));
    }

    @Test
    public void test_hl_add_sp_with_arbitrary_arguments() {
        Cpu cpu = runProgram(
                0x21, 0x5a, 0xa1,
                0x31, 0xb2, 0x93,
                0x39
        );
        assertEquals(0x350c, cpu.read(Word.Register.HL));
    }

    @Test
    public void test_sp_add_argument() {
        Cpu cpu = runProgram(
                0x31, 0xb7, 0x6c,
                0xe8, 0xf3
        );
        assertEquals(0x6daa, cpu.read(Word.Register.SP));
    }

    @Test
    public void test_sp_add_argument_uses_16_cycles() {
        Cpu cpu = runProgram(0xe8, 0x00);
        assertEquals(16, cpu.getCycles());
    }

    @Test
    public void test_increment_bc_from_0x00_to_0x01() {
        Cpu cpu = runProgram(0x03);
        assertEquals(0x0001, cpu.read(Word.Register.BC));
    }

    @Test
    public void test_increment_bc_uses_8_cycles() {
        Cpu cpu = runProgram(0x03);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_increment_bc_from_0xab_to_0xac() {
        Cpu cpu = runProgram(0x0e, 0xab, 0x03);
        assertEquals(0x00ac, cpu.read(Word.Register.BC));
    }

    @Test
    public void test_increment_bc_from_0x1f_to_0x20() {
        Cpu cpu = runProgram(0x0e, 0x1f, 0x03);
        assertEquals(0x0020, cpu.read(Word.Register.BC));
    }

    @Test
    public void test_increment_bc_rolls_over_at_0x010000() {
        Cpu cpu = runProgram(0x01, 0xff, 0xff, 0x03);
        assertEquals(0x0000, cpu.read(Word.Register.BC));
    }

    @Test
    public void test_increment_bc_does_not_roll_over_at_0x0100() {
        Cpu cpu = runProgram(0x0e, 0xff, 0x03);
        assertEquals(0x0100, cpu.read(Word.Register.BC));
    }

    @Test
    public void test_increment_bc_from_0xcaff_to_0xcb00() {
        Cpu cpu = runProgram(0x01, 0xff, 0xca, 0x03);
        assertEquals(0xcb00, cpu.read(Word.Register.BC));
    }

    @Test
    public void test_increment_bc_does_not_set_zero_flag() {
        Cpu cpu = runProgram(0x01, 0xff, 0xff, 0x03);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_increment_bc_does_not_reset_zero_flag() {
        Cpu cpu = runProgram(0x8f, 0x03);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_increment_bc_does_not_set_carry_flag() {
        Cpu cpu = runProgram(0x01, 0xff, 0xff, 0x03);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_increment_bc_does_not_reset_carry_flag() {
        Cpu cpu = runProgram(
                0x3e, 0x80, 0x87, // Set carry flag
                0x03              // INC BC
        );
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_increment_bc_does_not_set_nibble_flag() {
        Cpu cpu = runProgram(0x01, 0xff, 0xff, 0x03);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_increment_bc_does_not_reset_nibble_flag() {
        Cpu cpu = runProgram(
                0x3e, 0x08, 0x87, // Set nibble flag
                0x03              // INC BC
        );
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_increment_bc_does_not_set_operation_flag() {
        Cpu cpu = runProgram(0x01, 0xff, 0xff, 0x03);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_increment_bc_does_not_reset_operation_flag() {
        Cpu cpu = runProgram(0x97, 0x03);
        assertTrue(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_increment_de() {
        Cpu cpu = runProgram(0x11, 0xfc, 0x59, 0x13);
        assertEquals(0x59fd, cpu.read(Word.Register.DE));
    }

    @Test
    public void test_increment_hl() {
        Cpu cpu = runProgram(0x21, 0xee, 0xb1, 0x23);
        assertEquals(0xb1ef, cpu.read(Word.Register.HL));
    }

    @Test
    public void test_increment_sp() {
        Cpu cpu = runProgram(0x31, 0xc0, 0x99, 0x33);
        assertEquals(0x99c1, cpu.read(Word.Register.SP));
    }

    @Test
    public void test_decrement_bc_from_0x01_to_0x00() {
        Cpu cpu = runProgram(0x0e, 0x01, 0x0b);
        assertEquals(0x0000, cpu.read(Word.Register.BC));
    }

    @Test
    public void test_decrement_bc_from_0x20_to_0x1f() {
        Cpu cpu = runProgram(0x0e, 0x20, 0x0b);
        assertEquals(0x001f, cpu.read(Word.Register.BC));
    }

    @Test
    public void test_decrement_bc_from_0xff_to_0xfe() {
        Cpu cpu = runProgram(0x0e, 0xff, 0x0b);
        assertEquals(0x00fe, cpu.read(Word.Register.BC));
    }

    @Test
    public void test_decrement_bc_when_bc_is_0x00_rolls_over_to_0xffff() {
        Cpu cpu = runProgram(0x0b);
        assertEquals(0xffff, cpu.read(Word.Register.BC));
    }

    @Test
    public void test_decrement_bc_from_0xca00_to_0xc9ff() {
        Cpu cpu = runProgram(0x01, 0x00, 0xca, 0x0b);
        assertEquals(0xc9ff, cpu.read(Word.Register.BC));
    }

    @Test
    public void test_decrement_bc_uses_8_cycles() {
        Cpu cpu = runProgram(0x0b);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_decrement_bc_does_not_set_zero_flag() {
        Cpu cpu = runProgram(0x0e, 0x01, 0x0b);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_decrement_bc_does_not_reset_zero_flag() {
        Cpu cpu = runProgram(0x8f, 0x0b);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_decrement_bc_does_not_set_carry_flag() {
        Cpu cpu = runProgram(0x01, 0x00, 0x80, 0x0b);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_decrement_bc_does_not_reset_carry_flag() {
        Cpu cpu = runProgram(
                0x3e, 0x80, 0x87, // Set carry flag
                0x01, 0xff, 0xff, 0x0b);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_decrement_bc_does_not_set_nibble_flag() {
        Cpu cpu = runProgram(0x01, 0x00, 0x08, 0x0b);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_decrement_bc_does_not_reset_nibble_flag() {
        Cpu cpu = runProgram(
                0x3e, 0x08, 0x87, // Set nibble flag
                0x01, 0xff, 0xff, 0x0b);
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_decrement_bc_does_not_set_operation_flag() {
        Cpu cpu = runProgram(0x0b);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_decrement_bc_does_not_reset_operation_flag() {
        Cpu cpu = runProgram(0x97, 0x0b);
        assertTrue(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_decrement_de() {
        Cpu cpu = runProgram(0x11, 0x7c, 0x17, 0x1b);
        assertEquals(0x177b, cpu.read(Word.Register.DE));
    }

    @Test
    public void test_decrement_hl() {
        Cpu cpu = runProgram(0x21, 0x72, 0xa2, 0x2b);
        assertEquals(0xa271, cpu.read(Word.Register.HL));
    }

    @Test
    public void test_decrement_sp() {
        Cpu cpu = runProgram(0x31, 0x73, 0xff, 0x3b);
        assertEquals(0xff72, cpu.read(Word.Register.SP));
    }

    @Test
    public void test_swap_a_when_a_is_0x00_gives_0x00() {
        Cpu cpu = runProgram(0xcb, 0x37);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_swap_a_when_a_is_0x01_gives_0x10() {
        Cpu cpu = runProgram(0x3e, 0x01, 0xcb, 0x37);
        assertEquals(0x10, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_swap_a_uses_8_cycles() {
        Cpu cpu = runProgram(0xcb, 0x37);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_swap_a_when_a_is_0x10_gives_0x01() {
        Cpu cpu = runProgram(0x3e, 0x10, 0xcb, 0x37);
        assertEquals(0x01, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_swap_a_when_a_is_ab_gives_ba() {
        Cpu cpu = runProgram(0x3e, 0xab, 0xcb, 0x37);
        assertEquals(0xba, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_swap_a_when_a_is_zero_sets_zero_flag() {
        Cpu cpu = runProgram(0xcb, 0x37);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_swap_a_when_a_is_nonzero_resets_zero_flag() {
        Cpu cpu = runProgram(0x8f, 0x3e, 0x13, 0xcb, 0x37);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_swap_a_doesnt_set_carry_flag() {
        Cpu cpu = runProgram(0x3e, 0xf0, 0xcb, 0x37);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_swap_a_doesnt_reset_carry_flag() {
        Cpu cpu = runProgram(
                0x3e, 0x80, 0x87, // Set carry flag
                0x3e, 0xf0, 0xcb, 0x37
        );
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_swap_a_doesnt_set_nibble_flag() {
        Cpu cpu = runProgram(0x3e, 0x0f, 0xcb, 0x37);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_swap_a_doesnt_reset_nibble_flag() {
        Cpu cpu = runProgram(
                0x3e, 0x08, 0x87, // Set nibble flag
                0x3e, 0x0f, 0xcb, 0x37
        );
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_swap_a_doesnt_set_operation_flag() {
        Cpu cpu = runProgram(0xcb, 0x37);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_swap_a_doesnt_reset_operation_flag() {
        Cpu cpu = runProgram(0x97, 0xcb, 0x37);
        assertTrue(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_swap_b() {
        Cpu cpu = runProgram(0x06, 0x79, 0xcb, 0x30);
        assertEquals(0x97, cpu.read(Byte.Register.B));
    }

    @Test
    public void test_program_counter_correct_after_extended_operation() {
        Cpu cpu = runProgram(0xcb, 0x30, 0x3c);
        assertEquals(0x01, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_swap_c() {
        Cpu cpu = runProgram(0x0e, 0xfe, 0xcb, 0x31);
        assertEquals(0xef, cpu.read(Byte.Register.C));
    }

    @Test
    public void test_swap_d() {
        Cpu cpu = runProgram(0x16, 0xa7, 0xcb, 0x32);
        assertEquals(0x7a, cpu.read(Byte.Register.D));
    }

    @Test
    public void test_swap_e() {
        Cpu cpu = runProgram(0x1e, 0x0c, 0xcb, 0x33);
        assertEquals(0xc0, cpu.read(Byte.Register.E));
    }

    @Test
    public void test_swap_h() {
        Cpu cpu = runProgram(0x26, 0xfd, 0xcb, 0x34);
        assertEquals(0xdf, cpu.read(Byte.Register.H));
    }

    @Test
    public void test_swap_l() {
        Cpu cpu = runProgram(0x2e, 0x57, 0xcb, 0x35);
        assertEquals(0x75, cpu.read(Byte.Register.L));
    }

    @Test
    public void test_swap_hl_indirect() {
        Cpu cpu = runProgram(
                0x21, 0x98, 0x1f, // LD HL,   0x1f98
                0x36, 0x97,       // LD (HL), 0x97
                0xcb, 0x36
        );
        assertEquals(0x79, cpu.readFrom(Pointer.literal(0x1f98)));
    }

    @Test
    public void test_swap_hl_indirect_uses_16_cycles() {
        Cpu cpu = runProgram(0xcb, 0x36);
        assertEquals(16, cpu.getCycles());
    }

    @Test
    public void test_daa_on_0x00_when_flags_are_false() {
        Cpu cpu = runProgram(0x27);
        assertEquals(0x00, cpu.read(Byte.Register.A));
        assertFalse(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.OPERATION));
        assertTrue(cpu.isSet(Flag.ZERO));
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_uses_4_cycles() {
        Cpu cpu = runProgram(0x27);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_daa_on_0x76_when_flags_are_false() {
        Cpu cpu = runProgram(0x3e, 0x76, 0x27);
        assertEquals(0x76, cpu.read(Byte.Register.A));
        assertFalse(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.OPERATION));
        assertFalse(cpu.isSet(Flag.ZERO));
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_on_0x99_when_flags_are_false() {
        Cpu cpu = runProgram(0x3e, 0x99, 0x27);
        assertEquals(0x99, cpu.read(Byte.Register.A));
        assertFalse(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.OPERATION));
        assertFalse(cpu.isSet(Flag.ZERO));
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_on_0x0a_when_flags_are_false() {
        Cpu cpu = runProgram(0x3e, 0x0a, 0x27);
        assertEquals(0x10, cpu.read(Byte.Register.A));
        assertFalse(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.OPERATION));
        assertFalse(cpu.isSet(Flag.ZERO));
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_on_0x6d_when_flags_are_false() {
        Cpu cpu = runProgram(0x3e, 0x6d, 0x27);
        assertEquals(0x73, cpu.read(Byte.Register.A));
        assertFalse(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.OPERATION));
        assertFalse(cpu.isSet(Flag.ZERO));
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_on_0x8f_when_flags_are_false() {
        Cpu cpu = runProgram(0x3e, 0x8f, 0x27);
        assertEquals(0x95, cpu.read(Byte.Register.A));
        assertFalse(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.OPERATION));
        assertFalse(cpu.isSet(Flag.ZERO));
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_on_0xa0_when_flags_are_false() {
        Cpu cpu = runProgram(0x3e, 0xa0, 0x27);
        assertEquals(0x00, cpu.read(Byte.Register.A));
        assertTrue(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.OPERATION));
        assertTrue(cpu.isSet(Flag.ZERO));
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_on_0xc7_when_flags_are_false() {
        Cpu cpu = runProgram(0x3e, 0xc7, 0x27);
        assertEquals(0x27, cpu.read(Byte.Register.A));
        assertTrue(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.OPERATION));
        assertFalse(cpu.isSet(Flag.ZERO));
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_on_0xf9_when_flags_are_false() {
        Cpu cpu = runProgram(0x3e, 0xf9, 0x27);
        assertEquals(0x59, cpu.read(Byte.Register.A));
        assertTrue(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.OPERATION));
        assertFalse(cpu.isSet(Flag.ZERO));
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_on_0x9a_when_flags_are_false() {
        Cpu cpu = runProgram(0x3e, 0x9a, 0x27);
        assertEquals(0x00, cpu.read(Byte.Register.A));
        assertTrue(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.OPERATION));
        assertTrue(cpu.isSet(Flag.ZERO));
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_on_0xbe_when_flags_are_false() {
        Cpu cpu = runProgram(0x3e, 0xbe, 0x27);
        assertEquals(0x24, cpu.read(Byte.Register.A));
        assertTrue(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.OPERATION));
        assertFalse(cpu.isSet(Flag.ZERO));
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_on_0xff_when_flags_are_false() {
        Cpu cpu = runProgram(0x3e, 0xff, 0x27);
        assertEquals(0x65, cpu.read(Byte.Register.A));
        assertTrue(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.OPERATION));
        assertFalse(cpu.isSet(Flag.ZERO));
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_on_0x00_when_carry_is_true() {
        Cpu cpu = cpuWithProgram(0x27);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 1);
        assertEquals(0x60, cpu.read(Byte.Register.A));
        assertTrue(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.OPERATION));
        assertFalse(cpu.isSet(Flag.ZERO));
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_on_0x18_when_carry_is_true() {
        Cpu cpu = cpuWithProgram(0x3e, 0x18, 0x27);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertEquals(0x78, cpu.read(Byte.Register.A));
        assertTrue(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.OPERATION));
        assertFalse(cpu.isSet(Flag.ZERO));
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_on_0x29_when_carry_is_true() {
        Cpu cpu = cpuWithProgram(0x3e, 0x29, 0x27);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertEquals(0x89, cpu.read(Byte.Register.A));
        assertTrue(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.OPERATION));
        assertFalse(cpu.isSet(Flag.ZERO));
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_on_0x0a_when_carry_is_true() {
        Cpu cpu = cpuWithProgram(0x3e, 0x0a, 0x27);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertEquals(0x70, cpu.read(Byte.Register.A));
        assertTrue(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.OPERATION));
        assertFalse(cpu.isSet(Flag.ZERO));
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_on_0x1b_when_carry_is_true() {
        Cpu cpu = cpuWithProgram(0x3e, 0x1b, 0x27);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertEquals(0x81, cpu.read(Byte.Register.A));
        assertTrue(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.OPERATION));
        assertFalse(cpu.isSet(Flag.ZERO));
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_on_0x2f_when_carry_is_true() {
        Cpu cpu = cpuWithProgram(0x3e, 0x2f, 0x27);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertEquals(0x95, cpu.read(Byte.Register.A));
        assertTrue(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.OPERATION));
        assertFalse(cpu.isSet(Flag.ZERO));
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_on_0x00_when_nibble_is_true() {
        Cpu cpu = cpuWithProgram(0x27);
        cpu.set(Flag.NIBBLE, true);
        runProgram(cpu, 1);
        assertEquals(0x06, cpu.read(Byte.Register.A));
        assertFalse(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.OPERATION));
        assertFalse(cpu.isSet(Flag.ZERO));
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_on_0x82_when_nibble_is_true() {
        Cpu cpu = cpuWithProgram(0x3e, 0x82, 0x27);
        cpu.set(Flag.NIBBLE, true);
        runProgram(cpu, 3);
        assertEquals(0x88, cpu.read(Byte.Register.A));
        assertFalse(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.OPERATION));
        assertFalse(cpu.isSet(Flag.ZERO));
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_on_0x93_when_nibble_is_true() {
        Cpu cpu = cpuWithProgram(0x3e, 0x93, 0x27);
        cpu.set(Flag.NIBBLE, true);
        runProgram(cpu, 3);
        assertEquals(0x99, cpu.read(Byte.Register.A));
        assertFalse(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.OPERATION));
        assertFalse(cpu.isSet(Flag.ZERO));
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_on_0xa0_when_nibble_is_true() {
        Cpu cpu = cpuWithProgram(0x3e, 0xa0, 0x27);
        cpu.set(Flag.NIBBLE, true);
        runProgram(cpu, 3);
        assertEquals(0x06, cpu.read(Byte.Register.A));
        assertTrue(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.OPERATION));
        assertFalse(cpu.isSet(Flag.ZERO));
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_on_0xd1_when_nibble_is_true() {
        Cpu cpu = cpuWithProgram(0x3e, 0xd1, 0x27);
        cpu.set(Flag.NIBBLE, true);
        runProgram(cpu, 3);
        assertEquals(0x37, cpu.read(Byte.Register.A));
        assertTrue(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.OPERATION));
        assertFalse(cpu.isSet(Flag.ZERO));
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_on_0xf3_when_nibble_is_true() {
        Cpu cpu = cpuWithProgram(0x3e, 0xf3, 0x27);
        cpu.set(Flag.NIBBLE, true);
        runProgram(cpu, 3);
        assertEquals(0x59, cpu.read(Byte.Register.A));
        assertTrue(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.OPERATION));
        assertFalse(cpu.isSet(Flag.ZERO));
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_on_0x00_when_nibble_and_carry_are_true() {
        Cpu cpu = cpuWithProgram(0x3e, 0x00, 0x27);
        cpu.set(Flag.NIBBLE, true);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertEquals(0x66, cpu.read(Byte.Register.A));
        assertTrue(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.OPERATION));
        assertFalse(cpu.isSet(Flag.ZERO));
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_on_0x21_when_nibble_and_carry_are_true() {
        Cpu cpu = cpuWithProgram(0x3e, 0x21, 0x27);
        cpu.set(Flag.NIBBLE, true);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertEquals(0x87, cpu.read(Byte.Register.A));
        assertTrue(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.OPERATION));
        assertFalse(cpu.isSet(Flag.ZERO));
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_on_0x33_when_nibble_and_carry_are_true() {
        Cpu cpu = cpuWithProgram(0x3e, 0x33, 0x27);
        cpu.set(Flag.NIBBLE, true);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertEquals(0x99, cpu.read(Byte.Register.A));
        assertTrue(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.OPERATION));
        assertFalse(cpu.isSet(Flag.ZERO));
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_on_0x06_when_operation_and_nibble_are_true() {
        Cpu cpu = cpuWithProgram(0x3e, 0x06, 0x27);
        cpu.set(Flag.OPERATION, true);
        cpu.set(Flag.NIBBLE, true);
        runProgram(cpu, 3);
        assertEquals(0x00, cpu.read(Byte.Register.A));
        assertTrue(cpu.isSet(Flag.ZERO));
        assertFalse(cpu.isSet(Flag.CARRY));
        assertTrue(cpu.isSet(Flag.OPERATION));
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_on_0x6c_when_operation_and_nibble_are_true() {
        Cpu cpu = cpuWithProgram(0x3e, 0x6c, 0x27);
        cpu.set(Flag.OPERATION, true);
        cpu.set(Flag.NIBBLE, true);
        runProgram(cpu, 3);
        assertEquals(0x66, cpu.read(Byte.Register.A));
        assertFalse(cpu.isSet(Flag.ZERO));
        assertFalse(cpu.isSet(Flag.CARRY));
        assertTrue(cpu.isSet(Flag.OPERATION));
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_on_0x8f_when_operation_and_nibble_are_true() {
        Cpu cpu = cpuWithProgram(0x3e, 0x8f, 0x27);
        cpu.set(Flag.OPERATION, true);
        cpu.set(Flag.NIBBLE, true);
        runProgram(cpu, 3);
        assertEquals(0x89, cpu.read(Byte.Register.A));
        assertFalse(cpu.isSet(Flag.ZERO));
        assertFalse(cpu.isSet(Flag.CARRY));
        assertTrue(cpu.isSet(Flag.OPERATION));
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_on_0x73_when_operation_and_carry_are_true() {
        Cpu cpu = cpuWithProgram(0x3e, 0x73, 0x27);
        cpu.set(Flag.OPERATION, true);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertEquals(0x13, cpu.read(Byte.Register.A));
        assertFalse(cpu.isSet(Flag.ZERO));
        assertTrue(cpu.isSet(Flag.CARRY));
        assertTrue(cpu.isSet(Flag.OPERATION));
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_on_0x90_when_operation_and_carry_are_true() {
        Cpu cpu = cpuWithProgram(0x3e, 0x90, 0x27);
        cpu.set(Flag.OPERATION, true);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertEquals(0x30, cpu.read(Byte.Register.A));
        assertFalse(cpu.isSet(Flag.ZERO));
        assertTrue(cpu.isSet(Flag.CARRY));
        assertTrue(cpu.isSet(Flag.OPERATION));
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_on_0xf9_when_operation_and_carry_are_true() {
        Cpu cpu = cpuWithProgram(0x3e, 0xf9, 0x27);
        cpu.set(Flag.OPERATION, true);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertEquals(0x99, cpu.read(Byte.Register.A));
        assertFalse(cpu.isSet(Flag.ZERO));
        assertTrue(cpu.isSet(Flag.CARRY));
        assertTrue(cpu.isSet(Flag.OPERATION));
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_on_0x66_when_operation_carry_and_nibble_are_true() {
        Cpu cpu = cpuWithProgram(0x3e, 0x66, 0x27);
        cpu.set(Flag.OPERATION, true);
        cpu.set(Flag.CARRY, true);
        cpu.set(Flag.NIBBLE, true);
        runProgram(cpu, 3);
        assertEquals(0x00, cpu.read(Byte.Register.A));
        assertTrue(cpu.isSet(Flag.ZERO));
        assertTrue(cpu.isSet(Flag.CARRY));
        assertTrue(cpu.isSet(Flag.OPERATION));
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_on_0x7a_when_operation_carry_and_nibble_are_true() {
        Cpu cpu = cpuWithProgram(0x3e, 0x7a, 0x27);
        cpu.set(Flag.OPERATION, true);
        cpu.set(Flag.CARRY, true);
        cpu.set(Flag.NIBBLE, true);
        runProgram(cpu, 3);
        assertEquals(0x14, cpu.read(Byte.Register.A));
        assertFalse(cpu.isSet(Flag.ZERO));
        assertTrue(cpu.isSet(Flag.CARRY));
        assertTrue(cpu.isSet(Flag.OPERATION));
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_on_0xdc_when_operation_carry_and_nibble_are_true() {
        Cpu cpu = cpuWithProgram(0x3e, 0xdb, 0x27);
        cpu.set(Flag.OPERATION, true);
        cpu.set(Flag.CARRY, true);
        cpu.set(Flag.NIBBLE, true);
        runProgram(cpu, 3);
        assertEquals(0x75, cpu.read(Byte.Register.A));
        assertFalse(cpu.isSet(Flag.ZERO));
        assertTrue(cpu.isSet(Flag.CARRY));
        assertTrue(cpu.isSet(Flag.OPERATION));
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_on_0xff_when_operation_carry_and_nibble_are_true() {
        Cpu cpu = cpuWithProgram(0x3e, 0xff, 0x27);
        cpu.set(Flag.OPERATION, true);
        cpu.set(Flag.CARRY, true);
        cpu.set(Flag.NIBBLE, true);
        runProgram(cpu, 3);
        assertEquals(0x99, cpu.read(Byte.Register.A));
        assertFalse(cpu.isSet(Flag.ZERO));
        assertTrue(cpu.isSet(Flag.CARRY));
        assertTrue(cpu.isSet(Flag.OPERATION));
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_on_0x00_when_operation_is_true() {
        Cpu cpu = cpuWithProgram(0x27);
        cpu.set(Flag.OPERATION, true);
        runProgram(cpu, 1);
        assertEquals(0x00, cpu.read(Byte.Register.A));
        assertTrue(cpu.isSet(Flag.ZERO));
        assertFalse(cpu.isSet(Flag.CARRY));
        assertTrue(cpu.isSet(Flag.OPERATION));
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_on_0x71_when_operation_is_true() {
        Cpu cpu = cpuWithProgram(0x3e, 0x71, 0x27);
        cpu.set(Flag.OPERATION, true);
        runProgram(cpu, 3);
        assertEquals(0x71, cpu.read(Byte.Register.A));
        assertFalse(cpu.isSet(Flag.ZERO));
        assertFalse(cpu.isSet(Flag.CARRY));
        assertTrue(cpu.isSet(Flag.OPERATION));
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_on_0x99_when_operation_is_true() {
        Cpu cpu = cpuWithProgram(0x3e, 0x99, 0x27);
        cpu.set(Flag.OPERATION, true);
        runProgram(cpu, 3);
        assertEquals(0x99, cpu.read(Byte.Register.A));
        assertFalse(cpu.isSet(Flag.ZERO));
        assertFalse(cpu.isSet(Flag.CARRY));
        assertTrue(cpu.isSet(Flag.OPERATION));
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_doesnt_set_operation_flag() {
        Cpu cpu = runProgram(0x3e, 0xff, 0x27);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_daa_doesnt_reset_operation_flag() {
        Cpu cpu = cpuWithProgram(0x3e, 0xff, 0x27);
        cpu.set(Flag.OPERATION, true);
        runProgram(cpu, 3);
        assertTrue(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_daa_doesnt_set_nibble_flag() {
        Cpu cpu = runProgram(0x3e, 0xf2, 0x27);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_daa_doesnt_reset_nibble_flag() {
        Cpu cpu = cpuWithProgram(0x3e, 0xf2, 0x27);
        cpu.set(Flag.NIBBLE, true);
        runProgram(cpu, 3);
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_cpl_0x00() {
        Cpu cpu = runProgram(0x2f);
        assertEquals(0xff, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_cpl_0xff() {
        Cpu cpu = runProgram(0x3e, 0xff, 0x2f);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_cpl_0xa7() {
        Cpu cpu = runProgram(0x3e, 0xa7, 0x2f);
        assertEquals(0x58, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_cpl_uses_4_cycles() {
        Cpu cpu = runProgram(0x2f);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_cpl_sets_operation_flag() {
        Cpu cpu = runProgram(0x2f);
        assertTrue(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_cpl_sets_nibble_flag() {
        Cpu cpu = runProgram(0x2f);
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_cpl_doesnt_set_carry_flag() {
        Cpu cpu = runProgram(0x2f);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_cpl_doesnt_reset_carry_flag() {
        Cpu cpu = cpuWithProgram(0x2f);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 1);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_cpl_doesnt_set_zero_flag() {
        Cpu cpu = runProgram(0x3e, 0xff, 0x2f);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_cpl_doesnt_reset_zero_flag() {
        Cpu cpu = cpuWithProgram(0x3e, 0xff, 0x2f);
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 3);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_ccf_sets_carry_flag() {
        Cpu cpu = runProgram(0x3f);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_ccf_resets_carry_flag() {
        Cpu cpu = cpuWithProgram(0x3f);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 1);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_ccf_takes_4_cycles() {
        Cpu cpu = runProgram(0x3f);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_ccf_clears_nibble_flag() {
        Cpu cpu = cpuWithProgram(0x3f);
        cpu.set(Flag.NIBBLE, true);
        runProgram(cpu, 1);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_ccf_clears_operation_flag() {
        Cpu cpu = cpuWithProgram(0x3f);
        cpu.set(Flag.OPERATION, true);
        runProgram(cpu, 1);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_scf_sets_carry_flag() {
        Cpu cpu = runProgram(0x37);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_scf_uses_4_cycles() {
        Cpu cpu = runProgram(0x37);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_scf_doesnt_reset_carry_flag() {
        Cpu cpu = cpuWithProgram(0x37);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 1);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_scf_doesnt_set_nibble_flag() {
        Cpu cpu = runProgram(0x37);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_scf_resets_nibble_flag() {
        Cpu cpu = cpuWithProgram(0x37);
        cpu.set(Flag.NIBBLE, true);
        runProgram(cpu, 1);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_scf_doesnt_set_operation_flag() {
        Cpu cpu = runProgram(0x37);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_scf_resets_operation_flag() {
        Cpu cpu = cpuWithProgram(0x37);
        cpu.set(Flag.OPERATION, true);
        runProgram(cpu, 1);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test(expected=UnsupportedOperationException.class)
    public void test_stop_is_unsupported() {
        runProgram(0x10, 0x00);
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_0x10_is_not_stop_when_followed_by_0x01() {
        runProgram(0x10, 0x01);
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_0x10_is_not_stop_when_followed_by_0x17() {
        runProgram(0x10, 0x17);
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_0x10_is_not_stop_when_followed_by_0xff() {
        runProgram(0x10, 0xff);
    }

    @Test
    public void test_di_disables_interrupts() {
        Cpu cpu = cpuWithProgram(0xf3);
        cpu.setInterruptsEnabled(true);
        cpu.tick();
        assertFalse(cpu.interruptsEnabled);
    }

    @Test
    public void test_di_does_not_enable_interrupts() {
        Cpu cpu = cpuWithProgram(0xf3);
        cpu.setInterruptsEnabled(false);
        cpu.tick();
        assertFalse(cpu.interruptsEnabled);
    }

    @Test
    public void test_di_uses_4_cycles() {
        Cpu cpu = runProgram(0xf3);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_ei_enables_interrupts() {
        Cpu cpu = cpuWithProgram(0xfb);
        cpu.setInterruptsEnabled(false);
        cpu.tick();
        assertTrue(cpu.interruptsEnabled);
    }

    @Test
    public void test_ei_does_not_disable_interrupts() {
        Cpu cpu = cpuWithProgram(0xfb);
        cpu.setInterruptsEnabled(true);
        cpu.tick();
        assertTrue(cpu.interruptsEnabled);
    }

    @Test
    public void test_ei_uses_4_cycles() {
        Cpu cpu = runProgram(0xfb);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_rlca_of_0x00_is_0x00() {
        Cpu cpu = runProgram(0x07);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_rlca_uses_4_cycles() {
        Cpu cpu = runProgram(0x07);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_rlca_of_0x01_is_0x02() {
        Cpu cpu = runProgram(0x3e, 0x01, 0x07);
        assertEquals(0x02, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_rlca_of_0b01100111_is_0b11001110() {
        Cpu cpu = runProgram(0x3e, 0b01100111, 0x07);
        assertEquals(0b11001110, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_rlca_of_0x80_is_0x01() {
        Cpu cpu = runProgram(0x3e, 0x80, 0x07);
        assertEquals(0x01, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_rlca_of_0x00_is_0x00_even_if_carry_is_high() {
        Cpu cpu = cpuWithProgram(0x07);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 1);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_rlca_of_0b11010110_is_0b10101101() {
        Cpu cpu = runProgram(0x3e, 0b11010110, 0x07);
        assertEquals(0b10101101, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_rlca_of_0x00_is_still_0x00_when_carry_is_true() {
        Cpu cpu = cpuWithProgram(0x07);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 1);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_rlca_copies_bit_7_to_carry_flag_when_bit_7_is_high() {
        Cpu cpu = runProgram(0x3e, 0x80, 0x07);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rlca_copies_bit_7_to_carry_flag_when_7_and_6_are_high() {
        Cpu cpu = runProgram(0x3e, 0xc0, 0x07);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rlca_copies_bit_7_to_carry_flag_when_bit_7_is_low() {
        Cpu cpu = cpuWithProgram(0x07);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 1);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rlca_preserves_high_carry_flag_when_bit_7_is_high() {
        Cpu cpu = cpuWithProgram(0x3e, 0x80, 0x07);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rlca_preserves_low_carry_flag_when_bit_7_is_low() {
        Cpu cpu = runProgram(0x07);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rlca_does_not_set_zero_flag() {
        // Contra GBCPUMan, the official manual says zero should always
        // be reset when RCLA is called.
        Cpu cpu = runProgram(0x07);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_rlca_resets_zero_flag() {
        Cpu cpu = cpuWithProgram(0x07);
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 1);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_rlca_does_not_set_nibble_flag() {
        Cpu cpu = runProgram(0x3e, 0x08, 0x07);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_rlca_resets_nibble_flag() {
        Cpu cpu = cpuWithProgram(0x3e, 0x08, 0x07);
        cpu.set(Flag.NIBBLE, true);
        runProgram(cpu, 3);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_rlca_does_not_set_operation_flag() {
        Cpu cpu = runProgram(0x3e, 0x10, 0x07);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_rlca_resets_operation_flag() {
        Cpu cpu = cpuWithProgram(0x3e, 0x10, 0x07);
        cpu.set(Flag.OPERATION, true);
        runProgram(cpu, 3);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_rla_of_0x00_is_0x00_when_carry_is_false() {
        Cpu cpu = runProgram(0x17);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_rla_uses_4_cycles() {
        Cpu cpu = runProgram(0x17);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_rla_of_0x01_is_0x02_when_carry_is_false() {
        Cpu cpu = runProgram(0x3e, 0x01, 0x17);
        assertEquals(0x02, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_rla_of_0b01110111_is_0b11101110_when_carry_is_false() {
        Cpu cpu = runProgram(0x3e, 0b01110111, 0x17);
        assertEquals(0b11101110, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_rla_of_0b10110101_is_0b01101010_when_carry_is_false() {
        Cpu cpu = runProgram(0x3e, 0b10110101, 0x17);
        assertEquals(0b01101010, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_rla_of_0x80_is_0x00() {
        Cpu cpu = runProgram(0x3e, 0x80, 0x17);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_rla_of_0x00_is_0x01_when_carry_is_set() {
        Cpu cpu = cpuWithProgram(0x3e, 0x00, 0x17);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertEquals(0x01, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_rla_of_0xff_is_0xfe_when_carry_is_unset() {
        Cpu cpu = runProgram(0x3e, 0xff, 0x17);
        assertEquals(0xfe, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_rla_of_0xff_is_0xff_when_carry_is_set() {
        Cpu cpu = cpuWithProgram(0x3e, 0xff, 0x17);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertEquals(0xff, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_rla_of_0x80_sets_carry_high() {
        Cpu cpu = runProgram(0x3e, 0x80, 0x17);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rla_of_0xc0_sets_carry_high() {
        Cpu cpu = runProgram(0x3e, 0xc0, 0x17);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rla_of_0x80_doesnt_reset_carry() {
        Cpu cpu = cpuWithProgram(0x3e, 0x80, 0x17);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rla_of_0x7f_sets_carry_low() {
        Cpu cpu = cpuWithProgram(0x3e, 0x7f, 0x17);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rla_of_0x7f_doesnt_bring_carry_high() {
        Cpu cpu = runProgram(0x3e, 0x7f, 0x17);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rla_doesnt_set_zero_flag() {
        Cpu cpu = runProgram(0x17);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_rla_resets_zero_flag() {
        Cpu cpu = cpuWithProgram(0x17);
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 1);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_rla_resets_nibble_flag() {
        Cpu cpu = cpuWithProgram(0x3e, 0x08, 0x17);
        cpu.set(Flag.NIBBLE, true);
        runProgram(cpu, 3);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_rla_does_not_set_nibble_flag() {
        Cpu cpu = runProgram(0x3e, 0x08, 0x17);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_rla_resets_operation_flag() {
        Cpu cpu = cpuWithProgram(0x3e, 0xf0, 0x17);
        cpu.set(Flag.OPERATION, true);
        runProgram(cpu, 3);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_rla_does_not_set_operation_flag() {
        Cpu cpu = runProgram(0x3e, 0xf0, 0x17);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_rrca_of_0x00_is_0x00() {
        Cpu cpu = runProgram(0x0f);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_rrca_uses_4_cycles() {
        Cpu cpu = runProgram(0x0f);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_rrca_of_0x80_is_0x40() {
        Cpu cpu = runProgram(0x3e, 0x80, 0x0f);
        assertEquals(0x40, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_rrca_of_0b11101010_is_0b01110101() {
        Cpu cpu = runProgram(0x3e, 0b11101010, 0x0f);
        assertEquals(0b01110101, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_rrca_of_0x01_is_0x80() {
        Cpu cpu = runProgram(0x3e, 0x01, 0x0f);
        assertEquals(0x80, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_rrca_of_0b11101011_is_0b11110101() {
        Cpu cpu = runProgram(0x3e, 0b11101011, 0x0f);
        assertEquals(0b11110101, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_rrca_of_0x00_is_0x00_even_if_carry_is_high() {
        Cpu cpu = cpuWithProgram(0x0f);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 1);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_rrca_of_0x01_sets_carry_high() {
        Cpu cpu = runProgram(0x3e, 0x01, 0x0f);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rrca_of_0x11_sets_carry_high() {
        Cpu cpu = runProgram(0x3e, 0x11, 0x0f);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rrca_of_0xfe_resets_carry() {
        Cpu cpu = cpuWithProgram(0x3e, 0xfe, 0x0f);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rrca_of_0xfe_does_not_set_carry() {
        Cpu cpu = runProgram(0x3e, 0xfe, 0x0f);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rrca_does_not_set_zero() {
        Cpu cpu = runProgram(0x0f);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_rrca_resets_zero() {
        Cpu cpu = cpuWithProgram(0x0f);
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 1);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_rrca_does_not_set_nibble() {
        Cpu cpu = runProgram(0x3e, 0x10, 0x0f);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_rrca_resets_nibble() {
        Cpu cpu = cpuWithProgram(0x3e, 0x10, 0x0f);
        cpu.set(Flag.NIBBLE, true);
        runProgram(cpu, 3);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_rrca_does_not_set_operation() {
        Cpu cpu = runProgram(0x3e, 0x0f, 0x0f);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_rrca_resets_operation() {
        Cpu cpu = cpuWithProgram(0x3e, 0x0f, 0x0f);
        cpu.set(Flag.OPERATION, true);
        runProgram(cpu, 3);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_rra_of_0x00_is_0x00_when_carry_is_false() {
        Cpu cpu = runProgram(0x1f);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_rra_uses_4_cycles() {
        Cpu cpu = runProgram(0x1f);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_rra_of_0x80_is_0x40_when_carry_is_false() {
        Cpu cpu = runProgram(0x3e, 0x80, 0x1f);
        assertEquals(0x40, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_rra_of_0b10101101_is_0b01010110_when_carry_is_false() {
        Cpu cpu = runProgram(0x3e, 0b10101101, 0x1f);
        assertEquals(0b01010110, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_rra_of_0x01_is_0x00_when_carry_is_false() {
        Cpu cpu = runProgram(0x3e, 0x01, 0x1f);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_rra_of_0x00_is_0x80_when_carry_is_true() {
        Cpu cpu = cpuWithProgram(0x1f);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 1);
        assertEquals(0x80, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_rra_of_0xff_is_0x7f_when_carry_is_false() {
        Cpu cpu = runProgram(0x3e, 0xff, 0x1f);
        assertEquals(0x7f, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_rra_of_0xff_is_0xff_when_carry_is_true() {
        Cpu cpu = cpuWithProgram(0x3e, 0xff, 0x1f);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertEquals(0xff, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_rra_of_0x01_sets_carry_high() {
        Cpu cpu = runProgram(0x3e, 0x01, 0x1f);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rra_of_0x02_does_not_set_carry_high() {
        Cpu cpu = runProgram(0x3e, 0x02, 0x1f);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rra_of_0x11_sets_carry_high() {
        Cpu cpu = runProgram(0x3e, 0x11, 0x1f);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rra_of_0xfe_does_not_set_carry_high() {
        Cpu cpu = runProgram(0x3e, 0xfe, 0x1f);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rra_of_0xfe_brings_carry_low() {
        Cpu cpu = cpuWithProgram(0x3e, 0xfe, 0x1f);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rra_of_0x01_does_not_bring_carry_low() {
        Cpu cpu = cpuWithProgram(0x3e, 0x01, 0x1f);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rra_does_not_set_zero_flag() {
        Cpu cpu = runProgram(0x1f);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_rra_resets_zero_flag() {
        Cpu cpu = cpuWithProgram(0x1f);
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 1);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_rra_does_not_set_nibble_flag() {
        Cpu cpu = runProgram(0x3e, 0x10, 0x1f);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_rra_resets_nibble_flag() {
        Cpu cpu = cpuWithProgram(0x3e, 0x10, 0x1f);
        cpu.set(Flag.NIBBLE, true);
        runProgram(cpu, 3);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_rra_does_not_set_operation_flag() {
        Cpu cpu = runProgram(0x3e, 0x10, 0x1f);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_rra_resets_operation_flag() {
        Cpu cpu = cpuWithProgram(0x3e, 0x10, 0x1f);
        cpu.set(Flag.OPERATION, true);
        runProgram(cpu, 3);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_rlc_a() {
        Cpu cpu = runProgram(0x3e, 0x8b, 0xcb, 0x07);
        assertEquals(0x17, cpu.read(Byte.Register.A));
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rlc_a_uses_8_cycles() {
        Cpu cpu = runProgram(0xcb, 0x07);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_rlc_a_sets_zero_flag() {
        // Although RLCA doesn't set the zero flag, RLC does.
        Cpu cpu = runProgram(0xcb, 0x07);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_rlc_a_doesnt_set_zero_flag_if_result_is_nonzero() {
        Cpu cpu = runProgram(0x3e, 0x01, 0xcb, 0x07);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_rlc_b() {
        Cpu cpu = runProgram(0x06, 0x9d, 0xcb, 0x00);
        assertEquals(0x3b, cpu.read(Byte.Register.B));
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rlc_b_sets_zero_flag() {
        Cpu cpu = runProgram(0x06, 0x00, 0xcb, 0x00);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_rlc_c() {
        Cpu cpu = runProgram(0x0e, 0xac, 0xcb, 0x01);
        assertEquals(0x59, cpu.read(Byte.Register.C));
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rlc_d() {
        Cpu cpu = runProgram(0x16, 0x44, 0xcb, 0x02);
        assertEquals(0x88, cpu.read(Byte.Register.D));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rlc_e() {
        Cpu cpu = runProgram(0x1e, 0x28, 0xcb, 0x03);
        assertEquals(0x50, cpu.read(Byte.Register.E));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rlc_h() {
        Cpu cpu = runProgram(0x26, 0xbb, 0xcb, 0x04);
        assertEquals(0x77, cpu.read(Byte.Register.H));
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rlc_l() {
        Cpu cpu = runProgram(0x2e, 0xc7, 0xcb, 0x05);
        assertEquals(0x8f, cpu.read(Byte.Register.L));
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rlc_hl_indirect() {
        Cpu cpu = runProgram(0x36, 0x21, 0xcb, 0x06);
        assertEquals(0x42, cpu.readFrom(Pointer.of(Word.Register.HL)));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rlc_hl_indirect_uses_16_cycles() {
        Cpu cpu = runProgram(0xcb, 0x06);
        assertEquals(16, cpu.getCycles());
    }

    @Test
    public void test_rlc_hl_indirect_sets_zero_flag() {
        Cpu cpu = runProgram(0x36, 0x00, 0xcb, 0x06);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_rl_a_with_carry_low() {
        Cpu cpu = runProgram(0x3e, 0x42, 0xcb, 0x17);
        assertEquals(0x84, cpu.read(Byte.Register.A));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rl_a_with_carry_high() {
        Cpu cpu = cpuWithProgram(0x3e, 0x42, 0xcb, 0x17);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 4);
        assertEquals(0x85, cpu.read(Byte.Register.A));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rl_a_with_carry_low_sets_zero_flag() {
        Cpu cpu = runProgram(0x3e, 0x00, 0xcb, 0x17);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_rl_a_with_carry_low_sets_zero_flag_despite_nonzero_input() {
        Cpu cpu = runProgram(0x3e, 0x80, 0xcb, 0x17);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_rl_a_of_0x00_with_carry_high_resets_zero_flag() {
        Cpu cpu = cpuWithProgram(0x3e, 0x00, 0xcb, 0x17);
        cpu.set(Flag.CARRY, true);
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 4);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rl_a_uses_8_cycles() {
        Cpu cpu = runProgram(0xcb, 0x17);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_rl_b_with_carry_low() {
        Cpu cpu = runProgram(0x06, 0xf1, 0xcb, 0x10);
        assertEquals(0xe2, cpu.read(Byte.Register.B));
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rl_b_with_carry_high() {
        Cpu cpu = cpuWithProgram(0x06, 0xf1, 0xcb, 0x10);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 4);
        assertEquals(0xe3, cpu.read(Byte.Register.B));
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rl_c_with_carry_low() {
        Cpu cpu = runProgram(0x0e, 0x15, 0xcb, 0x11);
        assertEquals(0x2a, cpu.read(Byte.Register.C));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rl_c_with_carry_high() {
        Cpu cpu = cpuWithProgram(0x0e, 0x15, 0xcb, 0x11);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 4);
        assertEquals(0x2b, cpu.read(Byte.Register.C));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rl_d_with_carry_low() {
        Cpu cpu = runProgram(0x16, 0xbe, 0xcb, 0x12);
        assertEquals(0x7c, cpu.read(Byte.Register.D));
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rl_d_with_carry_high() {
        Cpu cpu = cpuWithProgram(0x16, 0xbe, 0xcb, 0x12);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 4);
        assertEquals(0x7d, cpu.read(Byte.Register.D));
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rl_e_with_carry_low() {
        Cpu cpu = runProgram(0x1e, 0x54, 0xcb, 0x13);
        assertEquals(0xa8, cpu.read(Byte.Register.E));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rl_e_with_carry_high() {
        Cpu cpu = cpuWithProgram(0x1e, 0x54, 0xcb, 0x13);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 4);
        assertEquals(0xa9, cpu.read(Byte.Register.E));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rl_h_with_carry_low() {
        Cpu cpu = runProgram(0x26, 0x4e, 0xcb, 0x14);
        assertEquals(0x9c, cpu.read(Byte.Register.H));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rl_h_with_carry_high() {
        Cpu cpu = cpuWithProgram(0x26, 0x4e, 0xcb, 0x14);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 4);
        assertEquals(0x9d, cpu.read(Byte.Register.H));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rl_l_with_carry_low() {
        Cpu cpu = runProgram(0x2e, 0xb2, 0xcb, 0x15);
        assertEquals(0x64, cpu.read(Byte.Register.L));
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rl_l_with_carry_high() {
        Cpu cpu = cpuWithProgram(0x2e, 0xb2, 0xcb, 0x15);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 4);
        assertEquals(0x65, cpu.read(Byte.Register.L));
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rl_hl_indirect_with_carry_low() {
        Cpu cpu = runProgram(0x36, 0x42, 0xcb, 0x16);
        assertEquals(0x84, cpu.readFrom(Pointer.of(Word.Register.HL)));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rl_hl_indirect_with_carry_high() {
        Cpu cpu = cpuWithProgram(0x36, 0x42, 0xcb, 0x16);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 4);
        assertEquals(0x85, cpu.readFrom(Pointer.of(Word.Register.HL)));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rl_hl_indirect_sets_zero_flag() {
        Cpu cpu = runProgram(0x36, 0x80, 0xcb, 0x16);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_rl_hl_indirect_doesnt_always_set_zero_flag() {
        Cpu cpu = runProgram(0xcb, 0x16);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_rl_hl_uses_16_cycles() {
        Cpu cpu = runProgram(0xcb, 0x16);
        assertEquals(16, cpu.getCycles());
    }

    @Test
    public void test_rrc_a() {
        Cpu cpu = runProgram(0x3e, 0x9d, 0xcb, 0x0f);
        assertEquals(0xce, cpu.read(Byte.Register.A));
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rrc_a_uses_8_cycles() {
        Cpu cpu = runProgram(0xcb, 0x0f);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_rrc_b() {
        Cpu cpu = runProgram(0x06, 0x50, 0xcb, 0x08);
        assertEquals(0x28, cpu.read(Byte.Register.B));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rrc_b_sets_zero_flag_on_0x00() {
        Cpu cpu = runProgram(0xcb, 0x08);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_rrc_b_does_not_set_zero_flag_on_0x01() {
        Cpu cpu = runProgram(0x06, 0x01, 0xcb, 0x08);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_rrc_b_sets_zero_flag_on_0x00_with_carry_high() {
        Cpu cpu = cpuWithProgram(0x06, 0x00, 0xcb, 0x08);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 4);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_rrc_b_doesnt_always_set_zero_flag() {
        Cpu cpu = runProgram(0x06, 0x02, 0xcb, 0x08);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_rrc_c() {
        Cpu cpu = runProgram(0x0e, 0x48, 0xcb, 0x09);
        assertEquals(0x24, cpu.read(Byte.Register.C));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rrc_d() {
        Cpu cpu = runProgram(0x16, 0xef, 0xcb, 0x0a);
        assertEquals(0xf7, cpu.read(Byte.Register.D));
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rrc_e() {
        Cpu cpu = runProgram(0x1e, 0x29, 0xcb, 0x0b);
        assertEquals(0x94, cpu.read(Byte.Register.E));
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rrc_h() {
        Cpu cpu = runProgram(0x26, 0x88, 0xcb, 0x0c);
        assertEquals(0x44, cpu.read(Byte.Register.H));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rrc_l() {
        Cpu cpu = runProgram(0x2e, 0x50, 0xcb, 0x0d);
        assertEquals(0x28, cpu.read(Byte.Register.L));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rrc_hl_indirect() {
        Cpu cpu = runProgram(0x36, 0x06, 0xcb, 0x0e);
        assertEquals(0x03, cpu.readFrom(Pointer.of(Word.Register.HL)));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rrc_hl_indirect_uses_16_cycles() {
        Cpu cpu = runProgram(0xcb, 0x0e);
        assertEquals(16, cpu.getCycles());
    }

    @Test
    public void test_rrc_hl_indirect_sets_zero_flag() {
        Cpu cpu = runProgram(0x36, 0x00, 0xcb, 0x0e);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_rrc_hl_indirect_doesnt_always_set_zero_flag() {
        Cpu cpu = runProgram(0xcb, 0x0e);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_rr_a_with_carry_low() {
        Cpu cpu = runProgram(0x3e, 0x02, 0xcb, 0x1f);
        assertEquals(0x01, cpu.read(Byte.Register.A));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rr_a_with_carry_high() {
        Cpu cpu = cpuWithProgram(0x3e, 0x02, 0xcb, 0x1f);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 4);
        assertEquals(0x81, cpu.read(Byte.Register.A));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rr_a_uses_8_cycles() {
        Cpu cpu = runProgram(0xcb, 0x1f);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_rr_b_with_carry_low() {
        Cpu cpu = runProgram(0x06, 0xd4, 0xcb, 0x18);
        assertEquals(0x6a, cpu.read(Byte.Register.B));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rr_b_with_carry_high() {
        Cpu cpu = cpuWithProgram(0x06, 0xd4, 0xcb, 0x18);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 4);
        assertEquals(0xea, cpu.read(Byte.Register.B));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rr_c_with_carry_low() {
        Cpu cpu = runProgram(0x0e, 0x85, 0xcb, 0x19);
        assertEquals(0x42, cpu.read(Byte.Register.C));
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rr_c_with_carry_high() {
        Cpu cpu = cpuWithProgram(0x0e, 0x85, 0xcb, 0x19);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 4);
        assertEquals(0xc2, cpu.read(Byte.Register.C));
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rr_d_with_carry_low() {
        Cpu cpu = runProgram(0x16, 0x60, 0xcb, 0x1a);
        assertEquals(0x30, cpu.read(Byte.Register.D));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rr_d_with_carry_high() {
        Cpu cpu = cpuWithProgram(0x16, 0x60, 0xcb, 0x1a);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 4);
        assertEquals(0xb0, cpu.read(Byte.Register.D));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rr_d_sets_zero_flag() {
        Cpu cpu = runProgram(0xcb, 0x1a);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_rr_d_doesnt_always_set_zero_flag() {
        Cpu cpu = runProgram(0x16, 0x02, 0xcb, 0x1a);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_rr_d_does_not_set_zero_flag_with_carry_high() {
        Cpu cpu = cpuWithProgram(0xcb, 0x1a);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 2);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_rr_e_with_carry_low() {
        Cpu cpu = runProgram(0x1e, 0xae, 0xcb, 0x1b);
        assertEquals(0x57, cpu.read(Byte.Register.E));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rr_e_with_carry_high() {
        Cpu cpu = cpuWithProgram(0x1e, 0xae, 0xcb, 0x1b);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 4);
        assertEquals(0xd7, cpu.read(Byte.Register.E));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rr_h_with_carry_low() {
        Cpu cpu = runProgram(0x26, 0xc8, 0xcb, 0x1c);
        assertEquals(0x64, cpu.read(Byte.Register.H));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rr_h_with_carry_high() {
        Cpu cpu = cpuWithProgram(0x26, 0xc8, 0xcb, 0x1c);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 4);
        assertEquals(0xe4, cpu.read(Byte.Register.H));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    public void test_rr_l_with_carry_low() {
        Cpu cpu = runProgram(0x2e, 0x8f, 0xcb, 0x1d);
        assertEquals(0x47, cpu.read(Byte.Register.L));
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rr_l_with_carry_high() {
        Cpu cpu = cpuWithProgram(0x2e, 0x8f, 0xcb, 0x1d);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 4);
        assertEquals(0xc7, cpu.read(Byte.Register.L));
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rr_hl_indirect_with_carry_low() {
        Cpu cpu = runProgram(0x36, 0x68, 0xcb, 0x1e);
        assertEquals(0x34, cpu.readFrom(Pointer.of(Word.Register.HL)));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rr_hl_indirect_with_carry_high() {
        Cpu cpu = cpuWithProgram(0x36, 0x68, 0xcb, 0x1e);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 4);
        assertEquals(0xb4, cpu.readFrom(Pointer.of(Word.Register.HL)));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_rr_hl_indirect_uses_16_cycles() {
        Cpu cpu = runProgram(0xcb, 0x1e);
        assertEquals(16, cpu.getCycles());
    }

    @Test
    public void test_rr_hl_indirect_sets_zero_flag() {
        Cpu cpu = runProgram(0x36, 0x01, 0xcb, 0x1e);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_rr_hl_indirect_doesnt_always_set_zero_flag() {
        Cpu cpu = runProgram(0xcb, 0x1e);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_left_shift_a_of_0x00_is_0x00() {
        Cpu cpu = runProgram(0xcb, 0x27);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_left_shift_a_uses_8_cycles() {
        Cpu cpu = runProgram(0xcb, 0x27);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_left_shift_a_of_0x01_is_0x02() {
        Cpu cpu = runProgram(0x3e, 0x01, 0xcb, 0x27);
        assertEquals(0x02, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_left_shift_a_of_0b01011101_is_0b1011101() {
        Cpu cpu = runProgram(0x3e, 0b01011101, 0xcb, 0x27);
        assertEquals(0b10111010, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_left_shift_a_of_0x80_is_0x00() {
        Cpu cpu = runProgram(0x3e, 0x80, 0xcb, 0x27);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_left_shift_a_of_0b10111011_is_0b01110110() {
        Cpu cpu = runProgram(0x3e, 0b10111011, 0xcb, 0x27);
        assertEquals(0b01110110, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_left_shift_a_sets_zero_flag_if_input_is_zero() {
        Cpu cpu = runProgram(0xcb, 0x27);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_left_shift_a_of_0xff_doesnt_set_zero_flag() {
        Cpu cpu = runProgram(0x3e, 0xff, 0xcb, 0x27);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_left_shift_a_of_0x01_doesnt_set_zero_flag() {
        Cpu cpu = runProgram(0x3e, 0x01, 0xcb, 0x27);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_left_shift_a_of_0x80_sets_zero_flag() {
        Cpu cpu = runProgram(0x3e, 0x80, 0xcb, 0x27);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_left_shift_a_of_0x81_resets_zero_flag() {
        Cpu cpu = cpuWithProgram(0x3e, 0x81, 0xcb, 0x27);
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 4);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_left_shift_a_doesnt_set_nibble_flag() {
        Cpu cpu = runProgram(0x3e, 0x08, 0xcb, 0x27);
        assertFalse(cpu.isSet(Flag.NIBBLE)) ;
    }

    @Test
    public void test_left_shift_a_resets_nibble_flag() {
        Cpu cpu = cpuWithProgram(0x3e, 0x08, 0xcb, 0x27);
        cpu.set(Flag.NIBBLE, true);
        runProgram(cpu, 4);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_left_shift_a_doesnt_set_operation_flag() {
        Cpu cpu = runProgram(0x3e, 0x80, 0xcb, 0x27);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_left_shift_a_resets_operation_flag() {
        Cpu cpu = cpuWithProgram(0x3e, 0x80, 0xcb, 0x27);
        cpu.set(Flag.OPERATION, true);
        runProgram(cpu, 4);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_left_shift_a_sets_carry_flag_if_input_is_0x80() {
        Cpu cpu = runProgram(0x3e, 0x80, 0xcb, 0x27);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_left_shift_a_does_not_set_carry_flag_if_input_is_0x7f() {
        Cpu cpu = runProgram(0x3e, 0x7f, 0xcb, 0x27);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_left_shift_a_sets_carry_flag_if_input_is_0xff() {
        Cpu cpu = runProgram(0x3e, 0xff, 0xcb, 0x27);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_left_shift_a_resets_carry_flag_if_bit_7_is_low() {
        Cpu cpu = cpuWithProgram(0x3e, 0x01, 0xcb, 0x27);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 4);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_left_shift_b() {
        Cpu cpu = runProgram(0x06, 0b10101010, 0xcb, 0x20);
        assertEquals(0b01010100, cpu.read(Byte.Register.B));
        assertTrue(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_left_shift_c() {
        Cpu cpu = runProgram(0x0e, 0b01111101, 0xcb, 0x21);
        assertEquals(0b11111010, cpu.read(Byte.Register.C));
        assertFalse(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_left_shift_d() {
        Cpu cpu = runProgram(0x16, 0b11011011, 0xcb, 0x22);
        assertEquals(0b10110110, cpu.read(Byte.Register.D));
        assertTrue(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_left_shift_e() {
        Cpu cpu = runProgram(0x1e, 0b10110101, 0xcb, 0x23);
        assertEquals(0b01101010, cpu.read(Byte.Register.E));
        assertTrue(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_left_shift_h() {
        Cpu cpu = runProgram(0x26, 0b10110101, 0xcb, 0x24);
        assertEquals(0b01101010, cpu.read(Byte.Register.H));
        assertTrue(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_left_shift_l() {
        Cpu cpu = runProgram(0x2e, 0b00101000, 0xcb, 0x25);
        assertEquals(0b01010000, cpu.read(Byte.Register.L));
        assertFalse(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_left_shift_indirect_hl() {
        Cpu cpu = runProgram(0x36, 0b00101111, 0xcb, 0x26);
        assertEquals(0b01011110, cpu.readFrom(Pointer.of(Word.Register.HL)));
        assertFalse(cpu.isSet(Flag.CARRY));
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_left_shift_indirect_hl_uses_16_cycles() {
        Cpu cpu = runProgram(0xcb, 0x26);
        assertEquals(16, cpu.getCycles());
    }

    @Test
    public void test_arithmetic_right_shift_a_of_0x00_is_0x00() {
        Cpu cpu = runProgram(0xcb, 0x2f);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_arithmetic_right_shift_a_of_0x40_is_0x20() {
        Cpu cpu = runProgram(0x3e, 0x40, 0xcb, 0x2f);
        assertEquals(0x20, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_arithmetic_right_shift_a_uses_8_cycles() {
        Cpu cpu = runProgram(0xcb, 0x2f);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_arithmetic_right_shift_a_of_0b01101011_is_0b00110101() {
        Cpu cpu = runProgram(0x3e, 0b01101011, 0xcb, 0x2f);
        assertEquals(0b00110101, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_arithmetic_right_shift_a_of_0x80_is_0xc0(){
        Cpu cpu = runProgram(0x3e, 0x80, 0xcb, 0x2f);
        assertEquals(0xc0, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_arithmetic_right_shift_a_of_0b10111011_is_0b11011101() {
        Cpu cpu = runProgram(0x3e, 0b10111011, 0xcb, 0x2f);
        assertEquals(0b11011101, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_arithmetic_right_shift_a_of_0x01_sets_carry() {
        Cpu cpu = runProgram(0x3e, 0x01, 0xcb, 0x2f)    ;
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_arithmetic_right_shift_a_of_0x02_does_not_set_carry() {
        Cpu cpu = runProgram(0x3e, 0x02, 0xcb, 0x2f);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_arithmetic_right_shift_a_of_0xf1_sets_carry() {
        Cpu cpu = runProgram(0x3e, 0xf1, 0xcb, 0x2f);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_arithmetic_right_shift_a_of_0xfe_resets_carry() {
        Cpu cpu = cpuWithProgram(0x3e, 0xfe, 0xcb, 0x2f);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 4);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_arithmetic_right_shift_a_sets_zero_flag_when_result_is_zero() {
        Cpu cpu = runProgram(0x3e, 0x01, 0xcb, 0x2f);
        assertTrue(cpu.isSet(Flag.ZERO)) ;
    }

    @Test
    public void test_arithmetic_right_shift_a_doesnt_set_zero_flag_when_result_is_nonzero() {
        Cpu cpu = runProgram(0x3e, 0x02, 0xcb, 0x2f);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_arithmetic_right_shift_a_resets_zero_flag_when_result_is_nonzero() {
        Cpu cpu = cpuWithProgram(0x3e, 0x03, 0xcb, 0x2f);
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 4);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_arithmetic_right_shift_a_doesnt_set_nibble_flag() {
        Cpu cpu = runProgram(0x3e, 0x10, 0xcb, 0x2f);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_arithmetic_right_shift_a_resets_nibble_flag() {
        Cpu cpu = cpuWithProgram(0x3e, 0x10, 0xcb, 0x2f);
        cpu.set(Flag.NIBBLE, true);
        runProgram(cpu, 4);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_arithmetic_right_shift_a_doesnt_set_operation_flag() {
        Cpu cpu = runProgram(0x3e, 0x10, 0xcb, 0x2f);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_arithmetic_right_shift_a_resets_operation_flag() {
        Cpu cpu = cpuWithProgram(0x3e, 0x10, 0xcb, 0x2f);
        cpu.set(Flag.OPERATION, true);
        runProgram(cpu, 4);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_arithmetic_right_shift_b() {
        Cpu cpu = runProgram(0x06, 0xb4, 0xcb, 0x28);
        assertEquals(0xda, cpu.read(Byte.Register.B));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_arithmetic_right_shift_c() {
        Cpu cpu = runProgram(0x0e, 0xd5, 0xcb, 0x29);
        assertEquals(0xea, cpu.read(Byte.Register.C));
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_arithmetic_right_shift_d() {
        Cpu cpu = runProgram(0x16, 0xfe, 0xcb, 0x2a);
        assertEquals(0xff, cpu.read(Byte.Register.D));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_arithmetic_right_shift_e() {
        Cpu cpu = runProgram(0x1e, 0xfc, 0xcb, 0x2b);
        assertEquals(0xfe, cpu.read(Byte.Register.E));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_arithmetic_right_shift_h() {
        Cpu cpu = runProgram(0x26, 0xd2, 0xcb, 0x2c);
        assertEquals(0xe9, cpu.read(Byte.Register.H));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_arithmetic_right_shift_l() {
        Cpu cpu = runProgram(0x2e, 0x16, 0xcb, 0x2d);
        assertEquals(0x0b, cpu.read(Byte.Register.L));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_arithmetic_right_shift_hl_indirect() {
        Cpu cpu = runProgram(0x36, 0x75, 0xcb, 0x2e);
        assertEquals(0x3a, cpu.readFrom(Pointer.of(Word.Register.HL)));
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_arithmetic_right_shift_hl_indirect_uses_16_cycles() {
        Cpu cpu = runProgram(0xcb, 0x2e);
        assertEquals(16, cpu.getCycles());
    }

    @Test
    public void test_logical_right_shift_a_of_0x00_is_0x00() {
        Cpu cpu = runProgram(0xcb, 0x3f);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_logical_right_shift_a_of_0x40_is_0x20() {
        Cpu cpu = runProgram(0x3e, 0x40, 0xcb, 0x3f);
        assertEquals(0x20, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_logical_right_shift_a_uses_8_cycles() {
        Cpu cpu = runProgram(0xcb, 0x3f);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_logical_right_shift_a_of_0b01101011_is_0b00110101() {
        Cpu cpu = runProgram(0x3e, 0b01101011, 0xcb, 0x3f);
        assertEquals(0b00110101, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_logical_right_shift_a_of_0x80_is_0x40(){
        Cpu cpu = runProgram(0x3e, 0x80, 0xcb, 0x3f);
        assertEquals(0x40, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_logical_right_shift_a_of_0b10111011_is_0b01011101() {
        Cpu cpu = runProgram(0x3e, 0b10111011, 0xcb, 0x3f);
        assertEquals(0b01011101, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_logical_right_shift_a_of_0x01_sets_carry() {
        Cpu cpu = runProgram(0x3e, 0x01, 0xcb, 0x3f)    ;
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_logical_right_shift_a_of_0x02_does_not_set_carry() {
        Cpu cpu = runProgram(0x3e, 0x02, 0xcb, 0x3f);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_logical_right_shift_a_of_0xf1_sets_carry() {
        Cpu cpu = runProgram(0x3e, 0xf1, 0xcb, 0x3f);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_logical_right_shift_a_of_0xfe_resets_carry() {
        Cpu cpu = cpuWithProgram(0x3e, 0xfe, 0xcb, 0x3f);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 4);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_logical_right_shift_a_sets_zero_flag_when_result_is_zero() {
        Cpu cpu = runProgram(0x3e, 0x01, 0xcb, 0x3f);
        assertTrue(cpu.isSet(Flag.ZERO)) ;
    }

    @Test
    public void test_logical_right_shift_a_doesnt_set_zero_flag_when_result_is_nonzero() {
        Cpu cpu = runProgram(0x3e, 0x02, 0xcb, 0x3f);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_logical_right_shift_a_resets_zero_flag_when_result_is_nonzero() {
        Cpu cpu = cpuWithProgram(0x3e, 0x03, 0xcb, 0x3f);
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 4);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_logical_right_shift_a_doesnt_set_nibble_flag() {
        Cpu cpu = runProgram(0x3e, 0x10, 0xcb, 0x3f);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_logical_right_shift_a_resets_nibble_flag() {
        Cpu cpu = cpuWithProgram(0x3e, 0x10, 0xcb, 0x3f);
        cpu.set(Flag.NIBBLE, true);
        runProgram(cpu, 4);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_logical_right_shift_a_doesnt_set_operation_flag() {
        Cpu cpu = runProgram(0x3e, 0x10, 0xcb, 0x3f);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_logical_right_shift_a_resets_operation_flag() {
        Cpu cpu = cpuWithProgram(0x3e, 0x10, 0xcb, 0x3f);
        cpu.set(Flag.OPERATION, true);
        runProgram(cpu, 4);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_logical_right_shift_b() {
        Cpu cpu = runProgram(0x06, 0xb4, 0xcb, 0x38);
        assertEquals(0x5a, cpu.read(Byte.Register.B));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_logical_right_shift_c() {
        Cpu cpu = runProgram(0x0e, 0xd5, 0xcb, 0x39);
        assertEquals(0x6a, cpu.read(Byte.Register.C));
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_logical_right_shift_d() {
        Cpu cpu = runProgram(0x16, 0xfe, 0xcb, 0x3a);
        assertEquals(0x7f, cpu.read(Byte.Register.D));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_logical_right_shift_e() {
        Cpu cpu = runProgram(0x1e, 0xfc, 0xcb, 0x3b);
        assertEquals(0x7e, cpu.read(Byte.Register.E));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_logical_right_shift_h() {
        Cpu cpu = runProgram(0x26, 0xd2, 0xcb, 0x3c);
        assertEquals(0x69, cpu.read(Byte.Register.H));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_logical_right_shift_l() {
        Cpu cpu = runProgram(0x2e, 0x16, 0xcb, 0x3d);
        assertEquals(0x0b, cpu.read(Byte.Register.L));
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_logical_right_shift_hl_indirect() {
        Cpu cpu = runProgram(0x36, 0x75, 0xcb, 0x3e);
        assertEquals(0x3a, cpu.readFrom(Pointer.of(Word.Register.HL)));
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_logical_right_shift_hl_indirect_uses_16_cycles() {
        Cpu cpu = runProgram(0xcb, 0x3e);
        assertEquals(16, cpu.getCycles());
    }

    @Test
    public void test_bit_test_register_a_bit_0_on_0x00() {
        Cpu cpu = runProgram(0xcb, 0x78);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_does_not_affect_register() {
        Cpu cpu = runProgram(0x3e, 0xd8, 0xcb, 0x78);
        assertEquals(0xd8, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_bit_test_uses_8_cycles() {
        Cpu cpu = runProgram(0xcb, 0x78);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_bit_test_register_a_bit_0_on_0xff() {
        Cpu cpu = runProgram(0x3e, 0xff, 0xcb, 0x47);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_a_bit_0_on_0xfe() {
        Cpu cpu = runProgram(0x3e, 0xfe, 0xcb, 0x47);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_a_bit_0_on_0x01() {
        Cpu cpu = runProgram(0x3e, 0x01, 0xcb, 0x47);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_a_bit_1_on_0x16() {
        Cpu cpu = runProgram(0x3e, 0x16, 0xcb, 0x4f);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_a_bit_2_on_0x16() {
        Cpu cpu = runProgram(0x3e, 0x16, 0xcb, 0x57);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_a_bit_3_on_0x08() {
        Cpu cpu = runProgram(0x3e, 0x08, 0xcb, 0x5f);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_a_bit_4_on_0x16() {
        Cpu cpu = runProgram(0x3e, 0x16, 0xcb, 0x67);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_a_bit_5_on_0x16() {
        Cpu cpu = runProgram(0x3e, 0x16, 0xcb, 0x6f);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_a_bit_6_on_0x16() {
        Cpu cpu = runProgram(0x3e, 0x16, 0xcb, 0x77);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_a_bit_7_on_0xff() {
        Cpu cpu = runProgram(0x3e, 0xff, 0xcb, 0x7f);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_a_bit_7_on_0x7f() {
        Cpu cpu = runProgram(0x3e, 0x7f, 0xcb, 0x7f);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    public void test_bit_test_register_a_doesnt_set_carry_flag() {
        Cpu cpu = runProgram(0xcb, 0x47);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_bit_test_register_a_doesnt_reset_carry_flag() {
        Cpu cpu = cpuWithProgram(0xcb, 0x47);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 2);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_bit_test_register_a_sets_operation_flag() {
        Cpu cpu = runProgram(0xcb, 0x47);
        assertTrue(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_bit_test_register_a_does_not_reset_operation_flag() {
        Cpu cpu = cpuWithProgram(0xcb, 0x47);
        cpu.set(Flag.OPERATION, true);
        runProgram(cpu, 2);
        assertTrue(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_bit_test_register_a_does_not_set_nibble_flag() {
        Cpu cpu = runProgram(0xcb, 0x47);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_bit_test_register_a_resets_nibble_flag() {
        Cpu cpu = cpuWithProgram(0xcb, 0x47);
        cpu.set(Flag.NIBBLE, true);
        runProgram(cpu, 3);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_bit_test_register_b_on_0xeb_bit_0() {
        Cpu cpu = runProgram(0x06, 0xeb, 0xcb, 0x40);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_b_on_0xeb_bit_1() {
        Cpu cpu = runProgram(0x06, 0xeb, 0xcb, 0x48);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_b_on_0xeb_bit_2() {
        Cpu cpu = runProgram(0x06, 0xeb, 0xcb, 0x50);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_b_on_0xeb_bit_3() {
        Cpu cpu = runProgram(0x06, 0xeb, 0xcb, 0x58);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_b_on_0xeb_bit_4() {
        Cpu cpu = runProgram(0x06, 0xeb, 0xcb, 0x60);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_b_on_0xeb_bit_5() {
        Cpu cpu = runProgram(0x06, 0xeb, 0xcb, 0x68);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_b_on_0xeb_bit_6() {
        Cpu cpu = runProgram(0x06, 0xeb, 0xcb, 0x70);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_b_on_0xeb_bit_7() {
        Cpu cpu = runProgram(0x06, 0xeb, 0xcb, 0x78);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_c_on_0x09_bit_0() {
        Cpu cpu = runProgram(0x0e, 0x09, 0xcb, 0x41);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_c_on_0x09_bit_1() {
        Cpu cpu = runProgram(0x0e, 0x09, 0xcb, 0x49);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_c_on_0x09_bit_2() {
        Cpu cpu = runProgram(0x0e, 0x09, 0xcb, 0x51);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_c_on_0x09_bit_3() {
        Cpu cpu = runProgram(0x0e, 0x09, 0xcb, 0x59);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_c_on_0x09_bit_4() {
        Cpu cpu = runProgram(0x0e, 0x09, 0xcb, 0x61);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_c_on_0x09_bit_5() {
        Cpu cpu = runProgram(0x0e, 0x09, 0xcb, 0x69);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_c_on_0x09_bit_6() {
        Cpu cpu = runProgram(0x0e, 0x09, 0xcb, 0x71);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_c_on_0x09_bit_7() {
        Cpu cpu = runProgram(0x0e, 0x09, 0xcb, 0x79);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_d_on_0x98_bit_0() {
        Cpu cpu = runProgram(0x16, 0x98, 0xcb, 0x42);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_d_on_0x98_bit_1() {
        Cpu cpu = runProgram(0x16, 0x98, 0xcb, 0x4a);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_d_on_0x98_bit_2() {
        Cpu cpu = runProgram(0x16, 0x98, 0xcb, 0x52);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_d_on_0x98_bit_3() {
        Cpu cpu = runProgram(0x16, 0x98, 0xcb, 0x5a);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_d_on_0x98_bit_4() {
        Cpu cpu = runProgram(0x16, 0x98, 0xcb, 0x62);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_d_on_0x98_bit_5() {
        Cpu cpu = runProgram(0x16, 0x98, 0xcb, 0x6a);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_d_on_0x98_bit_6() {
        Cpu cpu = runProgram(0x16, 0x98, 0xcb, 0x72);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_d_on_0x98_bit_7() {
        Cpu cpu = runProgram(0x16, 0x98, 0xcb, 0x7a);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_e_on_0x5b_bit_0() {
        Cpu cpu = runProgram(0x1e, 0x5b, 0xcb, 0x43);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_e_on_0x5b_bit_1() {
        Cpu cpu = runProgram(0x1e, 0x5b, 0xcb, 0x4b);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_e_on_0x5b_bit_2() {
        Cpu cpu = runProgram(0x1e, 0x5b, 0xcb, 0x53);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_e_on_0x5b_bit_3() {
        Cpu cpu = runProgram(0x1e, 0x5b, 0xcb, 0x5b);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_e_on_0x5b_bit_4() {
        Cpu cpu = runProgram(0x1e, 0x5b, 0xcb, 0x63);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_e_on_0x5b_bit_5() {
        Cpu cpu = runProgram(0x1e, 0x5b, 0xcb, 0x6b);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_e_on_0x5b_bit_6() {
        Cpu cpu = runProgram(0x1e, 0x5b, 0xcb, 0x73);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_e_on_0x5b_bit_7() {
        Cpu cpu = runProgram(0x1e, 0x5b, 0xcb, 0x7b);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_h_on_0xb0_bit_0() {
        Cpu cpu = runProgram(0x26, 0xb0, 0xcb, 0x44);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_h_on_0xb0_bit_1() {
        Cpu cpu = runProgram(0x26, 0xb0, 0xcb, 0x4c);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_h_on_0xb0_bit_2() {
        Cpu cpu = runProgram(0x26, 0xb0, 0xcb, 0x54);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_h_on_0xb0_bit_3() {
        Cpu cpu = runProgram(0x26, 0xb0, 0xcb, 0x5c);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_h_on_0xb0_bit_4() {
        Cpu cpu = runProgram(0x26, 0xb0, 0xcb, 0x64);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_h_on_0xb0_bit_5() {
        Cpu cpu = runProgram(0x26, 0xb0, 0xcb, 0x6c);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_h_on_0xb0_bit_6() {
        Cpu cpu = runProgram(0x26, 0xb0, 0xcb, 0x74);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_h_on_0xb0_bit_7() {
        Cpu cpu = runProgram(0x26, 0xb0, 0xcb, 0x7c);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_l_on_0x65_bit_0() {
        Cpu cpu = runProgram(0x2e, 0x65, 0xcb, 0x45);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_l_on_0x65_bit_1() {
        Cpu cpu = runProgram(0x2e, 0x65, 0xcb, 0x4d);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_l_on_0x65_bit_2() {
        Cpu cpu = runProgram(0x2e, 0x65, 0xcb, 0x55);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_l_on_0x65_bit_3() {
        Cpu cpu = runProgram(0x2e, 0x65, 0xcb, 0x5d);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_l_on_0x65_bit_4() {
        Cpu cpu = runProgram(0x2e, 0x65, 0xcb, 0x65);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_l_on_0x65_bit_5() {
        Cpu cpu = runProgram(0x2e, 0x65, 0xcb, 0x6d);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_l_on_0x65_bit_6() {
        Cpu cpu = runProgram(0x2e, 0x65, 0xcb, 0x75);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_l_on_0x65_bit_7() {
        Cpu cpu = runProgram(0x2e, 0x65, 0xcb, 0x7d);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_hl_indirect_on_0x0e_bit_0() {
        Cpu cpu = runProgram(0x36, 0x0e, 0xcb, 0x46);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_hl_indirect_on_0x0e_bit_1() {
        Cpu cpu = runProgram(0x36, 0x0e, 0xcb, 0x4e);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_hl_indirect_on_0x0e_bit_2() {
        Cpu cpu = runProgram(0x36, 0x0e, 0xcb, 0x56);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_hl_indirect_on_0x0e_bit_3() {
        Cpu cpu = runProgram(0x36, 0x0e, 0xcb, 0x5e);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_hl_indirect_on_0x0e_bit_4() {
        Cpu cpu = runProgram(0x36, 0x0e, 0xcb, 0x66);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_hl_indirect_on_0x0e_bit_5() {
        Cpu cpu = runProgram(0x36, 0x0e, 0xcb, 0x6e);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_hl_indirect_on_0x0e_bit_6() {
        Cpu cpu = runProgram(0x36, 0x0e, 0xcb, 0x76);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_hl_indirect_on_0x0e_bit_7() {
        Cpu cpu = runProgram(0x36, 0x0e, 0xcb, 0x7e);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_test_register_hl_indirect_uses_12_cycles() {
        Cpu cpu = runProgram(0xcb, 0x46);
        assertEquals(12, cpu.getCycles());  // GBCPUMan wrongly says 16 cycles.
    }

    @Test
    public void test_bit_set_register_a_bit_0() {
        Cpu cpu = runProgram(0xcb, 0xc7, 0x00);
        assertEquals(0x01, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_bit_set_register_a_bit_1() {
        Cpu cpu = runProgram(0xcb, 0xc7, 0x01);
        assertEquals(0x02, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_bit_set_register_a_takes_8_cycles() {
        Cpu cpu = runProgram(0xcb, 0xc7, 0x00);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_bit_set_register_a_bit_7() {
        Cpu cpu = runProgram(0xcb, 0xc7, 0x07);
        assertEquals(0x80, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_bit_set_register_a_preserves_existing_bits() {
        Cpu cpu = runProgram(0x3e, 0xfe, 0xcb, 0xc7, 0x00);
        assertEquals(0xff, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_successive_bit_sets_on_register_a() {
        Cpu cpu = runProgram(
                0xcb, 0xc7, 0x00,
                0xcb, 0xc7, 0x02,
                0xcb, 0xc7, 0x04
        );
        assertEquals(0x15, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_bit_set_on_register_a_is_idempotent() {
        Cpu cpu = runProgram(0xcb, 0xc7, 0x03, 0xcb, 0xc7, 0x03);
        assertEquals(0x08, cpu.read(Byte.Register.A));
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_bit_set_on_register_a_bit_8_throws_illegal_argument_exception() {
        Cpu cpu = runProgram(0xcb, 0xc7, 0x08);
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_bit_set_on_register_a_bit_19_throws_illegal_argument_exception() {
        Cpu cpu = runProgram(0xcb, 0xc7, 0x13);
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_bit_set_on_register_a_bit_255_throws_illegal_argument_exception() {
        Cpu cpu = runProgram(0xcb, 0xc7, 0xff);
    }

    @Test
    public void test_bit_set_on_register_a_does_not_set_zero_flag() {
        Cpu cpu = runProgram(0xcb, 0xc7, 0x00);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_set_on_register_a_does_not_reset_zero_flag() {
        Cpu cpu = cpuWithProgram(0xcb, 0xc7, 0x00);
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 3);
        assertTrue(cpu.isSet(Flag.ZERO));
    }
    @Test
    public void test_bit_set_on_register_a_does_not_set_carry_flag() {
        Cpu cpu = runProgram(0xcb, 0xc7, 0x00);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_bit_set_on_register_a_does_not_reset_carry_flag() {
        Cpu cpu = cpuWithProgram(0xcb, 0xc7, 0x00);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertTrue(cpu.isSet(Flag.CARRY));
    }
    @Test
    public void test_bit_set_on_register_a_does_not_set_nibble_flag() {
        Cpu cpu = runProgram(0xcb, 0xc7, 0x00);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_bit_set_on_register_a_does_not_reset_nibble_flag() {
        Cpu cpu = cpuWithProgram(0xcb, 0xc7, 0x00);
        cpu.set(Flag.NIBBLE, true);
        runProgram(cpu, 3);
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }
    @Test
    public void test_bit_set_on_register_a_does_not_set_operation_flag() {
        Cpu cpu = runProgram(0xcb, 0xc7, 0x00);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_bit_set_on_register_a_does_not_reset_operation_flag() {
        Cpu cpu = cpuWithProgram(0xcb, 0xc7, 0x00);
        cpu.set(Flag.OPERATION, true);
        runProgram(cpu, 3);
        assertTrue(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_bit_set_on_register_b() {
        Cpu cpu = runProgram(0x06, 0xb1, 0xcb, 0xc0, 0x02);
        assertEquals(0xb5, cpu.read(Byte.Register.B));
    }

    @Test
    public void test_bit_set_on_register_c() {
        Cpu cpu = runProgram(0x0e, 0x5a, 0xcb, 0xc1, 0x07);
        assertEquals(0xda, cpu.read(Byte.Register.C));
    }

    @Test
    public void test_bit_set_on_register_d() {
        Cpu cpu = runProgram(0x16, 0x28, 0xcb, 0xc2, 0x07);
        assertEquals(0xa8, cpu.read(Byte.Register.D));
    }

    @Test
    public void test_bit_set_on_register_e() {
        Cpu cpu = runProgram(0x1e, 0x2e, 0xcb, 0xc3, 0x04);
        assertEquals(0x3e, cpu.read(Byte.Register.E));
    }

    @Test
    public void test_bit_set_on_register_h() {
        Cpu cpu = runProgram(0x26, 0x45, 0xcb, 0xc4, 0x01);
        assertEquals(0x47, cpu.read(Byte.Register.H));
    }

    @Test
    public void test_bit_set_on_register_l() {
        Cpu cpu = runProgram(0x2e, 0x09, 0xcb, 0xc5, 0x01);
        assertEquals(0x0b, cpu.read(Byte.Register.L));
    }

    @Test
    public void test_bit_set_on_register_hl_indirect() {
        Cpu cpu = runProgram(0xcb, 0xc6, 0x05);
        assertEquals(0xeb, cpu.readFrom(Pointer.of(Word.Register.HL)));
    }

    @Test
    public void test_bit_set_on_register_hl_indirect_uses_16_cycles() {
        Cpu cpu = runProgram(0xcb, 0xc6, 0x05);
        assertEquals(16, cpu.getCycles());
    }

    @Test
    public void test_bit_reset_register_a_bit_0() {
        Cpu cpu = runProgram(0x3e, 0xff, 0xcb, 0x87, 0x00);
        assertEquals(0xfe, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_reset_register_a_bit_1() {
        Cpu cpu = runProgram(0x3e, 0xff, 0xcb, 0x87, 0x01);
        assertEquals(0xfd, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_reset_register_a_uses_8_cycles() {
        Cpu cpu = runProgram(0xcb, 0x87, 0x00);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_reset_register_a_preserves_existing_low_bits() {
        Cpu cpu = runProgram(0x3e, 0x03, 0xcb, 0x87, 0x01);
        assertEquals(0x01, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_reset_register_a_bit_6() {
        Cpu cpu = runProgram(0x3e, 0xff, 0xcb, 0x87, 0x06);
        assertEquals(0xbf, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_successive_bit_resets_on_register_a() {
        Cpu cpu = runProgram(
                0x3e, 0xff,
                0xcb, 0x87, 0x00,
                0xcb, 0x87, 0x01,
                0xcb, 0x87, 0x07
        );
        assertEquals(0x7c, cpu.read(Byte.Register.A));
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_bit_reset_on_register_a_bit_8_throws_illegal_argument_exception() {
        Cpu cpu = runProgram(0xcb, 0x87, 0x08);
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_bit_reset_on_register_a_bit_34_throws_illegal_argument_exception() {
        Cpu cpu = runProgram(0xcb, 0x87, 0x22);
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_bit_reset_on_register_a_bit_255_throws_illegal_argument_exception() {
        Cpu cpu = runProgram(0xcb, 0xff, 0x22);
    }

    @Test
    public void test_bit_reset_on_register_a_is_idempotent() {
        Cpu cpu = runProgram(0x3e, 0xa0, 0xcb, 0x87, 0x07, 0xcb, 0x87, 0x07);
        assertEquals(0x20, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_bit_reset_on_register_a_preserves_low_carry_bit() {
        Cpu cpu = runProgram(0xcb, 0x87, 0x00);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_bit_reset_on_register_a_preserves_high_carry_bit() {
        Cpu cpu = cpuWithProgram(0xcb, 0x87, 0x00);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_bit_reset_on_register_a_preserves_low_nibble_bit() {
        Cpu cpu = runProgram(0xcb, 0x87, 0x00);
        assertFalse(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_bit_reset_on_register_a_preserves_high_nibble_bit() {
        Cpu cpu = cpuWithProgram(0xcb, 0x87, 0x00);
        cpu.set(Flag.NIBBLE, true);
        runProgram(cpu, 3);
        assertTrue(cpu.isSet(Flag.NIBBLE));
    }

    @Test
    public void test_bit_reset_on_register_a_preserves_low_zero_bit() {
        Cpu cpu = runProgram(0xcb, 0x87, 0x00);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_reset_on_register_a_preserves_high_zero_bit() {
        Cpu cpu = cpuWithProgram(0xcb, 0x87, 0x00);
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 3);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_bit_reset_on_register_a_preserves_low_operation_bit() {
        Cpu cpu = runProgram(0xcb, 0x87, 0x00);
        assertFalse(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_bit_reset_on_register_a_preserves_high_operation_bit() {
        Cpu cpu = cpuWithProgram(0xcb, 0x87, 0x00);
        cpu.set(Flag.OPERATION, true);
        runProgram(cpu, 3);
        assertTrue(cpu.isSet(Flag.OPERATION));
    }

    @Test
    public void test_bit_reset_on_register_b() {
        Cpu cpu = runProgram(0x06, 0xf1, 0xcb, 0x80, 0x06);
        assertEquals(0xb1, cpu.read(Byte.Register.B));
    }

    @Test
    public void test_bit_reset_on_register_c() {
        Cpu cpu = runProgram(0x0e, 0xeb, 0xcb, 0x81, 0x00);
        assertEquals(0xea, cpu.read(Byte.Register.C));
    }

    @Test
    public void test_bit_reset_on_register_d() {
        Cpu cpu = runProgram(0x16, 0x59, 0xcb, 0x82, 0x03);
        assertEquals(0x51, cpu.read(Byte.Register.D));
    }

    @Test
    public void test_bit_reset_on_register_e() {
        Cpu cpu = runProgram(0x1e, 0xce, 0xcb, 0x83, 0x06);
        assertEquals(0x8e, cpu.read(Byte.Register.E));
    }

    @Test
    public void test_bit_reset_on_register_h() {
        Cpu cpu = runProgram(0x26, 0xf7, 0xcb, 0x84, 0x04);
        assertEquals(0xe7, cpu.read(Byte.Register.H));
    }

    @Test
    public void test_bit_reset_on_register_l() {
        Cpu cpu = runProgram(0x2e, 0x3b, 0xcb, 0x85, 0x03);
        assertEquals(0x33, cpu.read(Byte.Register.L));
    }

    @Test
    public void test_bit_reset_on_register_hl_indirect() {
        Cpu cpu = runProgram(0x36, 0x3b, 0xcb, 0x86, 0x05);
        assertEquals(0x1b, cpu.readFrom(Pointer.of(Word.Register.HL)));
    }

    @Test
    public void test_bit_reset_on_register_hl_uses_16_cycles() {
        Cpu cpu = runProgram(0xcb, 0x86, 0x00);
        assertEquals(16, cpu.getCycles());
    }

    @Test
    public void test_jmp_to_start() {
        Cpu cpu = cpuWithProgram(0xc3, 0x00, 0x00);
        step(cpu, 1);
        assertEquals(0x00, cpu.pc);
    }

    @Test
    public void test_jmp_uses_16_cycles() {
        Cpu cpu = cpuWithProgram(0xc3, 0x00, 0x00);
        step(cpu, 1);
        assertEquals(16, cpu.getCycles());  // GBCPUMan wrongly says 12 here.
    }

    @Test
    public void test_successive_jumps_in_initial_memory() {
        Cpu cpu = cpuWithProgram();
        // Jump through several checkpoints, doing INC A at each.
        // When complete check that A has been incremented the right number of times.
        memset(cpu, 0x0000, 0x3c, 0xc3, 0x10, 0x00);
        memset(cpu, 0x0010, 0x3c, 0xc3, 0x20, 0x00);
        memset(cpu, 0x0020, 0x3c, 0xc3, 0x30, 0x00);
        memset(cpu, 0x0030, 0x3c);
        step(cpu, 7);
        assertEquals(4, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_successive_jumps_in_deep_memory() {
        Cpu cpu = cpuWithProgram();
        // Jump through several checkpoints, doing INC A at each.
        // When complete check that A has been incremented the right number of times.
        memset(cpu, 0x0000, 0x3c, 0xc3, 0x22, 0x11);
        memset(cpu, 0x1122, 0x3c, 0xc3, 0x44, 0x33);
        memset(cpu, 0x3344, 0x3c, 0xc3, 0x66, 0x55);
        memset(cpu, 0x5566, 0x3c, 0xc3, 0x88, 0x77);
        memset(cpu, 0x7788, 0x3c);
        step(cpu, 9);
        assertEquals(5, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_chained_jumps() {
        Cpu cpu = cpuWithProgram();
        // Have each jump end directly on a new jump.
        // Include some jumps that go backwards in memory.
        memset(cpu, 0x0000, 0xc3, 0x00, 0x10); // 1->3
        memset(cpu, 0x0050, 0xc3, 0x50, 0x20); // 2->4
        memset(cpu, 0x1000, 0xc3, 0x50, 0x00); // 3->2
        memset(cpu, 0x2050, 0xc3, 0xff, 0xff); // 4->end
        step(cpu, 4);
        assertEquals(0xffff, cpu.pc);
    }

    @Test
    public void test_jnz_does_nothing_if_zero_is_set() {
        Cpu cpu = cpuWithProgram(0xc2, 0xff, 0xff);
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 3);
        assertEquals(0x03, cpu.pc);
    }

    @Test
    public void test_jnz_jumps_correctly_if_zero_is_not_set() {
        Cpu cpu = cpuWithProgram(0xc2, 0x34, 0x12);
        step(cpu, 1);
        assertEquals(0x1234, cpu.pc);
    }

    @Test
    public void test_jnz_is_unaffected_by_other_flags_if_zero_is_not_set() {
        Cpu cpu = cpuWithProgram(0xc2, 0xbb, 0xaa);
        cpu.set(Flag.CARRY, true);
        cpu.set(Flag.NIBBLE, true);
        cpu.set(Flag.OPERATION, true);
        step(cpu, 1);
        assertEquals(0xaabb, cpu.pc);
    }

    @Test
    public void test_jnz_is_unaffected_by_other_flags_if_zero_is_set() {
        Cpu cpu = cpuWithProgram(0xc2, 0x78, 0x56);
        cpu.set(Flag.CARRY, true);
        cpu.set(Flag.NIBBLE, true);
        cpu.set(Flag.OPERATION, true);
        cpu.set(Flag.ZERO, true);
        step(cpu, 1);
        assertEquals(0x03, cpu.pc);
    }

    @Test
    public void test_jnz_does_not_modify_zero_flag_when_initially_false() {
        Cpu cpu = runProgram(0xc2, 0xff, 0xff);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_jnz_does_not_modify_zero_flag_when_initially_true() {
        Cpu cpu = cpuWithProgram(0xc2, 0xff, 0xff);
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 3);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_jnz_simple_countdown() {
        Cpu cpu = runProgram(
                0x3e, 0xee,       // LD A, 0xee
                0xd6, 0x01,       // loop: SUB A, 0x01
                0xc2, 0x02, 0x00  // JNZ loop
        );
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_jnz_uses_16_cycles_if_jump_occurs() {
        Cpu cpu = runProgram(0xc2, 0xff, 0xff);
        assertEquals(16, cpu.getCycles());
    }

    @Test
    public void test_jnz_uses_12_cycles_if_no_jump_occurs() {
        Cpu cpu = cpuWithProgram(0xc2, 0xff, 0xff);
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 3);
        assertEquals(12, cpu.getCycles());
    }

    @Test
    public void test_jz_does_nothing_if_zero_is_not_set() {
        Cpu cpu = runProgram(0xca, 0xff, 0xff);
        assertEquals(0x03, cpu.pc);
    }

    @Test
    public void test_jz_jumps_correctly_zero_is_set() {
        Cpu cpu = cpuWithProgram(0xca, 0xff, 0xff);
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 3);
        assertEquals(0xffff, cpu.pc);
    }

    @Test
    public void test_jz_is_unaffected_by_other_flags_if_zero_is_not_set() {
        Cpu cpu = cpuWithProgram(0xca, 0xbb, 0xaa);
        cpu.set(Flag.CARRY, true);
        cpu.set(Flag.NIBBLE, true);
        cpu.set(Flag.OPERATION, true);
        step(cpu, 1);
        assertEquals(0x03, cpu.pc);
    }

    @Test
    public void test_jz_is_unaffected_by_other_flags_if_zero_is_set() {
        Cpu cpu = cpuWithProgram(0xca, 0x78, 0x56);
        cpu.set(Flag.CARRY, true);
        cpu.set(Flag.NIBBLE, true);
        cpu.set(Flag.OPERATION, true);
        cpu.set(Flag.ZERO, true);
        step(cpu, 1);
        assertEquals(0x5678, cpu.pc);
    }

    @Test
    public void test_jz_does_not_modify_zero_flag_when_initially_false() {
        Cpu cpu = runProgram(0xca, 0xff, 0xff);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_jz_does_not_modify_zero_flag_when_initially_true() {
        Cpu cpu = cpuWithProgram(0xca, 0xff, 0xff);
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 3);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_jz_uses_16_cycles_if_jump_occurs() {
        Cpu cpu = cpuWithProgram(0xca, 0xff, 0xff);
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 3);
        assertEquals(16, cpu.getCycles());
    }

    @Test
    public void test_jz_uses_12_cycles_if_no_jump_occurs() {
        Cpu cpu = runProgram(0xca, 0xff, 0xff);
        assertEquals(12, cpu.getCycles());
    }

    @Test
    public void test_jnc_does_nothing_if_carry_is_set() {
        Cpu cpu = cpuWithProgram(0xd2, 0xff, 0xff);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertEquals(0x03, cpu.pc);
    }

    @Test
    public void test_jnc_jumps_correctly_if_carry_is_not_set() {
        Cpu cpu = cpuWithProgram(0xd2, 0x34, 0x12);
        step(cpu, 1);
        assertEquals(0x1234, cpu.pc);
    }

    @Test
    public void test_jnc_is_unaffected_by_other_flags_if_carry_is_not_set() {
        Cpu cpu = cpuWithProgram(0xd2, 0xbb, 0xaa);
        cpu.set(Flag.ZERO, true);
        cpu.set(Flag.NIBBLE, true);
        cpu.set(Flag.OPERATION, true);
        step(cpu, 1);
        assertEquals(0xaabb, cpu.pc);
    }

    @Test
    public void test_jnc_is_unaffected_by_other_flags_if_carry_is_set() {
        Cpu cpu = cpuWithProgram(0xd2, 0x78, 0x56);
        cpu.set(Flag.CARRY, true);
        cpu.set(Flag.NIBBLE, true);
        cpu.set(Flag.OPERATION, true);
        cpu.set(Flag.ZERO, true);
        step(cpu, 1);
        assertEquals(0x03, cpu.pc);
    }

    @Test
    public void test_jnc_does_not_modify_carry_flag_when_initially_false() {
        Cpu cpu = runProgram(0xd2, 0xff, 0xff);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_jnc_does_not_modify_carry_flag_when_initially_true() {
        Cpu cpu = cpuWithProgram(0xd2, 0xff, 0xff);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_jnc_count_up_to_rollover() {
        Cpu cpu = runProgram(
                0x3e, 0x01,       // LD A, 0x01
                0xc6, 0x01,       // loop: ADD A, 0x01  -- Note that INC A wouldn't work because it doesn't set carry.
                0xd2, 0x02, 0x00  // JNC loop
        );
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_jnc_uses_16_cycles_if_jump_occurs() {
        Cpu cpu = runProgram(0xd2, 0xff, 0xff);
        assertEquals(16, cpu.getCycles());
    }

    @Test
    public void test_jnc_uses_12_cycles_if_no_jump_occurs() {
        Cpu cpu = cpuWithProgram(0xd2, 0xff, 0xff);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertEquals(12, cpu.getCycles());
    }

    @Test
    public void test_jc_does_nothing_if_carry_is_not_set() {
        Cpu cpu = runProgram(0xda, 0xff, 0xff);
        assertEquals(0x03, cpu.pc);
    }

    @Test
    public void test_jc_jumps_correctly_carry_is_set() {
        Cpu cpu = cpuWithProgram(0xda, 0xff, 0xff);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertEquals(0xffff, cpu.pc);
    }

    @Test
    public void test_jc_is_unaffected_by_other_flags_if_carry_is_not_set() {
        Cpu cpu = cpuWithProgram(0xda, 0xbb, 0xaa);
        cpu.set(Flag.ZERO, true);
        cpu.set(Flag.NIBBLE, true);
        cpu.set(Flag.OPERATION, true);
        step(cpu, 1);
        assertEquals(0x03, cpu.pc);
    }

    @Test
    public void test_jc_is_unaffected_by_other_flags_if_carry_is_set() {
        Cpu cpu = cpuWithProgram(0xda, 0x78, 0x56);
        cpu.set(Flag.CARRY, true);
        cpu.set(Flag.NIBBLE, true);
        cpu.set(Flag.OPERATION, true);
        cpu.set(Flag.ZERO, true);
        step(cpu, 1);
        assertEquals(0x5678, cpu.pc);
    }

    @Test
    public void test_jc_does_not_modify_carry_flag_when_initially_false() {
        Cpu cpu = runProgram(0xda, 0xff, 0xff);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_jc_does_not_modify_carry_flag_when_initially_true() {
        Cpu cpu = cpuWithProgram(0xda, 0xff, 0xff);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_jc_uses_16_cycles_if_jump_occurs() {
        Cpu cpu = cpuWithProgram(0xda, 0xff, 0xff);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertEquals(16, cpu.getCycles());
    }

    @Test
    public void test_jc_uses_12_cycles_if_no_jump_occurs() {
        Cpu cpu = runProgram(0xda, 0xff, 0xff);
        assertEquals(12, cpu.getCycles());
    }

    @Test
    public void test_jmp_hl_with_hl_equal_to_0x00() {
        Cpu cpu = cpuWithProgram(0xe9);
        step(cpu, 1);
        assertEquals(0x00, cpu.pc);
    }

    @Test
    public void test_jmp_hl_uses_4_cycles() {
        Cpu cpu = cpuWithProgram(0xe9);
        step(cpu, 1);
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_jmp_hl_with_hl_equal_to_0x1234() {
        Cpu cpu = cpuWithProgram(0x21, 0x34, 0x12, 0xe9);
        step(cpu, 2);
        assertEquals(0x1234, cpu.pc);
    }

    @Test
    public void test_successive_jumps_in_initial_memory_using_hl() {
        Cpu cpu = cpuWithProgram();
        // Jump through several checkpoints, doing INC A at each.
        // When complete check that A has been incremented the right number of times.
        memset(cpu, 0x0000, 0x3c, 0x21, 0x10, 0x00, 0xe9);
        memset(cpu, 0x0010, 0x3c, 0x21, 0x20, 0x00, 0xe9);
        memset(cpu, 0x0020, 0x3c, 0x21, 0x30, 0x00, 0xe9);
        memset(cpu, 0x0030, 0x3c);
        step(cpu, 10);
        assertEquals(4, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_successive_jumps_in_deep_memory_using_hl() {
        Cpu cpu = cpuWithProgram();
        // Jump through several checkpoints, doing INC A at each.
        // When complete check that A has been incremented the right number of times.
        memset(cpu, 0x0000, 0x3c, 0x21, 0x22, 0x11, 0xe9);
        memset(cpu, 0x1122, 0x3c, 0x21, 0x44, 0x33, 0xe9);
        memset(cpu, 0x3344, 0x3c, 0x21, 0x66, 0x55, 0xe9);
        memset(cpu, 0x5566, 0x3c, 0x21, 0x88, 0x77, 0xe9);
        memset(cpu, 0x7788, 0x3c);
        step(cpu, 13);
        assertEquals(5, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_jr_0x00_is_a_no_op() {
        Cpu cpu = cpuWithProgram(0x18, 0x00, 0x3c);
        step(cpu, 2);
        assertEquals(0x01, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_jr_0x01_skips_next_instruction() {
        Cpu cpu = runProgram(0x18, 0x01, 0x3c, 0x3c);
        assertEquals(0x01, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_jr_0x02_skips_two_instructions() {
        Cpu cpu = runProgram(0x18, 0x02, 0x3c, 0x3c);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_jr_0xfe_jumps_back_onto_itself() {
        Cpu cpu = cpuWithProgram(0x18, 0xfe);
        step(cpu, 1);
        assertEquals(0x00, cpu.pc);
    }

    @Test
    public void test_jr_uses_12_cycles() {
        Cpu cpu = runProgram(0x18, 0x00);
        assertEquals(12, cpu.getCycles());
    }

    @Test
    public void test_jrnz_does_nothing_if_zero_flag_is_set() {
        Cpu cpu = cpuWithProgram(0x20, 0x01, 0x3c);
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 3);
        assertEquals(0x01, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_jrnz_jumps_correctly_if_zero_is_not_set() {
        Cpu cpu = runProgram(0x20, 0x01, 0x3c);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_jrnz_is_unaffected_by_other_flags_if_zero_is_not_set() {
        Cpu cpu = cpuWithProgram(0x20, 0x01, 0x3c);
        cpu.set(Flag.CARRY, true);
        cpu.set(Flag.NIBBLE, true);
        cpu.set(Flag.OPERATION, true);
        runProgram(cpu, 3);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_jrnz_is_unaffected_by_other_flags_if_zero_is_set() {
        Cpu cpu = cpuWithProgram(0x20, 0x01, 0x3c);
        cpu.set(Flag.ZERO, true);
        cpu.set(Flag.CARRY, true);
        cpu.set(Flag.NIBBLE, true);
        cpu.set(Flag.OPERATION, true);
        runProgram(cpu, 3);
        assertEquals(0x01, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_jrnz_does_not_modify_zero_flag_when_initial_state_is_high() {
        Cpu cpu = cpuWithProgram(0x20, 0x00);
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 3);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_jrnz_does_not_modify_zero_flag_when_initial_state_is_low() {
        Cpu cpu = runProgram(0x20, 0x00);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_jrnz_simple_countdown() {
        Cpu cpu = runProgram(
                0x3e, 0xee,       // LD A, 0xee
                0xd6, 0x01,       // loop: SUB A, 0x01
                0x20, 0xfc        // JRNZ loop
        );
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_jrnz_uses_12_cycles_if_jump_occurs() {
        Cpu cpu = runProgram(0x20, 0x00);
        assertEquals(12, cpu.getCycles());
    }

    @Test
    public void test_jrnz_uses_8_cycles_if_no_jump_occurs() {
        Cpu cpu = cpuWithProgram(0x20, 0x00);
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 2);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_jrz_jumps_correctly_if_zero_flag_is_set() {
        Cpu cpu = cpuWithProgram(0x28, 0x01, 0x3c);
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 3);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_jrz_does_nothing_if_zero_is_not_set() {
        Cpu cpu = runProgram(0x28, 0x01, 0x3c);
        assertEquals(0x01, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_jrz_is_unaffected_by_other_flags_if_zero_is_not_set() {
        Cpu cpu = cpuWithProgram(0x28, 0x01, 0x3c);
        cpu.set(Flag.CARRY, true);
        cpu.set(Flag.NIBBLE, true);
        cpu.set(Flag.OPERATION, true);
        runProgram(cpu, 3);
        assertEquals(0x01, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_jrz_is_unaffected_by_other_flags_if_zero_is_set() {
        Cpu cpu = cpuWithProgram(0x28, 0x01, 0x3c);
        cpu.set(Flag.ZERO, true);
        cpu.set(Flag.CARRY, true);
        cpu.set(Flag.NIBBLE, true);
        cpu.set(Flag.OPERATION, true);
        runProgram(cpu, 3);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_jrz_does_not_modify_zero_flag_when_initial_state_is_high() {
        Cpu cpu = cpuWithProgram(0x28, 0x00);
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 3);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_jrz_does_not_modify_zero_flag_when_initial_state_is_low() {
        Cpu cpu = runProgram(0x28, 0x00);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_jrz_uses_8_cycles_if_no_jump_occurs() {
        Cpu cpu = runProgram(0x28, 0x00);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_jrz_uses_12_cycles_if_jump_occurs() {
        Cpu cpu = cpuWithProgram(0x28, 0x00);
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 2);
        assertEquals(12, cpu.getCycles());
    }

    @Test
    public void test_jrnc_does_nothing_if_carry_flag_is_set() {
        Cpu cpu = cpuWithProgram(0x30, 0x01, 0x3c);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertEquals(0x01, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_jrnc_jumps_correctly_if_carry_is_not_set() {
        Cpu cpu = runProgram(0x30, 0x01, 0x3c);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_jrnc_is_unaffected_by_other_flags_if_carry_is_not_set() {
        Cpu cpu = cpuWithProgram(0x30, 0x01, 0x3c);
        cpu.set(Flag.ZERO, true);
        cpu.set(Flag.NIBBLE, true);
        cpu.set(Flag.OPERATION, true);
        runProgram(cpu, 3);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_jrnc_is_unaffected_by_other_flags_if_carry_is_set() {
        Cpu cpu = cpuWithProgram(0x30, 0x01, 0x3c);
        cpu.set(Flag.ZERO, true);
        cpu.set(Flag.CARRY, true);
        cpu.set(Flag.NIBBLE, true);
        cpu.set(Flag.OPERATION, true);
        runProgram(cpu, 3);
        assertEquals(0x01, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_jrnc_does_not_modify_carry_flag_when_initial_state_is_high() {
        Cpu cpu = cpuWithProgram(0x30, 0x00);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_jrnc_does_not_modify_carry_flag_when_initial_state_is_low() {
        Cpu cpu = runProgram(0x30, 0x00);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_jrnc_simple_count_until_rollover() {
        Cpu cpu = runProgram(
                0x3e, 0x10,       // LD A, 0x10
                0xc6, 0x01,       // loop: ADD A, 0x01
                0x30, 0xfc        // JRNC loop
        );
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_jrnc_uses_12_cycles_if_jump_occurs() {
        Cpu cpu = runProgram(0x30, 0x00);
        assertEquals(12, cpu.getCycles());
    }

    @Test
    public void test_jrnc_uses_8_cycles_if_no_jump_occurs() {
        Cpu cpu = cpuWithProgram(0x30, 0x00);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 2);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_jrc_jumps_correctly_if_carry_flag_is_set() {
        Cpu cpu = cpuWithProgram(0x38, 0x01, 0x3c);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_jrc_does_nothing_if_carry_is_not_set() {
        Cpu cpu = runProgram(0x38, 0x01, 0x3c);
        assertEquals(0x01, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_jrc_is_unaffected_by_other_flags_if_carry_is_not_set() {
        Cpu cpu = cpuWithProgram(0x38, 0x01, 0x3c);
        cpu.set(Flag.ZERO, true);
        cpu.set(Flag.NIBBLE, true);
        cpu.set(Flag.OPERATION, true);
        runProgram(cpu, 3);
        assertEquals(0x01, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_jrc_is_unaffected_by_other_flags_if_carry_is_set() {
        Cpu cpu = cpuWithProgram(0x38, 0x01, 0x3c);
        cpu.set(Flag.ZERO, true);
        cpu.set(Flag.CARRY, true);
        cpu.set(Flag.NIBBLE, true);
        cpu.set(Flag.OPERATION, true);
        runProgram(cpu, 3);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_jrc_does_not_modify_carry_flag_when_initial_state_is_high() {
        Cpu cpu = cpuWithProgram(0x38, 0x00);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_jrc_does_not_modify_carry_flag_when_initial_state_is_low() {
        Cpu cpu = runProgram(0x38, 0x00);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_jrc_uses_8_cycles_if_no_jump_occurs() {
        Cpu cpu = runProgram(0x38, 0x00);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_jrc_uses_12_cycles_if_jump_occurs() {
        Cpu cpu = cpuWithProgram(0x38, 0x00);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 2);
        assertEquals(12, cpu.getCycles());
    }

    @Test
    public void test_call_0x0000_with_sp_0x0002_jumps_to_0x0000() {
        Cpu cpu = cpuWithProgram(0xcd, 0x00, 0x00);
        cpu.set(Word.Register.SP, Word.literal(0x0002));
        step(cpu, 1);
        assertEquals(0x0000, cpu.pc);
    }

    @Test
    public void test_call_0x0000_with_sp_0x0002_writes_0x00_0x03_backwards_at_0x0002() {
        Cpu cpu = cpuWithProgram(0xcd, 0x00, 0x00);
        cpu.set(Word.Register.SP, Word.literal(0x0002));
        step(cpu, 1);
        assertEquals(0x00, cpu.readFrom(Pointer.of(Word.literal(0x0001))));
        assertEquals(0x03, cpu.readFrom(Pointer.of(Word.literal(0x0000))));
    }

    @Test
    public void test_call_0x0000_with_sp_0xabcd_writes_0x00_0x03_backwards_at_0xabcc() {
        Cpu cpu = cpuWithProgram(0xcd, 0x00, 0x00);
        cpu.set(Word.Register.SP, Word.literal(0xabcd));
        step(cpu, 1);
        assertEquals(0x00, cpu.readFrom(Pointer.of(Word.literal(0xabcc))));
        assertEquals(0x03, cpu.readFrom(Pointer.of(Word.literal(0xabcb))));
    }

    @Test
    public void test_call_0x1234_with_sp_0xabcd_jumps_to_0x1234() {
        Cpu cpu = cpuWithProgram(0xcd, 0x34, 0x12);
        cpu.set(Word.Register.SP, Word.literal(0xabcd));
        step(cpu, 1);
        assertEquals(0x1234, cpu.pc);
    }

    @Test
    public void test_successive_calls() {
        Cpu cpu = cpuWithProgram();
        memset(cpu, 0x0000, 0xcd, 0x34, 0x12);
        memset(cpu, 0x1234, 0xcd, 0x78, 0x56);
        memset(cpu, 0x5678, 0xcd, 0xde, 0xbc);
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        step(cpu, 3);
        assertEquals(0xbcde, cpu.pc);
        assertEquals(0x00, cpu.unsafeRead(0xfffe));
        assertEquals(0x03, cpu.unsafeRead(0xfffd));
        assertEquals(0x12, cpu.unsafeRead(0xfffc));
        assertEquals(0x37, cpu.unsafeRead(0xfffb));
        assertEquals(0x56, cpu.unsafeRead(0xfffa));
        assertEquals(0x7b, cpu.unsafeRead(0xfff9));
    }

    @Test
    public void test_call_uses_24_cycles() {
        Cpu cpu = runProgram(0xcd, 0xff, 0xff);
        assertEquals(24, cpu.getCycles());
    }

    @Test
    public void test_call_nz_does_nothing_if_zero_is_set() {
        Cpu cpu = cpuWithProgram(0xc4, 0x34, 0x12);
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 3);
        assertEquals(0x03, cpu.pc);
        assertEquals("CALL_NZ shouldn't touch stack if not triggered", 0x00, cpu.unsafeRead(0xfffe));
        assertEquals("CALL_NZ shouldn't touch stack if not triggered", 0x00, cpu.unsafeRead(0xfffd));
    }

    @Test
    public void test_call_nz_jumps_correctly_if_zero_is_not_set() {
        Cpu cpu = cpuWithProgram(0xc4, 0x34, 0x12);
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        step(cpu, 1);
        assertEquals(0x1234, cpu.pc);
        assertEquals(0x00, cpu.unsafeRead(0xfffe));
        assertEquals(0x03, cpu.unsafeRead(0xfffd));
    }

    @Test
    public void test_call_nz_is_unaffected_by_other_flags_if_zero_is_not_set() {
        Cpu cpu = cpuWithProgram(0xc4, 0xbb, 0xaa);
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        cpu.set(Flag.CARRY, true);
        cpu.set(Flag.NIBBLE, true);
        cpu.set(Flag.OPERATION, true);
        step(cpu, 1);
        assertEquals(0xaabb, cpu.pc);
        assertEquals(0x00, cpu.unsafeRead(0xfffe));
        assertEquals(0x03, cpu.unsafeRead(0xfffd));
    }

    @Test
    public void test_call_nz_is_unaffected_by_other_flags_if_zero_is_set() {
        Cpu cpu = cpuWithProgram(0xc4, 0x78, 0x56);
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        cpu.set(Flag.CARRY, true);
        cpu.set(Flag.NIBBLE, true);
        cpu.set(Flag.OPERATION, true);
        cpu.set(Flag.ZERO, true);
        step(cpu, 1);
        assertEquals(0x03, cpu.pc);
        assertEquals("CALL_NZ shouldn't touch stack if not triggered", 0x00, cpu.unsafeRead(0xfffe));
        assertEquals("CALL_NZ shouldn't touch stack if not triggered", 0x00, cpu.unsafeRead(0xfffd));
    }

    @Test
    public void test_call_nz_does_not_modify_zero_flag_when_initially_false() {
        Cpu cpu = cpuWithProgram(0xc4, 0xff, 0xff);
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        step(cpu, 1);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_call_nz_does_not_modify_zero_flag_when_initially_true() {
        Cpu cpu = cpuWithProgram(0xc4, 0xff, 0xff);
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        cpu.set(Flag.ZERO, true);
        step(cpu, 1);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_call_nz_recursive_countdown() {
        Cpu cpu = cpuWithProgram(
                0x3e, 0xee,       // LD A, 0xee
                0xd6, 0x01,       // loop: SUB A, 0x01
                0xc4, 0x02, 0x00  // CALL NZ, loop
        );
        cpu.set(Word.Register.SP, Word.literal(0xfe99));
        runProgram(cpu, 7);
        assertEquals(0x00, cpu.read(Byte.Register.A));
        for (int i = 0; i < 0xed; i++) {
            assertEquals("Stack check at " + Integer.toHexString(0xfe98 - 2*i),
            0x00, cpu.unsafeRead(0xfe98 - 2*i));
            assertEquals("Stack check at " + Integer.toHexString(0xfe98 - 2*i - 1),
                    0x07, cpu.unsafeRead(0xfe98 - 2*i - 1));
        }
    }

    @Test
    public void test_call_nz_uses_24_cycles_if_triggered() {
        Cpu cpu = cpuWithProgram(0xc4, 0xff, 0xff);
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        step(cpu, 1);
        assertEquals(24, cpu.getCycles());
    }

    @Test
    public void test_call_nz_uses_12_cycles_if_not_triggered_() {
        Cpu cpu = cpuWithProgram(0xc4, 0xff, 0xff);
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        cpu.set(Flag.ZERO, true);
        step(cpu, 1);
        assertEquals(12, cpu.getCycles());
    }

    @Test
    public void test_call_z_does_nothing_if_zero_is_not_set() {
        Cpu cpu = cpuWithProgram(0xcc, 0x34, 0x12);
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        step(cpu, 1);
        assertEquals(0x03, cpu.pc);
        assertEquals("CALL_Z shouldn't touch stack if not triggered", 0x00, cpu.unsafeRead(0xfffe));
        assertEquals("CALL_Z shouldn't touch stack if not triggered", 0x00, cpu.unsafeRead(0xfffd));
    }

    @Test
    public void test_call_z_jumps_correctly_zero_is_set() {
        Cpu cpu = cpuWithProgram(0xcc, 0x34, 0x12);
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 3);
        assertEquals(0x1234, cpu.pc);
        assertEquals(0x00, cpu.unsafeRead(0xfffe));
        assertEquals(0x03, cpu.unsafeRead(0xfffd));
    }

    @Test
    public void test_call_z_is_unaffected_by_other_flags_if_zero_is_not_set() {
        Cpu cpu = cpuWithProgram(0xcc, 0xbb, 0xaa);
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        cpu.set(Flag.CARRY, true);
        cpu.set(Flag.NIBBLE, true);
        cpu.set(Flag.OPERATION, true);
        step(cpu, 1);
        assertEquals(0x03, cpu.pc);
        assertEquals("CALL_Z shouldn't touch stack if not triggered", 0x00, cpu.unsafeRead(0xfffe));
        assertEquals("CALL_Z shouldn't touch stack if not triggered", 0x00, cpu.unsafeRead(0xfffd));
    }

    @Test
    public void test_call_z_is_unaffected_by_other_flags_if_zero_is_set() {
        Cpu cpu = cpuWithProgram(0xcc, 0x78, 0x56);
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        cpu.set(Flag.CARRY, true);
        cpu.set(Flag.NIBBLE, true);
        cpu.set(Flag.OPERATION, true);
        cpu.set(Flag.ZERO, true);
        step(cpu, 1);
        assertEquals(0x5678, cpu.pc);
        assertEquals(0x00, cpu.unsafeRead(0xfffe));
        assertEquals(0x03, cpu.unsafeRead(0xfffd));
    }

    @Test
    public void test_call_z_does_not_modify_zero_flag_when_initially_false() {
        Cpu cpu = cpuWithProgram(0xcc, 0xff, 0xff);
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        step(cpu, 1);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_call_z_does_not_modify_zero_flag_when_initially_true() {
        Cpu cpu = cpuWithProgram(0xcc, 0xff, 0xff);
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 3);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_call_z_uses_24_cycles_if_jump_occurs() {
        Cpu cpu = cpuWithProgram(0xcc, 0xff, 0xff);
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 3);
        assertEquals(24, cpu.getCycles());
    }

    @Test
    public void test_call_z_uses_12_cycles_if_no_jump_occurs() {
        Cpu cpu = cpuWithProgram(0xca, 0xff, 0xff);
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        step(cpu, 1);
        assertEquals(12, cpu.getCycles());
    }

    @Test
    public void test_call_nc_does_nothing_if_carry_is_set() {
        Cpu cpu = cpuWithProgram(0xd4, 0xff, 0xff);
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertEquals(0x03, cpu.pc);
        assertEquals("CALL_NC shouldn't touch stack if not triggered", 0x00, cpu.unsafeRead(0xfffe));
        assertEquals("CALL_NC shouldn't touch stack if not triggered", 0x00, cpu.unsafeRead(0xfffd));
    }

    @Test
    public void test_call_nc_jumps_correctly_if_carry_is_not_set() {
        Cpu cpu = cpuWithProgram(0xd4, 0x34, 0x12);
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        step(cpu, 1);
        assertEquals(0x1234, cpu.pc);
        assertEquals(0x00, cpu.unsafeRead(0xfffe));
        assertEquals(0x03, cpu.unsafeRead(0xfffd));
    }

    @Test
    public void test_call_nc_is_unaffected_by_other_flags_if_carry_is_not_set() {
        Cpu cpu = cpuWithProgram(0xd4, 0xbb, 0xaa);
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        cpu.set(Flag.ZERO, true);
        cpu.set(Flag.NIBBLE, true);
        cpu.set(Flag.OPERATION, true);
        step(cpu, 1);
        assertEquals(0xaabb, cpu.pc);
        assertEquals(0x00, cpu.unsafeRead(0xfffe));
        assertEquals(0x03, cpu.unsafeRead(0xfffd));
    }

    @Test
    public void test_call_nc_is_unaffected_by_other_flags_if_carry_is_set() {
        Cpu cpu = cpuWithProgram(0xd4, 0x78, 0x56);
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        cpu.set(Flag.CARRY, true);
        cpu.set(Flag.NIBBLE, true);
        cpu.set(Flag.OPERATION, true);
        cpu.set(Flag.ZERO, true);
        step(cpu, 1);
        assertEquals(0x03, cpu.pc);
        assertEquals("CALL_NC shouldn't touch stack if not triggered", 0x00, cpu.unsafeRead(0xfffe));
        assertEquals("CALL_NC shouldn't touch stack if not triggered", 0x00, cpu.unsafeRead(0xfffd));
    }

    @Test
    public void test_call_nc_does_not_modify_carry_flag_when_initially_false() {
        Cpu cpu = cpuWithProgram(0xd4, 0xff, 0xff);
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        step(cpu, 1);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_call_nc_does_not_modify_carry_flag_when_initially_true() {
        Cpu cpu = cpuWithProgram(0xd4, 0xff, 0xff);
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_call_nc_count_up_to_rollover_recursively() {
        Cpu cpu = cpuWithProgram(
                0x3e, 0x01,       // LD A, 0x01
                0xc6, 0x01,       // loop: ADD A, 0x01  -- Note that INC A wouldn't work because it doesn't set carry.
                0xd4, 0x02, 0x00  // CALL_NC loop
        );
        cpu.set(Word.Register.SP, Word.literal(0xfe99));
        runProgram(cpu, 7);
        assertEquals(0x00, cpu.read(Byte.Register.A));
        for (int i = 0; i < 0xfe; i++) {
            assertEquals("Stack check at " + Integer.toHexString(0xfe98 - 2*i),
            0x00, cpu.unsafeRead(0xfe98 - 2*i));
            assertEquals("Stack check at " + Integer.toHexString(0xfe98 - 2*i),
                    0x07, cpu.unsafeRead(0xfe98 - 2*i - 1));
        }
    }

    @Test
    public void test_call_nc_uses_24_cycles_if_jump_occurs() {
        Cpu cpu = cpuWithProgram(0xd4, 0xff, 0xff);
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        step(cpu, 1);
        assertEquals(24, cpu.getCycles());
    }

    @Test
    public void test_call_nc_uses_12_cycles_if_no_jump_occurs() {
        Cpu cpu = cpuWithProgram(0xd4, 0xff, 0xff);
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertEquals(12, cpu.getCycles());
    }

    @Test
    public void test_call_c_does_nothing_if_carry_is_not_set() {
        Cpu cpu = cpuWithProgram(0xdc, 0xff, 0xff);
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        runProgram(cpu, 3);
        assertEquals(0x03, cpu.pc);
        assertEquals("CALL_C shouldn't touch stack if not triggered", 0x00, cpu.unsafeRead(0xfffe));
        assertEquals("CALL_C shouldn't touch stack if not triggered", 0x00, cpu.unsafeRead(0xfffd));
    }

    @Test
    public void test_call_c_jumps_correctly_if_carry_is_set() {
        Cpu cpu = cpuWithProgram(0xdc, 0xff, 0xff);
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertEquals(0xffff, cpu.pc);
        assertEquals(0x00, cpu.unsafeRead(0xfffe));
        assertEquals(0x03, cpu.unsafeRead(0xfffd));
    }

    @Test
    public void test_call_c_is_unaffected_by_other_flags_if_carry_is_not_set() {
        Cpu cpu = cpuWithProgram(0xdc, 0xbb, 0xaa);
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        cpu.set(Flag.ZERO, true);
        cpu.set(Flag.NIBBLE, true);
        cpu.set(Flag.OPERATION, true);
        step(cpu, 1);
        assertEquals(0x03, cpu.pc);
        assertEquals("CALL_C shouldn't touch stack if not triggered", 0x00, cpu.unsafeRead(0xfffe));
        assertEquals("CALL_C shouldn't touch stack if not triggered", 0x00, cpu.unsafeRead(0xfffd));
    }

    @Test
    public void test_call_c_is_unaffected_by_other_flags_if_carry_is_set() {
        Cpu cpu = cpuWithProgram(0xdc, 0x78, 0x56);
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        cpu.set(Flag.CARRY, true);
        cpu.set(Flag.NIBBLE, true);
        cpu.set(Flag.OPERATION, true);
        cpu.set(Flag.ZERO, true);
        step(cpu, 1);
        assertEquals(0x5678, cpu.pc);
        assertEquals(0x00, cpu.unsafeRead(0xfffe));
        assertEquals(0x03, cpu.unsafeRead(0xfffd));
    }

    @Test
    public void test_call_c_does_not_modify_carry_flag_when_initially_false() {
        Cpu cpu = cpuWithProgram(0xdc, 0xff, 0xff);
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        step(cpu, 1);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_call_c_does_not_modify_carry_flag_when_initially_true() {
        Cpu cpu = cpuWithProgram(0xdc, 0xff, 0xff);
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_call_c_uses_24_cycles_if_jump_occurs() {
        Cpu cpu = cpuWithProgram(0xdc, 0xff, 0xff);
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 3);
        assertEquals(24, cpu.getCycles());
    }

    @Test
    public void test_call_c_uses_12_cycles_if_no_jump_occurs() {
        Cpu cpu = cpuWithProgram(0xdc, 0xff, 0xff);
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        step(cpu, 1);
        assertEquals(12, cpu.getCycles());
    }

    @Test
    public void test_rst_00_from_address_0x1234() {
        Cpu cpu = cpuWithProgram();
        memset(cpu, 0x1234, 0xc7);
        cpu.pc = 0x1234;
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        step(cpu, 1);
        assertEquals(0x0000, cpu.pc);
        assertEquals(0x12, cpu.unsafeRead(0xfffe));
        assertEquals(0x35, cpu.unsafeRead(0xfffd));
    }

    @Test
    public void test_rst_00_from_address_0xabcd() {
        Cpu cpu = cpuWithProgram();
        memset(cpu, 0xabcd, 0xc7);
        cpu.pc = 0xabcd;
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        step(cpu, 1);
        assertEquals(0x0000, cpu.pc);
        assertEquals(0xab, cpu.unsafeRead(0xfffe));
        assertEquals(0xce, cpu.unsafeRead(0xfffd));
    }

    @Test
    public void test_rst_00_uses_16_cycles() {
        Cpu cpu = cpuWithProgram(0xc7);
        cpu.tick();
        assertEquals(16, cpu.getCycles());
    }

    @Test
    public void test_rst_08_from_address_0x1234() {
        Cpu cpu = cpuWithProgram();
        memset(cpu, 0x1234, 0xcf);
        cpu.pc = 0x1234;
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        step(cpu, 1);
        assertEquals(0x0008, cpu.pc);
        assertEquals(0x12, cpu.unsafeRead(0xfffe));
        assertEquals(0x35, cpu.unsafeRead(0xfffd));
    }

    @Test
    public void test_rst_08_from_address_0xabcd() {
        Cpu cpu = cpuWithProgram();
        memset(cpu, 0xabcd, 0xcf);
        cpu.pc = 0xabcd;
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        step(cpu, 1);
        assertEquals(0x0008, cpu.pc);
        assertEquals(0xab, cpu.unsafeRead(0xfffe));
        assertEquals(0xce, cpu.unsafeRead(0xfffd));
    }

    @Test
    public void test_rst_08_uses_16_cycles() {
        Cpu cpu = cpuWithProgram(0xcf);
        cpu.tick();
        assertEquals(16, cpu.getCycles());
    }

    @Test
    public void test_rst_10_from_address_0x1234() {
        Cpu cpu = cpuWithProgram();
        memset(cpu, 0x1234, 0xd7);
        cpu.pc = 0x1234;
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        step(cpu, 1);
        assertEquals(0x0010, cpu.pc);
        assertEquals(0x12, cpu.unsafeRead(0xfffe));
        assertEquals(0x35, cpu.unsafeRead(0xfffd));
    }

    @Test
    public void test_rst_10_from_address_0xabcd() {
        Cpu cpu = cpuWithProgram();
        memset(cpu, 0xabcd, 0xd7);
        cpu.pc = 0xabcd;
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        step(cpu, 1);
        assertEquals(0x0010, cpu.pc);
        assertEquals(0xab, cpu.unsafeRead(0xfffe));
        assertEquals(0xce, cpu.unsafeRead(0xfffd));
    }

    @Test
    public void test_rst_10_uses_16_cycles() {
        Cpu cpu = cpuWithProgram(0xd7);
        cpu.tick();
        assertEquals(16, cpu.getCycles());
    }

    @Test
    public void test_rst_18_from_address_0x1234() {
        Cpu cpu = cpuWithProgram();
        memset(cpu, 0x1234, 0xdf);
        cpu.pc = 0x1234;
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        step(cpu, 1);
        assertEquals(0x0018, cpu.pc);
        assertEquals(0x12, cpu.unsafeRead(0xfffe));
        assertEquals(0x35, cpu.unsafeRead(0xfffd));
    }

    @Test
    public void test_rst_18_from_address_0xabcd() {
        Cpu cpu = cpuWithProgram();
        memset(cpu, 0xabcd, 0xdf);
        cpu.pc = 0xabcd;
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        step(cpu, 1);
        assertEquals(0x0018, cpu.pc);
        assertEquals(0xab, cpu.unsafeRead(0xfffe));
        assertEquals(0xce, cpu.unsafeRead(0xfffd));
    }

    @Test
    public void test_rst_18_uses_16_cycles() {
        Cpu cpu = cpuWithProgram(0xdf);
        cpu.tick();
        assertEquals(16, cpu.getCycles());
    }

    @Test
    public void test_rst_20_from_address_0x1234() {
        Cpu cpu = cpuWithProgram();
        memset(cpu, 0x1234, 0xe7);
        cpu.pc = 0x1234;
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        step(cpu, 1);
        assertEquals(0x0020, cpu.pc);
        assertEquals(0x12, cpu.unsafeRead(0xfffe));
        assertEquals(0x35, cpu.unsafeRead(0xfffd));
    }

    @Test
    public void test_rst_20_from_address_0xabcd() {
        Cpu cpu = cpuWithProgram();
        memset(cpu, 0xabcd, 0xe7);
        cpu.pc = 0xabcd;
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        step(cpu, 1);
        assertEquals(0x0020, cpu.pc);
        assertEquals(0xab, cpu.unsafeRead(0xfffe));
        assertEquals(0xce, cpu.unsafeRead(0xfffd));
    }

    @Test
    public void test_rst_20_uses_16_cycles() {
        Cpu cpu = cpuWithProgram(0xe7);
        cpu.tick();
        assertEquals(16, cpu.getCycles());
    }

    @Test
    public void test_rst_28_from_address_0x1234() {
        Cpu cpu = cpuWithProgram();
        memset(cpu, 0x1234, 0xef);
        cpu.pc = 0x1234;
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        step(cpu, 1);
        assertEquals(0x0028, cpu.pc);
        assertEquals(0x12, cpu.unsafeRead(0xfffe));
        assertEquals(0x35, cpu.unsafeRead(0xfffd));
    }

    @Test
    public void test_rst_28_from_address_0xabcd() {
        Cpu cpu = cpuWithProgram();
        memset(cpu, 0xabcd, 0xef);
        cpu.pc = 0xabcd;
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        step(cpu, 1);
        assertEquals(0x0028, cpu.pc);
        assertEquals(0xab, cpu.unsafeRead(0xfffe));
        assertEquals(0xce, cpu.unsafeRead(0xfffd));
    }

    @Test
    public void test_rst_28_uses_16_cycles() {
        Cpu cpu = cpuWithProgram(0xef);
        cpu.tick();
        assertEquals(16, cpu.getCycles());
    }

    @Test
    public void test_rst_30_from_address_0x1234() {
        Cpu cpu = cpuWithProgram();
        memset(cpu, 0x1234, 0xf7);
        cpu.pc = 0x1234;
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        step(cpu, 1);
        assertEquals(0x0030, cpu.pc);
        assertEquals(0x12, cpu.unsafeRead(0xfffe));
        assertEquals(0x35, cpu.unsafeRead(0xfffd));
    }

    @Test
    public void test_rst_30_from_address_0xabcd() {
        Cpu cpu = cpuWithProgram();
        memset(cpu, 0xabcd, 0xf7);
        cpu.pc = 0xabcd;
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        step(cpu, 1);
        assertEquals(0x0030, cpu.pc);
        assertEquals(0xab, cpu.unsafeRead(0xfffe));
        assertEquals(0xce, cpu.unsafeRead(0xfffd));
    }

    @Test
    public void test_rst_30_uses_16_cycles() {
        Cpu cpu = cpuWithProgram(0xf7);
        cpu.tick();
        assertEquals(16, cpu.getCycles());
    }

    @Test
    public void test_rst_38_from_address_0x1234() {
        Cpu cpu = cpuWithProgram();
        memset(cpu, 0x1234, 0xff);
        cpu.pc = 0x1234;
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        step(cpu, 1);
        assertEquals(0x0038, cpu.pc);
        assertEquals(0x12, cpu.unsafeRead(0xfffe));
        assertEquals(0x35, cpu.unsafeRead(0xfffd));
    }

    @Test
    public void test_rst_38_from_address_0xabcd() {
        Cpu cpu = cpuWithProgram();
        memset(cpu, 0xabcd, 0xff);
        cpu.pc = 0xabcd;
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        step(cpu, 1);
        assertEquals(0x0038, cpu.pc);
        assertEquals(0xab, cpu.unsafeRead(0xfffe));
        assertEquals(0xce, cpu.unsafeRead(0xfffd));
    }

    @Test
    public void test_rst_38_uses_16_cycles() {
        Cpu cpu = cpuWithProgram(0xff);
        cpu.tick();
        assertEquals(16, cpu.getCycles());
    }

    @Test
    public void test_ret_with_sp_at_0x1234_jumping_to_0x5678() {
        Cpu cpu = cpuWithProgram(0xc9);
        setupStack(cpu, 0x1234, 0x56, 0x78);
        cpu.tick();
        assertEquals(0x5678, cpu.pc);
    }

    @Test
    public void test_ret_with_sp_at_0x3456_jumping_to_0xabcd() {
        Cpu cpu = cpuWithProgram(0xc9);
        setupStack(cpu, 0x3456, 0xab, 0xcd);
        cpu.tick();
        assertEquals(0xabcd, cpu.pc);
    }

    @Test
    public void test_call_and_return() {
        Cpu cpu = cpuWithProgram(
                0x3c,              // INC A
                0xcd, 0xcd, 0xab,  // CALL 0xabcd
                0x3c               // INC A
        );
        memset(cpu, 0xabcd, 0x3c, 0xc9); // INC A; RET
        cpu.set(Word.Register.SP, Word.literal(0xeeee));
        step(cpu, 5);
        assertEquals(3, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_ret_uses_16_cycles() {
        Cpu cpu = cpuWithProgram(0xc9);
        cpu.set(Word.Register.SP, Word.literal(0xeeee));
        cpu.tick();
        assertEquals(16, cpu.getCycles());
    }

    @Test
    public void test_ret_nz_does_nothing_if_zero_is_set() {
        Cpu cpu = cpuWithProgram(0xc0, 0x3c);
        setupStack(cpu, 0xeeee, 0xff, 0xff);
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 3);
        assertEquals(0x01, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_ret_nz_jumps_correctly_if_zero_is_not_set() {
        Cpu cpu = cpuWithProgram(0xc0, 0x3c);
        setupStack(cpu, 0xeeee, 0xff, 0xff);
        runProgram(cpu, 2);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_ret_nz_is_unaffected_by_other_flags_if_zero_is_not_set() {
        Cpu cpu = cpuWithProgram(0xc0, 0x3c);
        setupStack(cpu, 0xeeee, 0xff, 0xff);
        cpu.set(Flag.CARRY, true);
        cpu.set(Flag.NIBBLE, true);
        cpu.set(Flag.OPERATION, true);
        runProgram(cpu, 2);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_ret_nz_is_unaffected_by_other_flags_if_zero_is_set() {
        Cpu cpu = cpuWithProgram(0xc0, 0x3c);
        setupStack(cpu,0xeeee, 0xff, 0xff);
        cpu.set(Flag.CARRY, true);
        cpu.set(Flag.NIBBLE, true);
        cpu.set(Flag.OPERATION, true);
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 2);
        assertEquals(0x01, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_ret_nz_does_not_modify_zero_flag_when_initially_false() {
        Cpu cpu = cpuWithProgram(0xc0);
        setupStack(cpu, 0xeeee, 0x00, 0x00);
        step(cpu, 1);
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_ret_nz_does_not_modify_zero_flag_when_initially_true() {
        Cpu cpu = cpuWithProgram(0xc0);
        setupStack(cpu, 0xeeee, 0x00, 0x00);
        cpu.set(Flag.ZERO, true);
        step(cpu, 1);
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_ret_nz_uses_20_cycles_if_triggered() {
        Cpu cpu = cpuWithProgram(0xc0);
        setupStack(cpu, 0xeeee, 0xff, 0xff);
        step(cpu, 1);
        assertEquals(20, cpu.getCycles());
    }

    @Test
    public void test_ret_nz_uses_8_cycles_if_not_triggered_() {
        Cpu cpu = cpuWithProgram(0xc0, 0xff, 0xff);
        cpu.set(Word.Register.SP, Word.literal(0xffff));
        cpu.set(Flag.ZERO, true);
        step(cpu, 1);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_ret_z_does_nothing_if_zero_is_not_set() {
        Cpu cpu = cpuWithProgram(0xc8, 0x3c);
        setupStack(cpu, 0xeeee, 0xff, 0xff);
        runProgram(cpu, 2);
        assertEquals(0x01, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_ret_z_jumps_correctly_zero_is_set() {
        Cpu cpu = cpuWithProgram(0xc8, 0x3c);
        setupStack(cpu, 0xeeee, 0xff, 0xff);
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 2);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_ret_z_is_unaffected_by_other_flags_if_zero_is_not_set() {
        Cpu cpu = cpuWithProgram(0xc8, 0x3c);
        setupStack(cpu, 0xeeee, 0xff, 0xff);
        cpu.set(Flag.CARRY, true);
        cpu.set(Flag.NIBBLE, true);
        cpu.set(Flag.OPERATION, true);
        runProgram(cpu, 2);
        assertEquals(0x01, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_ret_z_is_unaffected_by_other_flags_if_zero_is_set() {
        Cpu cpu = cpuWithProgram(0xc8, 0x3c);
        setupStack(cpu, 0xeeee, 0xff, 0xff);
        cpu.set(Flag.CARRY, true);
        cpu.set(Flag.NIBBLE, true);
        cpu.set(Flag.OPERATION, true);
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 2);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_ret_z_does_not_modify_zero_flag_when_initially_false() {
        Cpu cpu = cpuWithProgram(0xc8);
        setupStack(cpu, 0xeeee);
        cpu.tick();
        assertFalse(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_ret_z_does_not_modify_zero_flag_when_initially_true() {
        Cpu cpu = cpuWithProgram(0xc8);
        setupStack(cpu, 0xeeee);
        cpu.set(Flag.ZERO, true);
        cpu.tick();
        assertTrue(cpu.isSet(Flag.ZERO));
    }

    @Test
    public void test_ret_z_uses_20_cycles_if_jump_occurs() {
        Cpu cpu = cpuWithProgram(0xc8);
        cpu.set(Flag.ZERO, true);
        setupStack(cpu, 0xeeee);
        cpu.tick();
        assertEquals(20, cpu.getCycles());
    }

    @Test
    public void test_ret_z_uses_8_cycles_if_no_jump_occurs() {
        Cpu cpu = cpuWithProgram(0xc8);
        setupStack(cpu, 0xeeee, 0xff, 0xff);
        cpu.tick();
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_ret_nc_does_nothing_if_carry_is_set() {
        Cpu cpu = cpuWithProgram(0xd0, 0x3c);
        setupStack(cpu, 0xeeee, 0xff, 0xff);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 2);
        assertEquals(0x01, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_ret_nc_jumps_correctly_if_carry_is_not_set() {
        Cpu cpu = cpuWithProgram(0xd0, 0x3c);
        setupStack(cpu, 0xeeee, 0xff, 0xff);
        step(cpu, 2);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_ret_nc_is_unaffected_by_other_flags_if_carry_is_not_set() {
        Cpu cpu = cpuWithProgram(0xd0, 0x3c);
        setupStack(cpu, 0xeeee, 0xff, 0xff);
        cpu.set(Flag.ZERO, true);
        cpu.set(Flag.NIBBLE, true);
        cpu.set(Flag.OPERATION, true);
        runProgram(cpu, 2);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_ret_nc_is_unaffected_by_other_flags_if_carry_is_set() {
        Cpu cpu = cpuWithProgram(0xd0, 0x3c);
        setupStack(cpu, 0xeeee, 0xff, 0xff);
        cpu.set(Flag.CARRY, true);
        cpu.set(Flag.NIBBLE, true);
        cpu.set(Flag.OPERATION, true);
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 2);
        assertEquals(0x01, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_ret_nc_does_not_modify_carry_flag_when_initially_false() {
        Cpu cpu = cpuWithProgram(0xd0);
        setupStack(cpu, 0xeeee, 0xff, 0xff);
        step(cpu, 1);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_ret_nc_does_not_modify_carry_flag_when_initially_true() {
        Cpu cpu = cpuWithProgram(0xd0);
        setupStack(cpu, 0xeeee, 0xff, 0xff);
        cpu.set(Flag.CARRY, true);
        step(cpu, 1);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_ret_nc_uses_20_cycles_if_jump_occurs() {
        Cpu cpu = cpuWithProgram(0xd0);
        setupStack(cpu, 0xeeee, 0xff, 0xff);
        step(cpu, 1);
        assertEquals(20, cpu.getCycles());
    }

    @Test
    public void test_ret_nc_uses_8_cycles_if_no_jump_occurs() {
        Cpu cpu = cpuWithProgram(0xd0);
        setupStack(cpu, 0xeeee, 0xff, 0xff);
        cpu.set(Flag.CARRY, true);
        step(cpu, 1);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_ret_c_does_nothing_if_carry_is_not_set() {
        Cpu cpu = cpuWithProgram(0xd8, 0x3c);
        setupStack(cpu, 0xeeee, 0xff, 0xff);
        runProgram(cpu, 2);
        assertEquals(0x01, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_ret_c_jumps_correctly_if_carry_is_set() {
        Cpu cpu = cpuWithProgram(0xd8, 0x3c);
        setupStack(cpu, 0xeeee, 0xff, 0xff);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 2);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_ret_c_is_unaffected_by_other_flags_if_carry_is_not_set() {
        Cpu cpu = cpuWithProgram(0xd8, 0x3c);
        setupStack(cpu, 0xeeee, 0xff, 0xff);
        cpu.set(Flag.ZERO, true);
        cpu.set(Flag.NIBBLE, true);
        cpu.set(Flag.OPERATION, true);
        runProgram(cpu, 2);
        assertEquals(0x01, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_ret_c_is_unaffected_by_other_flags_if_carry_is_set() {
        Cpu cpu = cpuWithProgram(0xd8, 0x3c);
        setupStack(cpu, 0xeeee, 0xff, 0xff);
        cpu.set(Flag.CARRY, true);
        cpu.set(Flag.NIBBLE, true);
        cpu.set(Flag.OPERATION, true);
        cpu.set(Flag.ZERO, true);
        runProgram(cpu, 2);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_ret_c_does_not_modify_carry_flag_when_initially_false() {
        Cpu cpu = cpuWithProgram(0xd8);
        setupStack(cpu, 0xeeee, 0xff, 0xff);
        step(cpu, 1);
        assertFalse(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_ret_c_does_not_modify_carry_flag_when_initially_true() {
        Cpu cpu = cpuWithProgram(0xd8);
        setupStack(cpu, 0xeeee, 0xff, 0xff);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 1);
        assertTrue(cpu.isSet(Flag.CARRY));
    }

    @Test
    public void test_ret_c_uses_20_cycles_if_jump_occurs() {
        Cpu cpu = cpuWithProgram(0xd8);
        setupStack(cpu, 0xeeee, 0xff, 0xff);
        cpu.set(Flag.CARRY, true);
        runProgram(cpu, 1);
        assertEquals(20, cpu.getCycles());
    }

    @Test
    public void test_call_c_uses_8_cycles_if_no_jump_occurs() {
        Cpu cpu = cpuWithProgram(0xd8);
        setupStack(cpu, 0xeeee, 0xff, 0xff);
        step(cpu, 1);
        assertEquals(8, cpu.getCycles());
    }

    @Test
    public void test_reti_returns_from_call() {
        Cpu cpu = cpuWithProgram(0xcd, 0x04, 0x00, 0x3c, 0x3c, 0xd9);
        step(cpu, 4);
        assertEquals(0x02, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_reti_enables_interrupts() {
        Cpu cpu = cpuWithProgram(0xd9);
        cpu.interruptsEnabled = false;
        cpu.tick();
        assertTrue(cpu.interruptsEnabled);
    }

    @Test
    public void test_reti_does_not_disable_interrupts() {
        Cpu cpu = cpuWithProgram(0xd9);
        cpu.interruptsEnabled = true;
        cpu.tick();
        assertTrue(cpu.interruptsEnabled);
    }

    @Test
    public void test_reti_uses_16_cycles() {
        Cpu cpu = cpuWithProgram(0xd9);
        cpu.tick();
        assertEquals(16, cpu.getCycles());
    }

    @Test
    public void test_vblank_interrupt_does_nothing_if_master_interrupt_switch_disabled() {
        Cpu cpu = cpuWithProgram(0x00, 0x00, 0x00);
        cpu.setInterruptsEnabled(false);
        memset(cpu, 0x0040, 0x3c, 0x3c, 0x3c);
        cpu.interrupt(Interrupt.V_BLANK);
        step(cpu, 3);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_vblank_interrupt_sets_0xff0f_bit_0() {
        Cpu cpu = cpuWithProgram();
        cpu.interrupt(Interrupt.V_BLANK);
        assertEquals(0x01, cpu.unsafeRead(0xff0f) & 0x01);
    }

    @Test
    public void test_vblank_interrupt_doesnt_set_0xff0f_bits_other_than_0() {
        Cpu cpu = cpuWithProgram();
        cpu.interrupt(Interrupt.V_BLANK);
        assertEquals(0x00, cpu.unsafeRead(0xff0f) & 0xfe);
    }

    @Test
    public void test_check_interrupt_vblank_returns_false_when_0xff0f_is_0x00() {
        Cpu cpu = cpuWithProgram();
        assertFalse(cpu.checkInterrupt(Interrupt.V_BLANK));
    }

    @Test
    public void test_check_interrupt_vblank_returns_false_when_0xff0f_is_0xfe() {
        Cpu cpu = cpuWithProgram();
        memset(cpu, 0xff0f, 0xfe);
        assertFalse(cpu.checkInterrupt(Interrupt.V_BLANK));
    }

    @Test
    public void test_check_interrupt_vblank_returns_true_when_0xff0f_is_0x01() {
        Cpu cpu = cpuWithProgram();
        memset(cpu, 0xff0f, 0x01);
        assertTrue(cpu.checkInterrupt(Interrupt.V_BLANK));
    }

    @Test
    public void test_check_interrupt_vblank_returns_true_when_0xff0f_is_0xff() {
        Cpu cpu = cpuWithProgram();
        memset(cpu, 0xff0f, 0xff);
        assertTrue(cpu.checkInterrupt(Interrupt.V_BLANK));
    }

    @Test
    public void test_reset_interrupt_vblank_sets_0xff0f_from_0x01_to_0x00() {
        Cpu cpu = cpuWithProgram();
        memset(cpu, 0xff0f, 0x01);
        cpu.resetInterrupt(Interrupt.V_BLANK);
        assertEquals(0x00, cpu.unsafeRead(0xff0f));
    }

    @Test
    public void test_reset_interrupt_vblank_sets_0xff0f_from_0xff_to_0xfe() {
        Cpu cpu = cpuWithProgram();
        memset(cpu, 0xff0f, 0xff);
        cpu.resetInterrupt(Interrupt.V_BLANK);
        assertEquals(0xfe, cpu.unsafeRead(0xff0f));
    }

    @Test
    public void test_reset_interrupt_vblank_has_no_effect_when_0xff0f_is_0x00() {
        Cpu cpu = cpuWithProgram();
        memset(cpu, 0xff0f, 0x00);
        cpu.resetInterrupt(Interrupt.V_BLANK);
        assertEquals(0x00, cpu.unsafeRead(0xff0f));
    }

    @Test
    public void test_reset_interrupt_vblank_has_no_effect_when_0xff0f_is_0xfe() {
        Cpu cpu = cpuWithProgram();
        memset(cpu, 0xff0f, 0xfe);
        cpu.resetInterrupt(Interrupt.V_BLANK);
        assertEquals(0xfe, cpu.unsafeRead(0xff0f));
    }

    public void test_is_enabled_vblank_returns_false_when_0xffff_is_0x00() {
        Cpu cpu = cpuWithProgram();
        memset(cpu, 0xffff, 0x00);
        assertFalse(cpu.isEnabled(Interrupt.V_BLANK));
    }

    @Test
    public void test_is_enabled_vblank_returns_false_when_0xffff_is_0xfe() {
        Cpu cpu = cpuWithProgram();
        memset(cpu, 0xffff, 0xfe);
        assertFalse(cpu.isEnabled(Interrupt.V_BLANK));
    }

    @Test
    public void test_is_enabled_vblank_returns_true_when_0xffff_is_0x01() {
        Cpu cpu = cpuWithProgram();
        memset(cpu, 0xffff, 0x01);
        assertTrue(cpu.isEnabled(Interrupt.V_BLANK));
    }

    @Test
    public void test_is_enabled_vblank_returns_true_when_0xffff_is_0xff() {
        Cpu cpu = cpuWithProgram();
        memset(cpu, 0xffff, 0xff);
        assertTrue(cpu.isEnabled(Interrupt.V_BLANK));
    }

    @Test
    public void test_vblank_interrupt_fires_when_enabled_and_triggered() {
        Cpu cpu = cpuWithProgram(0x00, 0x00, 0x00);
        cpu.setInterruptsEnabled(true);
        setupStack(cpu, 0xeeee);
        memset(cpu, 0xffff, 0x01);  // Enable V_BLANK interrupt
        memset(cpu, 0x0040, 0x3c, 0x3c, 0x3c); // Set up V_BLANK handler - this should be triggered
        cpu.interrupt(Interrupt.V_BLANK);
        step(cpu, 3);
        assertEquals(0x03, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_vblank_interrupt_doesnt_fire_if_specifically_disabled() {
        Cpu cpu = cpuWithProgram(0x00, 0x00, 0x00);
        cpu.setInterruptsEnabled(true);
        setupStack(cpu, 0xeeee);
        memset(cpu, 0xffff, 0x00); // Explicitly disable V_BLANK
        memset(cpu, 0x0040, 0x3c, 0x3c, 0x3c); // Set up V_BLANK handler - this should never run
        cpu.interrupt(Interrupt.V_BLANK);
        step(cpu, 3);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_vblank_interrupt_doesnt_fire_if_enabled_but_master_interrupt_off() {
        Cpu cpu = cpuWithProgram(0x00, 0x00, 0x00);
        cpu.setInterruptsEnabled(false);
        setupStack(cpu, 0xeeee);
        memset(cpu, 0xffff, 0x01); // Enable V_BLANK
        memset(cpu, 0x0040, 0x3c, 0x3c, 0x3c); // Set up V_BLANK handler - this should never run
        cpu.interrupt(Interrupt.V_BLANK);
        step(cpu, 3);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_vblank_interrupt_doesnt_fire_unless_triggered() {
        Cpu cpu = cpuWithProgram(0x00, 0x00, 0x00);
        cpu.setInterruptsEnabled(true);
        setupStack(cpu, 0xeeee);
        memset(cpu, 0xffff, 0x01); // Enable V_BLANK
        memset(cpu, 0x0040, 0x3c, 0x3c, 0x3c); // Set up V_BLANK handler - this should never run
        step(cpu, 3);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_vblank_interrupt_is_reset_when_handler_is_called() {
        Cpu cpu = cpuWithProgram();
        cpu.setInterruptsEnabled(true);
        setupStack(cpu, 0xeeee);
        memset(cpu, 0xffff, 0x01); // Enable V_BLANK
        cpu.interrupt(Interrupt.V_BLANK);
        cpu.tick();
        assertFalse(cpu.checkInterrupt(Interrupt.V_BLANK));
    }

    @Test
    public void test_master_interrupt_flag_disabled_when_vblank_handler_is_called() {
        Cpu cpu = cpuWithProgram();
        cpu.setInterruptsEnabled(true);
        setupStack(cpu, 0xeeee);
        memset(cpu, 0xffff, 0x01); // Enable V_BLANK
        cpu.interrupt(Interrupt.V_BLANK);
        cpu.tick();
        assertFalse(cpu.interruptsEnabled);
    }

    @Test
    public void test_a_different_vblank_interrupt_handler() {
        Cpu cpu = cpuWithProgram();
        cpu.setInterruptsEnabled(true);
        setupStack(cpu, 0xeeee);
        memset(cpu, 0xffff, 0x01); // Enable V_BLANK
        memset(cpu, 0x0040, 0x06, 0xab); // V_BLANK handler: set B to 0xab
        cpu.interrupt(Interrupt.V_BLANK);
        cpu.tick();
        assertEquals(0xab, cpu.read(Byte.Register.B));
    }

    @Test
    public void test_interrupt_on_third_clock_tick() {
        Cpu cpu = cpuWithProgram(0x3c, 0x3c, 0x3c, 0x3c);
        cpu.setInterruptsEnabled(true);
        setupStack(cpu, 0xeeee);
        memset(cpu, 0xffff, 0x01); // Enable V_BLANK
        memset(cpu, 0x0040, 0x04, 0x04); // V_BLANK handler: increment B twice
        step(cpu, 2); // Do the first two increments of A
        cpu.interrupt(Interrupt.V_BLANK); // Trigger interrupt (so remaining two increments of A should be skipped)
        step(cpu, 2); // These two ticks should be spend in the V_BLANK handler
        assertEquals(0x02, cpu.read(Byte.Register.A));
        assertEquals(0x02, cpu.read(Byte.Register.B));
    }

    @Test
    public void test_interrupt_and_return() {
        Cpu cpu = cpuWithProgram(0x3c, 0x3c, 0x3c);
        cpu.setInterruptsEnabled(true);
        setupStack(cpu, 0xeeee);
        memset(cpu, 0xffff, 0x01); // Enable V_BLANK
        memset(cpu, 0x0040, 0x04, 0x04, 0xc9);
        cpu.interrupt(Interrupt.V_BLANK);
        step(cpu, 6);
        assertEquals(0x03, cpu.read(Byte.Register.A));
        assertEquals(0x02, cpu.read(Byte.Register.B));
    }

    @Test
    public void test_successive_vblank_interrupts_ignored_if_using_ret() {
        Cpu cpu = cpuWithProgram();
        cpu.setInterruptsEnabled(true);
        setupStack(cpu, 0xeeee);
        memset(cpu, 0xffff, 0x01); // Enable V_BLANK
        memset(cpu, 0x0040, 0x3c, 0xc9); // V_BLANK handler: INC A; RET
        cpu.interrupt(Interrupt.V_BLANK);
        cpu.tick();
        cpu.interrupt(Interrupt.V_BLANK);
        cpu.tick();
        cpu.interrupt(Interrupt.V_BLANK);
        cpu.tick();
        assertEquals(0x01, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_successive_vblank_interrupts_using_reti() {
        Cpu cpu = cpuWithProgram();
        cpu.setInterruptsEnabled(true);
        setupStack(cpu, 0xeeee);
        memset(cpu, 0xffff, 0x01); // Enable V_BLANK
        memset(cpu, 0x0040, 0x3c, 0xd9); // V_BLANK handler: INC A; RETI
        cpu.interrupt(Interrupt.V_BLANK);
        step(cpu, 2);
        cpu.interrupt(Interrupt.V_BLANK);
        step(cpu, 2);
        cpu.interrupt(Interrupt.V_BLANK);
        step(cpu, 2);
        assertEquals(0x03, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_lcd_stat_interrupt_does_nothing_if_master_interrupt_switch_disabled() {
        Cpu cpu = cpuWithProgram(0x00, 0x00, 0x00);
        cpu.setInterruptsEnabled(false);
        memset(cpu, 0x0048, 0x3c, 0x3c, 0x3c);
        cpu.interrupt(Interrupt.LCD_STAT);
        step(cpu, 3);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_lcd_stat_interrupt_sets_0xff0f_bit_1() {
        Cpu cpu = cpuWithProgram();
        cpu.interrupt(Interrupt.LCD_STAT);
        assertEquals(0x02, cpu.unsafeRead(0xff0f) & 0x02);
    }

    @Test
    public void test_lcd_stat_interrupt_doesnt_set_0xff0f_bits_other_than_1() {
        Cpu cpu = cpuWithProgram();
        cpu.interrupt(Interrupt.LCD_STAT);
        assertEquals(0x00, cpu.unsafeRead(0xff0f) & 0xfd);
    }

    @Test
    public void test_check_interrupt_lcd_stat_returns_false_when_0xff0f_is_0x00() {
        Cpu cpu = cpuWithProgram();
        assertFalse(cpu.checkInterrupt(Interrupt.LCD_STAT));
    }

    @Test
    public void test_check_interrupt_lcd_stat_returns_false_when_0xff0f_is_0xfd() {
        Cpu cpu = cpuWithProgram();
        memset(cpu, 0xff0f, 0xfd);
        assertFalse(cpu.checkInterrupt(Interrupt.LCD_STAT));
    }

    @Test
    public void test_check_interrupt_lcd_stat_returns_true_when_0xff0f_is_0x02() {
        Cpu cpu = cpuWithProgram();
        memset(cpu, 0xff0f, 0x02);
        assertTrue(cpu.checkInterrupt(Interrupt.LCD_STAT));
    }

    @Test
    public void test_check_interrupt_lcd_stat_returns_true_when_0xff0f_is_0xff() {
        Cpu cpu = cpuWithProgram();
        memset(cpu, 0xff0f, 0xff);
        assertTrue(cpu.checkInterrupt(Interrupt.LCD_STAT));
    }

    @Test
    public void test_reset_interrupt_lcd_stat_sets_0xff0f_from_0x02_to_0x00() {
        Cpu cpu = cpuWithProgram();
        memset(cpu, 0xff0f, 0x02);
        cpu.resetInterrupt(Interrupt.LCD_STAT);
        assertEquals(0x00, cpu.unsafeRead(0xff0f));
    }

    @Test
    public void test_reset_interrupt_lcd_stat_sets_0xff0f_from_0xff_to_0xfd() {
        Cpu cpu = cpuWithProgram();
        memset(cpu, 0xff0f, 0xff);
        cpu.resetInterrupt(Interrupt.LCD_STAT);
        assertEquals(0xfd, cpu.unsafeRead(0xff0f));
    }

    @Test
    public void test_reset_interrupt_lcd_stat_has_no_effect_when_0xff0f_is_0x00() {
        Cpu cpu = cpuWithProgram();
        memset(cpu, 0xff0f, 0x00);
        cpu.resetInterrupt(Interrupt.LCD_STAT);
        assertEquals(0x00, cpu.unsafeRead(0xff0f));
    }

    @Test
    public void test_reset_interrupt_lcd_stat_has_no_effect_when_0xff0f_is_0xfd() {
        Cpu cpu = cpuWithProgram();
        memset(cpu, 0xff0f, 0xfd);
        cpu.resetInterrupt(Interrupt.LCD_STAT);
        assertEquals(0xfd, cpu.unsafeRead(0xff0f));
    }

    public void test_is_enabled_lcd_stat_returns_false_when_0xffff_is_0x00() {
        Cpu cpu = cpuWithProgram();
        memset(cpu, 0xffff, 0x00);
        assertFalse(cpu.isEnabled(Interrupt.LCD_STAT));
    }

    @Test
    public void test_is_enabled_lcd_stat_returns_false_when_0xffff_is_0xfd() {
        Cpu cpu = cpuWithProgram();
        memset(cpu, 0xffff, 0xfd);
        assertFalse(cpu.isEnabled(Interrupt.LCD_STAT));
    }

    @Test
    public void test_is_enabled_lcd_stat_returns_true_when_0xffff_is_0x02() {
        Cpu cpu = cpuWithProgram();
        memset(cpu, 0xffff, 0x02);
        assertTrue(cpu.isEnabled(Interrupt.LCD_STAT));
    }

    @Test
    public void test_is_enabled_lcd_stat_returns_true_when_0xffff_is_0xff() {
        Cpu cpu = cpuWithProgram();
        memset(cpu, 0xffff, 0xff);
        assertTrue(cpu.isEnabled(Interrupt.LCD_STAT));
    }

    @Test
    public void test_lcd_stat_interrupt_fires_when_enabled_and_triggered() {
        Cpu cpu = cpuWithProgram(0x00, 0x00, 0x00);
        cpu.setInterruptsEnabled(true);
        setupStack(cpu, 0xeeee);
        memset(cpu, 0xffff, 0x02);  // Enable LCD STAT interrupt
        memset(cpu, 0x0048, 0x3c, 0x3c, 0x3c); // Set up LCD STAT handler - this should be triggered
        cpu.interrupt(Interrupt.LCD_STAT);
        step(cpu, 3);
        assertEquals(0x03, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_lcd_stat_interrupt_doesnt_fire_if_specifically_disabled() {
        Cpu cpu = cpuWithProgram(0x00, 0x00, 0x00);
        cpu.setInterruptsEnabled(true);
        setupStack(cpu, 0xeeee);
        memset(cpu, 0xffff, 0x00); // Explicitly disable LCD STAT
        memset(cpu, 0x0048, 0x3c, 0x3c, 0x3c); // Set up LCD STAT handler - this should never run
        cpu.interrupt(Interrupt.LCD_STAT);
        step(cpu, 3);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_lcd_stat_interrupt_doesnt_fire_if_enabled_but_master_interrupt_off() {
        Cpu cpu = cpuWithProgram(0x00, 0x00, 0x00);
        cpu.setInterruptsEnabled(false);
        setupStack(cpu, 0xeeee);
        memset(cpu, 0xffff, 0x02); // Enable LCD STAT
        memset(cpu, 0x0048, 0x3c, 0x3c, 0x3c); // Set up LCD STAT handler - this should never run
        cpu.interrupt(Interrupt.LCD_STAT);
        step(cpu, 3);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_lcd_stat_interrupt_doesnt_fire_unless_triggered() {
        Cpu cpu = cpuWithProgram(0x00, 0x00, 0x00);
        cpu.setInterruptsEnabled(true);
        setupStack(cpu, 0xeeee);
        memset(cpu, 0xffff, 0x02); // Enable LCD STAT
        memset(cpu, 0x0048, 0x3c, 0x3c, 0x3c); // Set up LCD_STAT handler - this should never run
        step(cpu, 3);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_lcd_stat_interrupt_is_reset_when_handler_is_called() {
        Cpu cpu = cpuWithProgram();
        cpu.setInterruptsEnabled(true);
        setupStack(cpu, 0xeeee);
        memset(cpu, 0xffff, 0x02); // Enable LCD STAT
        cpu.interrupt(Interrupt.LCD_STAT);
        cpu.tick();
        assertFalse(cpu.checkInterrupt(Interrupt.V_BLANK));
    }

    @Test
    public void test_master_interrupt_flag_disabled_when_lcd_stat_handler_is_called() {
        Cpu cpu = cpuWithProgram();
        cpu.setInterruptsEnabled(true);
        setupStack(cpu, 0xeeee);
        memset(cpu, 0xffff, 0x02); // Enable LCD STAT
        cpu.interrupt(Interrupt.LCD_STAT);
        cpu.tick();
        assertFalse(cpu.interruptsEnabled);
    }

    @Test
    public void test_a_different_lcd_stat_interrupt_handler() {
        Cpu cpu = cpuWithProgram();
        cpu.setInterruptsEnabled(true);
        setupStack(cpu, 0xeeee);
        memset(cpu, 0xffff, 0x02); // Enable LCD STAT
        memset(cpu, 0x0048, 0x06, 0xab); // LCD STAT handler: set B to 0xab
        cpu.interrupt(Interrupt.LCD_STAT);
        cpu.tick();
        assertEquals(0xab, cpu.read(Byte.Register.B));
    }

    @Test
    public void test_timer_interrupt() {
        Cpu cpu = cpuWithProgram();
        cpu.setInterruptsEnabled(true);
        setupStack(cpu, 0xeeee);
        memset(cpu, 0xffff, 0x04);
        memset(cpu, 0x0050, 0x3c, 0x3c);
        cpu.interrupt(Interrupt.TIMER);
        step(cpu, 2);
        assertEquals(0x02, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_serial_interrupt() {
        Cpu cpu = cpuWithProgram();
        cpu.setInterruptsEnabled(true);
        setupStack(cpu, 0xeeee);
        memset(cpu, 0xffff, 0x08);
        memset(cpu, 0x0058, 0x3c, 0x3c);
        cpu.interrupt(Interrupt.SERIAL);
        step(cpu, 2);
        assertEquals(0x02, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_joypad_interrupt() {
        Cpu cpu = cpuWithProgram();
        cpu.setInterruptsEnabled(true);
        setupStack(cpu, 0xeeee);
        memset(cpu, 0xffff, 0x10);
        memset(cpu, 0x0060, 0x3c, 0x3c);
        cpu.interrupt(Interrupt.JOYPAD);
        step(cpu, 2);
        assertEquals(0x02, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_halt_ignores_subsequent_operations() {
        Cpu cpu = cpuWithProgram(0x76, 0x3c, 0x3c, 0x3c, 0x3c);
        setupStack(cpu, 0xeeee);
        step(cpu, 5);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_halt_is_aborted_after_vblank_if_vblank_is_enabled() {
        Cpu cpu = cpuWithProgram(0x76, 0x3c, 0x3c, 0x3c, 0x3c);
        setupStack(cpu, 0xeeee);
        memset(cpu, 0xffff, 0x01);
        step(cpu, 10);
        assertEquals(0x00, cpu.read(Byte.Register.A)); // At this point we are still halted
        cpu.interrupt(Interrupt.V_BLANK);
        step(cpu, 5);
        assertEquals(0x04, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_halt_is_aborted_after_timer_if_timer_is_enabled() {
        Cpu cpu = cpuWithProgram(0x76, 0x3c, 0x3c);
        setupStack(cpu, 0xeeee);
        memset(cpu, 0xffff, 0x04);
        step(cpu, 10);
        assertEquals(0x00, cpu.read(Byte.Register.A)); // Should still be halted here
        cpu.interrupt(Interrupt.TIMER);
        step(cpu, 3);
        assertEquals(0x02, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_halt_is_not_aborted_after_vblank_if_vblank_is_not_enabled() {
        Cpu cpu = cpuWithProgram(0x76, 0x3c, 0x3c);
        setupStack(cpu, 0xeeee);
        memset(cpu, 0xffff, 0xfe); // Enable everything except VBLANK
        step(cpu, 10);
        assertEquals(0x00, cpu.read(Byte.Register.A));
    }

    @Test
    public void test_halt_uses_4_cycles() {
        Cpu cpu = cpuWithProgram(0x76);
        cpu.tick();
        assertEquals(4, cpu.getCycles());
    }

    @Test
    public void test_halted_cycles_use_4_cycles() {
        Cpu cpu = cpuWithProgram(0x76);
        cpu.tick();
        int initialCycles = cpu.getCycles();
        step(cpu, 10);
        assertEquals(40, cpu.getCycles() - initialCycles);
    }

    @Test
    public void test_interrupt_is_fired_after_halt_abort_if_master_interrupt_flag_and_vblank_are_enabled() {
        Cpu cpu = cpuWithProgram(0x76, 0x3c, 0x3c);
        cpu.setInterruptsEnabled(true);
        setupStack(cpu, 0xeeee);
        memset(cpu, 0xffff, 0x01);
        memset(cpu, 0x0040, 0x04, 0x04);
        cpu.interrupt(Interrupt.V_BLANK);
        step(cpu, 5);
        assertEquals(0x00, cpu.read(Byte.Register.A));
        assertEquals(0x02, cpu.read(Byte.Register.B));
    }

    private static Cpu cpuWithProgram(int... program) {
        MemoryManagementUnit mmu = MemoryManagementUnit.build();
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

    private static void step(Cpu cpu, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            cpu.tick();
        }
    }

    private static void memset(Cpu cpu, int address, int... data) {
        for (int idx = 0; idx < data.length; idx++) {
            Pointer ptr = Pointer.of(Word.literal(address + idx));
            cpu.writeTo(ptr, Byte.literal(data[idx]));
        }
    }

    private static void setupStack(Cpu cpu, int stackStart, int... data) {
        cpu.set(Word.Register.SP, Word.literal(stackStart));
        for (int dataByte : data) {
            cpu.set(Word.Register.SP, Word.literal(cpu.read(Word.Register.SP) - 1));
            cpu.writeTo(Pointer.of(Word.Register.SP), Byte.literal(dataByte));
        }
    }
}
