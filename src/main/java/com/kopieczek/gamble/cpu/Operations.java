package com.kopieczek.gamble.cpu;

import java.util.logging.Logger;

class Operations {
    private static final Logger log = Logger.getLogger(Operations.class.getCanonicalName());

    static int nop(Cpu cpu) {
        return 4;
    }

    static int copy(Cpu cpu, Byte.Register to, Byte.Register from) {
        cpu.set(to, from);
        return 4;
    }

    static int copy(Cpu cpu, Byte.Register to, Byte.Argument from) {
        cpu.set(to, from);
        return 8;
    }

    static int load(Cpu cpu, Byte.Register to, Pointer from) {
        cpu.set(to, from);
        return (from.address instanceof Word.Argument) ? 16 : 8;
    }

    static int write(Cpu cpu, Pointer to, Byte.Register from) {
        cpu.writeTo(to, from);
        return 8;
    }

    static int write(Cpu cpu, Pointer to, Byte.Argument from) {
        cpu.writeTo(to, from);
        return 12;
    }

    static int write(Cpu cpu, Pointer to, Word.Register from) {
        int fromValue = cpu.read(from);
        Byte fromLsb = Byte.literal(fromValue & 0xff);
        Byte fromMsb = Byte.literal(fromValue >> 8);
        cpu.writeTo(to, fromLsb);
        cpu.writeTo(to.withOffset(1), fromMsb);
        return 20;
    }

    static int increment(Cpu cpu, Byte.Register r) {
        int oldValue = cpu.read(r);
        int newValue = (oldValue + 1) & 0xff;
        cpu.set(r, Byte.literal(newValue));
        cpu.set(Flag.ZERO, (newValue == 0));
        cpu.set(Flag.NIBBLE, shouldSetNibble(oldValue, 1));
        cpu.set(Flag.OPERATION, false);
        return 4;
    }

    static int increment(Cpu cpu, Pointer ptr) {
        int oldValue = cpu.readFrom(ptr);
        int newValue = (oldValue + 1) & 0xff;
        cpu.writeTo(ptr, Byte.literal(newValue));
        cpu.set(Flag.ZERO, (newValue == 0));
        cpu.set(Flag.NIBBLE, shouldSetNibble(oldValue, 1));
        cpu.set(Flag.OPERATION, false);
        return 12;
    }

    static int loadPartial(Cpu cpu, Byte.Register to, Byte.Register fromLsb) {
        Pointer fromPtr = Pointer.literal(0xff00 + cpu.read(fromLsb));
        cpu.set(to, fromPtr);
        return 8;
    }

    static int loadPartial(Cpu cpu, Byte.Register to, Byte.Argument fromLsb) {
        Pointer fromPtr = Pointer.literal(0xff00 + cpu.read(fromLsb));
        cpu.set(to, fromPtr);
        return 12;
    }

    static int writePartial(Cpu cpu, Byte.Register toLsb, Byte.Register from) {
        Pointer toPtr = Pointer.literal(0xff00 + cpu.read(toLsb));
        cpu.writeTo(toPtr, from);
        return 8;
    }

    static int loadDec(Cpu cpu, Byte.Register to, Word.Register from) {
        cpu.set(to, Pointer.of(from));
        decrementWord(from, cpu);
        return 8;
    }

    static int loadInc(Cpu cpu, Byte.Register to, Word.Register from) {
        cpu.set(to, Pointer.of(from));
        incrementWord(from, cpu);
        return 8;
    }

    static int writeDec(Cpu cpu, Word.Register to, Byte from) {
        cpu.writeTo(Pointer.of(to), from);
        decrementWord(to, cpu);
        return 8;
    }

    static int writeInc(Cpu cpu, Word.Register to, Byte from) {
        cpu.writeTo(Pointer.of(to), from);
        incrementWord(to, cpu);
        return 8;
    }

    static int writePartial(Cpu cpu, Byte toLsb, Byte from) {
        Pointer to = Pointer.literal(0xff00 + cpu.read(toLsb));
        cpu.writeTo(to, from);
        return 12;
    }

