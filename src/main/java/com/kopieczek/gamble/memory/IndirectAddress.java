package com.kopieczek.gamble.memory;

import com.kopieczek.gamble.cpu.Cpu;
import com.kopieczek.gamble.cpu.Register;

public class IndirectAddress {
    private Register left;
    private Register right;

    public static IndirectAddress from(Register left, Register right) {
        return new IndirectAddress(left, right);
    }

    public int getAddress(Cpu cpu) {
        return cpu.readByte(left) << 8 + cpu.readByte(right);
    }

    public int getValueAt(Cpu cpu) {
        return cpu.readByte((cpu.readByte(left) << 8) + cpu.readByte(right));
    }

    public void setValueAt(Cpu cpu, int newValue) {
        cpu.setByte((cpu.readByte(left) << 8) + cpu.readByte(right), newValue);
    }

    private IndirectAddress(Register left, Register right) {
        this.left = left;
        this.right = right;
    }
}
