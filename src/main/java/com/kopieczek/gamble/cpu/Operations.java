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

    static int push(Cpu cpu, Word.Register from) {
        Pointer stackPointer = Pointer.of(Word.Register.SP);
        decrementWord(Word.Register.SP, cpu);
        cpu.writeTo(stackPointer, from.left);
        decrementWord(Word.Register.SP, cpu);
        cpu.writeTo(stackPointer, from.right);
        return 16;
    }

    static Integer pop(Cpu cpu, Word.Register to) {
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

    static Integer add(Cpu cpu, Byte.Register destOperand, Byte.Register otherOperand) {
        int a = cpu.read(destOperand);
        int b = cpu.read(otherOperand);
        do8BitAdd(cpu, destOperand, a, b);
        return 4;
    }

    static Integer add(Cpu cpu, Byte.Register destOperand, Byte.Argument otherOperand) {
        int a = cpu.read(destOperand);
        int b = cpu.read(otherOperand);
        do8BitAdd(cpu, destOperand, a, b);
        return 8;
    }

    static Integer add(Cpu cpu, Byte.Register destOperand, Pointer ptrToOtherOperand) {
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

    static Integer addWithCarry(Cpu cpu, Byte.Register destOperand, Byte.Register otherOperand) {
        int a = cpu.read(destOperand);
        int b = cpu.read(otherOperand) + (cpu.isSet(Flag.CARRY) ? 1 : 0);
        do8BitAdd(cpu, destOperand, a, b);
        return 4;
    }

    static Integer addWithCarry(Cpu cpu, Byte.Register destOperand, Pointer otherOperandPtr) {
        int a = cpu.read(destOperand);
        int b = cpu.readFrom(otherOperandPtr) + (cpu.isSet(Flag.CARRY) ? 1 : 0);
        do8BitAdd(cpu, destOperand, a, b);
        return 8;
    }

    static Integer addWithCarry(Cpu cpu, Byte.Register destOperand, Byte.Argument arg) {
        int a = cpu.read(destOperand);
        int b = cpu.read(arg) + (cpu.isSet(Flag.CARRY) ? 1 : 0);
        do8BitAdd(cpu, destOperand, a, b);
        return 8;
    }

    static Integer subtract(Cpu cpu, Byte.Register leftArg, Byte.Register rightArg) {
        int a = cpu.read(leftArg);
        int b = cpu.read(rightArg);
        doSubtract(cpu, leftArg, a, b);
        return 4;
    }

    static Integer subtract(Cpu cpu, Byte.Register leftArg, Pointer rightArgPtr) {
        int a = cpu.read(leftArg);
        int b = cpu.readFrom(rightArgPtr);
        doSubtract(cpu, leftArg, a, b);
        return 8;
    }

    static Integer subtract(Cpu cpu, Byte.Register leftArg, Byte.Argument rightArg) {
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

    static int subtractWithCarry(Cpu cpu, Byte.Register leftArg, Byte.Register rightArg) {
        int a = cpu.read(leftArg);
        int b = cpu.read(rightArg) + (cpu.isSet(Flag.CARRY) ? 1 : 0);
        doSubtract(cpu, leftArg, a, b);
        return 4;
    }

    static int subtractWithCarry(Cpu cpu, Byte.Register leftArg, Pointer rightArgPtr) {
        int a = cpu.read(leftArg);
        int b = cpu.readFrom(rightArgPtr) + (cpu.isSet(Flag.CARRY) ? 1 : 0);
        doSubtract(cpu, leftArg, a, b);
        return 8;
    }

    static int subtractWithCarry(Cpu cpu, Byte.Register leftArg, Byte.Argument rightArg) {
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

    static int and(Cpu cpu, Byte.Register destArg, Byte.Register otherArg) {
        int a = cpu.read(destArg);
        int b = cpu.read(otherArg);
        doAnd(cpu, destArg, a, b);
        return 4;
    }

    static int and(Cpu cpu, Byte.Register destArg, Pointer otherArgPtr) {
        int a = cpu.read(destArg);
        int b = cpu.readFrom(otherArgPtr);
        doAnd(cpu, destArg, a, b);
        return 8;
    }

    static int and(Cpu cpu, Byte.Register destArg, Byte.Argument otherArg) {
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

    static int or(Cpu cpu, Byte.Register destArg, Byte.Register otherArg) {
        int a = cpu.read(destArg);
        int b = cpu.read(otherArg);
        doOr(cpu, destArg, a, b);
        return 4;
    }

    static int or(Cpu cpu, Byte.Register destArg, Pointer otherArgPtr) {
        int a = cpu.read(destArg);
        int b = cpu.readFrom(otherArgPtr);
        doOr(cpu, destArg, a, b);
        return 8;
    }

    static int or(Cpu cpu, Byte.Register destArg, Byte.Argument otherArg) {
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

    static int xor(Cpu cpu, Byte.Register destArg, Byte.Register otherArg) {
        int a = cpu.read(destArg);
        int b = cpu.read(otherArg);
        doXor(cpu, destArg, a, b);
        return 4;
    }

    static int xor(Cpu cpu, Byte.Register destArg, Pointer otherArgPtr) {
        int a = cpu.read(destArg);
        int b = cpu.readFrom(otherArgPtr);
        doXor(cpu, destArg, a, b);
        return 8;
    }

    static int xor(Cpu cpu, Byte.Register destArg, Byte.Argument otherArg) {
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

    static int compare(Cpu cpu, Byte.Register left, Byte.Register right) {
        int leftVal = cpu.read(left);
        int rightVal = cpu.read(right);
        compare(cpu, leftVal, rightVal);
        return 4;
    }

    static int compare(Cpu cpu, Byte.Register left, Pointer rightPtr) {
        int leftVal = cpu.read(left);
        int rightVal = cpu.readFrom(rightPtr);
        compare(cpu, leftVal, rightVal);
        return 8;
    }

    static int compare(Cpu cpu, Byte.Register left, Byte.Argument right) {
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

    static int add(Cpu cpu, Word.Register destArg, Word.Register otherArg) {
        int a = cpu.read(destArg);
        int b = cpu.read(otherArg);
        do16BitAdd(cpu, destArg, a, b);
        return 8;
    }

    static int add(Cpu cpu, Word.Register destArg, Byte.Argument otherArg) {
        int a = cpu.read(destArg);
        int b = cpu.read(otherArg);
        do16BitAdd(cpu, destArg, a, b);
        return 16;
    }

    static int increment(Cpu cpu, Word.Register register) {
        int oldValue = cpu.read(register);
        int newValue = (oldValue + 1) % 0x010000;
        cpu.set(register, Word.literal(newValue));
        return 8;
    }

    static int decrement(Cpu cpu, Word.Register register) {
        int oldValue = cpu.read(register);
        int newValue = (oldValue + 0xffff) % 0x010000;
        cpu.set(register, Word.literal(newValue));
        return 8;
    }

    static int swap(Cpu cpu, Byte.Register register) {
        int oldValue = cpu.read(register);
        cpu.set(register, Byte.literal(doSwap(cpu, oldValue)));
        return 8;
    }

    static int swap(Cpu cpu, Pointer ptr) {
        int oldValue = cpu.readFrom(ptr);
        cpu.writeTo(ptr, Byte.literal(doSwap(cpu, oldValue)));
        return 16;
    }

    private static int doSwap(Cpu cpu, int oldValue) {
        int newValue = ((oldValue << 4) % 0x0100) + (oldValue >> 4);
        cpu.set(Flag.ZERO, newValue == 0);
        return newValue;
    }

    static int bcdAdjust(Cpu cpu, Byte.Register register) {
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

    static int complement(Cpu cpu, Byte.Register register) {
        int newValue = 0xff & ~cpu.read(Byte.Register.A);
        cpu.set(register, Byte.literal(newValue));
        cpu.set(Flag.OPERATION, true);
        cpu.set(Flag.NIBBLE, true);
        return 4;
    }

    static int complementCarryFlag(Cpu cpu) {
        cpu.set(Flag.CARRY, !cpu.isSet(Flag.CARRY));
        cpu.set(Flag.NIBBLE, false);
        cpu.set(Flag.OPERATION, false);
        return 4;
    }

    static int setCarryFlag(Cpu cpu) {
        cpu.set(Flag.CARRY, true);
        cpu.set(Flag.NIBBLE, false);
        cpu.set(Flag.OPERATION, false);
        return 4;
    }

    static int halt(Cpu cpu) {
        throw new UnsupportedOperationException("CPU halt not yet implemented");
        // return 4;
    }

    static int stop(Cpu cpu, Byte.Argument nextByte) {
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

    static int disableInterrupts(Cpu cpu) {
        throw new UnsupportedOperationException("Interrupt management not yet implemented");
        // return 4
    }

    static int enableInterrupts(Cpu cpu) {
        throw new UnsupportedOperationException("Interrupt management not yet implemented");
        // return 4
    }

    private static int rotateLeft(Cpu cpu, int oldValue, RotateMode rotateMode) {
        final int oldBit7 = (0x80 & oldValue) >> 7;
        int newBit0;
        if (rotateMode == RotateMode.COPY_TO_CARRY) {
            newBit0 = oldBit7;
        } else if (rotateMode == RotateMode.INCLUDE_CARRY) {
            newBit0 = cpu.isSet(Flag.CARRY) ? 1 : 0;
        } else {
            throw new IllegalArgumentException("Unsupported rotate mode " + rotateMode);
        }

        final int newValue = ((oldValue << 1) & 0xff) + newBit0;
        cpu.set(Flag.CARRY, oldBit7 > 0);
        cpu.set(Flag.ZERO, newValue == 0x00);
        cpu.set(Flag.NIBBLE, false) ;
        cpu.set(Flag.OPERATION, false) ;
        return newValue;
    }

    private static int rotateRight(Cpu cpu, int oldValue, RotateMode rotateMode) {
        final int oldBit0 = 0x01 & oldValue;
        int newBit7;
        if (rotateMode == RotateMode.COPY_TO_CARRY) {
            newBit7 = oldBit0;
        } else if (rotateMode == RotateMode.INCLUDE_CARRY) {
            newBit7 = cpu.isSet(Flag.CARRY) ? 1 : 0;
        } else {
            throw new IllegalArgumentException("Unsupported rotate mode " + rotateMode);
        }

        final int newValue = (newBit7 << 7) + (oldValue >> 1);
        cpu.set(Flag.CARRY, oldBit0 > 0);
        cpu.set(Flag.ZERO, newValue == 0x00);
        cpu.set(Flag.NIBBLE, false);
        cpu.set(Flag.OPERATION, false);
        return newValue;
    }

    static int rotateLeft(Cpu cpu, Byte.Register r, RotateMode mode) {
        int newValue = rotateLeft(cpu, cpu.read(r), mode);
        cpu.set(r, Byte.literal(newValue));
        return 8;
    }

    static int rotateLeft(Cpu cpu, Pointer p, RotateMode mode) {
        int newValue = rotateLeft(cpu, cpu.readFrom(p), mode);
        cpu.writeTo(p, Byte.literal(newValue));
        return 16;
    }

    static int rotateRight(Cpu cpu, Byte.Register r, RotateMode mode) {
        int newValue = rotateRight(cpu, cpu.read(r), mode);
        cpu.set(r, Byte.literal(newValue));
        return 8;
    }

    static int rotateRight(Cpu cpu, Pointer p, RotateMode mode) {
        int newValue = rotateRight(cpu, cpu.readFrom(p), mode);
        cpu.writeTo(p, Byte.literal(newValue));
        return 16;
    }

    static int rotateALeft(Cpu cpu, RotateMode mode) {
        rotateLeft(cpu, Byte.Register.A, mode);
        cpu.set(Flag.ZERO, false); // Unlike RL/RLC, RLA/RLCA always reset ZERO
        return 4; // RLCA and RLA are 4 cycles even though RLC A and RL A are 8
    }

    static int rotateARight(Cpu cpu, RotateMode mode) {
        rotateRight(cpu, Byte.Register.A, mode);
        cpu.set(Flag.ZERO, false); // Unlike RR/RRC, RRA/RRCA always reset ZERO
        return 4; // RRCA is 4 cycles even though RRC A is 8
    }

    static int leftShift(Cpu cpu, Byte.Register r) {
        final int oldValue = cpu.read(r);
        final int newValue = (oldValue << 1) & 0xff;
        cpu.set(r, Byte.literal(newValue));
        cpu.set(Flag.ZERO, newValue == 0x00);
        cpu.set(Flag.CARRY, (oldValue & 0x80) > 0);
        cpu.set(Flag.NIBBLE, false);
        cpu.set(Flag.OPERATION, false);
        return 8;
    }

    static int leftShift(Cpu cpu, Pointer p) {
        final int oldValue = cpu.readFrom(p);
        final int newValue = (oldValue << 1) & 0xff;
        cpu.writeTo(p, Byte.literal(newValue));
        cpu.set(Flag.ZERO, newValue == 0x00);
        cpu.set(Flag.CARRY, (oldValue & 0x80) > 0);
        cpu.set(Flag.NIBBLE, false);
        cpu.set(Flag.OPERATION, false);
        return 16;
    }

    private static int rightShift(Cpu cpu, int oldValue, ShiftMode mode) {
        final int oldBit0 = oldValue & 0x01;
        final int newBit7 = mode.equals(ShiftMode.ARITHMETIC) ? (oldValue & 0x80) : 0;
        final int newValue = (newBit7) + (oldValue >> 1);
        cpu.set(Flag.CARRY, oldBit0 > 0);
        cpu.set(Flag.ZERO, newValue == 0x00);
        cpu.set(Flag.NIBBLE, false);
        cpu.set(Flag.OPERATION, false);
        return newValue;
    }

    static int rightShift(Cpu cpu, Byte.Register r, ShiftMode mode) {
        final int newValue = rightShift(cpu, cpu.read(r), mode);
        cpu.set(r, Byte.literal(newValue));
        return 8;
    }

    static int rightShift(Cpu cpu, Pointer p, ShiftMode mode) {
        final int newValue = rightShift(cpu, cpu.readFrom(p), mode);
        cpu.writeTo(p, Byte.literal(newValue));
        return 16;
    }

    private static void bitTest(Cpu cpu, int bitIndex, int value) {
        if (bitIndex > 7) {
            throw new IllegalArgumentException("Cannot test bit at index > 7: " + bitIndex);
        }
        final int bit = (0x01 << bitIndex & value);
        cpu.set(Flag.ZERO, bit == 0);
        cpu.set(Flag.NIBBLE, false);
        cpu.set(Flag.OPERATION, true);
    }

    static int bitTest(Cpu cpu, Byte bitIndex, Byte.Register r) {
        bitTest(cpu, cpu.read(bitIndex), cpu.read(r));
        return 8;
    }

    static int bitTest(Cpu cpu, Byte bitIndex, Pointer p) {
        bitTest(cpu, cpu.read(bitIndex), cpu.readFrom(p));
        return 12;
    }

    private static int bitSet(int oldValue, int bitIndex) {
        if (bitIndex > 7) {
            throw new IllegalArgumentException("Cannot set bit at index > 7: " + bitIndex);
        }
        final int bitToSet = 1 << bitIndex;
        final int newValue = oldValue | bitToSet;
        return newValue;
    }

    static int bitSet(Cpu cpu, Byte.Argument bitIndex, Byte.Register r) {
        final int newValue = bitSet(cpu.read(r), cpu.read(bitIndex));
        cpu.set(r, Byte.literal(newValue));
        return 8;
    }

    static int bitSet(Cpu cpu, Byte.Argument bitIndex, Pointer p) {
        final int newValue = bitSet(cpu.readFrom(p), cpu.read(bitIndex));
        cpu.writeTo(p, Byte.literal(newValue));
        return 16;
    }

    private static int bitReset(int oldValue, int bitIndex) {
        if (bitIndex > 7) {
            throw new IllegalArgumentException("Cannot reset bit at index > 7: " + bitIndex);
        }
        final int bitToReset = 1 << bitIndex;
        return oldValue & ~bitToReset;
    }

    static int bitReset(Cpu cpu, Byte.Argument bitIndex, Byte.Register r) {
        final int newValue = bitReset(cpu.read(r), cpu.read(bitIndex));
        cpu.set(r, Byte.literal(newValue));
        return 8;
    }

    static int bitReset(Cpu cpu, Byte.Argument bitIndex, Pointer p) {
        final int newValue = bitReset(cpu.readFrom(p), cpu.read(bitIndex));
        cpu.writeTo(p, Byte.literal(newValue));
        return 16;
    }

    private static void doJump(Cpu cpu, int address) {
        cpu.pc = address - 1;  //  -1 because the PC still needs to tick forward for this instruction.
    }

    static int jump(Cpu cpu, Word.Argument address) {
        doJump(cpu, cpu.read(address));
        return 16;
    }

    static int jump(Cpu cpu, Word.Register r) {
        doJump(cpu, cpu.read(r));
        return 4;
    }

    static int jumpIfNotSet(Cpu cpu, Word.Argument address, Flag flag) {
        final int targetAddress = cpu.read(address);
        if (!cpu.isSet(flag)) {
            doJump(cpu, targetAddress);
            return 16;
        }

        return 12;
    }

    static int jumpIfSet(Cpu cpu, Word.Argument address, Flag flag) {
        final int targetAddress = cpu.read(address);
        if (cpu.isSet(flag)) {
            doJump(cpu, targetAddress);
            return 16;
        }

        return 12;
    }

    enum RotateMode {
        COPY_TO_CARRY,
        INCLUDE_CARRY
    }

    enum ShiftMode {
        ARITHMETIC,
        LOGICAL
    }
}
