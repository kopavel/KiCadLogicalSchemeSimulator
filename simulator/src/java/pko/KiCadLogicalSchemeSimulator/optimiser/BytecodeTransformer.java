package pko.KiCadLogicalSchemeSimulator.optimiser;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;

public class BytecodeTransformer {
    public static byte[] transformThisAloadToDup(byte[] classBytes) {
        ClassReader cr = new ClassReader(classBytes);
        ClassNode classNode = new ClassNode();
        cr.accept(classNode, 0);
        for (MethodNode mn : classNode.methods) {
            if (mn.name.startsWith("set")) {
                List<AbstractInsnNode> aload0List = new ArrayList<>();
                // Собираем все инструкции ALOAD 0
                for (AbstractInsnNode insn : mn.instructions) {
                    if (insn.getOpcode() == Opcodes.ALOAD && ((VarInsnNode) insn).var == 0) {
                        aload0List.add(insn);
                    }
                }
                if (aload0List.size() < 2) {
                    continue;
                }
                // Первый оставить и сразу после него вставить DUP
                AbstractInsnNode first = aload0List.getFirst();
                mn.instructions.insert(first, new InsnNode(Opcodes.DUP));
                // Все промежуточные ALOAD 0 заменить на DUP
                for (int i = 1; i < aload0List.size() - 1; i++) {
                    mn.instructions.set(aload0List.get(i), new InsnNode(Opcodes.DUP));
                }
                // Последний ALOAD 0 удалить
                mn.instructions.remove(aload0List.getLast());
            }
        }
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classNode.accept(cw);
        return cw.toByteArray();
    }
}
