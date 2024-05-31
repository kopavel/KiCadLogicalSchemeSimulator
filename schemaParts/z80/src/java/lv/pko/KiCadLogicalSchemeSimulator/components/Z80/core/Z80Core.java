/*
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Based on https://github.com/codesqueak/Z80Processor
 * Reimplemented in ioRequest queue manner with callbacks.
 * */
package lv.pko.KiCadLogicalSchemeSimulator.components.Z80.core;
import lombok.Getter;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;

import static lv.pko.KiCadLogicalSchemeSimulator.components.Z80.core.CPUConstants.*;

/**
 * The ZiLOG Z80 processor core
 */
@SuppressWarnings("DuplicateBranchesInSwitch")
public class Z80Core {
    // maximum address size
    private final static int MAX_ADDRESS = 0xFFFF;
    public Queue<IoRequest> ioQueue = new LinkedList<>();
    private int instruction;
    @Getter
    private boolean halt;
    @Getter
    private long tStates;
    /* registers */
    private int reg_B, reg_C, reg_D, reg_E, reg_H, reg_L;
    private int reg_B_ALT, reg_C_ALT, reg_D_ALT, reg_E_ALT, reg_H_ALT, reg_L_ALT;
    private int reg_IX, reg_IY, reg_PC, reg_SP;
    private int reg_A, reg_A_ALT, reg_F, reg_F_ALT, reg_I, reg_R, reg_R8;
    private boolean EIDIFlag;
    private boolean IFF1, IFF2;
    private boolean NMI_FF;
    private boolean useIxReg;

    /*
     * Public interfaces to processor control functions
     */

    /**
     * Standard constructor. Set the processor up with a memory and I/O interface.
     */
    public Z80Core() {
        tStates = 0;
        //
    }

    /**
     * Reset the processor to a known state. Equivalent to a hardware reset.
     */
    public void reset() {
        processorReset();
        ioQueue.clear();
    }

    /**
     * Initiate an NMI request
     */
    public void setNMI() {
        NMI_FF = true;
    }

    /**
     * Recover the present program counter (PC) value
     *
     * @return Value in the range 0x0000 to 0xFFFF
     */
    public int getProgramCounter() {
        return reg_PC;
    }

    /**
     * Recover a register value via a register name
     *
     * @param name Register name
     * @return The register value
     */
    public int getRegisterValue(RegisterNames name) {
        return switch (name) {
            case BC -> getBC();
            case DE -> getDE();
            case HL -> getHL();
            case BC_ALT -> getBC_ALT();
            case DE_ALT -> getDE_ALT();
            case HL_ALT -> getHL_ALT();
            case IX -> reg_IX;
            case IY -> reg_IY;
            case SP -> reg_SP;
            case PC -> reg_PC;
            case A -> reg_A;
            case F -> reg_F;
            case A_ALT -> reg_A_ALT;
            case F_ALT -> reg_F_ALT;
            case I -> reg_I;
            case R -> getR();
        };
    }

    /**
     * Execute a single instruction at the present program counter (PC) then return. The internal state of the processor
     * is updated along with the T state count.
     */
    public void executeOneInstruction() {
        //
        // NMI check first
        if (NMI_FF) {
            // can't interrupt straight after an EI or DI
            if (!EIDIFlag) {
                NMI_FF = false; // interrupt accepted
                IFF2 = IFF1; // store IFF state
                dec2SP();
                if (halt) {
                    incPC(); // Was a bug ! - point to instruction after(!) interrupt location. HALT decrements PC !!!
                }
                writeWord(reg_SP, reg_PC);
                reg_PC = 0x0066; // NMI routine location
            }
        }
        halt = false;
        readByte(reg_PC, b -> {
            instruction = b;
            incPC();
            EIDIFlag = false; // clear prior to decoding next instruction
            decodeOneByteInstruction(instruction);
        });
    }

    /*
     * *****************************************************************************
     *
     * Extended Instruction area
     *
     * *****************************************************************************
     */

    /*
     * *****************************************************************************
     *
     * IX and IY index register processing
     *
     * *****************************************************************************
     */

    /**
     * Reset the processor to a known state. Equivalent to a hardware reset.
     */
    private void processorReset() {
        halt = false;
        //
        reg_B = reg_C = reg_D = reg_E = reg_H = reg_L = 0;
        reg_B_ALT = reg_C_ALT = reg_D_ALT = reg_E_ALT = reg_H_ALT = reg_L_ALT = 0;
        reg_IX = reg_IY = reg_SP = 0;
        reg_A = reg_A_ALT = reg_F = reg_F_ALT = reg_I = reg_R = reg_R8 = 0;
        IFF1 = IFF2 = false;
        EIDIFlag = false;
        NMI_FF = false;
        reg_PC = 0;
        tStates = 0;
    }

    /**
     * Execute all one byte instructions and pass multibyte instructions on for further processing
     *
     * @param opcode Instruction byte
     */
    private void decodeOneByteInstruction(int opcode) {
        tStates = tStates + OPCODE_T_STATES[opcode];
        switch (opcode) {
            case 0x00 -> {
            } // null
            case 0x01 -> readWord(reg_PC, word -> {
                setBC(word);
                inc2PC();
            }); // LD bc, nnnn
            case 0x02 -> writeByte(getBC(), reg_A); // LD (BC), A
            case 0x03 -> setBC(ALU16BitInc(getBC())); // inc BC
            case 0x04 -> reg_B = ALU8BitInc(reg_B); // inc b
            case 0x05 -> reg_B = ALU8BitDec(reg_B); // dec b
            case 0x06 -> readByte(reg_PC, b -> {
                reg_B = b;
                incPC();
            }); // ld b,nn
            case 0x07 -> RLCA(); // rlca
            case 0x08 -> EXAFAF(); // ex af,af'
            case 0x09 -> setHL(ALU16BitAdd(getBC())); // add hl,bc
            case 0x0A -> readByte(getBC(), (value) -> reg_A = value); // LD a, (bc)
            case 0x0B -> setBC(ALU16BitDec(getBC())); // dec bc
            case 0x0C -> reg_C = ALU8BitInc(reg_C); // inc c
            case 0x0D -> reg_C = ALU8BitDec(reg_C); // dec c
            case 0x0E -> readByte(reg_PC, b -> {
                reg_C = b;
                incPC();
            }); // ld c,n
            case 0x0F -> RRCA(); // rrca
            case 0x10 -> djnz(); // djnz
            case 0x11 -> readWord(reg_PC, word -> {
                setDE(word);
                inc2PC();
            }); // LD de, nnnn
            case 0x12 -> writeByte(getDE(), reg_A); // LD (de), A
            case 0x13 -> setDE(ALU16BitInc(getDE())); // inc de
            case 0x14 -> reg_D = ALU8BitInc(reg_D); // inc d
            case 0x15 -> reg_D = ALU8BitDec(reg_D); // dec d
            case 0x16 -> readByte(reg_PC, b -> {
                reg_D = b;
                incPC();
            }); // ld d,nn
            case 0x17 -> RLA(); // rla
            case 0x18 -> relativeJump(); // jr
            case 0x19 -> setHL(ALU16BitAdd(getDE())); // add hl,de
            case 0x1A -> readByte(getDE(), b -> reg_A = b); // LD a, (de)
            case 0x1B -> setDE(ALU16BitDec(getDE())); // dec de
            case 0x1C -> reg_E = ALU8BitInc(reg_E); // inc e
            case 0x1D -> reg_E = ALU8BitDec(reg_E); // dec e
            case 0x1E -> readByte(reg_PC, b -> {
                reg_E = b;
                incPC();
            }); // ld e,n
            case 0x1F -> RRA(); // rra
            case 0x20 -> {
                if (!getZ()) {
                    tStates = tStates + 12;
                    relativeJump();
                } else {
                    incPC();
                    tStates = tStates + 7;
                }
            } // jr nz
            case 0x21 -> readWord(reg_PC, word -> {
                setHL(word);
                inc2PC();
            }); // LD hl, nnnn
            case 0x22 -> readWord(reg_PC, word -> {
                writeWord(word, getHL());
                inc2PC();
            }); // LD (nnnn), hl
            case 0x23 -> setHL(ALU16BitInc(getHL())); // inc hl
            case 0x24 -> reg_H = ALU8BitInc(reg_H); // inc h
            case 0x25 -> reg_H = ALU8BitDec(reg_H); // dec h
            case 0x26 -> readByte(reg_PC, b -> {
                reg_H = b;
                incPC();
            }); // ld h,nn
            case 0x27 -> DAA(); // daa
            case 0x28 -> {
                if (getZ()) {
                    tStates = tStates + 12;
                    relativeJump();
                } else {
                    incPC();
                    tStates = tStates + 7;
                }
            } // jr z
            case 0x29 -> setHL(ALU16BitAdd(getHL())); // add hl,hl
            case 0x2A -> readWord(reg_PC, fromPc -> readWord(fromPc, fromMem -> {
                setHL(fromMem);
                inc2PC();
            })); // LD hl, (nnnn)
            case 0x2B -> setHL(ALU16BitDec(getHL())); // dec hl
            case 0x2C -> reg_L = ALU8BitInc(reg_L); // inc l
            case 0x2D -> reg_L = ALU8BitDec(reg_L); // dec l
            case 0x2E -> readByte(reg_PC, b -> {
                reg_L = b;
                incPC();
            }); // ld l,n
            case 0x2F -> CPL(); // rra
            case 0x30 -> {
                if (!getC()) {
                    tStates = tStates + 12;
                    relativeJump();
                } else {
                    incPC();
                    tStates = tStates + 7;
                }
            } // jr nc
            case 0x31 -> readWord(reg_PC, word -> {
                reg_SP = word;
                inc2PC();
            }); // LD sp, nnnn
            case 0x32 -> readWord(reg_PC, word -> {
                writeByte(word, reg_A);
                inc2PC();
            }); // LD (nnnn), A
            case 0x33 -> reg_SP = ALU16BitInc(reg_SP); // inc SP
            case 0x34 -> readByte(getHL(), b -> writeByte(getHL(), ALU8BitInc(b))); // inc (hl)
            case 0x35 -> readByte(getHL(), b -> writeByte(getHL(), ALU8BitDec(b))); // dec (hl)
            case 0x36 -> readByte(reg_PC, b -> {
                writeByte(getHL(), b);
                incPC();
            }); // ld (hl), nn
            case 0x37 -> SCF(); // scf
            case 0x38 -> {
                if (getC()) {
                    tStates = tStates + 12;
                    relativeJump();
                } else {
                    incPC();
                    tStates = tStates + 7;
                }
            } // jr c
            case 0x39 -> setHL(ALU16BitAdd(reg_SP)); // add hl,sp
            case 0x3A -> readWord(reg_PC, word -> {
                readByte(word, b -> reg_A = b);
                inc2PC();
            }); // LD a, (nnnn)
            case 0x3B -> reg_SP = ALU16BitDec(reg_SP); // dec sp
            case 0x3C -> reg_A = ALU8BitInc(reg_A); // inc a
            case 0x3D -> reg_A = ALU8BitDec(reg_A); // dec a
            case 0x3E -> readByte(reg_PC, b -> {
                reg_A = b;
                incPC();
            }); // ld a,n
            case 0x3F -> CCF(); // ccf
            // LD B,*
            case 0x40 -> {
            } /* reg_B = reg_B; */ // ld b,b
            case 0x41 -> reg_B = reg_C; // ld b,c
            case 0x42 -> reg_B = reg_D; // ld b,d
            case 0x43 -> reg_B = reg_E; // ld b,e
            case 0x44 -> reg_B = reg_H; // ld b,h
            case 0x45 -> reg_B = reg_L; // ld b,l
            case 0x46 -> readByte(getHL(), b -> reg_B = b); // ld b,(hl)
            case 0x47 -> reg_B = reg_A; // ld b,a
            // LD C,*
            case 0x48 -> reg_C = reg_B; // ld c,b
            case 0x49 -> {
            } /* reg_C = reg_C; */ // ld c,c
            case 0x4A -> reg_C = reg_D; // ld c,d
            case 0x4B -> reg_C = reg_E; // ld c,e
            case 0x4C -> reg_C = reg_H; // ld c,h
            case 0x4D -> reg_C = reg_L; // ld c,l
            case 0x4E -> readByte(getHL(), b -> reg_C = b); // ld c,(hl)
            case 0x4F -> reg_C = reg_A; // ld c,a
            // LD D,*
            case 0x50 -> reg_D = reg_B; // ld d,b
            case 0x51 -> reg_D = reg_C; // ld d,c
            case 0x52 -> {
            }  /* reg_D = reg_D; */ // ld d,d
            case 0x53 -> reg_D = reg_E; // ld d,e
            case 0x54 -> reg_D = reg_H; // ld d,h
            case 0x55 -> reg_D = reg_L; // ld d,l
            case 0x56 -> readByte(getHL(), b -> reg_D = b); // ld d,(hl)
            case 0x57 -> reg_D = reg_A; // ld d,a
            // LD E,*
            case 0x58 -> reg_E = reg_B; // ld e,b
            case 0x59 -> reg_E = reg_C; // ld e,c
            case 0x5A -> reg_E = reg_D; // ld e,d
            case 0x5B -> {
            }  /* reg_E = reg_E; */ // ld e,e
            case 0x5C -> reg_E = reg_H; // ld e,h
            case 0x5D -> reg_E = reg_L; // ld e,l
            case 0x5E -> readByte(getHL(), b -> reg_E = b); // ld e,(hl)
            case 0x5F -> reg_E = reg_A; // ld e,a
            // LD H,*
            case 0x60 -> reg_H = reg_B; // ld h,b
            case 0x61 -> reg_H = reg_C; // ld h,c
            case 0x62 -> reg_H = reg_D; // ld h,d
            case 0x63 -> reg_H = reg_E; // ld h,e
            case 0x64 -> {
            }  /* reg_H = reg_H; */ // ld h,h
            case 0x65 -> reg_H = reg_L; // ld h,l
            case 0x66 -> readByte(getHL(), b -> reg_H = b); // ld h,(hl)
            case 0x67 -> reg_H = reg_A; // ld h,a
            // LD L,*
            case 0x68 -> reg_L = reg_B; // ld l,b
            case 0x69 -> reg_L = reg_C; // ld l,c
            case 0x6A -> reg_L = reg_D; // ld l,d
            case 0x6B -> reg_L = reg_E; // ld l,e
            case 0x6C -> reg_L = reg_H; // ld l,h
            case 0x6D -> {
            }  /* reg_L = reg_L; */ // ld l,l
            case 0x6E -> readByte(getHL(), b -> reg_L = b); // ld l,(hl)
            case 0x6F -> reg_L = reg_A; // ld l,a
            // LD (HL),*
            case 0x70 -> writeByte(getHL(), reg_B); // ld (hl),b
            case 0x71 -> writeByte(getHL(), reg_C); // ld (hl),c
            case 0x72 -> writeByte(getHL(), reg_D); // ld (hl),d
            case 0x73 -> writeByte(getHL(), reg_E); // ld (hl),e
            case 0x74 -> writeByte(getHL(), reg_H); // ld (hl),h
            case 0x75 -> writeByte(getHL(), reg_L); // ld (hl),l
            // HALT
            case 0x76 -> {
                decPC(); // execute it forever !
                halt = true;
            }
            case 0x77 -> writeByte(getHL(), reg_A); // ld (hl),a
            // LD A,*
            case 0x78 -> reg_A = reg_B; // ld a,b
            case 0x79 -> reg_A = reg_C; // ld a,c
            case 0x7A -> reg_A = reg_D; // ld a,d
            case 0x7B -> reg_A = reg_E; // ld a,e
            case 0x7C -> reg_A = reg_H; // ld a,h
            case 0x7D -> reg_A = reg_L; // ld a,l
            case 0x7E -> readByte(getHL(), b -> reg_A = b); // ld a,(hl)
            case 0x7F -> {
            }  /* reg_A = reg_A; */ // ld a,a
            // add
            case 0x80 -> ALU8BitAdd(reg_B);
            case 0x81 -> ALU8BitAdd(reg_C);
            case 0x82 -> ALU8BitAdd(reg_D);
            case 0x83 -> ALU8BitAdd(reg_E);
            case 0x84 -> ALU8BitAdd(reg_H);
            case 0x85 -> ALU8BitAdd(reg_L);
            case 0x86 -> readByte(getHL(), this::ALU8BitAdd);
            case 0x87 -> ALU8BitAdd(reg_A);
            // adc
            case 0x88 -> ALU8BitAdc(reg_B);
            case 0x89 -> ALU8BitAdc(reg_C);
            case 0x8A -> ALU8BitAdc(reg_D);
            case 0x8B -> ALU8BitAdc(reg_E);
            case 0x8C -> ALU8BitAdc(reg_H);
            case 0x8D -> ALU8BitAdc(reg_L);
            case 0x8E -> readByte(getHL(), this::ALU8BitAdc);
            case 0x8F -> ALU8BitAdc(reg_A);
            // sub
            case 0x90 -> ALU8BitSub(reg_B);
            case 0x91 -> ALU8BitSub(reg_C);
            case 0x92 -> ALU8BitSub(reg_D);
            case 0x93 -> ALU8BitSub(reg_E);
            case 0x94 -> ALU8BitSub(reg_H);
            case 0x95 -> ALU8BitSub(reg_L);
            case 0x96 -> readByte(getHL(), this::ALU8BitSub);
            case 0x97 -> ALU8BitSub(reg_A);
            // sbc
            case 0x98 -> ALU8BitSbc(reg_B);
            case 0x99 -> ALU8BitSbc(reg_C);
            case 0x9A -> ALU8BitSbc(reg_D);
            case 0x9B -> ALU8BitSbc(reg_E);
            case 0x9C -> ALU8BitSbc(reg_H);
            case 0x9D -> ALU8BitSbc(reg_L);
            case 0x9E -> readByte(getHL(), this::ALU8BitSbc);
            case 0x9F -> ALU8BitSbc(reg_A);
            // and
            case 0xA0 -> ALU8BitAnd(reg_B);
            case 0xA1 -> ALU8BitAnd(reg_C);
            case 0xA2 -> ALU8BitAnd(reg_D);
            case 0xA3 -> ALU8BitAnd(reg_E);
            case 0xA4 -> ALU8BitAnd(reg_H);
            case 0xA5 -> ALU8BitAnd(reg_L);
            case 0xA6 -> readByte(getHL(), this::ALU8BitAnd);
            case 0xA7 -> ALU8BitAnd(reg_A);
            // xor
            case 0xA8 -> ALU8BitXor(reg_B);
            case 0xA9 -> ALU8BitXor(reg_C);
            case 0xAA -> ALU8BitXor(reg_D);
            case 0xAB -> ALU8BitXor(reg_E);
            case 0xAC -> ALU8BitXor(reg_H);
            case 0xAD -> ALU8BitXor(reg_L);
            case 0xAE -> readByte(getHL(), this::ALU8BitXor);
            case 0xAF -> ALU8BitXor(reg_A);
            // or
            case 0xB0 -> ALU8BitOr(reg_B);
            case 0xB1 -> ALU8BitOr(reg_C);
            case 0xB2 -> ALU8BitOr(reg_D);
            case 0xB3 -> ALU8BitOr(reg_E);
            case 0xB4 -> ALU8BitOr(reg_H);
            case 0xB5 -> ALU8BitOr(reg_L);
            case 0xB6 -> readByte(getHL(), this::ALU8BitOr);
            case 0xB7 -> ALU8BitOr(reg_A);
            // cp
            case 0xB8 -> ALU8BitCp(reg_B);
            case 0xB9 -> ALU8BitCp(reg_C);
            case 0xBA -> ALU8BitCp(reg_D);
            case 0xBB -> ALU8BitCp(reg_E);
            case 0xBC -> ALU8BitCp(reg_H);
            case 0xBD -> ALU8BitCp(reg_L);
            case 0xBE -> readByte(getHL(), this::ALU8BitCp);
            case 0xBF -> ALU8BitCp(reg_A);
            //
            case 0xC0 -> ret(!getZ());
            case 0xC1 -> readWord(reg_SP, word -> {
                setBC(word);
                inc2SP();
            });
            case 0xC2 -> jp(!getZ());
            case 0xC3 -> jp();
            case 0xC4 -> call(!getZ());
            case 0xC5 -> {
                dec2SP();
                writeWord(reg_SP, getBC());
            }
            case 0xC6 -> readByte(reg_PC, b -> {
                ALU8BitAdd(b);
                incPC();
            });
            case 0xc7 -> rst(0);
            case 0xC8 -> ret(getZ());
            case 0xC9 -> ret();
            case 0xCA -> jp(getZ());
            case 0xCB -> extendedCB();
            case 0xCC -> call(getZ());
            case 0xCD -> call();
            case 0xCE -> readByte(reg_PC, b -> {
                ALU8BitAdc(b);
                incPC();
            });
            case 0xCF -> rst(1);
            //
            case 0xD0 -> ret(!getC());
            case 0xD1 -> readWord(reg_SP, word -> {
                setDE(word);
                inc2SP();
            });
            case 0xD2 -> jp(!getC());
            case 0xD3 -> outNA();
            case 0xD4 -> call(!getC());
            case 0xD5 -> {
                dec2SP();
                writeWord(reg_SP, getDE());
            }
            case 0xD6 -> readByte(reg_PC, b -> {
                ALU8BitSub(b);
                incPC();
            });
            case 0xD7 -> rst(2);
            case 0xD8 -> ret(getC());
            case 0xD9 -> EXX();
            case 0xDA -> jp(getC());
            case 0xDB -> inAN();
            case 0xDC -> call(getC());
            case 0xDD -> extendedDD();
            case 0xDE -> readByte(reg_PC, b -> {
                ALU8BitSbc(b);
                incPC();
            });
            case 0xDF -> rst(3);
            //
            case 0xE0 -> ret(!getPV());
            case 0xE1 -> readWord(reg_SP, word -> {
                setHL(word);
                inc2SP();
            });
            case 0xE2 -> jp(!getPV());
            case 0xE3 -> EXSPHL();
            case 0xE4 -> call(!getPV());
            case 0xE5 -> {
                dec2SP();
                writeWord(reg_SP, getHL());
            }
            case 0xE6 -> readByte(reg_PC, b -> {
                ALU8BitAnd(b);
                incPC();
            });
            case 0xE7 -> rst(4);
            case 0xE8 -> ret(getPV());
            case 0xE9 -> reg_PC = getHL();
            case 0xEA -> jp(getPV());
            case 0xEB -> EXDEHL();
            case 0xEC -> call(getPV());
            case 0xED -> extendedED();
            case 0xEE -> readByte(reg_PC, b -> {
                ALU8BitXor(b);
                incPC();
            });
            case 0xEF -> rst(5);
            //
            case 0xF0 -> ret(!getS());
            case 0xF1 -> readWord(reg_SP, word -> {
                int temp = word;
                inc2SP();
                reg_F = (temp & lsb);
                reg_A = ((temp & msb) >> 8);
            });
            case 0xF2 -> jp(!getS());
            case 0xF3 -> DI();
            case 0xF4 -> call(!getS());
            case 0xF5 -> {
                dec2SP();
                writeWord(reg_SP, (reg_A << 8) | reg_F);
            }
            case 0xF6 -> readByte(reg_PC, b -> {
                ALU8BitOr(b);
                incPC();
            });
            case 0xF7 -> rst(6);
            case 0xF8 -> ret(getS());
            case 0xF9 -> reg_SP = getHL();
            case 0xFA -> jp(getS());
            case 0xFB -> EI();
            case 0xFC -> call(getS());
            case 0xFD -> extendedFD();
            case 0xFE -> readByte(reg_PC, b -> {
                ALU8BitCp(b);
                incPC();
            });
            case 0xFF -> rst(7);
        }
    }

