/*
 * Copyright (c) 2024 Pavel Korzh
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package pko.KiCadLogicalSchemeSimulator.optimiser;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.Printer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BytecodeTransformer {
    public static byte[] transformThisAloadToDup(byte[] classBytes, Map<String, List<ReplaceParams>> replaceMap) {
        ClassReader cr = new ClassReader(classBytes);
        ClassNode classNode = new ClassNode();
        cr.accept(classNode, 0);
        for (MethodNode mn : classNode.methods) {
            if (replaceMap.containsKey(mn.name)) {
                for (ReplaceParams params : replaceMap.get(mn.name)) {
                    List<AbstractInsnNode> opcodeList = new ArrayList<>();
                    for (AbstractInsnNode insn : mn.instructions) {
                        if (insn.getOpcode() == params.target && (params.varNo == null || ((VarInsnNode) insn).var == params.varNo)) {
                            opcodeList.add(insn);
                        }
                    }
                    if (params.dup != null) {
                        AbstractInsnNode first = opcodeList.getFirst();
                        for (int i = 1; i <= params.dup; i++) {
                            InsnNode dup = new InsnNode(Opcodes.DUP);
                            mn.instructions.insert(first, dup);
                        }
                        if (params.skip.isEmpty()) {
                            for (int i = 1; i <= params.dup; i++) {
                                mn.instructions.remove(opcodeList.get(i));
                            }
                        }
                    }
                    for (Map.Entry<Integer, List<Integer>> additions : params.add.entrySet()) {
                        for (Integer position : additions.getValue()) {
                            InsnNode newOpcode = new InsnNode(additions.getKey());
                            mn.instructions.insert(opcodeList.get(position), newOpcode);
                        }
                    }
                    for (Map.Entry<Integer, List<Integer>> replacement : params.replacements.entrySet()) {
                        for (Integer position : replacement.getValue()) {
                            InsnNode newOpcode = new InsnNode(replacement.getKey());
                            mn.instructions.insert(opcodeList.get(position), newOpcode);
                            mn.instructions.remove(opcodeList.get(position));
                        }
                    }
                    for (Integer i : params.skip) {
                        mn.instructions.remove(opcodeList.get(i));
                    }
                }
/*
                Log.debug(BytecodeTransformer.class,
                        "result\n{}",
                        () -> Arrays.stream(mn.instructions.toArray())
                                .map(BytecodeTransformer::insnToString)
                                .collect(Collectors.joining("\n")));
*/
            }
        }
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classNode.accept(cw);
        return cw.toByteArray();
    }

    private static String insnToString(AbstractInsnNode insn) {
        return switch (insn.getType()) {
            case AbstractInsnNode.LABEL -> ((LabelNode) insn).getLabel().toString();
            case AbstractInsnNode.LINE -> "LINE " + ((LineNumberNode) insn).line;
            case AbstractInsnNode.FRAME -> "FRAME";
            case AbstractInsnNode.FIELD_INSN -> {
                FieldInsnNode fin = (FieldInsnNode) insn;
                yield opcode(insn.getOpcode()) + " " + fin.owner + "." + fin.name + " " + fin.desc;
            }
            case AbstractInsnNode.METHOD_INSN -> {
                MethodInsnNode min = (MethodInsnNode) insn;
                yield opcode(insn.getOpcode()) + " " + min.owner + "." + min.name + " " + min.desc;
            }
            case AbstractInsnNode.INVOKE_DYNAMIC_INSN -> opcode(insn.getOpcode()) + " (INVOKE DYNAMIC)";
            case AbstractInsnNode.JUMP_INSN -> {
                JumpInsnNode jin = (JumpInsnNode) insn;
                yield opcode(insn.getOpcode()) + " " + jin.label.getLabel();
            }
            case AbstractInsnNode.LDC_INSN -> {
                LdcInsnNode ldc = (LdcInsnNode) insn;
                yield "LDC " + ldc.cst;
            }
            case AbstractInsnNode.IINC_INSN -> {
                IincInsnNode iinc = (IincInsnNode) insn;
                yield "IINC var=" + iinc.var + " by " + iinc.incr;
            }
            case AbstractInsnNode.VAR_INSN -> {
                VarInsnNode var = (VarInsnNode) insn;
                yield opcode(insn.getOpcode()) + " var=" + var.var;
            }
            case AbstractInsnNode.INT_INSN -> {
                IntInsnNode intInsn = (IntInsnNode) insn;
                yield opcode(insn.getOpcode()) + " " + intInsn.operand;
            }
            case AbstractInsnNode.INSN -> opcode(insn.getOpcode());
            default -> opcode(insn.getOpcode()) + " (unknown insn)";
        };
    }

    private static String opcode(int opcode) {
        return Printer.OPCODES[opcode];
    }

    @SuppressWarnings("UnusedReturnValue")
    @Setter
    @Accessors(chain = true, fluent = true)
    @ToString
    public static class ReplaceParams {
        public final int target;
        public final Integer varNo;
        public Integer dup;
        public List<Integer> skip = new ArrayList<>();
        public Map<Integer, List<Integer>> replacements = new HashMap<>();
        public Map<Integer, List<Integer>> add = new HashMap<>();

        public ReplaceParams(int target, Integer varNo) {
            this.target = target;
            this.varNo = varNo;
        }

        public ReplaceParams replace(int replacement, int... s) {
            List<Integer> positions = replacements.computeIfAbsent(replacement, k -> new ArrayList<>());
            for (Integer i : s) {
                positions.add(i - 1);
            }
            return this;
        }

        public ReplaceParams add(int newOpcode, int... s) {
            List<Integer> positions = add.computeIfAbsent(newOpcode, k -> new ArrayList<>());
            for (Integer i : s) {
                positions.add(i - 1);
            }
            return this;
        }

        public ReplaceParams skip(int... s) {
            for (int i : s) {
                skip.add(i - 1);
            }
            return this;
        }
    }
}