    static int copy(Cpu cpu, Word.Register to, Word.Argument from) {
        cpu.set(to, from);
        return 12;
    }

    static int copy(Cpu cpu, Word.Register to, Word.Register from) {
        cpu.set(to, from);
        return 8;
    }

    static int copyWithOffset(Cpu cpu, Word.Register to, Word.Register from, Byte offset) {
        int fromValue = cpu.read(from);
        int offsetValue = cpu.read(offset);
        Word newValue = Word.literal((fromValue + offsetValue) & 0xffff);
        cpu.set(to, newValue);

        cpu.set(Flag.CARRY, shouldSetCarry(fromValue, offsetValue));
        cpu.set(Flag.NIBBLE, shouldSetNibble(fromValue, offsetValue));
        cpu.set(Flag.ZERO, false);
        cpu.set(Flag.OPERATION, false);

        return 12;
    }

    public static int push(Cpu cpu, Word.Register from) {
        Pointer stackPointer = Pointer.of(Word.Register.SP);
        decrementWord(Word.Register.SP, cpu);
        cpu.writeTo(stackPointer, from.left);
        decrementWord(Word.Register.SP, cpu);
        cpu.writeTo(stackPointer, from.right);
        return 16;
    }

    public static Integer pop(Cpu cpu, Word.Register to) {
        Pointer stackPointer = Pointer.of(Word.Register.SP);
        cpu.set(to.right, stackPointer);
        incrementWord(Word.Register.SP, cpu);
        cpu.set(to.left, stackPointer);
        incrementWord(Word.Register.SP, cpu);
        return 12;
    }

    private static void decrementWord(Word.Register r, Cpu cpu) {
        int current = cpu.read(r);
        Word next = Word.literal((current - 1) & 0xffff);
        cpu.set(r, next);
    }

    private static void incrementWord(Word.Register r, Cpu cpu) {
        int current = cpu.read(r);
        Word next = Word.literal((current + 1) & 0xffff);
        cpu.set(r, next);
    }

    private static boolean shouldSetCarry(int original, int offset) {
        return (((original & 0xff) + (offset & 0xff)) & 0x100) > 0;
    }

    private static boolean shouldSetNibble(int original, int offset) {
        return (((original & 0x0f) + (offset & 0x0f)) & 0x10) > 0;
    }

    public static Integer add(Cpu cpu, Byte.Register destOperand, Byte.Register otherOperand) {
        int a = cpu.read(destOperand);
        int b = cpu.read(otherOperand);
        do8BitAdd(cpu, destOperand, a, b);
        return 4;
    }

    public static Integer add(Cpu cpu, Byte.Register destOperand, Byte.Argument otherOperand) {
        int a = cpu.read(destOperand);
        int b = cpu.read(otherOperand);
        do8BitAdd(cpu, destOperand, a, b);
        return 8;
    }

    public static Integer add(Cpu cpu, Byte.Register destOperand, Pointer ptrToOtherOperand) {
        int a = cpu.read(destOperand);
        int b = cpu.readFrom(ptrToOtherOperand);
        do8BitAdd(cpu, destOperand, a, b);
        return 8;
    }

    private static void do8BitAdd(Cpu cpu, Byte.Register dest, int a, int b) {
        int sum = (a + b) & 0xff;
        cpu.set(dest, Byte.literal(sum));
        cpu.set(Flag.ZERO, (sum == 0x00));
        cpu.set(Flag.OPERATION, false);
        cpu.set(Flag.NIBBLE, shouldSetNibble(a, b));
        cpu.set(Flag.CARRY, shouldSetCarry(a, b));
    }

    public static Integer addWithCarry(Cpu cpu, Byte.Register destOperand, Byte.Register otherOperand) {
        int a = cpu.read(destOperand);
        int b = cpu.read(otherOperand) + (cpu.isSet(Flag.CARRY) ? 1 : 0);
        do8BitAdd(cpu, destOperand, a, b);
        return 4;
    }