    /*
     * *****************************************************************************
     *
     * Extended Instruction area
     *
     * *****************************************************************************
     */
    /*
     * *****************************************************************************
     *
     * CB Bit twiddling and shifting instructions
     *
     * *****************************************************************************
     */
    private void extendedCB() {
        readByte(reg_PC, instructionByte -> {
            instruction = instructionByte;
            incPC();
            tStates = tStates + OPCODE_CB_STATES[instruction];
            // decode stage
            switch (instruction) {
                case 0x00 -> reg_B = shiftGenericRLC(reg_B);
                case 0x01 -> reg_C = shiftGenericRLC(reg_C);
                case 0x02 -> reg_D = shiftGenericRLC(reg_D);
                case 0x03 -> reg_E = shiftGenericRLC(reg_E);
                case 0x04 -> reg_H = shiftGenericRLC(reg_H);
                case 0x05 -> reg_L = shiftGenericRLC(reg_L);
                case 0x06 -> readByte(getHL(), b -> writeByte(getHL(), shiftGenericRLC(b)));
                case 0x07 -> reg_A = shiftGenericRLC(reg_A);
                case 0x08 -> reg_B = shiftGenericRRC(reg_B);
                case 0x09 -> reg_C = shiftGenericRRC(reg_C);
                case 0x0A -> reg_D = shiftGenericRRC(reg_D);
                case 0x0B -> reg_E = shiftGenericRRC(reg_E);
                case 0x0C -> reg_H = shiftGenericRRC(reg_H);
                case 0x0D -> reg_L = shiftGenericRRC(reg_L);
                case 0x0E -> readByte(getHL(), b -> writeByte(getHL(), shiftGenericRRC(b)));
                case 0x0F -> reg_A = shiftGenericRRC(reg_A);
                //
                case 0x10 -> reg_B = shiftGenericRL(reg_B);
                case 0x11 -> reg_C = shiftGenericRL(reg_C);
                case 0x12 -> reg_D = shiftGenericRL(reg_D);
                case 0x13 -> reg_E = shiftGenericRL(reg_E);
                case 0x14 -> reg_H = shiftGenericRL(reg_H);
                case 0x15 -> reg_L = shiftGenericRL(reg_L);
                case 0x16 -> readByte(getHL(), b -> writeByte(getHL(), shiftGenericRL(b)));
                case 0x17 -> reg_A = shiftGenericRL(reg_A);
                case 0x18 -> reg_B = shiftGenericRR(reg_B);
                case 0x19 -> reg_C = shiftGenericRR(reg_C);
                case 0x1A -> reg_D = shiftGenericRR(reg_D);
                case 0x1B -> reg_E = shiftGenericRR(reg_E);
                case 0x1C -> reg_H = shiftGenericRR(reg_H);
                case 0x1D -> reg_L = shiftGenericRR(reg_L);
                case 0x1E -> readByte(getHL(), b -> writeByte(getHL(), shiftGenericRR(b)));
                case 0x1F -> reg_A = shiftGenericRR(reg_A);
                //
                case 0x20 -> reg_B = shiftGenericSLA(reg_B);
                case 0x21 -> reg_C = shiftGenericSLA(reg_C);
                case 0x22 -> reg_D = shiftGenericSLA(reg_D);
                case 0x23 -> reg_E = shiftGenericSLA(reg_E);
                case 0x24 -> reg_H = shiftGenericSLA(reg_H);
                case 0x25 -> reg_L = shiftGenericSLA(reg_L);
                case 0x26 -> readByte(getHL(), b -> writeByte(getHL(), shiftGenericSLA(b)));
                case 0x27 -> reg_A = shiftGenericSLA(reg_A);
                case 0x28 -> reg_B = shiftGenericSRA(reg_B);
                case 0x29 -> reg_C = shiftGenericSRA(reg_C);
                case 0x2A -> reg_D = shiftGenericSRA(reg_D);
                case 0x2B -> reg_E = shiftGenericSRA(reg_E);
                case 0x2C -> reg_H = shiftGenericSRA(reg_H);
                case 0x2D -> reg_L = shiftGenericSRA(reg_L);
                case 0x2E -> readByte(getHL(), b -> writeByte(getHL(), shiftGenericSRA(b)));
                case 0x2F -> reg_A = shiftGenericSRA(reg_A);
                //
                // Undocumented SLL [0x30 to 0x37]. Instruction faulty, feeds in 1 to bit 0
                case 0x30 -> reg_B = shiftGenericSLL(reg_B);
                case 0x31 -> reg_C = shiftGenericSLL(reg_C);
                case 0x32 -> reg_D = shiftGenericSLL(reg_D);
                case 0x33 -> reg_E = shiftGenericSLL(reg_E);
                case 0x34 -> reg_H = shiftGenericSLL(reg_H);
                case 0x35 -> reg_L = shiftGenericSLL(reg_L);
                case 0x36 -> readByte(getHL(), b -> writeByte(getHL(), shiftGenericSLL(b)));
                case 0x37 -> reg_A = shiftGenericSLL(reg_A);
                //
                case 0x38 -> reg_B = shiftGenericSRL(reg_B);
                case 0x39 -> reg_C = shiftGenericSRL(reg_C);
                case 0x3A -> reg_D = shiftGenericSRL(reg_D);
                case 0x3B -> reg_E = shiftGenericSRL(reg_E);
                case 0x3C -> reg_H = shiftGenericSRL(reg_H);
                case 0x3D -> reg_L = shiftGenericSRL(reg_L);
                case 0x3E -> readByte(getHL(), b -> writeByte(getHL(), shiftGenericSRL(b)));
                case 0x3F -> reg_A = shiftGenericSRL(reg_A);
                //
                case 0x40 -> testBit(reg_B, 0);
                case 0x41 -> testBit(reg_C, 0);
                case 0x42 -> testBit(reg_D, 0);
                case 0x43 -> testBit(reg_E, 0);
                case 0x44 -> testBit(reg_H, 0);
                case 0x45 -> testBit(reg_L, 0);
                case 0x46 -> testBitInMemory(0);
                case 0x47 -> testBit(reg_A, 0);
                case 0x48 -> testBit(reg_B, 1);
                case 0x49 -> testBit(reg_C, 1);
                case 0x4A -> testBit(reg_D, 1);
                case 0x4B -> testBit(reg_E, 1);
                case 0x4C -> testBit(reg_H, 1);
                case 0x4D -> testBit(reg_L, 1);
                case 0x4E -> testBitInMemory(1);
                case 0x4F -> testBit(reg_A, 1);
                //
                case 0x50 -> testBit(reg_B, 2);
                case 0x51 -> testBit(reg_C, 2);
                case 0x52 -> testBit(reg_D, 2);
                case 0x53 -> testBit(reg_E, 2);
                case 0x54 -> testBit(reg_H, 2);
                case 0x55 -> testBit(reg_L, 2);
                case 0x56 -> testBitInMemory(2);
                case 0x57 -> testBit(reg_A, 2);
                case 0x58 -> testBit(reg_B, 3);
                case 0x59 -> testBit(reg_C, 3);
                case 0x5A -> testBit(reg_D, 3);
                case 0x5B -> testBit(reg_E, 3);
                case 0x5C -> testBit(reg_H, 3);
                case 0x5D -> testBit(reg_L, 3);
                case 0x5E -> testBitInMemory(3);
                case 0x5F -> testBit(reg_A, 3);
                //
                case 0x60 -> testBit(reg_B, 4);
                case 0x61 -> testBit(reg_C, 4);
                case 0x62 -> testBit(reg_D, 4);
                case 0x63 -> testBit(reg_E, 4);
                case 0x64 -> testBit(reg_H, 4);
                case 0x65 -> testBit(reg_L, 4);
                case 0x66 -> testBitInMemory(4);
                case 0x67 -> testBit(reg_A, 4);
                case 0x68 -> testBit(reg_B, 5);
                case 0x69 -> testBit(reg_C, 5);
                case 0x6A -> testBit(reg_D, 5);
                case 0x6B -> testBit(reg_E, 5);
                case 0x6C -> testBit(reg_H, 5);
                case 0x6D -> testBit(reg_L, 5);
                case 0x6E -> testBitInMemory(5);
                case 0x6F -> testBit(reg_A, 5);
                //
                case 0x70 -> testBit(reg_B, 6);
                case 0x71 -> testBit(reg_C, 6);
                case 0x72 -> testBit(reg_D, 6);
                case 0x73 -> testBit(reg_E, 6);
                case 0x74 -> testBit(reg_H, 6);
                case 0x75 -> testBit(reg_L, 6);
                case 0x76 -> testBitInMemory(6);
                case 0x77 -> testBit(reg_A, 6);
                case 0x78 -> testBit(reg_B, 7);
                case 0x79 -> testBit(reg_C, 7);
                case 0x7A -> testBit(reg_D, 7);
                case 0x7B -> testBit(reg_E, 7);
                case 0x7C -> testBit(reg_H, 7);
                case 0x7D -> testBit(reg_L, 7);
                case 0x7E -> testBitInMemory(7);
                case 0x7F -> testBit(reg_A, 7);
                //
                case 0x80 -> reg_B = reg_B & resetBit0;
                case 0x81 -> reg_C = reg_C & resetBit0;
                case 0x82 -> reg_D = reg_D & resetBit0;
                case 0x83 -> reg_E = reg_E & resetBit0;
                case 0x84 -> reg_H = reg_H & resetBit0;
                case 0x85 -> reg_L = reg_L & resetBit0;
                case 0x86 -> readByte(getHL(), b -> writeByte(getHL(), b & resetBit0));
                case 0x87 -> reg_A = reg_A & resetBit0;
                case 0x88 -> reg_B = reg_B & resetBit1;
                case 0x89 -> reg_C = reg_C & resetBit1;
                case 0x8A -> reg_D = reg_D & resetBit1;
                case 0x8B -> reg_E = reg_E & resetBit1;
                case 0x8C -> reg_H = reg_H & resetBit1;
                case 0x8D -> reg_L = reg_L & resetBit1;
                case 0x8E -> readByte(getHL(), b -> writeByte(getHL(), b & resetBit1));
                case 0x8F -> reg_A = reg_A & resetBit1;
                //
                case 0x90 -> reg_B = reg_B & resetBit2;
                case 0x91 -> reg_C = reg_C & resetBit2;
                case 0x92 -> reg_D = reg_D & resetBit2;
                case 0x93 -> reg_E = reg_E & resetBit2;
                case 0x94 -> reg_H = reg_H & resetBit2;
                case 0x95 -> reg_L = reg_L & resetBit2;
                case 0x96 -> readByte(getHL(), b -> writeByte(getHL(), b & resetBit2));
                case 0x97 -> reg_A = reg_A & resetBit2;
                case 0x98 -> reg_B = reg_B & resetBit3;
                case 0x99 -> reg_C = reg_C & resetBit3;
                case 0x9A -> reg_D = reg_D & resetBit3;
                case 0x9B -> reg_E = reg_E & resetBit3;
                case 0x9C -> reg_H = reg_H & resetBit3;
                case 0x9D -> reg_L = reg_L & resetBit3;
                case 0x9E -> readByte(getHL(), b -> writeByte(getHL(), b & resetBit3));
                case 0x9F -> reg_A = reg_A & resetBit3;
                //
                case 0xA0 -> reg_B = reg_B & resetBit4;
                case 0xA1 -> reg_C = reg_C & resetBit4;
                case 0xA2 -> reg_D = reg_D & resetBit4;
                case 0xA3 -> reg_E = reg_E & resetBit4;
                case 0xA4 -> reg_H = reg_H & resetBit4;
                case 0xA5 -> reg_L = reg_L & resetBit4;
                case 0xA6 -> readByte(getHL(), b -> writeByte(getHL(), b & resetBit4));
                case 0xA7 -> reg_A = reg_A & resetBit4;
                case 0xA8 -> reg_B = reg_B & resetBit5;
                case 0xA9 -> reg_C = reg_C & resetBit5;
                case 0xAA -> reg_D = reg_D & resetBit5;
                case 0xAB -> reg_E = reg_E & resetBit5;
                case 0xAC -> reg_H = reg_H & resetBit5;
                case 0xAD -> reg_L = reg_L & resetBit5;
                case 0xAE -> readByte(getHL(), b -> writeByte(getHL(), b & resetBit5));
                case 0xAF -> reg_A = reg_A & resetBit5;
                //
                case 0xB0 -> reg_B = reg_B & resetBit6;
                case 0xB1 -> reg_C = reg_C & resetBit6;
                case 0xB2 -> reg_D = reg_D & resetBit6;
                case 0xB3 -> reg_E = reg_E & resetBit6;
                case 0xB4 -> reg_H = reg_H & resetBit6;
                case 0xB5 -> reg_L = reg_L & resetBit6;
                case 0xB6 -> readByte(getHL(), b -> writeByte(getHL(), b & resetBit6));
                case 0xB7 -> reg_A = reg_A & resetBit6;
                case 0xB8 -> reg_B = reg_B & resetBit7;
                case 0xB9 -> reg_C = reg_C & resetBit7;
                case 0xBA -> reg_D = reg_D & resetBit7;
                case 0xBB -> reg_E = reg_E & resetBit7;
                case 0xBC -> reg_H = reg_H & resetBit7;
                case 0xBD -> reg_L = reg_L & resetBit7;
                case 0xBE -> readByte(getHL(), b -> writeByte(getHL(), b & resetBit7));
                case 0xBF -> reg_A = reg_A & resetBit7;
                //
                case 0xC0 -> reg_B = reg_B | setBit0;
                case 0xC1 -> reg_C = reg_C | setBit0;
                case 0xC2 -> reg_D = reg_D | setBit0;
                case 0xC3 -> reg_E = reg_E | setBit0;
                case 0xC4 -> reg_H = reg_H | setBit0;
                case 0xC5 -> reg_L = reg_L | setBit0;
                case 0xC6 -> readByte(getHL(), b -> writeByte(getHL(), b | setBit0));
                case 0xC7 -> reg_A = reg_A | setBit0;
                case 0xC8 -> reg_B = reg_B | setBit1;
                case 0xC9 -> reg_C = reg_C | setBit1;
                case 0xCA -> reg_D = reg_D | setBit1;
                case 0xCB -> reg_E = reg_E | setBit1;
                case 0xCC -> reg_H = reg_H | setBit1;
                case 0xCD -> reg_L = reg_L | setBit1;
                case 0xCE -> readByte(getHL(), b -> writeByte(getHL(), b | setBit1));
                case 0xCF -> reg_A = reg_A | setBit1;
                //
                case 0xD0 -> reg_B = reg_B | setBit2;
                case 0xD1 -> reg_C = reg_C | setBit2;
                case 0xD2 -> reg_D = reg_D | setBit2;
                case 0xD3 -> reg_E = reg_E | setBit2;
                case 0xD4 -> reg_H = reg_H | setBit2;
                case 0xD5 -> reg_L = reg_L | setBit2;
                case 0xD6 -> readByte(getHL(), b -> writeByte(getHL(), b | setBit2));
                case 0xD7 -> reg_A = reg_A | setBit2;
                case 0xD8 -> reg_B = reg_B | setBit3;
                case 0xD9 -> reg_C = reg_C | setBit3;
                case 0xDA -> reg_D = reg_D | setBit3;
                case 0xDB -> reg_E = reg_E | setBit3;
                case 0xDC -> reg_H = reg_H | setBit3;
                case 0xDD -> reg_L = reg_L | setBit3;
                case 0xDE -> readByte(getHL(), b -> writeByte(getHL(), b | setBit3));
                case 0xDF -> reg_A = reg_A | setBit3;
                //
                case 0xE0 -> reg_B = reg_B | setBit4;
                case 0xE1 -> reg_C = reg_C | setBit4;
                case 0xE2 -> reg_D = reg_D | setBit4;
                case 0xE3 -> reg_E = reg_E | setBit4;
                case 0xE4 -> reg_H = reg_H | setBit4;
                case 0xE5 -> reg_L = reg_L | setBit4;
                case 0xE6 -> readByte(getHL(), b -> writeByte(getHL(), b | setBit4));
                case 0xE7 -> reg_A = reg_A | setBit4;
                case 0xE8 -> reg_B = reg_B | setBit5;
                case 0xE9 -> reg_C = reg_C | setBit5;
                case 0xEA -> reg_D = reg_D | setBit5;
                case 0xEB -> reg_E = reg_E | setBit5;
                case 0xEC -> reg_H = reg_H | setBit5;
                case 0xED -> reg_L = reg_L | setBit5;
                case 0xEE -> readByte(getHL(), b -> writeByte(getHL(), b | setBit5));
                case 0xEF -> reg_A = reg_A | setBit5;
                //
                case 0xF0 -> reg_B = reg_B | setBit6;
                case 0xF1 -> reg_C = reg_C | setBit6;
                case 0xF2 -> reg_D = reg_D | setBit6;
                case 0xF3 -> reg_E = reg_E | setBit6;
                case 0xF4 -> reg_H = reg_H | setBit6;
                case 0xF5 -> reg_L = reg_L | setBit6;
                case 0xF6 -> readByte(getHL(), b -> writeByte(getHL(), b | setBit6));
                case 0xF7 -> reg_A = reg_A | setBit6;
                case 0xF8 -> reg_B = reg_B | setBit7;
                case 0xF9 -> reg_C = reg_C | setBit7;
                case 0xFA -> reg_D = reg_D | setBit7;
                case 0xFB -> reg_E = reg_E | setBit7;
                case 0xFC -> reg_H = reg_H | setBit7;
                case 0xFD -> reg_L = reg_L | setBit7;
                case 0xFE -> readByte(getHL(), b -> writeByte(getHL(), b | setBit7));
                case 0xFF -> reg_A = reg_A | setBit7;
            }
        });
    }

