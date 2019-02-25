package com.kopieczek.gamble.savefiles;

import java.util.Optional;

public interface SaveFileDb<T> {
    void put(T key, int[] ramData);
    Optional<int[]> get(T key);
}