    public static Integer addWithCarry(Cpu cpu, Byte.Register destOperand, Pointer otherOperandPtr) {
        int a = cpu.read(destOperand);
        int b = cpu.readFrom(otherOperandPtr) + (cpu.isSet(Flag.CARRY) ? 1 : 0);
        do8BitAdd(cpu, destOperand, a, b);
        return 8;
    }

    public static Integer addWithCarry(Cpu cpu, Byte.Register destOperand, Byte.Argument arg) {
        int a = cpu.read(destOperand);
        int b = cpu.read(arg) + (cpu.isSet(Flag.CARRY) ? 1 : 0);
        do8BitAdd(cpu, destOperand, a, b);
        return 8;
    }

    public static Integer subtract(Cpu cpu, Byte.Register leftArg, Byte.Register rightArg) {
        int a = cpu.read(leftArg);
        int b = cpu.read(rightArg);
        doSubtract(cpu, leftArg, a, b);
        return 4;
    }

    public static Integer subtract(Cpu cpu, Byte.Register leftArg, Pointer rightArgPtr) {
        int a = cpu.read(leftArg);
        int b = cpu.readFrom(rightArgPtr);
        doSubtract(cpu, leftArg, a, b);
        return 8;
    }

    public static Integer subtract(Cpu cpu, Byte.Register leftArg, Byte.Argument rightArg) {
        int a = cpu.read(leftArg);
        int b = cpu.read(rightArg);
        doSubtract(cpu, leftArg, a, b);
        return 8;
    }

    private static void doSubtract(Cpu cpu, Byte.Register dest, int leftArg, int rightArg) {
        int result = (leftArg - rightArg + 0x0100) % (0x0100);
        cpu.set(dest, Byte.literal(result));
        cpu.set(Flag.OPERATION, true);
        cpu.set(Flag.ZERO, (result == 0x00));
        cpu.set(Flag.CARRY, ((~leftArg) & rightArg) > 0x00);
        cpu.set(Flag.NIBBLE, (rightArg & 0x0f) > (leftArg & 0x0f));
    }

    public static int subtractWithCarry(Cpu cpu, Byte.Register leftArg, Byte.Register rightArg) {
        int a = cpu.read(leftArg);
        int b = cpu.read(rightArg) + (cpu.isSet(Flag.CARRY) ? 1 : 0);
        doSubtract(cpu, leftArg, a, b);
        return 4;
    }

    public static int subtractWithCarry(Cpu cpu, Byte.Register leftArg, Pointer rightArgPtr) {
        int a = cpu.read(leftArg);
        int b = cpu.readFrom(rightArgPtr) + (cpu.isSet(Flag.CARRY) ? 1 : 0);
        doSubtract(cpu, leftArg, a, b);
        return 8;
    }

    public static int subtractWithCarry(Cpu cpu, Byte.Register leftArg, Byte.Argument rightArg) {
        int a = cpu.read(leftArg);
        int b = cpu.read(rightArg) + (cpu.isSet(Flag.CARRY) ? 1 : 0);
        doSubtract(cpu, leftArg, a, b);
        return 8;
    }

    private static void doAnd(Cpu cpu, Byte.Register dest, int arg1, int arg2) {
        int res = arg1 & arg2;
        cpu.set(dest, Byte.literal(res));
        cpu.set(Flag.ZERO, res == 0x00);
        cpu.set(Flag.NIBBLE, true); // For some reason, AND always sets the NIBBLE flag. ¯\_(ツ)_/¯
        cpu.set(Flag.OPERATION, false);
        cpu.set(Flag.CARRY, false);
    }

    public static int and(Cpu cpu, Byte.Register destArg, Byte.Register otherArg) {
        int a = cpu.read(destArg);
        int b = cpu.read(otherArg);
        doAnd(cpu, destArg, a, b);
        return 4;
    }

    public static int and(Cpu cpu, Byte.Register destArg, Pointer otherArgPtr) {
        int a = cpu.read(destArg);
        int b = cpu.readFrom(otherArgPtr);
        doAnd(cpu, destArg, a, b);
        return 8;
    }