    private void extendedED() {
        readByte(reg_PC, instructionByte -> {
            instruction = instructionByte;
            incPC();
            tStates = tStates + OPCODE_ED_STATES[instruction];
            if ((instruction < 0x40) || (instruction >= 0xC0)) {
                // A does nothing operation, similar to NOP but not interrupt capable
                return;
            }
            switch (instruction) {
                case 0x40 -> inC(regCodeB);
                case 0x41 -> outC(regCodeB);
                case 0x42 -> ALU16BitSBC(regCodeBC);
                case 0x43 -> LDnnnnRegInd16Bit(regCodeBC);
                case 0x44 -> NEG();
                case 0x45 -> retn();
                case 0x46 -> IM(0);
                case 0x47 -> LDIA();
                case 0x48 -> inC(regCodeC);
                case 0x49 -> outC(regCodeC);
                case 0x4A -> ALU16BitADC(regCodeBC);
                case 0x4B -> LDRegnnnnInd16Bit(regCodeBC);
                case 0x4C -> NEG();
                case 0x4D -> reti();
                case 0x4E -> IM(0);
                case 0x4F -> LDRA();
                //
                case 0x50 -> inC(regCodeD);
                case 0x51 -> outC(regCodeD);
                case 0x52 -> ALU16BitSBC(regCodeDE);
                case 0x53 -> LDnnnnRegInd16Bit(regCodeDE);
                case 0x54 -> NEG();
                case 0x55 -> retn();
                case 0x56 -> IM(1);
                case 0x57 -> LDAI();
                case 0x58 -> inC(regCodeE);
                case 0x59 -> outC(regCodeE);
                case 0x5A -> ALU16BitADC(regCodeDE);
                case 0x5B -> LDRegnnnnInd16Bit(regCodeDE);
                case 0x5C -> NEG();
                case 0x5D -> retn();
                case 0x5E -> IM(2);
                case 0x5F -> LDAR();
                //
                case 0x60 -> inC(regCodeH);
                case 0x61 -> outC(regCodeH);
                case 0x62 -> ALU16BitSBC(regCodeHL);
                case 0x63 -> LDnnnnRegInd16Bit(regCodeHL);
                case 0x64 -> NEG();
                case 0x65 -> retn();
                case 0x66 -> IM(1);
                case 0x67 -> RRD();
                case 0x68 -> inC(regCodeL);
                case 0x69 -> outC(regCodeL);
                case 0x6A -> ALU16BitADC(regCodeHL);
                case 0x6B -> LDRegnnnnInd16Bit(regCodeHL);
                case 0x6C -> NEG();
                case 0x6D -> retn();
                case 0x6E -> IM(1);
                case 0x6F -> RLD();
                //
                case 0x70 -> inC(regCodeF);
                case 0x71 -> outC(regCodeF);
                case 0x72 -> ALU16BitSBC(regCodeSP);
                case 0x73 -> LDnnnnRegInd16Bit(regCodeSP);
                case 0x74 -> NEG();
                case 0x75 -> retn();
                case 0x76 -> IM(1);
                case 0x77 -> {
                } // NOP
                case 0x78 -> inC(regCodeA);
                case 0x79 -> outC(regCodeA);
                case 0x7A -> ALU16BitADC(regCodeSP);
                case 0x7B -> LDRegnnnnInd16Bit(regCodeSP);
                case 0x7C -> NEG();
                case 0x7D -> retn();
                case 0x7E -> IM(2);
                case 0x7F -> {
                } // NOP
                case 0x80, 0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88, 0x89, 0x8A, 0x8B, 0x8C, 0x8D, 0x8E, 0x8F -> {
                } // NOP
                case 0x90, 0x91, 0x92, 0x93, 0x94, 0x95, 0x96, 0x97, 0x98, 0x99, 0x9A, 0x9B, 0x9C, 0x9D, 0x9E, 0x9F -> {
                } // NOP
                case 0xA0 -> readByte(getHL(), this::LDI);
                case 0xA1 -> readByte(getHL(), this::CPI);
                case 0xA2 -> IORead(getBC(), this::INI);
                case 0xA3 -> readByte(getHL(), this::OUTI);
                case 0xA4, 0xA5, 0xA6, 0xA7 -> {
                } // NOP
                case 0xA8 -> readByte(getHL(), this::LDD);
                case 0xA9 -> readByte(getHL(), this::CPD);
                case 0xAA -> IORead(getBC(), this::IND);
                case 0xAB -> readByte(getHL(), this::OUTD);
                case 0xAC, 0xAD, 0xAE, 0xAF -> {
                } // NOP
                case 0xB0 -> LDIR();
                case 0xB1 -> CPIR();
                case 0xB2 -> INIR();
                case 0xB3 -> OTIR();
                case 0xB4, 0xB5, 0xB6, 0xB7 -> {
                } // NOP
                case 0xB8 -> LDDR();
                case 0xB9 -> CPDR();
                case 0xBA -> INDR();
                case 0xBB -> OTDR();
                case 0xBC, 0xBD, 0xBE, 0xBF -> {
                } // NOP
            }
        });
    }

