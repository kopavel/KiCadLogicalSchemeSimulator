/*
 * Copyright (c) 2008-2025 Seth J. Morabito <web@loomcom.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
/* 2025.04 Pavel Korzh.
    Source tuned for generating IO request in queue instead us read/write to/from BUS directly.
*/
package pko.KiCadLogicalSchemeSimulator.components.mos6502.core;
import lombok.Getter;
import lombok.Setter;
import pko.KiCadLogicalSchemeSimulator.components.mos6502.queue.ArrayCallback;
import pko.KiCadLogicalSchemeSimulator.components.mos6502.queue.Callback;
import pko.KiCadLogicalSchemeSimulator.components.mos6502.queue.IoQueue;

/**
 * This class provides a simulation of the MOS 6502 CPU's state machine.
 * A simple interface allows this 6502 to read and write to a simulated bus,
 * and exposes some internal state for inspection and debugging.
 */
public class Cpu implements InstructionTable {
    /* Process status register mnemonics */
    public static final int P_CARRY = 0x01;
    public static final int P_ZERO = 0x02;
    public static final int P_IRQ_DISABLE = 0x04;
    public static final int P_DECIMAL = 0x08;
    public static final int P_BREAK = 0x10;
    // Bit 5 always '1'
    public static final int P_OVERFLOW = 0x40;
    public static final int P_NEGATIVE = 0x80;
    // NMI vector
    public static final int NMI_VECTOR_L = 0xfffa;
    public static final int NMI_VECTOR_H = 0xfffb;
    // Reset vector
    public static final int RST_VECTOR_L = 0xfffc;
    public static final int RST_VECTOR_H = 0xfffd;
    // IRQ vector
    public static final int IRQ_VECTOR_L = 0xfffe;
    public static final int IRQ_VECTOR_H = 0xffff;
    /* The CPU state */
    public final CpuState state = new CpuState();
    /**
     * @value The value of the Process Status Register bits to be set.
     */
    public final Callback setProcessorStatus = value -> {
        if ((value & 1) != 0) {
            setCarryFlag();
        } else {
            clearCarryFlag();
        }
        if ((value & 2) != 0) {
            setZeroFlag();
        } else {
            clearZeroFlag();
        }
        if ((value & 4) != 0) {
            setIrqDisableFlag();
        } else {
            clearIrqDisableFlag();
        }
        state.decimalModeFlag = (value & 8) != 0;
        state.breakFlag = (value & 16) != 0;
        state.overflowFlag = (value & 64) != 0;
        if ((value & 128) != 0) {
            setNegativeFlag();
        } else {
            clearNegativeFlag();
        }
    };
    private final Callback statePcCallback = word -> state.pc = word;
    private final Callback rstCallback = data -> state.pc = ((data + 1) & 0xffff);
    private final Callback setArithmeticFlagsStateA = data -> {
        state.a = data;
        setArithmeticFlags(data);
    };
    private final Callback setArithmeticFlagsOrStateA = data -> {
        state.a |= data;
        setArithmeticFlags(data);
    };
    private final Callback setArithmeticFlagsAndStateA = data -> {
        state.a &= data;
        setArithmeticFlags(data);
    };
    private final Callback setArithmeticFlagsXorStateA = data -> {
        state.a ^= data;
        setArithmeticFlags(data);
    };
    private final Callback step3cCallback = data -> {
        setZeroFlag((state.a & data) == 0);
        state.negativeFlag = (data & 0x80) != 0;
        state.overflowFlag = (data & 0x40) != 0;
    };
    private final Callback cmpStateA = data -> cmp(state.a, data);
    private final Callback cmpStateY = data -> cmp(state.y, data);
    private final Callback cmpStateX = data -> cmp(state.x, data);
    private final Callback setArithmeticFlagsStateY = data -> {
        state.y = data;
        setArithmeticFlags(data);
    };
    private final Callback setArithmeticFlagsStateX = data -> {
        state.x = data;
        setArithmeticFlags(data);
    };
    private final Callback stepffCallback = tmp -> stepNXf(tmp & 128);
    private final Callback stepefCallback = tmp -> stepNXf(tmp & 64);
    private final Callback stepdfCallback = tmp -> stepNXf(tmp & 32);
    private final Callback stepcfCallback = tmp -> stepNXf(tmp & 16);
    private final Callback stepbfCallback = tmp -> stepNXf(tmp & 8);
    private final Callback stepafCallback = tmp -> stepNXf(tmp & 4);
    private final Callback step9fCallback = tmp -> stepNXf(tmp & 2);
    private final Callback step8fCallback = tmp -> stepNXf(tmp & 1);
    private final Callback step7fCallback = tmp -> stepXf(tmp & 128);
    private final Callback step6fCallback = tmp -> stepXf(tmp & 64);
    private final Callback step5fCallback = tmp -> stepXf(tmp & 32);
    private final Callback step4fCallback = tmp -> stepXf(tmp & 16);
    private final Callback step3fCallback = tmp -> stepXf(tmp & 8);
    private final Callback step2fCallback = tmp -> stepXf(tmp & 4);
    private final Callback step1fCallback = tmp -> stepXf(tmp & 2);
    private final Callback step0fCallback = tmp -> stepXf(tmp & 1);
    int[] addressArray = new int[2];
    /* Simulated behavior */
    @Setter
    @Getter
    private CpuBehavior behavior;
    private final Callback stepFdCallback = data -> {
        if (state.decimalModeFlag) {
            state.a = sbcDecimal(state.a, data);
        } else {
            state.a = sbc(state.a, data);
        }
    };
    private final Callback step7dCallback = data -> {
        if (state.decimalModeFlag) {
            state.a = adcDecimal(state.a, data);
        } else {
            state.a = adc(state.a, data);
        }
    };
    /* The Bus */
    @Setter
    @Getter
    private IoQueue ioQueue;
    private int writeAddress;
    private final Callback step0cCallback = tmp -> {
        setZeroFlag((state.a & tmp) == 0);
        tmp |= state.a;
        tmp = tmp & 0xff;
        ioQueue.write(writeAddress, tmp);
    };
    private final Callback step1cCallback = tmp -> {
        setZeroFlag((state.a & tmp) == 0);
        tmp &= ~(state.a);
        tmp &= 0xff;
        ioQueue.write(writeAddress, tmp);
    };
    private final Callback stepf7Callback = data -> ioQueue.write(writeAddress, data & 0xff | 0b10000000);
    private final Callback stepe7Callback = data -> ioQueue.write(writeAddress, data & 0xff | 0b01000000);
    private final Callback stepd7Callback = data -> ioQueue.write(writeAddress, data & 0xff | 0b00100000);
    private final Callback stepc7Callback = data -> ioQueue.write(writeAddress, data & 0xff | 0b00010000);
    private final Callback stepb7Callback = data -> ioQueue.write(writeAddress, data & 0xff | 0b00001000);
    private final Callback stepa7Callback = data -> ioQueue.write(writeAddress, data & 0xff | 0b00000100);
    private final Callback step97Callback = data -> ioQueue.write(writeAddress, data & 0xff | 0b00000010);
    private final Callback step87Callback = data -> ioQueue.write(writeAddress, data & 0xff | 0b00000001);
    private final Callback step77Callback = data -> ioQueue.write(writeAddress, data & 0b01111111);
    private final Callback step67Callback = data -> ioQueue.write(writeAddress, data & 0b10111111);
    private final Callback step57Callback = data -> ioQueue.write(writeAddress, data & 0b11011111);
    private final Callback step47Callback = data -> ioQueue.write(writeAddress, data & 0b11101111);
    private final Callback step37Callback = data -> ioQueue.write(writeAddress, data & 0b11110111);
    private final Callback step27Callback = data -> ioQueue.write(writeAddress, data & 0b11111011);
    private final Callback step17Callback = data -> ioQueue.write(writeAddress, data & 0b11111101);
    private final Callback step07Callback = data -> ioQueue.write(writeAddress, data & 0b11111110);
    private final Callback stap1aCallback = data -> {
        int tmp = asl(data);
        ioQueue.write(writeAddress, tmp);
        setArithmeticFlags(tmp);
    };
    private final Callback stepFeCallback = data -> {
        int tmp = (data + 1) & 0xff;
        ioQueue.write(writeAddress, tmp);
        setArithmeticFlags(tmp);
    };
    private final Callback step3eCallback = data -> {
        int tmp = rol(data);
        ioQueue.write(writeAddress, tmp);
        setArithmeticFlags(tmp);
    };
    private final Callback step5eCallback = data -> {
        int tmp = lsr(data);
        ioQueue.write(writeAddress, tmp);
        setArithmeticFlags(tmp);
    };
    private final Callback stepDeCallback = data -> {
        int tmp = (data - 1) & 0xff;
        ioQueue.write(writeAddress, tmp);
        setArithmeticFlags(tmp);
    };
    private final Callback step7eCallback = data -> {
        int tmp = ror(data);
        ioQueue.write(writeAddress, tmp);
        setArithmeticFlags(tmp);
    };
    private final Callback step3Callback = effectiveAddress -> {
        int hi, lo; // Address calculation
        // Execute
        switch (state.ir) {
            // Single Byte Instructions; Implied and Relative
            case 0x00: // BRK - Force Interrupt - Implied
                handleBrk(state.pc + 1);
                return;
            case 0x08: // PHP - Push Processor Status - Implied
                // Break flag always set in the stack value.
                stackPush(state.getStatusFlag() | 0x10);
                return;
            case 0x10: // BPL - Branch if Positive - Relative
                if (!state.negativeFlag) {
                    state.pc = relAddress(state.args[0]);
                }
                return;
            case 0x18: // CLC - Clear Carry Flag - Implied
                clearCarryFlag();
                return;
            case 0x20: // JSR - Jump to Subroutine - Implied
                stackPush((state.pc - 1 >> 8) & 0xff); // PC high byte
                stackPush(state.pc - 1 & 0xff);        // PC low byte
                state.pc = Utils.address(state.args[0], state.args[1]);
                return;
            case 0x28: // PLP - Pull Processor Status - Implied
                stackPop(setProcessorStatus);
                return;
            case 0x30: // BMI - Branch if Minus - Relative
                if (state.negativeFlag) {
                    state.pc = relAddress(state.args[0]);
                }
                return;
            case 0x38: // SEC - Set Carry Flag - Implied
                setCarryFlag();
                return;
            case 0x40: // RTI - Return from Interrupt - Implied
                stackPop(setProcessorStatus);
                stackWordPop(statePcCallback);
                return;
            case 0x48: // PHA - Push Accumulator - Implied
                stackPush(state.a);
                return;
            case 0x50: // BVC - Branch if Overflow Clear - Relative
                if (!state.overflowFlag) {
                    state.pc = relAddress(state.args[0]);
                }
                return;
            case 0x58: // CLI - Clear Interrupt Disable - Implied
                clearIrqDisableFlag();
                return;
            case 0x5a: // 65C02 PHY - Push Y to stack
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                stackPush(state.y);
                return;
            case 0x60: // RTS - Return from Subroutine - Implied
                stackWordPop(rstCallback);
                return;
            case 0x68: // PLA - Pull Accumulator - Implied
                stackPop(setArithmeticFlagsStateA);
                return;
            case 0x70: // BVS - Branch if Overflow Set - Relative
                if (state.overflowFlag) {
                    state.pc = relAddress(state.args[0]);
                }
                return;
            case 0x78: // SEI - Set Interrupt Disable - Implied
                setIrqDisableFlag();
                return;
            case 0x7a: // 65C02 PLY - Pull Y from Stack
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                stackPop(setArithmeticFlagsStateY);
                return;
            case 0x80: // 65C02 BRA - Branch Always
                if (behavior == CpuBehavior.CMOS_6502 || behavior == CpuBehavior.CMOS_65816) {
                    state.pc = relAddress(state.args[0]);
                }
                return;
            case 0x88: // DEY - Decrement Y Register - Implied
                state.y = --state.y & 0xff;
                setArithmeticFlags(state.y);
                return;
            case 0x8a: // TXA - Transfer X to Accumulator - Implied
                state.a = state.x;
                setArithmeticFlags(state.a);
                return;
            case 0x90: // BCC - Branch if Carry Clear - Relative
                if (!getCarryFlag()) {
                    state.pc = relAddress(state.args[0]);
                }
                return;
            case 0x98: // TYA - Transfer Y to Accumulator - Implied
                state.a = state.y;
                setArithmeticFlags(state.a);
                return;
            case 0x9a: // TXS - Transfer X to Stack Pointer - Implied
                state.sp = state.x;
                return;
            case 0xa8: // TAY - Transfer Accumulator to Y - Implied
                state.y = state.a;
                setArithmeticFlags(state.y);
                return;
            case 0xaa: // TAX - Transfer Accumulator to X - Implied
                state.x = state.a;
                setArithmeticFlags(state.x);
                return;
            case 0xb0: // BCS - Branch if Carry Set - Relative
                if (getCarryFlag()) {
                    state.pc = relAddress(state.args[0]);
                }
                return;
            case 0xb8: // CLV - Clear Overflow Flag - Implied
                state.overflowFlag = false;
                return;
            case 0xba: // TSX - Transfer Stack Pointer to X - Implied
                state.x = state.sp;
                setArithmeticFlags(state.x);
                return;
            case 0xc8: // INY - Increment Y Register - Implied
                state.y = ++state.y & 0xff;
                setArithmeticFlags(state.y);
                return;
            case 0xca: // DEX - Decrement X Register - Implied
                state.x = --state.x & 0xff;
                setArithmeticFlags(state.x);
                return;
            case 0xd0: // BNE - Branch if Not Equal to Zero - Relative
                if (!getZeroFlag()) {
                    state.pc = relAddress(state.args[0]);
                }
                return;
            case 0xd8: // CLD - Clear Decimal Mode - Implied
                state.decimalModeFlag = false;
                return;
            case 0xda: // 65C02 PHX - Push X to stack
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                stackPush(state.x);
                return;
            case 0xe8: // INX - Increment X Register - Implied
                state.x = ++state.x & 0xff;
                setArithmeticFlags(state.x);
                return;
            case 0xea: // NOP
                // Do nothing.
                return;
            case 0xf0: // BEQ - Branch if Equal to Zero - Relative
                if (getZeroFlag()) {
                    state.pc = relAddress(state.args[0]);
                }
                return;
            case 0xf8: // SED - Set Decimal Flag - Implied
                state.decimalModeFlag = true;
                return;
            case 0xfa: // 65C02 PLX - Pull X from Stack
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                stackPop(setArithmeticFlagsStateX);
                return;
            // JMP
            case 0x4c: // JMP - Absolute
                state.pc = Utils.address(state.args[0], state.args[1]);
                return;
            case 0x6c: // JMP - Indirect
                lo = Utils.address(state.args[0], state.args[1]); // Address of low byte
                if (state.args[0] == 0xff && (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG)) {
                    hi = Utils.address(0x00, state.args[1]);
                } else {
                    hi = lo + 1;
                }
                ioQueue.readWord(lo, hi, statePcCallback);
                /* TODO: For accuracy, allow a flag to enable broken behavior of early 6502s:
                 *
                 * "An original 6502 has does not correctly fetch the target
                 * address if the indirect vector falls on a page boundary
                 * (e.g. $xxFF where xx is and value from $00 to $FF). In this
                 * case fetches the LSB from $xxFF as expected but takes the MSB
                 * from $xx00. This is fixed in some later chips like the 65SC02
                 * so for compatibility always ensure the indirect vector is not
                 * at the end of the page."
                 * (http://www.obelisk.demon.co.uk/6502/reference.html#JMP)
                 */
                return;
            case 0x7c: // 65C02 JMP - (Absolute Indexed Indirect,X)
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                lo = (((state.args[1] << 8) | state.args[0]) + state.x) & 0xffff;
                hi = lo + 1;
                ioQueue.readWord(lo, hi, statePcCallback);
                return;
            // ORA - Logical Inclusive Or
            case 0x09: // #Immediate
                state.a |= state.args[0];
                setArithmeticFlags(state.a);
                return;
            case 0x12: // 65C02 ORA (ZP)
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
            case 0x01: // (Zero Page,X)
            case 0x05: // Zero Page
            case 0x0d: // Absolute
            case 0x11: // (Zero Page),Y
            case 0x15: // Zero Page,X
            case 0x19: // Absolute,Y
            case 0x1d: // Absolute,X
                ioQueue.read(effectiveAddress, setArithmeticFlagsOrStateA);
                return;
            // ASL - Arithmetic Shift Left
            case 0x0a: // Accumulator
                state.a = asl(state.a);
                setArithmeticFlags(state.a);
                return;
            case 0x06: // Zero Page
            case 0x0e: // Absolute
            case 0x16: // Zero Page,X
            case 0x1e: // Absolute,X
                writeAddress = effectiveAddress;
                ioQueue.read(effectiveAddress, stap1aCallback);
                return;
            // BIT - Bit Test
            case 0x89: // 65C02 #Immediate
                setZeroFlag((state.a & state.args[0]) == 0);
                return;
            case 0x34: // 65C02 Zero Page,X
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
            case 0x24: // Zero Page
            case 0x2c: // Absolute
            case 0x3c: // Absolute,X
                ioQueue.read(effectiveAddress, step3cCallback);
                return;
            // AND - Logical AND
            case 0x29: // #Immediate
                state.a &= state.args[0];
                setArithmeticFlags(state.a);
                return;
            case 0x32: // 65C02 AND (ZP)
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
            case 0x21: // (Zero Page,X)
            case 0x25: // Zero Page
            case 0x2d: // Absolute
            case 0x31: // (Zero Page),Y
            case 0x35: // Zero Page,X
            case 0x39: // Absolute,Y
            case 0x3d: // Absolute,X
                ioQueue.read(effectiveAddress, setArithmeticFlagsAndStateA);
                return;
            // ROL - Rotate Left
            case 0x2a: // Accumulator
                state.a = rol(state.a);
                setArithmeticFlags(state.a);
                return;
            case 0x26: // Zero Page
            case 0x2e: // Absolute
            case 0x36: // Zero Page,X
            case 0x3e: // Absolute,X
                writeAddress = effectiveAddress;
                ioQueue.read(effectiveAddress, step3eCallback);
                return;
            // EOR - Exclusive OR
            case 0x49: // #Immediate
                state.a ^= state.args[0];
                setArithmeticFlags(state.a);
                return;
            case 0x52: // 65C02 EOR (ZP)
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
            case 0x41: // (Zero Page,X)
            case 0x45: // Zero Page
            case 0x4d: // Absolute
            case 0x51: // (Zero Page,Y)
            case 0x55: // Zero Page,X
            case 0x59: // Absolute,Y
            case 0x5d: // Absolute,X
                ioQueue.read(effectiveAddress, setArithmeticFlagsXorStateA);
                return;
            // LSR - Logical Shift Right
            case 0x4a: // Accumulator
                state.a = lsr(state.a);
                setArithmeticFlags(state.a);
                return;
            case 0x46: // Zero Page
            case 0x4e: // Absolute
            case 0x56: // Zero Page,X
            case 0x5e: // Absolute,X
                writeAddress = effectiveAddress;
                ioQueue.read(effectiveAddress, step5eCallback);
                return;
            // ADC - Add with Carry
            case 0x69: // #Immediate
                if (state.decimalModeFlag) {
                    state.a = adcDecimal(state.a, state.args[0]);
                } else {
                    state.a = adc(state.a, state.args[0]);
                }
                return;
            case 0x72: // 65C02 ADC (ZP)
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
            case 0x61: // (Zero Page,X)
            case 0x65: // Zero Page
            case 0x6d: // Absolute
            case 0x71: // (Zero Page),Y
            case 0x75: // Zero Page,X
            case 0x79: // Absolute,Y
            case 0x7d: // Absolute,X
                ioQueue.read(effectiveAddress, step7dCallback);
                return;
            // ROR - Rotate Right
            case 0x6a: // Accumulator
                state.a = ror(state.a);
                setArithmeticFlags(state.a);
                return;
            case 0x66: // Zero Page
            case 0x6e: // Absolute
            case 0x76: // Zero Page,X
            case 0x7e: // Absolute,X
                writeAddress = effectiveAddress;
                ioQueue.read(effectiveAddress, step7eCallback);
                return;
            // STA - Store Accumulator
            case 0x92: // 65C02 STA (ZP)
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
            case 0x81: // (Zero Page,X)
            case 0x85: // Zero Page
            case 0x8d: // Absolute
            case 0x91: // (Zero Page),Y
            case 0x95: // Zero Page,X
            case 0x99: // Absolute,Y
            case 0x9d: // Absolute,X
                ioQueue.write(effectiveAddress, state.a);
                return;
            // STY - Store Y Register
            case 0x84: // Zero Page
            case 0x8c: // Absolute
            case 0x94: // Zero Page,X
                ioQueue.write(effectiveAddress, state.y);
                return;
            // STX - Store X Register
            case 0x86: // Zero Page
            case 0x8e: // Absolute
            case 0x96: // Zero Page,Y
                ioQueue.write(effectiveAddress, state.x);
                return;
            // STZ - 65C02 Store Zero
            case 0x64: // Zero Page
            case 0x74: // Zero Page,X
            case 0x9c: // Absolute
            case 0x9e: // Absolute,X
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                ioQueue.write(effectiveAddress, 0);
                return;
            // LDY - Load Y Register
            case 0xa0: // #Immediate
                state.y = state.args[0];
                setArithmeticFlags(state.y);
                return;
            case 0xa4: // Zero Page
            case 0xac: // Absolute
            case 0xb4: // Zero Page,X
            case 0xbc: // Absolute,X
                ioQueue.read(effectiveAddress, setArithmeticFlagsStateY);
                return;
            // LDX - Load X Register
            case 0xa2: // #Immediate
                state.x = state.args[0];
                setArithmeticFlags(state.x);
                return;
            case 0xa6: // Zero Page
            case 0xae: // Absolute
            case 0xb6: // Zero Page,Y
            case 0xbe: // Absolute,Y
                ioQueue.read(effectiveAddress, setArithmeticFlagsStateX);
                return;
            // LDA - Load Accumulator
            case 0xa9: // #Immediate
                state.a = state.args[0];
                setArithmeticFlags(state.a);
                return;
            case 0xb2: // 65C02 LDA (ZP)
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
            case 0xa1: // (Zero Page,X)
            case 0xa5: // Zero Page
            case 0xad: // Absolute
            case 0xb1: // (Zero Page),Y
            case 0xb5: // Zero Page,X
            case 0xb9: // Absolute,Y
            case 0xbd: // Absolute,X
                ioQueue.read(effectiveAddress, setArithmeticFlagsStateA);
                return;
            // CPY - Compare Y Register
            case 0xc0: // #Immediate
                cmp(state.y, state.args[0]);
                return;
            case 0xc4: // Zero Page
            case 0xcc: // Absolute
                ioQueue.read(effectiveAddress, cmpStateY);
                return;
            // CMP - Compare Accumulator
            case 0xc9: // #Immediate
                cmp(state.a, state.args[0]);
                return;
            case 0xd2: // 65C02 CMP (ZP)
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
            case 0xc1: // (Zero Page,X)
            case 0xc5: // Zero Page
            case 0xcd: // Absolute
            case 0xd1: // (Zero Page),Y
            case 0xd5: // Zero Page,X
            case 0xd9: // Absolute,Y
            case 0xdd: // Absolute,X
                ioQueue.read(effectiveAddress, cmpStateA);
                return;
            // DEC - Decrement Memory
            case 0x3a: // 65C02 Immediate
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                state.a = --state.a & 0xFF;
                setArithmeticFlags(state.a);
                return;
            case 0xc6: // Zero Page
            case 0xce: // Absolute
            case 0xd6: // Zero Page,X
            case 0xde: // Absolute,X
                writeAddress = effectiveAddress;
                ioQueue.read(effectiveAddress, stepDeCallback);
                return;
            // CPX - Compare X Register
            case 0xe0: // #Immediate
                cmp(state.x, state.args[0]);
                return;
            case 0xe4: // Zero Page
            case 0xec: // Absolute
                ioQueue.read(effectiveAddress, cmpStateX);
                return;
            // SBC - Subtract with Carry (Borrow)
            case 0xe9: // #Immediate
                if (state.decimalModeFlag) {
                    state.a = sbcDecimal(state.a, state.args[0]);
                } else {
                    state.a = sbc(state.a, state.args[0]);
                }
                return;
            case 0xf2: // 65C02 SBC (ZP)
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
            case 0xe1: // (Zero Page,X)
            case 0xe5: // Zero Page
            case 0xed: // Absolute
            case 0xf1: // (Zero Page),Y
            case 0xf5: // Zero Page,X
            case 0xf9: // Absolute,Y
            case 0xfd: // Absolute,X
                ioQueue.read(effectiveAddress, stepFdCallback);
                return;
            // INC - Increment Memory
            case 0x1a: // 65C02 Increment Immediate
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                state.a = ++state.a & 0xff;
                setArithmeticFlags(state.a);
                return;
            case 0xe6: // Zero Page
            case 0xee: // Absolute
            case 0xf6: // Zero Page,X
            case 0xfe: // Absolute,X
                writeAddress = effectiveAddress;
                ioQueue.read(effectiveAddress, stepFeCallback);
                return;
            // 65C02 RMB - Reset Memory Bit
            case 0x07: // 65C02 RMB0 - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                writeAddress = effectiveAddress;
                ioQueue.read(effectiveAddress, step07Callback);
                return;
            case 0x17: // 65C02 RMB1 - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                writeAddress = effectiveAddress;
                ioQueue.read(effectiveAddress, step17Callback);
                return;
            case 0x27: // 65C02 RMB2 - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                writeAddress = effectiveAddress;
                ioQueue.read(effectiveAddress, step27Callback);
                return;
            case 0x37: // 65C02 RMB3 - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                writeAddress = effectiveAddress;
                ioQueue.read(effectiveAddress, step37Callback);
                return;
            case 0x47: // 65C02 RMB4 - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                writeAddress = effectiveAddress;
                ioQueue.read(effectiveAddress, step47Callback);
                return;
            case 0x57: // 65C02 RMB5 - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                writeAddress = effectiveAddress;
                ioQueue.read(effectiveAddress, step57Callback);
                return;
            case 0x67: // 65C02 RMB6 - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                writeAddress = effectiveAddress;
                ioQueue.read(effectiveAddress, step67Callback);
                return;
            case 0x77: // 65C02 RMB7 - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                writeAddress = effectiveAddress;
                ioQueue.read(effectiveAddress, step77Callback);
                return;
            // 65C02 SMB - Set Memory Bit
            case 0x87: // 65C02 SMB0 - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                writeAddress = effectiveAddress;
                ioQueue.read(effectiveAddress, step87Callback);
                return;
            case 0x97: // 65C02 SMB1 - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                writeAddress = effectiveAddress;
                ioQueue.read(effectiveAddress, step97Callback);
                return;
            case 0xa7: // 65C02 SMB2 - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                writeAddress = effectiveAddress;
                ioQueue.read(effectiveAddress, stepa7Callback);
                return;
            case 0xb7: // 65C02 SMB3 - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                writeAddress = effectiveAddress;
                ioQueue.read(effectiveAddress, stepb7Callback);
                return;
            case 0xc7: // 65C02 SMB4 - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                writeAddress = effectiveAddress;
                ioQueue.read(effectiveAddress, stepc7Callback);
                return;
            case 0xd7: // 65C02 SMB5 - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                writeAddress = effectiveAddress;
                ioQueue.read(effectiveAddress, stepd7Callback);
                return;
            case 0xe7: // 65C02 SMB6 - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                writeAddress = effectiveAddress;
                ioQueue.read(effectiveAddress, stepe7Callback);
                return;
            case 0xf7: // 65C02 SMB7 - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                writeAddress = effectiveAddress;
                ioQueue.read(effectiveAddress, stepf7Callback);
                return;
            // 65C02 TRB/TSB - Test and Reset Bit/Test and Set Bit
            case 0x14: // 65C02 TRB - Test and Reset bit - Zero Page
            case 0x1c: // 65C02 TRB - Test and Reset bit - Absolute
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                writeAddress = effectiveAddress;
                ioQueue.read(effectiveAddress, step1cCallback);
                return;
            case 0x04: // 65C02 TSB - Test and Set bit - Zero Page
            case 0x0c: // 65C02 TSB - Test and Set bit - Absolute
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                writeAddress = effectiveAddress;
                ioQueue.read(effectiveAddress, step0cCallback);
                return;
            // 65C02 BBR - Branch if Bit Reset
            case 0x0f: // 65C02 BBR - Branch if bit 0 reset - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                ioQueue.read(effectiveAddress, step0fCallback);
                return;
            case 0x1f: // 65C02 BBR - Branch if bit 1 reset - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                ioQueue.read(effectiveAddress, step1fCallback);
                return;
            case 0x2f: // 65C02 BBR - Branch if bit 2 reset - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                ioQueue.read(effectiveAddress, step2fCallback);
                return;
            case 0x3f: // 65C02 BBR - Branch if bit 3 reset - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                ioQueue.read(effectiveAddress, step3fCallback);
                return;
            case 0x4f: // 65C02 BBR - Branch if bit 4 reset - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                ioQueue.read(effectiveAddress, step4fCallback);
                return;
            case 0x5f: // 65C02 BBR - Branch if bit 5 reset - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                ioQueue.read(effectiveAddress, step5fCallback);
                return;
            case 0x6f: // 65C02 BBR - Branch if bit 6 reset - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                ioQueue.read(effectiveAddress, step6fCallback);
                return;
            case 0x7f: // 65C02 BBR - Branch if bit 5 reset - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                ioQueue.read(effectiveAddress, step7fCallback);
                return;
            // 65C02 BBS - Branch if Bit Set
            case 0x8f: // 65C02 BBS - Branch if bit 0 set - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                ioQueue.read(effectiveAddress, step8fCallback);
                return;
            case 0x9f: // 65C02 BBS - Branch if bit 1 set - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                ioQueue.read(effectiveAddress, step9fCallback);
                return;
            case 0xaf: // 65C02 BBS - Branch if bit 2 set - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                ioQueue.read(effectiveAddress, stepafCallback);
                return;
            case 0xbf: // 65C02 BBS - Branch if bit 3 set - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                ioQueue.read(effectiveAddress, stepbfCallback);
                return;
            case 0xcf: // 65C02 BBS - Branch if bit 4 set - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                ioQueue.read(effectiveAddress, stepcfCallback);
                return;
            case 0xdf: // 65C02 BBS - Branch if bit 5 set - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                ioQueue.read(effectiveAddress, stepdfCallback);
                return;
            case 0xef: // 65C02 BBS - Branch if bit 6 set - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                ioQueue.read(effectiveAddress, stepefCallback);
                return;
            case 0xff: // 65C02 BBS - Branch if bit 5 set - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    return;
                }
                ioQueue.read(effectiveAddress, stepffCallback);
                return;
            // Unimplemented Instructions
            // TODO: Create a flag to enable highly-accurate emulation of unimplemented instructions.
            default:
                state.opTrap = true;
                return;
        }
    };
    private final Callback step2_1Callback = read -> step3Callback.accept((read + state.y) & 0xffff);
    private final ArrayCallback step2Callback = () -> {
        state.lastIr = state.ir;
        state.lastPc = state.irPc;
        state.lastArgs[0] = state.args[0];
        state.lastArgs[1] = state.args[1];
        int irAddressMode = (state.ir >> 2) & 0x07;  // Bits 3-5 of IR:  [ | | |X|X|X| | ]
        int irOpMode = state.ir & 0x03;              // Bits 6-7 of IR:  [ | | | | | |X|X]
        state.stepCounter++;
        // Get the data from the effective address (if any)
        int tmp; // Temporary storage
        switch (irOpMode) {
            case 0:
            case 2:
                switch (irAddressMode) {
                    case 0: // #Immediate
                        step3Callback.accept(-1);
                        return;
                    case 1: // Zero Page
                        step3Callback.accept(state.args[0]);
                        return;
                    case 2: // Accumulator - ignored
                        step3Callback.accept(-1);
                        return;
                    case 3: // Absolute
                        step3Callback.accept(Utils.address(state.args[0], state.args[1]));
                        return;
                    case 4: // 65C02 (Zero Page)
                        if (behavior == CpuBehavior.CMOS_6502 || behavior == CpuBehavior.CMOS_65816) {
                            ioQueue.readWord(state.args[0], (state.args[0] + 1) & 0xff, step3Callback);
                        }
                        step3Callback.accept(-1);
                        return;
                    case 5: // Zero Page,X / Zero Page,Y
                        if (state.ir == 0x14) { // 65C02 TRB Zero Page
                            step3Callback.accept(state.args[0]);
                        } else if (state.ir == 0x96 || state.ir == 0xb6) {
                            step3Callback.accept(zpyAddress(state.args[0]));
                        } else {
                            step3Callback.accept(zpxAddress(state.args[0]));
                        }
                        return;
                    case 7:
                        if (state.ir == 0x9c || state.ir == 0x1c) { // 65C02 STZ & TRB Absolute
                            step3Callback.accept(Utils.address(state.args[0], state.args[1]));
                        } else if (state.ir == 0xbe) { // Absolute,X / Absolute,Y
                            step3Callback.accept(yAddress(state.args[0], state.args[1]));
                        } else {
                            step3Callback.accept(xAddress(state.args[0], state.args[1]));
                        }
                        return;
                }
                return;
            case 3: // Rockwell/WDC 65C02
                switch (irAddressMode) {
                    case 1: // Zero Page
                    case 3:
                    case 5:
                    case 7: // Zero Page, Relative
                        step3Callback.accept(state.args[0]);
                        return;
                }
                return;
            case 1:
                switch (irAddressMode) {
                    case 0: // (Zero Page,X)
                        tmp = (state.args[0] + state.x) & 0xff;
                        ioQueue.readWord(tmp, tmp + 1, step3Callback);
                        return;
                    case 1: // Zero Page
                        step3Callback.accept(state.args[0]);
                        return;
                    case 2: // #Immediate
                        step3Callback.accept(-1);
                        return;
                    case 3: // Absolute
                        step3Callback.accept(Utils.address(state.args[0], state.args[1]));
                        return;
                    case 4: // (Zero Page),Y
                        ioQueue.readWord(state.args[0], (state.args[0] + 1) & 0xff, step2_1Callback);
                        return;
                    case 5: // Zero Page,X
                        step3Callback.accept(zpxAddress(state.args[0]));
                        return;
                    case 6: // Absolute, Y
                        step3Callback.accept(yAddress(state.args[0], state.args[1]));
                        return;
                    case 7: // Absolute, X
                        step3Callback.accept(xAddress(state.args[0], state.args[1]));
                        return;
                }
                return;
        }
    };
    private final Callback step1Callback = read -> {
        state.ir = read;
        incrementPC();
        state.opTrap = false;
        // Decode the instruction and operands
        state.instSize = Cpu.instructionSizes[state.ir];
        int length = state.instSize - 1;
        if (length > 0) {
            for (int i = 0; i < length; i++) {
                addressArray[i] = state.pc;
                // Increment PC after reading
                incrementPC();
            }
            ioQueue.readArray(addressArray, state.args, length, step2Callback);
        } else {
            step2Callback.accept();
        }
    };

    /**
     * Build a new CPU.
     */
    public Cpu() {
        this(CpuBehavior.NMOS_6502);
    }

    public Cpu(CpuBehavior behavior) {
        this.behavior = behavior;
    }

    /**
     * Return a formatted string representing the last instruction and
     * operands that were executed.
     *
     * @return A string representing the mnemonic and operands of the instruction
     */
    public static String disassembleOp(int opCode, int[] args) {
        if (opCode < 0) {
            return "Reading";
        }
        String mnemonic = opcodeNames[opCode];
        if (mnemonic == null) {
            return "???";
        }
        StringBuilder sb = new StringBuilder(mnemonic);
        switch (instructionModes[opCode]) {
            case ABS:
                sb.append(" $").append(Utils.wordToHex(Utils.address(args[0], args[1])));
                break;
            case AIX:
                sb.append(" ($").append(Utils.wordToHex(Utils.address(args[0], args[1]))).append(",X)");
                break;
            case ABX:
                sb.append(" $").append(Utils.wordToHex(Utils.address(args[0], args[1]))).append(",X");
                break;
            case ABY:
                sb.append(" $").append(Utils.wordToHex(Utils.address(args[0], args[1]))).append(",Y");
                break;
            case IMM:
                sb.append(" #$").append(Utils.byteToHex(args[0]));
                break;
            case IND:
                sb.append(" ($").append(Utils.wordToHex(Utils.address(args[0], args[1]))).append(")");
                break;
            case ZPI:
                sb.append(" ($").append(Utils.byteToHex(args[0])).append(")");
                break;
            case XIN:
                sb.append(" ($").append(Utils.byteToHex(args[0])).append(",X)");
                break;
            case INY:
                sb.append(" ($").append(Utils.byteToHex(args[0])).append("),Y");
                break;
            case REL:
            case ZPR:
            case ZPG:
                sb.append(" $").append(Utils.byteToHex(args[0]));
                break;
            case ZPX:
                sb.append(" $").append(Utils.byteToHex(args[0])).append(",X");
                break;
            case ZPY:
                sb.append(" $").append(Utils.byteToHex(args[0])).append(",Y");
                break;
        }
        return sb.toString();
    }

    /**
     * Reset the CPU to known initial values.
     */
    public void reset() {
        /* TODO: In reality, the stack pointer could be anywhere
           on the stack after reset. This non-deterministic behavior might be
           worth while to simulate. */
        state.sp = 0xff;
        // Set the PC to the address stored in the reset vector.
        ioQueue.readWord(RST_VECTOR_L, RST_VECTOR_H, statePcCallback);
        // Clear instruction register.
        state.ir = 0;
        // Clear status register bits.
        state.carryFlag = false;
        state.zeroFlag = false;
        state.irqDisableFlag = true;
        state.decimalModeFlag = false;
        state.breakFlag = false;
        state.overflowFlag = false;
        state.negativeFlag = false;
        state.irqAsserted = false;
        // Clear illegal opcode trap.
        state.opTrap = false;
        // Reset step counter
        state.stepCounter = 0L;
        // Reset registers.
        state.a = 0;
        state.x = 0;
        state.y = 0;
    }

    /**
     * Performs an individual instruction cycle.
     */
    public void step() {
        // Store the address from which the IR was read, for debugging.
        state.irPc = state.pc;
        // Check for Interrupts before doing anything else.
        // This will set the PC and jump to the interrupt vector.
        if (state.nmiAsserted) {
            handleNmi();
        } else if (state.irqAsserted && !getIrqDisableFlag()) {
            handleIrq(state.pc);
        } else {
            // Fetch memory location for this instruction.
            ioQueue.read(state.pc, step1Callback);
        }
    }

    public void setNegativeFlag() {
        state.negativeFlag = true;
    }

    public void clearNegativeFlag() {
        state.negativeFlag = false;
    }

    /**
     * @return the carry flag
     */
    public boolean getCarryFlag() {
        return state.carryFlag;
    }

    /**
     * @param carryFlag the carry flag to set
     */
    public void setCarryFlag(boolean carryFlag) {
        state.carryFlag = carryFlag;
    }

    /**
     * @return 1 if the carry flag is set, 0 if it is clear.
     */
    public int getCarryBit() {
        return (state.carryFlag ? 1 : 0);
    }

    /**
     * Sets the Carry Flag
     */
    public void setCarryFlag() {
        state.carryFlag = true;
    }

    /**
     * Clears the Carry Flag
     */
    public void clearCarryFlag() {
        state.carryFlag = false;
    }

    /**
     * @return the zero flag
     */
    public boolean getZeroFlag() {
        return state.zeroFlag;
    }

    /**
     * @param zeroFlag the zero flag to set
     */
    public void setZeroFlag(boolean zeroFlag) {
        state.zeroFlag = zeroFlag;
    }

    /**
     * Sets the Zero Flag
     */
    public void setZeroFlag() {
        state.zeroFlag = true;
    }

    /**
     * Clears the Zero Flag
     */
    public void clearZeroFlag() {
        state.zeroFlag = false;
    }

    /**
     * @return the irq disable flag
     */
    public boolean getIrqDisableFlag() {
        return state.irqDisableFlag;
    }

    public void setIrqDisableFlag() {
        state.irqDisableFlag = true;
    }

    public void clearIrqDisableFlag() {
        state.irqDisableFlag = false;
    }

    private void stepXf(int tmp) {
        if (tmp == 0) {
            state.pc = relAddress(state.args[1]);
        }
    }

    private void stepNXf(int tmp) {
        if (tmp != 0) {
            state.pc = relAddress(state.args[1]);
        }
    }

    private void handleBrk(int returnPc) {
        handleInterrupt(returnPc, IRQ_VECTOR_L, IRQ_VECTOR_H, true);
        state.irqAsserted = false;
    }

    private void handleIrq(int returnPc) {
        handleInterrupt(returnPc, IRQ_VECTOR_L, IRQ_VECTOR_H, false);
        state.irqAsserted = false;
    }

    private void handleNmi() {
        handleInterrupt(state.pc, NMI_VECTOR_L, NMI_VECTOR_H, false);
        state.nmiAsserted = false;
    }

    /**
     * Handle the common behavior of BRK, /IRQ, and /NMI
     * <p>
     *
     * @ on memory access failure
     */
    private void handleInterrupt(int returnPc, int vectorLow, int vectorHigh, boolean isBreak) {
        // Set the break flag before pushing.
        // IRQ & NMI clear break flag
        state.breakFlag = isBreak;
        // Push program counter + 1 onto the stack
        stackPush((returnPc >> 8) & 0xff); // PC high byte
        stackPush(returnPc & 0xff);        // PC low byte
        stackPush(state.getStatusFlag());
        // Set the Interrupt Disabled flag. RTI will clear it.
        setIrqDisableFlag();
        // 65C02 & 65816 clear Decimal flag after pushing Processor status to the stack.
        if (behavior == CpuBehavior.CMOS_6502 || behavior == CpuBehavior.CMOS_65816) {
            state.decimalModeFlag = false;
        }
        // Load interrupt vector address into PC
        ioQueue.readWord(vectorLow, vectorHigh, statePcCallback);
    }

    /**
     * Add with Carry, used by all addressing mode implementations of ADC.
     * As a side effect, this will set the overflow and carry flags as
     * needed.
     *
     * @param acc     The current value of the accumulator
     * @param operand The operand
     * @return The sum of the accumulator and the operand
     */
    private int adc(int acc, int operand) {
        int result = (operand & 0xff) + (acc & 0xff) + getCarryBit();
        int carry6 = (operand & 0x7f) + (acc & 0x7f) + getCarryBit();
        setCarryFlag((result & 0x100) != 0);
        state.overflowFlag = state.carryFlag ^ ((carry6 & 0x80) != 0);
        result &= 0xff;
        setArithmeticFlags(result);
        return result;
    }

    /**
     * Add with Carry (BCD).
     */
    private int adcDecimal(int acc, int operand) {
        int l, h, result;
        l = (acc & 0x0f) + (operand & 0x0f) + getCarryBit();
        if ((l & 0xff) > 9) {
            l += 6;
        }
        h = (acc >> 4) + (operand >> 4) + (l > 15 ? 1 : 0);
        if ((h & 0xff) > 9) {
            h += 6;
        }
        result = (l & 0x0f) | (h << 4);
        result &= 0xff;
        setCarryFlag(h > 15);
        setZeroFlag(result == 0);
        state.overflowFlag = false; // BCD never sets overflow flag
        if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
            state.negativeFlag = false; // BCD never negative on NMOS 6502
        } else {
            state.negativeFlag = (result & 0x80) != 0; // N Flag is valid on CMOS 6502/65816
        }
        return result;
    }

    /**
     * Common code for Subtract with Carry. Just calls ADC of the
     * one's complement of the operand. This lets the N, V, C, and Z
     * flags work out nicely without any additional logic.
     */
    private int sbc(int acc, int operand) {
        int result;
        result = adc(acc, ~operand);
        setArithmeticFlags(result);
        return result;
    }

    /**
     * Subtract with Carry, BCD mode.
     */
    private int sbcDecimal(int acc, int operand) {
        int l, h, result;
        l = (acc & 0x0f) - (operand & 0x0f) - (state.carryFlag ? 0 : 1);
        if ((l & 0x10) != 0) {
            l -= 6;
        }
        h = (acc >> 4) - (operand >> 4) - ((l & 0x10) != 0 ? 1 : 0);
        if ((h & 0x10) != 0) {
            h -= 6;
        }
        result = (l & 0x0f) | (h << 4) & 0xff;
        setCarryFlag((h & 0xff) < 15);
        setZeroFlag(result == 0);
        state.overflowFlag = false; // BCD never sets overflow flag
        if (behavior == CpuBehavior.NMOS_6502 || behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
            state.negativeFlag = false; // BCD never negative on NMOS 6502
        } else {
            state.negativeFlag = (result & 0x80) != 0; // N Flag is valid on CMOS 6502/65816
        }
        return (result & 0xff);
    }

    /**
     * Compare values. Set carry, zero and negative flags
     * appropriately.
     */
    private void cmp(int reg, int operand) {
        int tmp = (reg - operand) & 0xff;
        setCarryFlag(reg >= operand);
        setZeroFlag(tmp == 0);
        state.negativeFlag = (tmp & 0x80) != 0; // Negative bit set
    }

    /**
     * Set the Negative and Zero flags based on the current value of the
     * register operand.
     */
    private void setArithmeticFlags(int reg) {
        state.zeroFlag = (reg == 0);
        state.negativeFlag = (reg & 0x80) != 0;
    }

    /**
     * Shifts the given value left by one bit, and sets the carry
     * flag to the high bit of the initial value.
     *
     * @param m The value to shift left.
     * @return the left shifted value (m × 2).
     */
    private int asl(int m) {
        setCarryFlag((m & 0x80) != 0);
        return (m << 1) & 0xff;
    }

    /**
     * Shifts the given value right by one bit, filling with zeros,
     * and sets the carry flag to the low bit of the initial value.
     */
    private int lsr(int m) {
        setCarryFlag((m & 0x01) != 0);
        return (m & 0xff) >>> 1;
    }

    /**
     * Rotates the given value left by one bit, setting bit 0 to the value
     * of the carry flag, and setting the carry flag to the original value
     * of bit 7.
     */
    private int rol(int m) {
        int result = ((m << 1) | getCarryBit()) & 0xff;
        setCarryFlag((m & 0x80) != 0);
        return result;
    }

    /**
     * Rotates the given value right by one bit, setting bit 7 to the value
     * of the carry flag, and setting the carry flag to the original value
     * of bit 1.
     */
    private int ror(int m) {
        int result = ((m >>> 1) | (getCarryBit() << 7)) & 0xff;
        setCarryFlag((m & 0x01) != 0);
        return result;
    }

    /**
     * Push an item onto the stack, and decrement the stack counter.
     * Will wrap-around if already at the bottom of the stack (This
     * is the same behavior as the real 6502)
     */
    void stackPush(int data) {
        ioQueue.write(0x100 + state.sp, data);
        if (state.sp == 0) {
            state.sp = 0xff;
        } else {
            --state.sp;
        }
    }

    /**
     * Pre-increment the stack pointer, and return the top of the stack.
     * Will wrap-around if already at the top of the stack (This
     * is the same behavior as the real 6502)
     */
    void stackPop(Callback callback) {
        if (state.sp == 0xff) {
            state.sp = 0x00;
        } else {
            ++state.sp;
        }
        ioQueue.read(0x100 + state.sp, callback);
    }

    void stackWordPop(Callback callback) {
        if (state.sp == 0xff) {
            state.sp = 0x00;
        } else {
            ++state.sp;
        }
        int low = state.sp;
        if (state.sp == 0xff) {
            state.sp = 0x00;
        } else {
            ++state.sp;
        }
        ioQueue.readWord(0x100 + low, 0x100 + state.sp, callback);
    }

    /*
     * Increment the PC, rolling over if necessary.
     */
    void incrementPC() {
        if (state.pc == 0xffff) {
            state.pc = 0;
        } else {
            ++state.pc;
        }
    }

    /**
     * Given a hi byte and a low byte, return the Absolute X
     * offset address.
     */
    int xAddress(int lowByte, int hiByte) {
        return (Utils.address(lowByte, hiByte) + state.x) & 0xffff;
    }

    /**
     * Given a hi byte and a low byte, return the Absolute Y
     * offset address.
     */
    int yAddress(int lowByte, int hiByte) {
        return (Utils.address(lowByte, hiByte) + state.y) & 0xffff;
    }

    /**
     * Given a single byte, compute the Zero Page X offset address.
     */
    int zpxAddress(int zp) {
        return (zp + state.x) & 0xff;
    }

    /**
     * Given a single byte, compute the Zero Page Y offset address.
     */
    int zpyAddress(int zp) {
        return (zp + state.y) & 0xff;
    }

    /**
     * Given a single byte, compute the offset address.
     */
    int relAddress(int offset) {
        // Cast the offset to a signed byte to handle negative offsets.
        return (state.pc + (byte) offset) & 0xffff;
    }
}