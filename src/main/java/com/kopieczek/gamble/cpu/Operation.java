package com.kopieczek.gamble.cpu;

import java.util.function.Function;

public interface Operation extends Function<Cpu, Integer> {
    Integer apply(Cpu cpu);
}