    private int getIndexReg() {
        if (useIxReg) {
            return reg_IX;
        } else {
            return reg_IY;
        }
    }

    /* IX register processing */
    private void setIndexReg(int value) {
        if (useIxReg) {
            reg_IX = value;
        } else {
            reg_IY = value;
        }
    }

    private void extendedDD() {
        useIxReg = true;
        extendedDDFD();
    }

    /* IY register processing */
    private void extendedFD() {
        useIxReg = false;
        extendedDDFD();
    }

    /* generic index register processing */
    private void extendedDDFD() {
        readByte(reg_PC, instructionByte -> {
            instruction = instructionByte;
            incPC();
            tStates = tStates + OPCODE_DD_FD_STATES[instruction];
            int reg_index = getIndexReg();
            // primary decode stage
            switch (instruction) {
                case 0x00 -> {
                } // null
                case 0x01 -> readWord(reg_PC, word -> {
                    setBC(word);
                    inc2PC();
                }); // LD bc, nnnn
                case 0x02 -> writeByte(getBC(), reg_A); // LD (BC), A
                case 0x03 -> setBC(ALU16BitInc(getBC())); // inc BC
                case 0x04 -> reg_B = ALU8BitInc(reg_B); // inc b
                case 0x05 -> reg_B = ALU8BitDec(reg_B); // dec b
                case 0x06 -> readByte(reg_PC, b -> {
                    reg_B = b;
                    incPC();
                }); // ld b,nn
                case 0x07 -> RLCA(); // rlca
                case 0x08 -> EXAFAF(); // ex af,af'
                case 0x09 -> setIndexReg(ALU16BitAddIndexed(getBC()));
                case 0x0A -> readByte(getBC(), b -> reg_A = b); // LD a, (bc)
                case 0x0B -> setBC(ALU16BitDec(getBC())); // dec bc
                case 0x0C -> reg_C = ALU8BitInc(reg_C); // inc c
                case 0x0D -> reg_C = ALU8BitDec(reg_C); // dec c
                case 0x0E -> readByte(reg_PC, b -> {
                    reg_C = b;
                    incPC();
                }); // ld c,n
                case 0x0F -> RRCA(); // rrca
                case 0x10 -> djnz(); // djnz
                //
                case 0x11 -> readWord(reg_PC, word -> {
                    setDE(word);
                    inc2PC();
                }); // LD de, nnnn
                case 0x12 -> writeByte(getDE(), reg_A); // LD (de), A
                case 0x13 -> setDE(ALU16BitInc(getDE())); // inc de
                case 0x14 -> reg_D = ALU8BitInc(reg_D); // inc d
                case 0x15 -> reg_D = ALU8BitDec(reg_D); // dec d
                case 0x16 -> readByte(reg_PC, b -> {
                    reg_D = b;
                    incPC();
                }); // ld d,nn
                case 0x17 -> RLA(); // rla
                case 0x18 -> relativeJump(); // jr
                case 0x19 -> setIndexReg(ALU16BitAddIndexed(getDE()));
                case 0x1A -> readByte(getDE(), b -> reg_A = b); // LD a, (de)
                case 0x1B -> setDE(ALU16BitDec(getDE())); // dec de
                case 0x1C -> reg_E = ALU8BitInc(reg_E); // inc e
                case 0x1D -> reg_E = ALU8BitDec(reg_E); // dec e
                case 0x1E -> readByte(reg_PC, b -> {
                    reg_E = b;
                    incPC();
                }); // ld e,n
                case 0x1F -> RRA(); // rra
                //
                case 0x20 -> {
                    if (!getZ()) {
                        tStates = tStates + 12;
                        relativeJump();
                    } else {
                        incPC();
                        tStates = tStates + 7;
                    }
                } // jr nz
                case 0x21 -> readWord(reg_PC, word -> {
                    setIndexReg(word);
                    inc2PC();
                });
                case 0x22 -> readWord(reg_PC, word -> {
                    writeWord(word, reg_index);
                    inc2PC();
                });
                case 0x23 -> setIndexReg(ALU16BitInc(reg_index));
                case 0x24 -> {
                    int temp = reg_index >>> 8;
                    temp = ALU8BitInc(temp);
                    setIndexReg((reg_index & 0x00FF) | (temp << 8));
                } // inc IXh
                case 0x25 -> {
                    int temp = reg_index >>> 8;
                    temp = ALU8BitDec(temp);
                    setIndexReg((reg_index & 0x00FF) | (temp << 8));
                } // dec IXh
                case 0x26 -> readByte(reg_PC, b -> {
                    int temp = b << 8;
                    setIndexReg((reg_index & 0x00FF) | temp);
                    incPC();
                }); // ld IXh, nn
                case 0x27 -> DAA(); // daa
                case 0x28 -> {
                    if (getZ()) {
                        tStates = tStates + 12;
                        relativeJump();
                    } else {
                        incPC();
                        tStates = tStates + 7;
                    }
                } // jr z
                case 0x29 -> setIndexReg(ALU16BitAddIndexed(reg_index));
                case 0x2A -> readWord(reg_PC, fromPc -> readWord(fromPc, fromMem -> {
                    setIndexReg(fromMem);
                    inc2PC();
                }));
                case 0x2B -> setIndexReg(ALU16BitDec(reg_index));
                case 0x2C -> {
                    int temp = reg_index & 0x00FF;
                    temp = ALU8BitInc(temp);
                    setIndexReg((reg_index & 0xFF00) | temp);
                } // inc IXl
                case 0x2D -> {
                    int temp = reg_index & 0x00FF;
                    temp = ALU8BitDec(temp);
                    setIndexReg((reg_index & 0xFF00) | temp);
                } // dec IXl
                case 0x2E -> readByte(reg_PC, b -> {
                    setIndexReg((reg_index & 0xFF00) | b);
                    incPC();
                }); // ld IXl, nn
                case 0x2F -> CPL(); // rra
                //
                case 0x30 -> {
                    if (!getC()) {
                        tStates = tStates + 12;
                        relativeJump();
                    } else {
                        incPC();
                        tStates = tStates + 7;
                    }
                } // jr nc
                case 0x31 -> readWord(reg_PC, word -> {
                    reg_SP = word;
                    inc2PC();
                }); // LD sp, nnnn
                case 0x32 -> readWord(reg_PC, word -> {
                    writeByte(word, reg_A);
                    inc2PC();
                }); // LD (nnnn), A
                case 0x33 -> reg_SP = ALU16BitInc(reg_SP); // inc SP
                case 0x34 -> incIndex();
                case 0x35 -> decIndex();
                case 0x36 -> loadIndex8BitImmediate();
                case 0x37 -> SCF(); // scf
                case 0x38 -> {
                    if (getC()) {
                        tStates = tStates + 12;
                        relativeJump();
                    } else {
                        incPC();
                        tStates = tStates + 7;
                    }
                } // jr c
                case 0x39 -> setIndexReg(ALU16BitAddIndexed(reg_SP));
                case 0x3A -> readWord(reg_PC, fromPc -> readByte(fromPc, fromMem -> {
                    reg_A = fromMem;
                    inc2PC();
                })); // LD a, (nnnn)
                case 0x3B -> reg_SP = ALU16BitDec(reg_SP); // dec sp
                case 0x3C -> reg_A = ALU8BitInc(reg_A); // inc a
                case 0x3D -> reg_A = ALU8BitDec(reg_A); // dec a
                case 0x3E -> readByte(reg_PC, b -> {
                    reg_A = b;
                    incPC();
                }); // ld a,n
                case 0x3F -> CCF(); // ccf
                //
                case 0x40 -> { /* reg_B = reg_B; */
                } // ld b, b
                case 0x41 -> reg_B = reg_C; // ld b, c
                case 0x42 -> reg_B = reg_D; // ld b, d
                case 0x43 -> reg_B = reg_E; // ld b, e
                case 0x44 -> getIndexAddressUndocumented(regCodeIXH, b -> reg_B = b); // ld b, IXh
                case 0x45 -> getIndexAddressUndocumented(regCodeIXL, b -> reg_B = b); // ld b, IXl
                case 0x46 -> get8BitRegisterIndexed(regCodeM, b -> reg_B = b); // ld b, (ix+dd)
                case 0x47 -> reg_B = reg_A; // ld b, a
                case 0x48 -> reg_C = reg_B; // ld c, b
                case 0x49 -> { /* reg_C = reg_C; */
                } // ld c, c
                case 0x4A -> reg_C = reg_D; // ld c, d
                case 0x4B -> reg_C = reg_E; // ld c, e
                case 0x4C -> getIndexAddressUndocumented(regCodeIXH, b -> reg_C = b); // ld c, IXh
                case 0x4D -> getIndexAddressUndocumented(regCodeIXL, b -> reg_C = b); // ld c, IXl
                case 0x4E -> get8BitRegisterIndexed(regCodeM, b -> reg_C = b); // ld c, (ix+dd)
                case 0x4F -> reg_C = reg_A; // ld c a
                //
                case 0x50 -> reg_D = reg_B; // ld d, b
                case 0x51 -> reg_D = reg_C; // ld d, c
                case 0x52 -> { /* reg_D = reg_D; */
                } // ld d, d
                case 0x53 -> reg_D = reg_E; // ld d, e
                case 0x54 -> getIndexAddressUndocumented(regCodeIXH, b -> reg_D = b); // ld d, IXh
                case 0x55 -> getIndexAddressUndocumented(regCodeIXL, b -> reg_D = b); // ld d, IXl
                case 0x56 -> get8BitRegisterIndexed(regCodeM, b -> reg_D = b); // ld d, (ix+dd)
                case 0x57 -> reg_D = reg_A; // ld d, a
                case 0x58 -> reg_E = reg_B; // ld e, b
                case 0x59 -> reg_E = reg_C; // ld e, c
                case 0x5A -> reg_E = reg_D; // ld e, d
                case 0x5B -> { /* reg_E = reg_E; */
                } // ld e, e
                case 0x5C -> getIndexAddressUndocumented(regCodeIXH, b -> reg_E = b); // ld e, IXh
                case 0x5D -> getIndexAddressUndocumented(regCodeIXL, b -> reg_E = b); // ld e, IXl
                case 0x5E -> get8BitRegisterIndexed(regCodeM, b -> reg_E = b); // ld e, (ix+dd)
                case 0x5F -> reg_E = reg_A; // ld e a
                //
                case 0x60 -> setIndexAddressUndocumented(reg_B, regCodeIXH); // ld ixh, b
                case 0x61 -> setIndexAddressUndocumented(reg_C, regCodeIXH); // ld ixh, c
                case 0x62 -> setIndexAddressUndocumented(reg_D, regCodeIXH); // ld ixh, d
                case 0x63 -> setIndexAddressUndocumented(reg_E, regCodeIXH); // ld ixh, e
                case 0x64 -> getIndexAddressUndocumented(regCodeIXH, b -> setIndexAddressUndocumented(b, regCodeIXH)); // ld ixh, IXh
                case 0x65 -> getIndexAddressUndocumented(regCodeIXL, b -> setIndexAddressUndocumented(b, regCodeIXH)); // ld ixh, IXl
                case 0x66 -> get8BitRegisterIndexed(regCodeM, b -> reg_H = b); // ld h, (ix+dd)
                case 0x67 -> setIndexAddressUndocumented(reg_A, regCodeIXH); // ld ixh, a
                case 0x68 -> setIndexAddressUndocumented(reg_B, regCodeIXL); // ld ixl, b
                case 0x69 -> setIndexAddressUndocumented(reg_C, regCodeIXL); // ld ixl, c
                case 0x6A -> setIndexAddressUndocumented(reg_D, regCodeIXL); // ld ixl, d
                case 0x6B -> setIndexAddressUndocumented(reg_E, regCodeIXL); // ld ixl, e
                case 0x6C -> getIndexAddressUndocumented(regCodeIXH, b -> setIndexAddressUndocumented(b, regCodeIXL)); // ld ixl, IXh
                case 0x6D -> getIndexAddressUndocumented(regCodeIXL, b -> setIndexAddressUndocumented(b, regCodeIXL)); // ld ixl, IXl
                case 0x6E -> get8BitRegisterIndexed(regCodeM, b -> reg_L = b); // ld l, (ix+dd)
                case 0x6F -> setIndexAddressUndocumented(reg_A, regCodeIXL); // ld ixl, a
                //
                case 0x70 -> setIndexAddressUndocumented(reg_B, regCodeM); // ld (ix+d), b
                case 0x71 -> setIndexAddressUndocumented(reg_C, regCodeM); // ld (ix+d), c
                case 0x72 -> setIndexAddressUndocumented(reg_D, regCodeM); // ld (ix+d), d
                case 0x73 -> setIndexAddressUndocumented(reg_E, regCodeM); // ld (ix+d), e
                case 0x74 -> get8BitRegisterIndexed(regCodeH, b -> setIndexAddressUndocumented(b, regCodeM)); // ld (ix+d), IXh
                case 0x75 -> get8BitRegisterIndexed(regCodeL, b -> setIndexAddressUndocumented(b, regCodeM)); // ld (ix+d), IXl
                case 0x76 -> {
                    decPC(); // execute it forever !
                    halt = true;
                }
                case 0x77 -> get8BitRegisterIndexed(regCodeA, b -> setIndexAddressUndocumented(b, regCodeM)); // ld (ix+d), a
                case 0x78 -> reg_A = reg_B; // ld a, b
                case 0x79 -> reg_A = reg_C; // ld a, c
                case 0x7A -> reg_A = reg_D; // ld a, d
                case 0x7B -> reg_A = reg_E; // ld a, e
                case 0x7C -> getIndexAddressUndocumented(regCodeIXH, b -> reg_A = b); // ld a, IXh
                case 0x7D -> getIndexAddressUndocumented(regCodeIXL, b -> reg_A = b); // ld a, IXl
                case 0x7E -> get8BitRegisterIndexed(regCodeM, b -> reg_A = b); // ld a, (ix+dd)
                case 0x7F -> { /* reg_A = reg_A; */
                } // ld a,a
                // add
                case 0x80 -> ALU8BitAdd(reg_B);
                case 0x81 -> ALU8BitAdd(reg_C);
                case 0x82 -> ALU8BitAdd(reg_D);
                case 0x83 -> ALU8BitAdd(reg_E);
                case 0x84 -> ALU8BitAdd((reg_index & 0xFF00) >>> 8); // IXh
                case 0x85 -> ALU8BitAdd(reg_index & 0x00FF); // IXy
                case 0x86 -> getIndexAddressUndocumented(regCodeM, this::ALU8BitAdd); // CP (IX+dd)
                case 0x87 -> ALU8BitAdd(reg_A);
                // adc
                case 0x88 -> ALU8BitAdc(reg_B);
                case 0x89 -> ALU8BitAdc(reg_C);
                case 0x8A -> ALU8BitAdc(reg_D);
                case 0x8B -> ALU8BitAdc(reg_E);
                case 0x8C -> ALU8BitAdc((reg_index & 0xFF00) >>> 8); // IXh
                case 0x8D -> ALU8BitAdc(reg_index & 0x00FF); // IXy
                case 0x8E -> getIndexAddressUndocumented(regCodeM, this::ALU8BitAdc); // CP (IX+dd)
                case 0x8F -> ALU8BitAdc(reg_A);
                // sub
                case 0x90 -> ALU8BitSub(reg_B);
                case 0x91 -> ALU8BitSub(reg_C);
                case 0x92 -> ALU8BitSub(reg_D);
                case 0x93 -> ALU8BitSub(reg_E);
                case 0x94 -> ALU8BitSub((reg_index & 0xFF00) >>> 8); // IXh
                case 0x95 -> ALU8BitSub(reg_index & 0x00FF); // IXy
                case 0x96 -> getIndexAddressUndocumented(regCodeM, this::ALU8BitSub); // CP (IX+dd)
                case 0x97 -> ALU8BitSub(reg_A);
                // sbc
                case 0x98 -> ALU8BitSbc(reg_B);
                case 0x99 -> ALU8BitSbc(reg_C);
                case 0x9A -> ALU8BitSbc(reg_D);
                case 0x9B -> ALU8BitSbc(reg_E);
                case 0x9C -> ALU8BitSbc((reg_index & 0xFF00) >>> 8); // IXh
                case 0x9D -> ALU8BitSbc(reg_index & 0x00FF); // IXy
                case 0x9E -> getIndexAddressUndocumented(regCodeM, this::ALU8BitSbc); // CP (IX+dd)
                case 0x9F -> ALU8BitSbc(reg_A);
                // and
                case 0xA0 -> ALU8BitAnd(reg_B);
                case 0xA1 -> ALU8BitAnd(reg_C);
                case 0xA2 -> ALU8BitAnd(reg_D);
                case 0xA3 -> ALU8BitAnd(reg_E);
                case 0xA4 -> ALU8BitAnd((reg_index & 0xFF00) >>> 8); // IXh
                case 0xA5 -> ALU8BitAnd(reg_index & 0x00FF); // IXy
                case 0xA6 -> getIndexAddressUndocumented(regCodeM, this::ALU8BitAnd); // CP (IX+dd)
                case 0xA7 -> ALU8BitAnd(reg_A);
                // xor
                case 0xA8 -> ALU8BitXor(reg_B);
                case 0xA9 -> ALU8BitXor(reg_C);
                case 0xAA -> ALU8BitXor(reg_D);
                case 0xAB -> ALU8BitXor(reg_E);
                case 0xAC -> ALU8BitXor((reg_index & 0xFF00) >>> 8); // IXh
                case 0xAD -> ALU8BitXor(reg_index & 0x00FF); // IXy
                case 0xAE -> getIndexAddressUndocumented(regCodeM, this::ALU8BitXor); // CP (IX+dd)
                case 0xAF -> ALU8BitXor(reg_A);
                // or
                case 0xB0 -> ALU8BitOr(reg_B);
                case 0xB1 -> ALU8BitOr(reg_C);
                case 0xB2 -> ALU8BitOr(reg_D);
                case 0xB3 -> ALU8BitOr(reg_E);
                case 0xB4 -> ALU8BitOr((reg_index & 0xFF00) >>> 8); // IXh
                case 0xB5 -> ALU8BitOr(reg_index & 0x00FF); // IXy
                case 0xB6 -> getIndexAddressUndocumented(regCodeM, this::ALU8BitOr); // CP (IX+dd)
                case 0xB7 -> ALU8BitOr(reg_A);
                // cp
                case 0xB8 -> ALU8BitCp(reg_B);
                case 0xB9 -> ALU8BitCp(reg_C);
                case 0xBA -> ALU8BitCp(reg_D);
                case 0xBB -> ALU8BitCp(reg_E);
                case 0xBC -> ALU8BitCp((reg_index & 0xFF00) >>> 8); // IXh
                case 0xBD -> ALU8BitCp(reg_index & 0x00FF); // IXy
                case 0xBE -> getIndexAddressUndocumented(regCodeM, this::ALU8BitCp); // CP (IX+dd)
                case 0xBF -> ALU8BitCp(reg_A);
                //
                case 0xC0 -> ret(!getZ());
                case 0xC1 -> readWord(reg_SP, word -> {
                    setBC(word);
                    inc2SP();
                });
                case 0xC2 -> jp(!getZ());
                case 0xC3 -> jp();
                case 0xC4 -> call(!getZ());
                case 0xC5 -> {
                    dec2SP();
                    writeWord(reg_SP, getBC());
                }
                case 0xC6 -> readByte(reg_PC, b -> {
                    ALU8BitAdd(b);
                    incPC();
                });
                case 0xc7 -> rst(0);
                case 0xC8 -> ret(getZ());
                case 0xC9 -> ret();
                case 0xCA -> jp(getZ());
                case 0xCB -> extendedIndexCB();
                case 0xCC -> call(getZ());
                case 0xCD -> call();
                case 0xCE -> readByte(reg_PC, b -> {
                    ALU8BitAdc(b);
                    incPC();
                });
                case 0xCF -> rst(1);
                //
                case 0xD0 -> ret(!getC());
                case 0xD1 -> readWord(reg_SP, word -> {
                    setDE(word);
                    inc2SP();
                });
                case 0xD2 -> jp(!getC());
                case 0xD3 -> outNA();
                case 0xD4 -> call(!getC());
                case 0xD5 -> {
                    dec2SP();
                    writeWord(reg_SP, getDE());
                }
                case 0xD6 -> readByte(reg_PC, b -> {
                    ALU8BitSub(b);
                    incPC();
                });
                case 0xD7 -> rst(2);
                case 0xD8 -> ret(getC());
                case 0xD9 -> EXX();
                case 0xDA -> jp(getC());
                case 0xDB -> inAN();
                case 0xDC -> call(getC());
                case 0xDD -> extendedDD();
                case 0xDE -> readByte(reg_PC, b -> {
                    ALU8BitSbc(b);
                    incPC();
                });
                case 0xDF -> rst(3);
                //
                case 0xE0 -> ret(!getPV());
                case 0xE1 -> readWord(reg_SP, word -> {
                    setIndexReg(word);
                    inc2SP();
                }); // pop ix
                case 0xE2 -> jp(!getPV());
                case 0xE3 -> EXSPIndex(); // ex (sp),ix
                case 0xE4 -> call(!getPV());
                case 0xE5 -> {
                    dec2SP();
                    writeWord(reg_SP, reg_index);
                } // push ix
                case 0xE6 -> readByte(reg_PC, b -> {
                    ALU8BitAnd(b);
                    incPC();
                });
                case 0xE7 -> rst(4);
                case 0xE8 -> ret(getPV());
                case 0xE9 -> reg_PC = reg_index; // jp (ix)
                case 0xEA -> jp(getPV());
                case 0xEB -> EXDEHL();
                case 0xEC -> call(getPV());
                case 0xED -> extendedED();
                case 0xEE -> readByte(reg_PC, b -> {
                    ALU8BitXor(b);
                    incPC();
                });
                case 0xEF -> rst(5);
                //
                case 0xF0 -> ret(!getS());
                case 0xF1 -> readWord(reg_SP, word -> {
                    inc2SP();
                    reg_F = (word & lsb);
                    reg_A = ((word & msb) >> 8);
                });
                case 0xF2 -> jp(!getS());
                case 0xF3 -> DI();
                case 0xF4 -> call(!getS());
                case 0xF5 -> {
                    dec2SP();
                    writeWord(reg_SP, (reg_A << 8) | reg_F);
                }
                case 0xF6 -> readByte(reg_PC, b -> {
                    ALU8BitOr(b);
                    incPC();
                });
                case 0xF7 -> rst(6);
                case 0xF8 -> ret(getS());
                case 0xF9 -> reg_SP = reg_index; // ld sp,ix
                case 0xFA -> jp(getS());
                case 0xFB -> EI();
                case 0xFC -> call(getS());
                case 0xFD -> extendedFD();
                case 0xFE -> readByte(reg_PC, b -> {
                    ALU8BitCp(b);
                    incPC();
                });
                case 0xFF -> rst(7);
            }
        });
    }