    public static int and(Cpu cpu, Byte.Register destArg, Byte.Argument otherArg) {
        int a = cpu.read(destArg);
        int b = cpu.read(otherArg);
        doAnd(cpu, destArg, a, b);
        return 8;
    }

    private static void doOr(Cpu cpu, Byte.Register destArg, int a, int b) {
        int res = a | b;
        cpu.set(destArg, Byte.literal(res));
        cpu.set(Flag.ZERO, res == 0x00);
        cpu.set(Flag.OPERATION, false);
        cpu.set(Flag.NIBBLE, false);
        cpu.set(Flag.CARRY, false);
    }

    public static int or(Cpu cpu, Byte.Register destArg, Byte.Register otherArg) {
        int a = cpu.read(destArg);
        int b = cpu.read(otherArg);
        doOr(cpu, destArg, a, b);
        return 4;
    }

    public static int or(Cpu cpu, Byte.Register destArg, Pointer otherArgPtr) {
        int a = cpu.read(destArg);
        int b = cpu.readFrom(otherArgPtr);
        doOr(cpu, destArg, a, b);
        return 8;
    }

    public static int or(Cpu cpu, Byte.Register destArg, Byte.Argument otherArg) {
        int a = cpu.read(destArg);
        int b = cpu.read(otherArg);
        doOr(cpu, destArg, a, b);
        return 8;
    }

    private static void doXor(Cpu cpu, Byte.Register destArg, int a, int b) {
        int res = a ^ b;
        cpu.set(destArg, Byte.literal(res));
        cpu.set(Flag.ZERO, a == b);
        cpu.set(Flag.CARRY, false);
        cpu.set(Flag.NIBBLE, false);
        cpu.set(Flag.OPERATION, false);
    }

    public static int xor(Cpu cpu, Byte.Register destArg, Byte.Register otherArg) {
        int a = cpu.read(destArg);
        int b = cpu.read(otherArg);
        doXor(cpu, destArg, a, b);
        return 4;
    }

    public static int xor(Cpu cpu, Byte.Register destArg, Pointer otherArgPtr) {
        int a = cpu.read(destArg);
        int b = cpu.readFrom(otherArgPtr);
        doXor(cpu, destArg, a, b);
        return 8;
    }

    public static int xor(Cpu cpu, Byte.Register destArg, Byte.Argument otherArg) {
        int a = cpu.read(destArg);
        int b = cpu.read(otherArg);
        doXor(cpu, destArg, a, b);
        return 8;
    }

    private static void compare(Cpu cpu, int a, int b) {
        cpu.set(Flag.ZERO, a == b);
        cpu.set(Flag.OPERATION, true);
        cpu.set(Flag.CARRY, a < b);
        cpu.set(Flag.NIBBLE, (a & 0xf) < (b & 0xf));
    }

    public static int compare(Cpu cpu, Byte.Register left, Byte.Register right) {
        int leftVal = cpu.read(left);
        int rightVal = cpu.read(right);
        compare(cpu, leftVal, rightVal);
        return 4;
    }

    public static int compare(Cpu cpu, Byte.Register left, Pointer rightPtr) {
        int leftVal = cpu.read(left);
        int rightVal = cpu.readFrom(rightPtr);
        compare(cpu, leftVal, rightVal);
        return 8;
    }

    public static int compare(Cpu cpu, Byte.Register left, Byte.Argument right) {
        int leftVal = cpu.read(left);
        int rightVal = cpu.read(right);
        compare(cpu, leftVal, rightVal);
        return 8;
    }

    private static void do16BitAdd(Cpu cpu, Word.Register dest, int a, int b) {
        int rawResult = a + b;
        int boundedResult = rawResult % 0x10000;
        cpu.set(dest, Word.literal(boundedResult));
        cpu.set(Flag.OPERATION, false);
        cpu.set(Flag.CARRY, (boundedResult < rawResult));
        cpu.set(Flag.NIBBLE, (((a & 0x0fff) + (b & 0x0fff)) & 0x1000) > 0);
    }

    public static int add(Cpu cpu, Word.Register destArg, Word.Register otherArg) {
        int a = cpu.read(destArg);
        int b = cpu.read(otherArg);
        do16BitAdd(cpu, destArg, a, b);
        return 8;
    }

