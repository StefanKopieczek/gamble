package com.kopieczek.gamble.savefiles;

import java.util.Optional;

public interface SaveFileDb<T> {
    void put(T key, byte[] ramData);
    Optional<byte[]> get(T key);
}