    /*
     * *****************************************************************************
     *
     * CB Bit twiddling and shifting instructions for Index
     *
     * *****************************************************************************
     */
    private void extendedIndexCB() {
        readByte(reg_PC + 1/* fudge for DD CB dd ii */, instructionByte -> {
            instruction = instructionByte;
            tStates = tStates + OPCODE_INDEXED_CB_STATES[instruction];
            //
            var r = instruction & 0x07;
            switch (instruction) {
                case 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 -> shiftRLCIndexed(r);
                case 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F -> shiftRRCIndexed(r);
                case 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17 -> shiftRLIndexed(r);
                case 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F -> shiftRRIndexed(r);
                case 0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27 -> shiftSLAIndexed(r);
                case 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F -> shiftSRAIndexed(r);
                case 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37 -> shiftSLLIndexed(r);
                case 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x3E, 0x3F -> shiftSRLIndexed(r);
                //
                case 0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47 -> testIndexBit(0);
                case 0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x4F -> testIndexBit(1);
                case 0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57 -> testIndexBit(2);
                case 0x58, 0x59, 0x5A, 0x5B, 0x5C, 0x5D, 0x5E, 0x5F -> testIndexBit(3);
                case 0x60, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67 -> testIndexBit(4);
                case 0x68, 0x69, 0x6A, 0x6B, 0x6C, 0x6D, 0x6E, 0x6F -> testIndexBit(5);
                case 0x70, 0x71, 0x72, 0x73, 0x74, 0x75, 0x76, 0x77 -> testIndexBit(6);
                case 0x78, 0x79, 0x7A, 0x7B, 0x7C, 0x7D, 0x7E, 0x7F -> testIndexBit(7);
                //
                case 0x80, 0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87 -> bitIndexReset(0, r);
                case 0x88, 0x89, 0x8A, 0x8B, 0x8C, 0x8D, 0x8E, 0x8F -> bitIndexReset(1, r);
                case 0x90, 0x91, 0x92, 0x93, 0x94, 0x95, 0x96, 0x97 -> bitIndexReset(2, r);
                case 0x98, 0x99, 0x9A, 0x9B, 0x9C, 0x9D, 0x9E, 0x9F -> bitIndexReset(3, r);
                case 0xA0, 0xA1, 0xA2, 0xA3, 0xA4, 0xA5, 0xA6, 0xA7 -> bitIndexReset(4, r);
                case 0xA8, 0xA9, 0xAA, 0xAB, 0xAC, 0xAD, 0xAE, 0xAF -> bitIndexReset(5, r);
                case 0xB0, 0xB1, 0xB2, 0xB3, 0xB4, 0xB5, 0xB6, 0xB7 -> bitIndexReset(6, r);
                case 0xB8, 0xB9, 0xBA, 0xBB, 0xBC, 0xBD, 0xBE, 0xBF -> bitIndexReset(7, r);
                //
                case 0xC0, 0xC1, 0xC2, 0xC3, 0xC4, 0xC5, 0xC6, 0xC7 -> bitIndexSet(0, r);
                case 0xC8, 0xC9, 0xCA, 0xCB, 0xCC, 0xCD, 0xCE, 0xCF -> bitIndexSet(1, r);
                case 0xD0, 0xD1, 0xD2, 0xD3, 0xD4, 0xD5, 0xD6, 0xD7 -> bitIndexSet(2, r);
                case 0xD8, 0xD9, 0xDA, 0xDB, 0xDC, 0xDD, 0xDE, 0xDF -> bitIndexSet(3, r);
                case 0xE0, 0xE1, 0xE2, 0xE3, 0xE4, 0xE5, 0xE6, 0xE7 -> bitIndexSet(4, r);
                case 0xE8, 0xE9, 0xEA, 0xEB, 0xEC, 0xED, 0xEE, 0xEF -> bitIndexSet(5, r);
                case 0xF0, 0xF1, 0xF2, 0xF3, 0xF4, 0xF5, 0xF6, 0xF7 -> bitIndexSet(6, r);
                case 0xF8, 0xF9, 0xFA, 0xFB, 0xFC, 0xFD, 0xFE, 0xFF -> bitIndexSet(7, r);
            }
            incPC();
        });
    }

    /*
     * return an 8 bit register based on its code 000 -> 111
     */
    private int get8BitRegister(int reg) {
        return switch (reg) {
            case 0 -> reg_B;
            // B
            case 1 -> reg_C;
            // C
            case 2 -> reg_D;
            // D
            case 3 -> reg_E;
            // E
            case 4 -> reg_H;
            // H
            case 5 -> reg_L;
            // L
            case 7 -> reg_A;
            // F
            default -> 0;
        };
    }

    /*
     * set an 8 bit register based on its code 000 -> 111
     */
    private void set8BitRegister(int reg, int v) {
        switch (reg) {
            case 0 -> reg_B = v;
            // B
            case 1 -> reg_C = v;
            // C
            case 2 -> reg_D = v;
            // D
            case 3 -> reg_E = v;
            // E
            case 4 -> reg_H = v;
            // H
            case 5 -> reg_L = v;
            // L
            case 7 -> reg_A = v;
            // F
            default -> {
            }
        }
    }

    /*
     * return a 16 bit register based on its code 00 -> 11
     */
    private int get16BitRegister(int reg) {
        return switch (reg) {
            case 0 -> getBC();
            case 1 -> getDE();
            case 2 -> getHL();
            default -> reg_SP;
        };
    }

    /*
     * set a 16 bit register based on its code 00 -> 11
     */
    private void set16BitRegister(int value, int reg) {
        switch (reg) {
            case 0 -> setBC(value);
            case 1 -> setDE(value);
            case 2 -> setHL(value);
            default -> reg_SP = value;
        }
    }

    /*
     * increment (and wrap) the program counter
     */
    private void incPC() {
        reg_PC++;
        reg_PC = reg_PC & MAX_ADDRESS;
    }

    /*
     * ALU Operations
     */

