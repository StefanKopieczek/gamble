package com.kopieczek.gamble.hardware.memory;

public interface GraphicsAccessController {
    void setVramAccessible(boolean isAccessible);
    void setOamAccessible(boolean isAccessible);
}
