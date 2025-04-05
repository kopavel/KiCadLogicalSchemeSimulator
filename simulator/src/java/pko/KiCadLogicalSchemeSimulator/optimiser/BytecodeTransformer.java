package pko.KiCadLogicalSchemeSimulator.optimiser;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BytecodeTransformer {
    public static byte[] transformThisAloadToDup(byte[] classBytes, Map<String, Integer> replaceMap) {
        ClassReader cr = new ClassReader(classBytes);
        ClassNode classNode = new ClassNode();
        cr.accept(classNode, 0);
        for (MethodNode mn : classNode.methods) {
            if (replaceMap.containsKey(mn.name)) {
                List<AbstractInsnNode> aload0List = new ArrayList<>();
                for (AbstractInsnNode insn : mn.instructions) {
                    if (insn.getOpcode() == Opcodes.ALOAD && ((VarInsnNode) insn).var == 0) {
                        aload0List.add(insn);
                    }
                }
                if (aload0List.size() < 2) {
                    continue;
                }
                AbstractInsnNode first = aload0List.getFirst();
                for (int i = 1; i <= replaceMap.get(mn.name); i++) {
                    InsnNode dup = new InsnNode(Opcodes.DUP);
                    mn.instructions.insert(first, dup);
                    mn.instructions.remove(aload0List.get(i));
                }
            }
        }
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classNode.accept(cw);
        return cw.toByteArray();
    }
}