    private void decPC() {
        reg_PC--;
        reg_PC = reg_PC & MAX_ADDRESS;
    }

    private void inc2PC() {
        reg_PC = reg_PC + 2;
        reg_PC = reg_PC & MAX_ADDRESS;
    }

    private void dec2PC() {
        reg_PC = reg_PC - 2;
        reg_PC = reg_PC & MAX_ADDRESS;
    }

    /*
     * increment / decrement (and wrap) the stack pointer
     */
    private void inc2SP() {
        reg_SP = reg_SP + 2;
        reg_SP = reg_SP & MAX_ADDRESS;
    }

    /* half carry flag control */

    private void dec2SP() {
        reg_SP = reg_SP - 2;
        reg_SP = reg_SP & MAX_ADDRESS;
    }

    /* half carry flag control */
    private void setHalfCarryFlagAdd(int left, int right, int carry) {
        left = left & 0x000F;
        right = right & 0x000F;
        setH((right + left + carry) > 0x0F);
    }

    /* half carry flag control */
    private void setHalfCarryFlagAdd(int left, int right) {
        left = left & 0x000F;
        right = right & 0x000F;
        setH((right + left) > 0x0F);
    }

    /* half carry flag control */
    private void setHalfCarryFlagSub(int left, int right) {
        left = left & 0x000F;
        right = right & 0x000F;
        setH(left < right);
    }

    /* half carry flag control */
    private void setHalfCarryFlagSub(int left, int right, int carry) {
        left = left & 0x000F;
        right = right & 0x000F;
        setH(left < (right + carry));
    }

    /* 2's compliment overflow flag control */
    private void setOverflowFlagAdd(int left, int right, int carry) {
        if (left > 127) {
            left = left - 256;
        }
        if (right > 127) {
            right = right - 256;
        }
        left = left + right + carry;
        setPV((left < -128) || (left > 127));
    }

    /* 2's compliment overflow flag control */
    private void setOverflowFlagAdd(int left, int right) {
        setOverflowFlagAdd(left, right, 0);
    }

    /* 2's compliment overflow flag control */
    private void setOverflowFlagAdd16(int left, int right, int carry) {
        if (left > 32767) {
            left = left - 65536;
        }
        if (right > 32767) {
            right = right - 65536;
        }
        left = left + right + carry;
        setPV((left < -32768) || (left > 32767));
    }

    /* 2's compliment overflow flag control */
    private void setOverflowFlagSub(int left, int right, int carry) {
        if (left > 127) {
            left = left - 256;
        }
        if (right > 127) {
            right = right - 256;
        }
        left = left - right - carry;
        setPV((left < -128) || (left > 127));
    }

    /* 2's compliment overflow flag control */
    private void setOverflowFlagSub(int left, int right) {
        setOverflowFlagSub(left, right, 0);
    }

    /* 2's compliment overflow flag control */
    private void setOverflowFlagSub16(int left, int right, int carry) {
        if (left > 32767) {
            left = left - 65536;
        }
        if (right > 32767) {
            right = right - 65536;
        }
        left = left - right - carry;
        setPV((left < -32768) || (left > 32767));
    }

    /* 8 bit ADD */
    private void ALU8BitAdd(int value) {
        int local_reg_A = reg_A;
        setHalfCarryFlagAdd(local_reg_A, value);
        setOverflowFlagAdd(local_reg_A, value);
        local_reg_A = local_reg_A + value;
        setS((local_reg_A & 0x0080) != 0);
        setC((local_reg_A & 0xFF00) != 0);
        local_reg_A = local_reg_A & 0x00FF;
        setZ(local_reg_A == 0);
        resetN();
        reg_A = local_reg_A;
        setUnusedFlags(reg_A);
    }

    /* 8 bit ADC */
    private void ALU8BitAdc(int value) {
        int local_reg_A = reg_A;
        int carry;
        if (getC()) {
            carry = 1;
        } else {
            carry = 0;
        }
        setHalfCarryFlagAdd(local_reg_A, value, carry);
        setOverflowFlagAdd(local_reg_A, value, carry);
        local_reg_A = local_reg_A + value + carry;
        setS((local_reg_A & 0x0080) != 0);
        setC((local_reg_A & 0xFF00) != 0);
        local_reg_A = local_reg_A & 0x00FF;
        setZ(local_reg_A == 0);
        resetN();
        reg_A = local_reg_A;
        setUnusedFlags(reg_A);
    }

    /* 8 bit SUB */
    private void ALU8BitSub(int value) {
        int local_reg_A = reg_A;
        setHalfCarryFlagSub(local_reg_A, value);
        setOverflowFlagSub(local_reg_A, value);
        local_reg_A = local_reg_A - value;
        setS((local_reg_A & 0x0080) != 0);
        setC((local_reg_A & 0xFF00) != 0);
        local_reg_A = local_reg_A & 0x00FF;
        setZ(local_reg_A == 0);
        setN();
        reg_A = local_reg_A;
        setUnusedFlags(reg_A);
    }

    /* 8 bit SBC */
    private void ALU8BitSbc(int value) {
        int local_reg_A = reg_A;
        int carry;
        if (getC()) {
            carry = 1;
        } else {
            carry = 0;
        }
        setHalfCarryFlagSub(local_reg_A, value, carry);
        setOverflowFlagSub(local_reg_A, value, carry);
        local_reg_A = local_reg_A - value - carry;
        setS((local_reg_A & 0x0080) != 0);
        setC((local_reg_A & 0xFF00) != 0);
        local_reg_A = local_reg_A & 0x00FF;
        setZ(local_reg_A == 0);
        setN();
        reg_A = local_reg_A;
        setUnusedFlags(reg_A);
    }

    /* 8 bit AND (version II) */
    private void ALU8BitAnd(int value) {
        reg_F = 0x10; // set the H flag
        reg_A = reg_A & value;
        setS((reg_A & 0x0080) != 0);
        setZ(reg_A == 0);
        setPV(PARITY_TABLE[reg_A]);
        setUnusedFlags(reg_A);
    }

    /* 8 bit OR (Version II) */
    private void ALU8BitOr(int value) {
        reg_F = 0;
        reg_A = reg_A | value;
        setS((reg_A & 0x0080) != 0);
        setZ(reg_A == 0);
        setPV(PARITY_TABLE[reg_A]);
        setUnusedFlags(reg_A);
    }

    /* 8 bit XOR (Version II) */
    private void ALU8BitXor(int value) {
        reg_F = 0;
        reg_A = reg_A ^ value;
        setS((reg_A & 0x0080) != 0);
        setZ(reg_A == 0);
        setPV(PARITY_TABLE[reg_A]);
        setUnusedFlags(reg_A);
    }

    /* 8 bit CP */
    private void ALU8BitCp(int b) {
        final int a = reg_A;
        final int wans = a - b;
        final int ans = wans & 0xff;
        reg_F = 0x02;
        setS((ans & flag_S) != 0);
        set3((b & flag_3) != 0);
        set5((b & flag_5) != 0);
        setZ(ans == 0);
        setC((wans & 0x100) != 0);
        setH((((a & 0x0F) - (b & 0x0F)) & flag_H) != 0);
        setPV(((a ^ b) & (a ^ ans) & 0x80) != 0);
    }

    /* 8 bit INC */
    private int ALU8BitInc(int value) {
        setHalfCarryFlagAdd(value, 1);
        // setOverflowFlagAdd(value, 1);
        setPV(value == 0x7F);
        value++;
        setS((value & 0x0080) != 0);
        value = value & 0x00FF;
        setZ(value == 0);
        resetN();
        setUnusedFlags(value);
        return (value);
    }

    /* 8 bit DEC */
    private int ALU8BitDec(int value) {
        setHalfCarryFlagSub(value, 1);
        // setOverflowFlagSub(value, 1);
        setPV(value == 0x80);
        value--;
        setS((value & 0x0080) != 0);
        value = value & 0x00FF;
        setZ(value == 0);
        setN();
        setUnusedFlags(value);
        return (value);
    }

    /* 16 bit INC */
    private int ALU16BitInc(int value) {
        value++;
        return (value & lsw);
    }

    /* 16 bit DEC */
    private int ALU16BitDec(int value) {
        value--;
        return (value & lsw);
    }

    /* 16 bit ADD */
    private int ALU16BitAdd(int value) {
        int result = getHL() + value; // ADD HL,rr
        resetN(); // N = 0;
        //
        int temp = (getHL() & 0x0FFF) + (value & 0x0FFF);
        if ((temp & 0xF000) != 0) {
            setH();
        } else {
            resetH();
        }
        // temp = result >> 8;
        if ((result & 0x0800) != 0) {
            set3();
        } else {
            reset3();
        }
        if ((result & 0x2000) != 0) {
            set5();
        } else {
            reset5();
        }
        //
        if (result > lsw) // overflow ?
        {
            setC();
            return (result & lsw);
        } else {
            resetC();
            return result;
        }
    }

    /* 16 bit ADD */
    private int ALU16BitAddIndexed(int value) {
        int reg_index = getIndexReg();
        int result = reg_index + value; // ADD IX,rr
        resetN(); // N = 0;
        int temp = (reg_index & 0x0FFF) + (value & 0x0FFF);
        if ((temp & 0xF000) != 0) {
            setH();
        } else {
            resetH();
        }
        // temp = result >> 8;
        if ((result & 0x0800) != 0) {
            set3();
        } else {
            reset3();
        }
        if ((result & 0x2000) != 0) {
            set5();
        } else {
            reset5();
        }
        //
        if (result > lsw) // overflow ?
        {
            setC();
            return (result & lsw);
        } else {
            resetC();
            return result;
        }
    }

    /* 16 bit ADC */
    private void ALU16BitADC(int regCode) {
        int a = getHL();
        int b = get16BitRegister((byte) regCode);
        int c = getC() ? 1 : 0;
        int lans = a + b + c;
        int ans = lans & 0xffff;
        setS((ans & (flag_S << 8)) != 0);
        set3((ans & (0x08 << 8)) != 0);
        set5((ans & (0x20 << 8)) != 0);
        setZ(ans == 0);
        setC(lans > 0xFFFF);
        // setPV( ((a ^ b) & (a ^ ans) & 0x8000)!=0 );
        setOverflowFlagAdd16(a, b, c);
        if ((((a & 0x0Fff) + (b & 0x0Fff) + c) & 0x1000) != 0) {
            setH();
        } else {
            resetH();
        }
        resetN();
        setHL(ans);
    }

    /* 16 bit SBC */
    private void ALU16BitSBC(int regCode) {
        int a = getHL();
        int b = get16BitRegister((byte) regCode);
        int c = getC() ? 1 : 0;
        int lans = a - b - c;
        int ans = lans & 0xffff;
        setS((ans & (flag_S << 8)) != 0);
        set3((ans & (0x08 << 8)) != 0);
        set5((ans & (0x20 << 8)) != 0);
        setZ(ans == 0);
        setC(lans < 0);
        // setPV( ((a ^ b) & (a ^ ans) & 0x8000)!=0 );
        setOverflowFlagSub16(a, b, c);
        if ((((a & 0x0Fff) - (b & 0x0Fff) - c) & 0x1000) != 0) {
            setH();
        } else {
            resetH();
        }
        setN();
        setHL(ans);
    }

    /*
     * varous register swap operations
     */
    private void EXAFAF() {
        int temp;
        temp = reg_A;
        reg_A = reg_A_ALT;
        reg_A_ALT = temp;
        temp = reg_F;
        reg_F = reg_F_ALT;
        reg_F_ALT = temp;
    }

    private void EXDEHL() {
        int temp = getHL();
        setHL(getDE());
        setDE(temp);
    }

    private void EXSPHL() {
        int temp = getHL();
        readWord(reg_SP, word -> {
            setHL(word);
            inc2SP();
            dec2SP();
            writeWord(reg_SP, temp);
        });
    }

    private void EXX() {
        int temp;
        temp = getBC();
        setBC(getBC_ALT());
        setBC_ALT(temp);
        temp = getDE();
        setDE(getDE_ALT());
        setDE_ALT(temp);
        temp = getHL();
        setHL(getHL_ALT());
        setHL_ALT(temp);
    }

    /*
     * test & set flag states
     */
    private boolean getS() {
        return ((reg_F & flag_S) != 0);
    }

    private void setS(boolean b) {
        if (b) {
            setS();
        } else {
            resetS();
        }
    }

    private boolean getZ() {
        return ((reg_F & flag_Z) != 0);
    }

    private void setZ(boolean b) {
        if (b) {
            setZ();
        } else {
            resetZ();
        }
    }

    private boolean getH() {
        return ((reg_F & flag_H) != 0);
    }

    private void setH(boolean b) {
        if (b) {
            setH();
        } else {
            resetH();
        }
    }

    private boolean getPV() {
        return ((reg_F & flag_PV) != 0);
    }

    private void setPV(boolean b) {
        if (b) {
            setPV();
        } else {
            resetPV();
        }
    }

    private boolean getN() {
        return ((reg_F & flag_N) != 0);
    }

    private boolean getC() {
        return ((reg_F & flag_C) != 0);
    }

    // private void setN(boolean b) { if (b) setN(); else resetN(); }
    private void setC(boolean b) {
        if (b) {
            setC();
        } else {
            resetC();
        }
    }

    private void setS() {
        reg_F = reg_F | flag_S;
    }

    private void setZ() {
        reg_F = reg_F | flag_Z;
    }

    private void set5() {
        reg_F = reg_F | flag_5;
    }

    private void setH() {
        reg_F = reg_F | flag_H;
    }

    private void set3() {
        reg_F = reg_F | flag_3;
    }

    private void setPV() {
        reg_F = reg_F | flag_PV;
    }

    private void setN() {
        reg_F = reg_F | flag_N;
    }

    private void setC() {
        reg_F = reg_F | flag_C;
    }

    private void set5(boolean b) {
        if (b) {
            set5();
        } else {
            reset5();
        }
    }

    private void set3(boolean b) {
        if (b) {
            set3();
        } else {
            reset3();
        }
    }

    private void setUnusedFlags(int value) {
        value = value & 0x28;
        reg_F = reg_F & 0xD7;
        reg_F = reg_F | value;
    }

    private void flipC() {
        reg_F = reg_F ^ flag_C;
    }

    private void resetS() {
        reg_F = reg_F & flag_S_N;
    }

    private void resetZ() {
        reg_F = reg_F & flag_Z_N;
    }

    private void reset5() {
        reg_F = reg_F & flag_5_N;
    }

    private void resetH() {
        reg_F = reg_F & flag_H_N;
    }

    private void reset3() {
        reg_F = reg_F & flag_3_N;
    }

    private void resetPV() {
        reg_F = reg_F & flag_PV_N;
    }

    private void resetN() {
        reg_F = reg_F & flag_N_N;
    }

    private void resetC() {
        reg_F = reg_F & flag_C_N;
    }

    private int getR() {
        return reg_R & 0x7F + reg_R8;
    }

    private void setR(int r) {
        reg_R = r; // internally reg_R is unbounded
        reg_R8 = r & 0x80;
    }

    private int getBC() {
        return (reg_B << 8) + reg_C;
    }