    public static int add(Cpu cpu, Word.Register destArg, Byte.Argument otherArg) {
        int a = cpu.read(destArg);
        int b = cpu.read(otherArg);
        do16BitAdd(cpu, destArg, a, b);
        return 16;
    }

    public static int increment(Cpu cpu, Word.Register register) {
        int oldValue = cpu.read(register);
        int newValue = (oldValue + 1) % 0x010000;
        cpu.set(register, Word.literal(newValue));
        return 8;
    }

    public static int decrement(Cpu cpu, Word.Register register) {
        int oldValue = cpu.read(register);
        int newValue = (oldValue + 0xffff) % 0x010000;
        cpu.set(register, Word.literal(newValue));
        return 8;
    }

    public static int swap(Cpu cpu, Byte.Register register) {
        int oldValue = cpu.read(register);
        cpu.set(register, Byte.literal(doSwap(cpu, oldValue)));
        return 8;
    }

    public static int swap(Cpu cpu, Pointer ptr) {
        int oldValue = cpu.readFrom(ptr);
        cpu.writeTo(ptr, Byte.literal(doSwap(cpu, oldValue)));
        return 16;
    }

    private static int doSwap(Cpu cpu, int oldValue) {
        int newValue = ((oldValue << 4) % 0x0100) + (oldValue >> 4);
        cpu.set(Flag.ZERO, newValue == 0);
        return newValue;
    }

    public static int bcdAdjust(Cpu cpu, Byte.Register register) {
        logInvalidDaaContexts(cpu, register);
        int result = cpu.read(register);
        boolean shouldSetCarry = false;

        if (cpu.isSet(Flag.OPERATION)) {
            if (cpu.isSet(Flag.NIBBLE)) {
                result += 0xfa;
            }
            if (cpu.isSet(Flag.CARRY)) {
                result += 0xa0;
                shouldSetCarry = true;
            }
        } else {
            int rightNibble = (result & 0x0f);
            if ((rightNibble >= 0x0a) || cpu.isSet(Flag.NIBBLE)) {
                result += 0x06;
            }

            // Mask here with 0x1f0 instead of 0xf0 because the lower nibble
            // addition may trigger a full carry - e.g. [DAA 0xfa]. We need
            // to make sure we still adjust the upper nibble in this case.
            int leftNibble = (result & 0x1f0);
            if ((leftNibble >= 0xa0) || cpu.isSet(Flag.CARRY)) {
                result += 0x60;
                shouldSetCarry = true;
            }
        }

        result %= 0x0100;

        cpu.set(register, Byte.literal(result));
        cpu.set(Flag.ZERO, result == 0x00);
        cpu.set(Flag.CARRY, shouldSetCarry);
        return 4;
    }

    private static void logInvalidDaaContexts(Cpu cpu, Byte.Register register) {
        int leftNibble = cpu.read(register) & 0xf0;
        int rightNibble = cpu.read(register) & 0x0f;
        if (cpu.isSet(Flag.OPERATION)) {
            if (cpu.isSet(Flag.NIBBLE) && rightNibble < 0x06) {
                log.warning("Invalid context during DAA operation at position " +
                        cpu.getProgramCounter() +
                        ": right nibble is <0x06 (0x" + Integer.toHexString(rightNibble) +
                        "), but operation and nibble are set");
            } else if (cpu.isSet(Flag.CARRY)) {
                int leftLimit = 0x70 - (cpu.isSet(Flag.NIBBLE) ? 0x10 : 0x00);
                if (leftNibble < leftLimit) {
                    log.warning("Invalid context during DAA operation at position " +
                        cpu.getProgramCounter() +
                        ": left nibble is <0x" + Integer.toHexString(leftLimit) +
                        " (0x" + Integer.toHexString(leftNibble) +
                        "), but operation is set and nibble=" + cpu.isSet(Flag.NIBBLE));
                }
            }
        } else {
            int leftLimit = 0x20 + (cpu.isSet(Flag.NIBBLE) ? 0x10 : 0x00);
            if ((leftNibble > leftLimit) && cpu.isSet(Flag.CARRY)) {
                log.warning("Invalid context during DAA operation at position " +
                        cpu.getProgramCounter() +
                        ":　left nibble is >0x" + Integer.toHexString(leftLimit) +
                        " (0x" + Integer.toHexString(leftNibble) +
                        "), but carry is set and nibble=" + cpu.isSet(Flag.NIBBLE));
            }

            if ((rightNibble > 0x03) && cpu.isSet(Flag.NIBBLE)) {
                log.warning("Invalid context during DAA operation at position " +
                        cpu.getProgramCounter() +
                        ":　right nibble is >0x03 (0x" + Integer.toHexString(rightNibble) +
                        "), but nibble flag is set");
            }
        }
    }

