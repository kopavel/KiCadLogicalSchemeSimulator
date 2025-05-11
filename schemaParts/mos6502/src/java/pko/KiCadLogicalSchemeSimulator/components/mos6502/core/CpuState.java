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
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package pko.KiCadLogicalSchemeSimulator.components.mos6502.core;
/**
 * A compact, struct-like representation of CPU state.
 */
public class CpuState {
    public static final int P_CARRY = 0x01;
    public static final int P_ZERO = 0x02;
    public static final int P_IRQ_DISABLE = 0x04;
    public static final int P_DECIMAL = 0x08;
    public static final int P_BREAK = 0x10;
    // Bit 5 always '1'
    public static final int P_OVERFLOW = 0x40;
    public static final int P_NEGATIVE = 0x80;

    /**
     * Accumulator
     */
    public int a;
    /**
     * X index register
     */
    public int x;
    /**
     * Y index register
     */
    public int y;
    /**
     * Stack Pointer
     */
    public int sp;
    /**
     * Program Counter
     */
    public int pc;
    /**
     * Last Loaded Instruction Register
     */
    public int ir;
    /**
     * Peek-Ahead to next IR
     */
    public int lastIr = -1;
    public final int[] args = new int[2];
    public final int[] lastArgs = new int[2];
    public int instSize;
    public boolean opTrap;
    public boolean irqAsserted;
    public boolean nmiAsserted;
    public int irPc;
    public int lastPc;
    /* Status Flag Register bits */
    public boolean carryFlag;
    public boolean negativeFlag;
    public boolean zeroFlag;
    public boolean irqDisableFlag;
    public boolean decimalModeFlag;
    public boolean breakFlag;
    public boolean overflowFlag;
    public int stepCounter;

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
        String mnemonic = InstructionTable.opcodeNames[opCode];
        if (mnemonic == null) {
            return "???";
        }
        StringBuilder sb = new StringBuilder(mnemonic);
        switch (InstructionTable.instructionModes[opCode]) {
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
     * Returns a string formatted for the trace log.
     *
     * @return a string formatted for the trace log.
     */
    public String toTraceEvent() {
        String opcode = disassembleOp(lastIr, lastArgs);
        return getInstructionByteStatus() + "\n" + String.format("%-14s", opcode) + "\nA:" + Utils.byteToHex(a) + "\nX:" + Utils.byteToHex(x) + "\nY:" +
                Utils.byteToHex(y) + "\nF:" + Utils.byteToHex(getStatusFlag()) + "\nS:1" + Utils.byteToHex(sp) + "\n" + getProcessorStatusString() + "\n";
    }

    /**
     * @return The value of the Process Status Register, as a byte.
     */
    public int getStatusFlag() {
        int status = 0x20;
        if (carryFlag) {
            status |= P_CARRY;
        }
        if (zeroFlag) {
            status |= P_ZERO;
        }
        if (irqDisableFlag) {
            status |= P_IRQ_DISABLE;
        }
        if (decimalModeFlag) {
            status |= P_DECIMAL;
        }
        if (breakFlag) {
            status |= P_BREAK;
        }
        if (overflowFlag) {
            status |= P_OVERFLOW;
        }
        if (negativeFlag) {
            status |= P_NEGATIVE;
        }
        return status;
    }

    public String getInstructionByteStatus() {
        String retVal = Utils.wordToHex(lastPc) + "  ";
        if (lastIr < 0) {
            return retVal + "Reading";
        }
        return retVal + switch (Cpu.instructionSizes[lastIr]) {
            case 0, 1 -> Utils.byteToHex(lastIr) + "      ";
            case 2 -> Utils.byteToHex(lastIr) + " " + Utils.byteToHex(lastArgs[0]) + "   ";
            case 3 -> Utils.byteToHex(lastIr) + " " + Utils.byteToHex(lastArgs[0]) + " " + Utils.byteToHex(lastArgs[1]);
            default -> null;
        };
    }

    /**
     * @return A string representing the current status register state.
     */
    public String getProcessorStatusString() {
        return "[" + (negativeFlag ? 'N' : '.') + (overflowFlag ? 'V' : '.') + "-" + (breakFlag ? 'B' : '.') + (decimalModeFlag ? 'D' : '.') +
                (irqDisableFlag ? 'I' : '.') + (zeroFlag ? 'Z' : '.') + (carryFlag ? 'C' : '.') + "]";
    }
}