    private void setBC(int bc) {
        reg_B = (bc & 0xFF00) >> 8;
        reg_C = bc & 0x00FF;
    }

    private int getDE() {
        return (reg_D << 8) + reg_E;
    }

    private void setDE(int de) {
        reg_D = (de & 0xFF00) >> 8;
        reg_E = de & 0x00FF;
    }

    private int getHL() {
        return (reg_H << 8) + reg_L;
    }

    private void setHL(int hl) {
        reg_H = (hl & 0xFF00) >> 8;
        reg_L = hl & 0x00FF;
    }

    private int getBC_ALT() {
        return (reg_B_ALT << 8) + reg_C_ALT;
    }

    /*
     * shifts and rotates
     */

    private void setBC_ALT(int bc) {
        reg_B_ALT = (bc & 0xFF00) >> 8;
        reg_C_ALT = bc & 0x00FF;
    }

    private int getDE_ALT() {
        return (reg_D_ALT << 8) + reg_E_ALT;
    }

    private void setDE_ALT(int de) {
        reg_D_ALT = (de & 0xFF00) >> 8;
        reg_E_ALT = de & 0x00FF;
    }

    private int getHL_ALT() {
        return (reg_H_ALT << 8) + reg_L_ALT;
    }

    private void setHL_ALT(int hl) {
        reg_H_ALT = (hl & 0xFF00) >> 8;
        reg_L_ALT = hl & 0x00FF;
    }

    private void RLCA() {
        boolean carry = (reg_A & 0x0080) != 0;
        reg_A = ((reg_A << 1) & 0x00FF);
        if (carry) {
            setC();
            reg_A = (reg_A | 0x0001);
        } else {
            resetC();
        }
        resetH();
        resetN();
        setUnusedFlags(reg_A);
    }

    private void RLA() {
        boolean carry = (reg_A & 0x0080) != 0;
        reg_A = ((reg_A << 1) & 0x00FF);
        if (getC()) {
            reg_A = reg_A | 0x01;
        }
        if (carry) {
            setC();
        } else {
            resetC();
        }
        resetH();
        resetN();
        setUnusedFlags(reg_A);
    }

    private void RRCA() {
        boolean carry = (reg_A & 0x0001) != 0;
        reg_A = (reg_A >> 1);
        if (carry) {
            setC();
            reg_A = (reg_A | 0x0080);
        } else {
            resetC();
        }
        resetH();
        resetN();
        setUnusedFlags(reg_A);
    }

    private void RRA() {
        boolean carry = (reg_A & 0x01) != 0;
        reg_A = (reg_A >> 1);
        if (getC()) {
            reg_A = (reg_A | 0x0080);
        }
        if (carry) {
            setC();
        } else {
            resetC();
        }
        resetH();
        resetN();
        setUnusedFlags(reg_A);
    }

    private void CPL() {
        reg_A = reg_A ^ 0x00FF;
        setH();
        setN();
        setUnusedFlags(reg_A);
    }

    private void NEG() { // ToDo - improve
        setHalfCarryFlagSub(0, reg_A, 0);
        // if ((value & 0x0F) == 0x00) setH(); else resetH();
        setOverflowFlagSub(0, reg_A, 0);
        // if (value == 0x80) setPV(); else resetPV();
        reg_A = -reg_A;
        if ((reg_A & 0xFF00) != 0) {
            setC();
        } else {
            resetC();
        }
        setN();
        reg_A = reg_A & 0x00FF;
        if (reg_A == 0) {
            setZ();
        } else {
            resetZ();
        }
        if ((reg_A & 0x0080) != 0) {
            setS();
        } else {
            resetS();
        }
        setUnusedFlags(reg_A);
    }

    private void SCF() {
        setC();
        resetH();
        resetN();
        setUnusedFlags(reg_A);
    }

    private void CCF() {
        if (getC()) {
            setH();
        } else {
            resetH();
        }
        flipC();
        resetN();
        setUnusedFlags(reg_A);
    }

    /*
     * DAA is weird, can't find Zilog algorithm so using +0110 if Nibble>9 algorithm.
     */
    private void DAA() {
        int ans = reg_A;
        int incr = 0;
        boolean carry = getC();
        if ((getH()) || ((ans & 0x0F) > 0x09)) {
            incr = 0x06;
        }
        if (carry || (ans > 0x9f) || ((ans > 0x8f) && ((ans & 0x0F) > 0x09))) {
            incr |= 0x60;
        }
        if (ans > 0x99) {
            carry = true;
        }
        if (getN()) {
            ALU8BitSub(incr); // sub_a(incr);
        } else {
            ALU8BitAdd(incr); // add_a(incr);
        }
        if (carry) {
            setC();
        } else {
            resetC();
        }
        setPV(PARITY_TABLE[reg_A]);
    }

    private int shiftGenericRLC(int temp) {
        temp = temp << 1;
        if ((temp & 0x0FF00) != 0) {
            setC();
            temp = temp | 0x01;
        } else {
            resetC();
        }
        // standard flag updates
        if ((temp & flag_S) == 0) {
            resetS();
        } else {
            setS();
        }
        if ((temp & 0x00FF) == 0) {
            setZ();
        } else {
            resetZ();
        }
        resetH();
        resetN();
        // put value back
        temp = temp & 0x00FF;
        setPV(PARITY_TABLE[temp]);
        setUnusedFlags(temp);
        return temp;
    }

    /**
     * very odd instructions
     * RLC  (IX+nn), followed by LD rr,(IX+nn), but not if rr = 6
     */
    private void shiftRLCIndexed(int reg) {
        getIndexAddress(address -> readByte(address, b -> {
            int regValue = shiftGenericRLC(b);
            writeByte(address, regValue);
            //
            if (reg != 6) { // (rr)
                set8BitRegister(reg, regValue);
            }
            //
            reg_R++;
        }));
    }

    private int shiftGenericRL(int temp) {
        // do shift operation
        temp = temp << 1;
        if (getC()) {
            temp = temp | 0x01;
        }
        // standard flag updates
        setS((temp & 0x0080) != 0);
        if ((temp & 0x0FF00) == 0) {
            resetC();
        } else {
            setC();
        }
        temp = temp & lsb;
        if ((temp & 0x00FF) == 0) {
            setZ();
        } else {
            resetZ();
        }
        setPV(PARITY_TABLE[temp]);
        resetH();
        resetN();
        // put value back
        setUnusedFlags(temp);
        return temp;
    }

    private void shiftRLIndexed(int reg) {
        getIndexAddress(address -> readByte(address, b -> {
            var regValue = shiftGenericRL(b);
            writeByte(address, regValue);
            //
            if (reg != 6) { // (rr)
                set8BitRegister(reg, regValue);
            }
            //
            reg_R++;
        }));
    }

    private int shiftGenericRRC(int temp) {
        // do shift operation
        setC((temp & 0x0001) != 0);
        temp = temp >> 1;
        if (getC()) {
            temp = temp | 0x80;
        }
        // standard flag updates
        setS((temp & 0x0080) != 0);
        if (temp == 0) {
            setZ();
        } else {
            resetZ();
        }
        resetH();
        setPV(PARITY_TABLE[temp]);
        resetN();
        // put value back
        setUnusedFlags(temp);
        return temp;
    }

    private void shiftRRCIndexed(int reg) {
        getIndexAddress(address -> readByte(address, b -> {
            int regValue = shiftGenericRRC(b);
            writeByte(address, regValue);
            //
            if (reg != 6) { // (rr)
                set8BitRegister(reg, regValue);
            }
            //
            reg_R++;
        }));
    }

    private int shiftGenericRR(int temp) {
        boolean tempC;
        // do shift operation
        tempC = getC();
        setC((temp & 0x0001) != 0);
        temp = temp >> 1;
        if (tempC) {
            temp = temp | 0x80;
        }
        // standard flag updates
        setS((temp & 0x0080) != 0);
        if (temp == 0) {
            setZ();
        } else {
            resetZ();
        }
        resetH();
        setPV(PARITY_TABLE[temp]);
        resetN();
        // put value back
        setUnusedFlags(temp);
        return temp;
    }

    private void shiftRRIndexed(int reg) {
        getIndexAddress(address -> readByte(address, b -> {
            int regValue = shiftGenericRR(b);
            writeByte(address, regValue);
            //
            if (reg != 6) { // (rr)
                set8BitRegister(reg, regValue);
            }
            //
            reg_R++;
        }));
    }

    private int shiftGenericSLA(int temp) {
        // do shift operation
        temp = temp << 1;
        // standard flag updates
        setS((temp & 0x0080) != 0);
        if ((temp & 0x00FF) == 0) {
            setZ();
        } else {
            resetZ();
        }
        resetH();
        if ((temp & 0x0FF00) != 0) {
            setC();
        } else {
            resetC();
        }
        temp = temp & 0x00FF;
        setPV(PARITY_TABLE[temp]);
        resetN();
        // put value back
        setUnusedFlags(temp);
        return temp;
    }

    private void shiftSLAIndexed(int reg) {
        getIndexAddress(address -> readByte(address, b -> {
            var regValue = shiftGenericSLA(b);
            writeByte(address, regValue);
            //
            if (reg != 6) { // (rr)
                set8BitRegister(reg, regValue);
            }
            //
            reg_R++;
        }));
    }

    /**
     * Note: This implements the broken (and undocumented) SLL instructions. Faulty as it feeds in a one into bit 0
     *
     * @param temp Register value
     * @return Incorrect SLL value
     */
    private int shiftGenericSLL(int temp) {
        // do shift operation
        temp = (temp << 1) | 0x01; // the fault
        // standard flag updates
        setS((temp & 0x0080) != 0);
        resetZ();
        resetH();
        if ((temp & 0x0FF00) != 0) {
            setC();
        } else {
            resetC();
        }
        temp = temp & 0x00FF;
        setPV(PARITY_TABLE[temp]);
        resetN();
        // put value back
        setUnusedFlags(temp);
        return temp;
    }

    private void shiftSLLIndexed(int reg) {
        getIndexAddress(address -> readByte(address, b -> {
            var regValue = shiftGenericSLL(b);
            writeByte(address, regValue);
            //
            if (reg != 6) { // (rr)
                set8BitRegister(reg, regValue);
            }
            //
            reg_R++;
        }));
    }

    private int shiftGenericSRA(int temp) {
        // do shift operation
        setC((temp & 0x0001) != 0);
        if ((temp & 0x0080) == 0) {
            temp = temp >> 1;
            resetS();
        } else {
            temp = (temp >> 1) | 0x0080;
            setS();
        }
        // standard flag updates
        if (temp == 0) {
            setZ();
        } else {
            resetZ();
        }
        resetH();
        setPV(PARITY_TABLE[temp]);
        resetN();
        setUnusedFlags(temp);
        return temp;
    }

    private void shiftSRAIndexed(int reg) {
        getIndexAddress(address -> readByte(address, b -> {
            var regValue = shiftGenericSRA(b);
            writeByte(address, regValue);
            //
            if (reg != 6) { // (rr)
                set8BitRegister(reg, regValue);
            }
            //
            reg_R++;
        }));
    }

    private int shiftGenericSRL(int temp) {
        // do shift operation
        setC((temp & 0x0001) != 0);
        temp = temp >> 1;
        // standard flag updates
        resetS();
        setZ(temp == 0);
        resetH();
        setPV(PARITY_TABLE[temp]);
        resetN();
        // put value back
        setUnusedFlags(temp);
        return temp;
    }

    private void shiftSRLIndexed(int reg) {
        getIndexAddress(address -> readByte(address, b -> {
            var regValue = shiftGenericSRL(b);
            writeByte(address, regValue);
            //
            if (reg != 6) { // (rr)
                set8BitRegister(reg, regValue);
            }
            //
            reg_R++;
        }));
    }

    private void RRD() {
        readByte(getHL(), b -> {
            reg_R++;
            int nibble1 = (reg_A & 0x00F0) >> 4;
            int nibble2 = reg_A & 0x000F;
            int nibble3 = (b & 0x00F0) >> 4;
            int nibble4 = b & 0x000F;
            //
            reg_A = (nibble1 << 4) | nibble4;
            b = (nibble2 << 4) | nibble3;
            //
            writeByte(getHL(), b);
            // standard flag updates
            if ((reg_A & 0x80) == 0) {
                resetS();
            } else {
                setS();
            }
            setZ(reg_A == 0);
            resetH();
            setPV(PARITY_TABLE[reg_A]);
            resetN();
            setUnusedFlags(reg_A);
        });
    }

    private void RLD() {
        readByte(getHL(), b -> {
            reg_R++;
            int nibble1 = (reg_A & 0x00F0) >> 4;
            int nibble2 = reg_A & 0x000F;
            int nibble3 = (b & 0x00F0) >> 4;
            int nibble4 = b & 0x000F;
            //
            reg_A = (nibble1 << 4) | nibble3;
            b = (nibble4 << 4) | nibble2;
            //
            writeByte(getHL(), b);
            // standard flag updates
            if ((reg_A & 0x80) == 0) {
                resetS();
            } else {
                setS();
            }
            if (reg_A == 0) {
                setZ();
            } else {
                resetZ();
            }
            resetH();
            setPV(PARITY_TABLE[reg_A]);
            resetN();
            setUnusedFlags(reg_A);
        });
    }

    /*
     * calls, jumps and returns + associated stack operations
     */
    private void relativeJump() {
        reg_R++;
        readByte(reg_PC, offset -> {
            if (offset > 0x007F) {
                offset = offset - 0x0100;
            }
            reg_PC++;
            reg_PC = (reg_PC + offset) & MAX_ADDRESS;
        });
    }

    private void djnz() {
        int local_B = getBC() & msb;
        local_B = local_B - 256; // ( 1 * 2**8) - saves a shift >> 8
        setBC((getBC() & lsb) | (local_B & msb));
        if (local_B != 0) {
            tStates = tStates + 13;
            relativeJump();
        } else {
            tStates = tStates + 8;
            incPC();
        }
    }

    private void jp(boolean cc) {
        tStates = tStates + 10;
        if (cc) {
            readWord(reg_PC, word -> reg_PC = word);
        } else {
            inc2PC();
        }
    }

    private void jp() {
        tStates = tStates + 10;
        readWord(reg_PC, word -> reg_PC = word);
    }

    private void ret(boolean cc) {
        if (cc) {
            readWord(reg_SP, word -> {
                reg_PC = word;
                inc2SP();
                tStates = tStates + 11;
            });
        } else {
            tStates = tStates + 5;
        }
    }

    private void ret() {
        tStates = tStates + 10;
        readWord(reg_SP, word -> {
            reg_PC = word;
            inc2SP();
        });
    }

    private void retn() {
        readWord(reg_SP, word -> {
            reg_PC = word;
            inc2SP();
            IFF1 = IFF2;
        });
    }

    private void reti() {
        readWord(reg_SP, word -> {
            reg_PC = word;
            inc2SP();
        });
    }

    /*
     * IO port handling
     */

    private void call(boolean cc) {
        if (cc) {
            call();
        } else {
            tStates = tStates + 10;
            inc2PC();
        }
    }

