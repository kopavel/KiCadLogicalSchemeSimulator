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
public interface InstructionTable {
    // 6502 opcodes.  No 65C02 opcodes implemented.
    /**
     * Size, in bytes, required for each instruction. This table
     * includes sizes for all instructions for NMOS 6502, CMOS 65C02,
     * and CMOS 65C816
     */
    int[] instructionSizes = {1, 2, 2, 1, 2, 2, 2, 2, 1, 2, 1, 1, 3, 3, 3, 3,   // 0x00-0x0f
            2, 2, 2, 1, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 3,   // 0x10-0x1f
            3, 2, 2, 1, 2, 2, 2, 2, 1, 2, 1, 1, 3, 3, 3, 3,   // 0x20-0x2f
            2, 2, 2, 1, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 3,   // 0x30-0x3f
            1, 2, 2, 1, 2, 2, 2, 2, 1, 2, 1, 1, 3, 3, 3, 3,   // 0x40-0x4f
            2, 2, 2, 1, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 3,   // 0x50-0x5f
            1, 2, 2, 1, 2, 2, 2, 2, 1, 2, 1, 1, 3, 3, 3, 3,   // 0x60-0x6f
            2, 2, 2, 1, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 3,   // 0x70-0x7f
            2, 2, 2, 1, 2, 2, 2, 2, 1, 2, 1, 1, 3, 3, 3, 3,   // 0x80-0x8f
            2, 2, 2, 1, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 3,   // 0x90-0x9f
            2, 2, 2, 1, 2, 2, 2, 2, 1, 2, 1, 1, 3, 3, 3, 3,   // 0xa0-0xaf
            2, 2, 2, 1, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 3,   // 0xb0-0xbf
            2, 2, 2, 1, 2, 2, 2, 2, 1, 2, 1, 1, 3, 3, 3, 3,   // 0xc0-0xcf
            2, 2, 2, 1, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 3,   // 0xd0-0xdf
            2, 2, 2, 1, 2, 2, 2, 2, 1, 2, 1, 1, 3, 3, 3, 3,   // 0xe0-0xef
            2, 2, 2, 1, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 3    // 0xf0-0xff
    };

    /**
     * Enumeration of valid CPU behaviors. These determine what behavior and instruction
     * set will be simulated, depending on desired version of 6502.
     */
    enum CpuBehavior {
        /**
         * The earliest NMOS 6502 includes a bug that causes the ROR instruction
         * to behave like an ASL that does not affect the carry bit. This version
         * is very rare in the wild.
         */
        NMOS_WITH_ROR_BUG,
        /**
         * All NMOS 6502's have a bug with the indirect JMP instruction. If the
         */
        NMOS_6502,
        /**
         * Emulate a CMOS 65C02, with all CMOS instructions and addressing modes.
         */
        CMOS_6502,
        /**
         * Emulate a CMOS 65C816.
         */
        CMOS_65816
    }
}