    public static int complement(Cpu cpu, Byte.Register register) {
        int newValue = 0xff & ~cpu.read(Byte.Register.A);
        cpu.set(register, Byte.literal(newValue));
        cpu.set(Flag.OPERATION, true);
        cpu.set(Flag.NIBBLE, true);
        return 4;
    }

    public static int complementCarryFlag(Cpu cpu) {
        cpu.set(Flag.CARRY, !cpu.isSet(Flag.CARRY));
        cpu.set(Flag.NIBBLE, false);
        cpu.set(Flag.OPERATION, false);
        return 4;
    }

    public static int setCarryFlag(Cpu cpu) {
        cpu.set(Flag.CARRY, true);
        cpu.set(Flag.NIBBLE, false);
        cpu.set(Flag.OPERATION, false);
        return 4;
    }

    public static int halt(Cpu cpu) {
        throw new UnsupportedOperationException("CPU halt not yet implemented");
        // return 4;
    }

    public static int stop(Cpu cpu, Byte.Argument nextByte) {
        final int nextByteVal = nextByte.getValue(cpu);
        if (nextByteVal == 0x00) {
            throw new UnsupportedOperationException("CPU stop not yet implemented");
        } else {
            throw new IllegalArgumentException("Unexpected opcode 0x" +
                    Integer.toHexString(nextByteVal) +
                    " following 0x10");
        }
        // return 4;
    }

    public static int disableInterrupts(Cpu cpu) {
        throw new UnsupportedOperationException("Interrupt management not yet implemented");
        // return 4
    }

    public static int enableInterrupts(Cpu cpu) {
        throw new UnsupportedOperationException("Interrupt management not yet implemented");
        // return 4
    }

    public static int rotateLeftOntoCarry(Cpu cpu, Byte.Register r) {
        int newValue = cpu.read(r) << 1;
        int bit7 = (0x0100 & newValue) >> 8;
        newValue = (0xff & newValue) + bit7;
        cpu.set(r, Byte.literal(newValue));
        cpu.set(Flag.CARRY, (bit7 > 0));
        cpu.set(Flag.ZERO, false);
        cpu.set(Flag.NIBBLE, false);
        cpu.set(Flag.OPERATION, false);
        return 8;
    }

    public static int rotateLeftOntoCarry(Cpu cpu, Pointer p) {
        int newValue = cpu.readFrom(p) << 1;
        int bit7 = (0x0100 & newValue) >> 8;
        newValue = (0xff & newValue) + bit7;
        cpu.writeTo(p, Byte.literal(newValue));
        cpu.set(Flag.CARRY, (bit7 > 0));
        cpu.set(Flag.ZERO, false);
        cpu.set(Flag.NIBBLE, false);
        cpu.set(Flag.OPERATION, false);
        return 16;
    }

    public static int rotateLeftThroughCarry(Cpu cpu, Byte.Register r) {
        final int oldValue = r.getValue(cpu);
        final int bit7 = (0x80 & oldValue) >> 7;
        final int carryAdjustment = (cpu.isSet(Flag.CARRY) ? 1 : 0);
        final int newValue = (0xff & (oldValue << 1)) + carryAdjustment;
        cpu.set(r, Byte.literal(newValue));
        cpu.set(Flag.CARRY, bit7 > 0);
        cpu.set(Flag.ZERO, false);
        cpu.set(Flag.NIBBLE, false);
        cpu.set(Flag.OPERATION, false);
        return 8;
    }