    private void call() {
        readWord(reg_PC, word -> {
            tStates = tStates + 17;
            int destination = word;
            inc2PC();
            dec2SP();
            writeWord(reg_SP, reg_PC);
            reg_PC = destination;
        });
    }

    private void rst(int code) {
        tStates = tStates + 11;
        dec2SP();
        writeWord(reg_SP, reg_PC);
        switch (code) {
            case 0 -> reg_PC = 0x0000;
            case 1 -> reg_PC = 0x0008;
            case 2 -> reg_PC = 0x0010;
            case 3 -> reg_PC = 0x0018;
            case 4 -> reg_PC = 0x0020;
            case 5 -> reg_PC = 0x0028;
            case 6 -> reg_PC = 0x0030;
            default -> reg_PC = 0x0038;
        }
    }

    /*
     * Interrupt handling
     */
    private void DI() {
        IFF1 = false;
        IFF2 = false; // load both
        EIDIFlag = true;
    }

    private void EI() {
        IFF1 = true;
        IFF2 = true; // load both
        EIDIFlag = true;
    }

    /*
     * bit manipulation
     */

    /* IN A,(NN) */
    private void inAN() {
        getInOutAddressRegA(address -> IORead(address, b -> {
            reg_A = b;
            incPC();
            reg_R++;
        }));
    }

    /* OUT (NN),A */
    private void outNA() {
        getInOutAddressRegA(address -> {
            IOWrite(address, reg_A);
            incPC();
            reg_R++;
        });
    }

    private void getInOutAddressRegA(Consumer<Integer> consumer) {
        // high order address bits from A reg - for IN,OUT A
        readByte(reg_PC, b -> consumer.accept((reg_A << 8) + b));
    }

    /* IN rr,(c) */
    private void inC(int reg) {
        IORead(getBC(), b -> {
            // set8BitRegister( temp, reg );
            switch (reg) {
                case 0 -> reg_B = b; // B
                case 1 -> reg_C = b; // C
                case 2 -> reg_D = b; // D
                case 3 -> reg_E = b; // E
                case 4 -> reg_H = b; // H
                case 5 -> reg_L = b; // L
                case 7 -> reg_A = b; // A
                case 6 -> {
                    // Does nothing, just affects flags
                }
            }
            if ((b & 0x0080) == 0) {
                resetS();
            } else {
                setS();
            }
            if (b == 0) {
                setZ();
            } else {
                resetZ();
            }
            if (PARITY_TABLE[b]) {
                setPV();
            } else {
                resetPV();
            }
            resetN();
            resetH();
        });
    }

    /* OUT (rr),c */
    private void outC(int reg) {
        IOWrite(getBC(), get8BitRegister(reg));
    }

    private void testBit(int v, int bit) {
        //
        resetS();
        set3((v & 0x08) != 0);
        set5((v & 0x20) != 0);
        v = switch (bit) {
            case 0 -> v & setBit0;
            case 1 -> v & setBit1;
            case 2 -> v & setBit2;
            case 3 -> v & setBit3;
            case 4 -> v & setBit4;
            case 5 -> v & setBit5;
            case 6 -> v & setBit6;
            default -> {
                var result = v & setBit7;
                setS(result != 0);
                yield result;
            }
        };
        setZ(0 == v);
        setPV(0 == v);
        resetN();
        setH();
    }

    private void testBitInMemory(int bit) {
        readByte(getHL(), b -> testBitGeneric(bit, b));
    }

    private void testBitGeneric(int bit, int v) {
        resetS();
        v = switch (bit) {
            case 0 -> v & setBit0;
            case 1 -> v & setBit1;
            case 2 -> v & setBit2;
            case 3 -> v & setBit3;
            case 4 -> v & setBit4;
            case 5 -> v & setBit5;
            case 6 -> v & setBit6;
            default -> {
                var result = v & setBit7;
                setS(result != 0);
                yield result;
            }
        };
        setZ(0 == v);
        setPV(0 == v);
        resetN();
        setH();
    }

    /*
     * Increment / decrement repeat type instructions
     */
    /* loads */
    private void LDI(int value) {
        reg_R++;
        writeByte(getDE(), value);
        setDE(ALU16BitInc(getDE()));
        setHL(ALU16BitInc(getHL()));
        setBC(ALU16BitDec(getBC()));
        resetH();
        resetN();
        setPV(getBC() != 0);
        int temp = value + reg_A;
        if ((temp & 0x02) == 0) {
            reset5();
        } else {
            set5();
        }
        if ((temp & 0x08) == 0) {
            reset3();
        } else {
            set3();
        }
    }

    private void LDIR() {
        tStates = tStates + 21;
        readByte(getHL(), b -> {
            LDI(b);
            if (getBC() != 0) {
                LDIR();
            }
        });
    }

    private void LDD(int value) {
        reg_R++;
        writeByte(getDE(), value);
        //
        setDE(ALU16BitDec(getDE()));
        setHL(ALU16BitDec(getHL()));
        setBC(ALU16BitDec(getBC()));
        resetH();
        resetN();
        setPV(getBC() != 0);
        int temp = reg_A + value;
        if ((temp & 0x02) == 0) {
            reset5();
        } else {
            set5();
        }
        if ((temp & 0x08) == 0) {
            reset3();
        } else {
            set3();
        }
    }

    private void LDDR() {
        readByte(getHL(), b -> {
            tStates = tStates + 21;
            LDD(b);
            if (getBC() != 0) {
                LDDR();
            }
        });
    }

    /*
     * block compares
     */
    private void CPI(int value) {
        reg_R++;
        int result = (reg_A - value) & lsb;
        setHL(ALU16BitInc(getHL()));
        setBC(ALU16BitDec(getBC()));
        //
        setS((result & 0x80) != 0);
        setZ(result == 0);
        setHalfCarryFlagSub(reg_A, value);
        setPV(getBC() != 0);
        setN();
        //
        if (getH()) {
            result--;
        }
        if ((result & 0x00002) == 0) {
            reset5();
        } else {
            set5();
        }
        if ((result & 0x00008) == 0) {
            reset3();
        } else {
            set3();
        }
    }

    private void CPIR() {
        tStates = tStates + 21;
        readByte(getHL(), b -> {
            CPI(b);
            if (!getZ() && (getBC() != 0)) {
                dec2PC();
            }
        });
    }

    private void CPD(int value) {
        reg_R++;
        int result = (reg_A - value) & lsb;
        setHL(ALU16BitDec(getHL()));
        setBC(ALU16BitDec(getBC()));
        //
        setS((result & 0x80) != 0);
        setZ(result == 0);
        setHalfCarryFlagSub(reg_A, value);
        setPV(getBC() != 0);
        setN();
        //
        if (getH()) {
            result--;
        }
        if ((result & 0x02) == 0) {
            reset5();
        } else {
            set5();
        }
        if ((result & 0x08) == 0) {
            reset3();
        } else {
            set3();
        }
    }

    private void CPDR() {
        readByte(getHL(), b -> {
            tStates = tStates + 21;
            CPD(b);
            if (!getZ() && (getBC() != 0)) {
                dec2PC();
            }
        });
    }

    /* block IO */
    private void INI(int value) {
        writeByte(getHL(), value);
        reg_B = (reg_B - 1) & lsb;
        setHL(ALU16BitInc(getHL()));
        setZ(reg_B == 0);
        setN();
    }

    private void INIR() {
        IORead(getBC(), b -> {
            tStates = tStates + 21;
            INI(b);
            if (!getZ()) {
                dec2PC();
            }
        });
    }

    private void IND(int value) {
        writeByte(getHL(), value);
        reg_B = (reg_B - 1) & lsb;
        setHL(ALU16BitDec(getHL()));
        setZ(reg_B == 0);
        setN();
    }

    private void INDR() {
        IORead(getBC(), b -> {
            tStates = tStates + 21;
            IND(b);
            if (!getZ()) {
                dec2PC();
            }
        });
    }

    private void OUTI(int value) {
        IOWrite(getBC(), value);
        reg_R++;
        reg_B = (reg_B - 1) & lsb;
        setHL(ALU16BitInc(getHL()));
        setZ(reg_B == 0);
        setN();
    }

    /*
     * odds & ends
     */

    private void OTIR() {
        readByte(getHL(), b -> {
            tStates = tStates + 21;
            OUTI(b);
            if (!getZ()) {
                dec2PC();
            }
        });
    }

    private void OUTD(int value) {
        IOWrite(getBC(), value);
        reg_R++;
        reg_B = (reg_B - 1) & lsb;
        setHL(ALU16BitDec(getHL()));
        setZ(reg_B == 0);
        setN();
    }

    private void OTDR() {
        readByte(getHL(), b -> {
            tStates = tStates + 21;
            OUTD(b);
            if (!getZ()) {
                dec2PC();
            }
        });
    }

    /*
     * special R reg loads
     */

    /*
     * extended 16 bit loads for ED instructions
     */
    private void LDRegnnnnInd16Bit(int regCode) {
        readWord(reg_PC, address -> readWord(address, data -> {
            set16BitRegister(data, regCode);
            inc2PC();
        }));
    }

    private void LDnnnnRegInd16Bit(int regCode) {
        readWord(reg_PC, address -> {
            writeWord(address, get16BitRegister(regCode));
            inc2PC();
        });
    }
    //
    //
    // Index IX & IY register special instructions
    //
    //

    private void IM(int mode) {
        // interruptMode = mode;
    }

    /*
     * special I reg loads
     */
    private void LDAI() {
        reg_A = reg_I;
        setS((reg_A & flag_S) != 0);
        setZ(reg_A == 0);
        resetH();
        resetN();
        setPV(IFF2);
        setUnusedFlags(reg_A);
    }

    private void LDIA() {
        reg_I = reg_A;
    }

    private void LDAR() {
        reg_A = getR();
        resetS();
        setZ(reg_A == 0);
        resetH();
        resetN();
        setPV(IFF2);
        setUnusedFlags(reg_A);
    }

    private void LDRA() {
        setR(reg_A);
    }

    /**
     * Get the index value, make signed as its two's compliment.
     * pass to consumer The index register offset value in the range -128...+127
     */
    private void getIndexOffset(Consumer<Integer> consumer) {
        reg_R++;
        readByte(reg_PC, b -> {
            int index = b;
            incPC();
            if (index > 0x007F) {
                consumer.accept(index - 256);
            } else {
                consumer.accept(index);
            }
        });
    }

    private void getIndexAddress(Consumer<Integer> consumer) {
        getIndexOffset(offset -> consumer.accept((getIndexReg() + offset) & lsw));
    }

    /*
     * Support for 8 bit index register manipulation (IX as IXH IXL)
     */
    private void getIndexAddressUndocumented(int reg, Consumer<Integer> consumer) {
        int reg_index = getIndexReg();
        getIndexOffset(offset -> {
            switch (reg) {
                case 4 -> consumer.accept((reg_index & msb) >> 8); // IXH
                case 5 -> consumer.accept(reg_index & lsb); // IXL
                default -> {
                    reg_R++;
                    readByte((reg_index + offset) & lsw, consumer);
                } // (index+dd)
            }
        });
    }

    /*
     * Support for 8 bit index register manipulation (IX as IXH IXL)
     */
    private void setIndexAddressUndocumented(int value, int reg) {
        int reg_index = getIndexReg();
        switch (reg) {
            case 4 -> {
                setIndexReg(reg_index & lsb);
                setIndexReg(reg_index | (value << 8));
            } // IXH
            case 5 -> {
                setIndexReg(reg_index & msb);
                setIndexReg(reg_index | value);
            } // IXL
            default -> getIndexAddress(address -> writeByte(address, value)); // (index+dd)
        }
    }

    /*
     * return an 8 bit register based on its code 000 -> 111
     */
    private void get8BitRegisterIndexed(int reg, Consumer<Integer> consumer) {
        switch (reg) {
            case 4 -> consumer.accept(reg_H); // H
            case 5 -> consumer.accept(reg_L); // L
            case 7 -> consumer.accept(reg_A); // A
            default -> {
                reg_R++;
                getIndexAddress(address -> readByte(address, consumer));
            } // (index+dd)
        }
    }

    /* inc / dec (index+dd) */
    private void incIndex() {
        getIndexAddress(address -> readByte(address, b -> {
            int data = ALU8BitInc(b);
            reg_R++;
            writeByte(address, data);
        }));
    }

    private void decIndex() {
        getIndexAddress(address -> readByte(address, b -> {
            int data = ALU8BitDec(b);
            reg_R++;
            writeByte(address, data);
        }));
    }

    /* index register swap */
    private void EXSPIndex() {
        int temp = getIndexReg();
        readWord(reg_SP, word -> {
            setIndexReg(word);
            inc2SP();
            dec2SP();
            writeWord(reg_SP, temp);
        });
    }

    /* indexed CB bit twiddling */
    private void testIndexBit(int bit) {
        reg_R++;
        getIndexAddress(address -> readByte(address, b ->
                // check the bit position
                testBitGeneric(bit, b)));
    }

    private void bitIndexSet(int bit, int reg) {
        reg_R++;
        getIndexAddress(address -> readByte(address, b -> {
            b = switch (bit) {
                case 0 -> b | setBit0;
                case 1 -> b | setBit1;
                case 2 -> b | setBit2;
                case 3 -> b | setBit3;
                case 4 -> b | setBit4;
                case 5 -> b | setBit5;
                case 6 -> b | setBit6;
                default -> b | setBit7;
            };
            if (reg != 6) { // (rr)
                set8BitRegister(reg, b);
            }
            writeByte(address, b);
        }));
    }

    private void bitIndexReset(int bit, int reg) {
        reg_R++;
        getIndexAddress(address -> readByte(address, b -> {
            b = switch (bit) {
                case 0 -> b & resetBit0;
                case 1 -> b & resetBit1;
                case 2 -> b & resetBit2;
                case 3 -> b & resetBit3;
                case 4 -> b & resetBit4;
                case 5 -> b & resetBit5;
                case 6 -> b & resetBit6;
                default -> b & resetBit7;
            };
            if (reg != 6) { // (rr)
                set8BitRegister(reg, b);
            }
            writeByte(address, b);
        }));
    }

    /* LD (ix+dd),nn */
    private void loadIndex8BitImmediate() {
        reg_R++;
        getIndexAddress(address -> readByte(reg_PC, data -> {
            incPC();
            writeByte(address, data);
        }));
    }

    private void writeWord(int address, int value) {
        ioQueue.add(new IoRequest(address, value & 0xff, true));
        ioQueue.add(new IoRequest(address + 1, value >> 8, true));
    }

    private void writeByte(int address, int value) {
        ioQueue.add(new IoRequest(address, value, true));
    }

    private void readByte(int address, Consumer<Integer> callback) {
        ioQueue.add(new IoRequest(address, true, callback));
    }

    private void readWord(int address, Consumer<Integer> callback) {
        readByte(address, (lowByte) -> readByte(address + 1, (hiByte) -> callback.accept((hiByte << 8) + lowByte)));
    }

    private void IORead(int address, Consumer<Integer> callback) {
        ioQueue.add(new IoRequest(address, false, callback));
    }

    private void IOWrite(Integer address, int value) {
        ioQueue.add(new IoRequest(address, value, false));
    }
}
