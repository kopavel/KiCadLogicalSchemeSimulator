package pko.KiCadLogicalSchemeSimulator.optimiser;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser.ReplaceParams;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BytecodeTransformer {
    public static byte[] transformThisAloadToDup(byte[] classBytes, Map<String, ReplaceParams> replaceMap) {
        ClassReader cr = new ClassReader(classBytes);
        ClassNode classNode = new ClassNode();
        cr.accept(classNode, 0);
        for (MethodNode mn : classNode.methods) {
            if (replaceMap.containsKey(mn.name)) {
                ReplaceParams params = replaceMap.get(mn.name);
                List<AbstractInsnNode> aload0List = new ArrayList<>();
                for (AbstractInsnNode insn : mn.instructions) {
                    if (insn.getOpcode() == Opcodes.ALOAD && ((VarInsnNode) insn).var == 0) {
                        aload0List.add(insn);
                    }
                }
                if (aload0List.size() < 2) {
                    continue;
                }
                if (params.size != null) {
                    AbstractInsnNode first = aload0List.getFirst();
                    for (int i = 1; i <= params.size; i++) {
                        InsnNode dup = new InsnNode(Opcodes.DUP);
                        mn.instructions.insert(first, dup);
                    }
                }
                for (int i = 1; i < aload0List.size(); i++) {
                    AbstractInsnNode node = aload0List.get(i);
                    InsnNode dup = new InsnNode(Opcodes.DUP);
                    InsnNode swap = new InsnNode(Opcodes.SWAP);
                    if (params.swap.contains(i)) {
                        mn.instructions.insert(node, swap);
                        mn.instructions.remove(node);
                    } else if (params.dup.contains(i)) {
                        mn.instructions.insert(dup, swap);
                        mn.instructions.remove(node);
                    } else if (params.size != null && params.size >= i) {
                        mn.instructions.remove(node);
                    }
                }
            }
        }
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classNode.accept(cw);
        return cw.toByteArray();
    }
}
