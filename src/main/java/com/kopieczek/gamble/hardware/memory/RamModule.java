package com.kopieczek.gamble.hardware.memory;

import com.google.common.base.Preconditions;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Arrays;

public class RamModule extends MemoryModule {
    public static final int DEFAULT_SIZE = 0xffff;
    private int[] memory;

    public RamModule() {
        this(DEFAULT_SIZE);
    }

    public RamModule(int size) {
        super(size);
        memory = new int[size];
    }

    @Override
    public int readByte(int address) {
        try {
            return memory[address];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Invalid memory address: " + address, e);
        }
    }

    @Override
    public void setByteDirect(int address, int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Cannot loadPartial negative value to memory: " + value);
        } else if (value > 0xff) {
            throw new IllegalArgumentException("Cannot loadPartial overlarge value " + value + "; must fit in one byte");
        }

        try {
            memory[address] = value;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Invalid memory address: " + address, e);
        }
    }

    public byte[] exportData() {
        byte[] output = new byte[memory.length * 4]; // 4 bytes in an int
        ByteBuffer bb = ByteBuffer.wrap(output).order(ByteOrder.LITTLE_ENDIAN);
        Arrays.stream(memory).forEach(bb::putInt);
        return output;
    }

    public void importData(byte[] data) {
        Preconditions.checkArgument(data.length == getSizeInBytes() * 4);
        IntBuffer intBuf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
        memory = new int[intBuf.remaining()];
        intBuf.get(memory);
    }
}
