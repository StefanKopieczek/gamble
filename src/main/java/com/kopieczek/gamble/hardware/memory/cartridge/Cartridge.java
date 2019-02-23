package com.kopieczek.gamble.hardware.memory.cartridge;

import com.kopieczek.gamble.hardware.memory.MemoryModule;
import com.kopieczek.gamble.savefiles.SaveFileDb;

public abstract class Cartridge {
    private SaveFileDb<String> saveFileDb;

    public abstract MemoryModule getRom0();

    public abstract MemoryModule getRom1();

    public abstract byte[] exportRamData();

    public abstract void importRamData(byte[] data);

    protected abstract String getSignature();

    protected abstract MemoryModule getRamInternal();

    public void setSaveFileDb(SaveFileDb<String> saveFileDb) {
        this.saveFileDb = saveFileDb;
    }

    public MemoryModule getRam() {
        MemoryModule delegate = getRamInternal();
        return new MemoryModule(delegate.getSizeInBytes()) {
            @Override
            public int readByte(int address) {
                return delegate.readByte(address);
            }

            @Override
            protected void setByteDirect(int address, int value) {
                delegate.setByte(address, value);
                if (saveFileDb != null) {
                    saveFileDb.put(getSignature(), exportRamData());
                }
            }
        };
    }

    public boolean hasSaveData() {
        return saveFileDb.get(getSignature()).isPresent();
    }

    public void loadFromSave() {
        importRamData(saveFileDb.get(getSignature()).get());
    }
}
