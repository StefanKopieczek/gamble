package com.kopieczek.gamble.hardware.memory;

public class DmaProcess {
    private int sourceAddress;
    private int targetAddress;
    private int copyTicksLeft = 160;
    private int restTicksLeft = 2;

    DmaProcess(int sourceAddress, int targetAddress) {
        this.sourceAddress = sourceAddress;
        this.targetAddress = targetAddress;
    }

    void tick(Mmu mmu, int cycles) {
        for (int i = 0; i < cycles; i++) {
            tick(mmu);
        }
    }

    private void tick(Mmu mmu) {
        if (restTicksLeft > 0) {
            restTicksLeft--;
        } else if (copyTicksLeft > 0) {
            mmu.setByte(targetAddress, mmu.readByte(sourceAddress));
            sourceAddress++;
            targetAddress++;
            copyTicksLeft--;
        }
    }

    boolean isFinished() {
        return (copyTicksLeft == 0);
    }
}
