package com.kopieczek.gamble.hardware.cpu.timer;

import com.kopieczek.gamble.hardware.cpu.Interrupt;
import com.kopieczek.gamble.hardware.memory.InterruptLine;
import com.kopieczek.gamble.hardware.memory.TimerRegisters;
import org.junit.Test;

import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

public class TestTimer {
    @Test
    public void test_one_tick_doesnt_increment_div() {
        timerTest((registers, interrupts, timer) -> {
            timer.tick(1);
            assertEquals(0, registers.getTimerDiv());
        });
    }

    @Test
    public void test_255_ticks_dont_increment_div() {
        timerTest((registers, interrupts, timer) -> {
            timer.tick(255);
            assertEquals(0, registers.getTimerDiv());
        });
    }

    @Test
    public void test_255_separate_ticks_dont_increment_div() {
        timerTest((registers, interrupts, timer) -> {
            IntStream.range(0, 255).forEach(i -> timer.tick(1));
            assertEquals(0, registers.getTimerDiv());
        });
    }

    @Test
    public void test_256_ticks_increments_div() {
        timerTest((registers, interrupts, timer) -> {
            timer.tick(256);
            assertEquals(1, registers.getTimerDiv());
        });
    }

    @Test
    public void test_256_separate_ticks_increments_div() {
        timerTest((registers, interrupts, timer) -> {
            IntStream.range(0, 256).forEach(i -> timer.tick(1));
            assertEquals(1, registers.getTimerDiv());
        });
    }

    @Test
    public void test_256_ticks_in_groups_of_two_increments_div() {
        timerTest((registers, interrupts, timer) -> {
            IntStream.range(0, 128).forEach(i -> timer.tick(2));
            assertEquals(1, registers.getTimerDiv());
        });
    }

    @Test
    public void test_100_div_increments_in_groups_of_256_ticks() {
        timerTest((registers, interrupts, timer) -> {
            IntStream.range(0, 100).forEach(i -> timer.tick(256));
            assertEquals(100, registers.getTimerDiv());
        });
    }

    @Test
    public void test_100_div_increments_in_individual_ticks() {
        timerTest((registers, interrupts, timer) -> {
            IntStream.range(0, 100 * 256).forEach(i -> timer.tick(1));
            assertEquals(100, registers.getTimerDiv());
        });
    }

    @Test
    public void test_100_div_increments_in_one_go() {
        timerTest((registers, interrupts, timer) -> {
            timer.tick(100 * 256);
            assertEquals(100, registers.getTimerDiv());
        });
    }

    @Test
    public void test_div_rollover() {
        timerTest((registers, interrupts, timer) -> {
            timer.tick(0xff * 256 + 255);
            assertEquals("Div shouldn't have rolled over yet", 0xff, registers.getTimerDiv());
            timer.tick(1);
            assertEquals("Div should now have rolled over", 0x00, registers.getTimerDiv());
        });
    }

    @FunctionalInterface
    private interface TimerTest {
        void apply(MockTimerRegisters registers, MockInterrupts interrupts, Timer timer);
    }

    private static void timerTest(TimerTest t) {
        MockTimerRegisters registers = new MockTimerRegisters();
        MockInterrupts interrupts = new MockInterrupts();
        Timer timer = new Timer(registers, interrupts);
        t.apply(registers, interrupts, timer);
    }

    private static class MockTimerRegisters implements TimerRegisters {
        private boolean isTimerEnabled;
        private int cyclesPerTimerCounterTick = 1;
        private int timerDiv;
        private int timerCounter;
        private int timerModulus;

        public void setTimerEnabled(boolean isTimerEnabled) {
            this.isTimerEnabled = isTimerEnabled;
        }

        @Override
        public boolean isTimerEnabled() {
            return isTimerEnabled;
        }

        public void setCyclesPerTimerCounterTick(int newValue) {
            cyclesPerTimerCounterTick = newValue;
        }

        @Override
        public int getCyclesPerTimerCounterTick() {
            return cyclesPerTimerCounterTick;
        }

        @Override
        public int getTimerDiv() {
            return timerDiv;
        }

        @Override
        public void setTimerDiv(int newValue) {
            timerDiv = newValue;
        }

        @Override
        public int getTimerCounter() {
            return timerCounter;
        }

        @Override
        public void setTimerCounter(int newValue) {
            timerCounter = newValue;
        }

        @Override
        public void resetTimerCounter() {
            timerCounter = timerModulus;
        }
    };

    private static class MockInterrupts implements InterruptLine {
        private Interrupt lastSet = null;

        @Override
        public void setInterrupt(Interrupt interrupt) {
           lastSet = interrupt;
        }

        @Override
        public boolean checkInterrupt(Interrupt interrupt) {
            return false;
        }

        @Override
        public void resetInterrupt(Interrupt interrupt) {
        }

        @Override
        public int checkInterrupts() {
            return 0;
        }

        public void reset() {
            lastSet = null;
        }

        public Interrupt getLastSet() {
            return lastSet;
        }
    };
}
