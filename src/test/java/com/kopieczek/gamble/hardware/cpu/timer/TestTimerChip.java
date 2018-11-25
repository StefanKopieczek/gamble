package com.kopieczek.gamble.hardware.cpu.timer;

import com.kopieczek.gamble.hardware.cpu.Interrupt;
import com.kopieczek.gamble.hardware.memory.InterruptLine;
import com.kopieczek.gamble.hardware.memory.TimerRegisters;
import org.junit.Test;

import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TestTimerChip {
    @Test
    public void test_div_is_initially_zero() {
        timerTest((registers, interrupts, timer) -> {
            assertEquals(0, registers.getTimerDiv());
        });
    }
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

    @Test
    public void test_div_rollover_mid_tick() {
        timerTest((registers, interrupts, timer) -> {
            timer.tick(0x123456);
            assertEquals(0x34, registers.getTimerDiv());
        });
    }

    @Test
    public void test_counter_is_initially_zero() {
        timerTest((registers, interrupts, timer) -> {
            assertEquals(0, registers.getTimerCounter());
        });
    }

    @Test
    public void test_counter_increments_when_cycles_per_counter_is_one() {
        timerTest((registers, interrupts, timer) -> {
            registers.setCyclesPerTimerCounterTick(1);
            assertEquals(0, registers.getTimerCounter());
            timer.tick(1);
            assertEquals(1, registers.getTimerCounter());
            timer.tick(1);
            assertEquals(2, registers.getTimerCounter());
        });
    }

    @Test
    public void test_counter_does_not_increment_if_clock_is_disabled() {
        timerTest((registers, interrupts, timer) -> {
            registers.setCyclesPerTimerCounterTick(1);
            registers.setTimerEnabled(false);
            timer.tick(10);
            assertEquals(0, registers.getTimerCounter());
        });
    }

    @Test
    public void test_counter_increment_on_multi_tick_when_cycles_per_counter_is_one() {
        timerTest((registers, interrupts, timer) -> {
            registers.setCyclesPerTimerCounterTick(1);
            timer.tick(7);
            assertEquals(7, registers.getTimerCounter());
        });
    };

    @Test
    public void test_counter_increment_when_cycles_per_counter_is_100() {
        timerTest((registers, interrupts, timer) -> {
            registers.setCyclesPerTimerCounterTick(100);
            assertEquals(0, registers.getTimerCounter());
            IntStream.range(0, 99).forEach(i -> {
                timer.tick(1);
                assertEquals("TIMA should be 0 after " + i + " ticks", 0, registers.getTimerCounter());
            });
            timer.tick(1);
            assertEquals(1, registers.getTimerCounter());
        });
    }

    @Test
    public void test_counter_increment_on_multi_tick_when_cycles_per_counter_is_100() {
        timerTest((registers, interrupts, timer) -> {
            registers.setCyclesPerTimerCounterTick(100);
            timer.tick(100);
            assertEquals(1, registers.getTimerCounter());
            timer.tick(100);
            assertEquals(2, registers.getTimerCounter());
            timer.tick(100);
            assertEquals(3, registers.getTimerCounter());
        });
    }

    @Test
    public void test_change_of_cycles_per_counter_immediately_after_increment() {
        timerTest((registers, interrupts, timer) -> {
            registers.setCyclesPerTimerCounterTick(2);
            timer.tick(8);
            assertEquals(4, registers.getTimerCounter());
            registers.setCyclesPerTimerCounterTick(5);
            timer.tick(10);
            assertEquals(6, registers.getTimerCounter());
        });
    }

    @Test
    public void test_change_of_cycles_just_before_increment() {
        timerTest(((registers, interrupts, timer) -> {
            registers.setCyclesPerTimerCounterTick(3);
            timer.tick(11);
            assertEquals(3, registers.getTimerCounter());
            registers.setCyclesPerTimerCounterTick(5);
            timer.tick(3);
            assertEquals(3, registers.getTimerCounter());
            timer.tick(1);
            assertEquals(4, registers.getTimerCounter());
        }));
    }

    @Test
    public void test_counter_rollover() {
        timerTest((registers, interrupts, timer) -> {
            registers.setTimerModulus(0x77);
            registers.setCyclesPerTimerCounterTick(3);
            timer.tick(3 * 0xff + 2);
            assertEquals(0xff, registers.getTimerCounter());
            timer.tick(1);
            assertEquals(0x77, registers.getTimerCounter());
        });
    }

    @Test
    public void test_counter_rollover_mid_tick() {
        timerTest((registers, interrupts, timer) -> {
            registers.setTimerModulus(0x00);
            registers.setCyclesPerTimerCounterTick(256);
            timer.tick(0x654321);
            assertEquals(0x43, registers.getTimerCounter());
        });
    }

    @Test
    public void test_exception_fired_on_counter_rollover() {
        timerTest((registers, interrupts, timer) -> {
            registers.setCyclesPerTimerCounterTick(1);
            timer.tick(0xff);
            assertNull(interrupts.getLastSet());
            timer.tick(1);
            assertEquals(Interrupt.TIMER, interrupts.getLastSet());
        });
    }

    @Test
    public void test_interrupt_is_fired_on_successive_rollovers() {
        timerTest((registers, interrupts, timer) -> {
            registers.setCyclesPerTimerCounterTick(1);
            timer.tick(0x1ff);
            interrupts.reset();
            assertNull("Check test harness correctly reset interrupt", interrupts.getLastSet());
            timer.tick(1);
            assertEquals(Interrupt.TIMER, interrupts.getLastSet());
        });
    }

    @Test
    public void test_interrupt_is_fired_on_rollover_mid_tick() {
        timerTest((registers, interrupts, timer) -> {
            registers.setCyclesPerTimerCounterTick(1);
            timer.tick(0x1bc);
            assertEquals(Interrupt.TIMER, interrupts.getLastSet());
        });
    }

    @Test
    public void test_interrupt_not_fired_on_div_rollover() {
        timerTest((registers, interrupts, timer) -> {
            registers.setCyclesPerTimerCounterTick(0x10000000);
            timer.tick(0x10000);
            assertEquals("DIV should have rolled over", 0, registers.getTimerDiv());
            assertNull("Interrupt should not have fired", interrupts.getLastSet());
        });
    }

    @Test
    public void test_div_always_rolls_over_to_zero() {
        timerTest((registers, interrupts, timer) -> {
            registers.setTimerModulus(0x12);
            timer.tick(0x10000);
            assertEquals(0x00, registers.getTimerDiv());
        });
    }

    @FunctionalInterface
    private interface TimerTest {
        void apply(MockTimerRegisters registers, MockInterrupts interrupts, TimerChip timer);
    }

    private static void timerTest(TimerTest t) {
        MockTimerRegisters registers = new MockTimerRegisters();
        MockInterrupts interrupts = new MockInterrupts();
        TimerChip timer = new TimerChip(registers, interrupts);
        t.apply(registers, interrupts, timer);
    }

    private static class MockTimerRegisters implements TimerRegisters {
        private boolean isTimerEnabled = true;
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

        public void setTimerModulus(int newValue) {
            timerModulus = newValue;
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
