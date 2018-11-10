package com.kopieczek.gamble.hardware.memory;

public class FastBiosModule extends RomModule {
    FastBiosModule() {
        super(getCode());
    }

    private static int[] getCode() {
        int[] bios = new int[256];
        bios[0x00] = 0x31; // LD SP, 0xfffe
        bios[0x01] = 0xfe;
        bios[0x02] = 0xff;
        bios[0x03] = 0x3e; // LD A, 0x01
        bios[0x04] = 0x01;
        bios[0x05] = 0xc3; // JMP 0x00fd
        bios[0x06] = 0xfd;
        bios[0x07] = 0x00;
        bios[0xfd] = 0xea; // LD 0xff50, A (Disable BIOS)
        bios[0xfe] = 0x50;
        bios[0xff] = 0xff;
        return bios;
    }
}