    public static int rotateLeftThroughCarry(Cpu cpu, Pointer p) {
        final int oldValue = cpu.readFrom(p);
        final int bit7 = (0x80 & oldValue) >> 7;
        final int carryAdjustment = (cpu.isSet(Flag.CARRY) ? 1 : 0);
        final int newValue = (0xff & (oldValue << 1)) + carryAdjustment;
        cpu.writeTo(p, Byte.literal(newValue));
        cpu.set(Flag.CARRY, bit7 > 0);
        cpu.set(Flag.ZERO, false);
        cpu.set(Flag.NIBBLE, false);
        cpu.set(Flag.OPERATION, false);
        return 16;
    }

    public static int rotateRightOntoCarry(Cpu cpu, Byte.Register r) {
        final int oldValue = cpu.read(r);
        final int bit0 = oldValue & 0x01;
        final int newValue = (bit0 << 7) + (oldValue >> 1);
        cpu.set(r, Byte.literal(newValue));
        cpu.set(Flag.CARRY, bit0 > 0);
        cpu.set(Flag.ZERO, false);
        cpu.set(Flag.NIBBLE, false);
        cpu.set(Flag.OPERATION, false);
        return 8;
    }

    public static int rotateRightOntoCarry(Cpu cpu, Pointer p) {
        final int oldValue = cpu.readFrom(p);
        final int bit0 = oldValue & 0x01;
        final int newValue = (bit0 << 7) + (oldValue >> 1);
        cpu.writeTo(p, Byte.literal(newValue));
        cpu.set(Flag.CARRY, bit0 > 0);
        cpu.set(Flag.ZERO, false);
        cpu.set(Flag.NIBBLE, false);
        cpu.set(Flag.OPERATION, false);
        return 16;
    }

    public static int rotateRightThroughCarry(Cpu cpu, Byte.Register r) {
        final int oldValue = cpu.read(r);
        final int bit0 = oldValue & 0x01;
        final int carryAdjustment = (cpu.isSet(Flag.CARRY)) ? 1 : 0;
        final int newValue = (carryAdjustment << 7) + (oldValue >> 1);
        cpu.set(r, Byte.literal(newValue));
        cpu.set(Flag.CARRY, bit0 > 0);
        cpu.set(Flag.ZERO, false);
        cpu.set(Flag.NIBBLE, false);
        cpu.set(Flag.OPERATION, false);
        return 8;
    }

    public static int rotateRightThroughCarry(Cpu cpu, Pointer p) {
        final int oldValue = cpu.readFrom(p);
        final int bit0 = oldValue & 0x01;
        final int carryAdjustment = (cpu.isSet(Flag.CARRY)) ? 1 : 0;
        final int newValue = (carryAdjustment << 7) + (oldValue >> 1);
        cpu.writeTo(p, Byte.literal(newValue));
        cpu.set(Flag.CARRY, bit0 > 0);
        cpu.set(Flag.ZERO, false);
        cpu.set(Flag.NIBBLE, false);
        cpu.set(Flag.OPERATION, false);
        return 16;
    }

    public static int rotateALeftOntoCarry(Cpu cpu) {
        rotateLeftOntoCarry(cpu, Byte.Register.A);
        return 4; // RLCA is 4 cycles even though RLC A is 8.
    }

    public static int rotateALeftThroughCarry(Cpu cpu) {
        rotateLeftThroughCarry(cpu, Byte.Register.A);
        return 4; // RLA is 4 cycles even though RL A is 8.
    }

    public static int rotateARightOntoCarry(Cpu cpu) {
        rotateRightOntoCarry(cpu, Byte.Register.A);
        return 4; // RRCA is 4 cycles even though RRC A is 8.
    }

    public static int rotateARightThroughCarry(Cpu cpu) {
        rotateRightThroughCarry(cpu, Byte.Register.A);
        return 4; // RRA is 4 cycles even though RR A is 8.
    }
}
