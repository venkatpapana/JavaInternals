// src/Stage2_Inspector.java
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.TraceClassVisitor;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

public class Stage2_Inspector {

    public static void main(String[] args) throws Exception {
        byte[] classBytes = Files.readAllBytes(Path.of("out/Calculator.class"));

        System.out.println("=== Stage 2A: ASM TraceClassVisitor ===\n");
        traceClass(classBytes);

        System.out.println("\n=== Stage 2B: Custom opcode explainer ===\n");
        explainOpcodes(classBytes);
    }

    // --- 2A: Use ASM's built-in tracer ---
    static void traceClass(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        // TraceClassVisitor prints a readable assembly-style listing
        // PrintWriter(System.out) streams it to console
        TraceClassVisitor tracer = new TraceClassVisitor(new PrintWriter(System.out));
        reader.accept(tracer, ClassReader.EXPAND_FRAMES);
    }

    // --- 2B: Walk every method and explain each opcode ---
    static void explainOpcodes(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        reader.accept(new ExplainingClassVisitor(), 0);
    }

    static class ExplainingClassVisitor extends ClassVisitor {
        ExplainingClassVisitor() { super(Opcodes.ASM9); }

        @Override
        public MethodVisitor visitMethod(int access, String name,
                String descriptor, String signature, String[] exceptions) {
            System.out.println("Method: " + name + descriptor);
            System.out.println("─".repeat(50));
            return new ExplainingMethodVisitor(name);
        }
    }

    static class ExplainingMethodVisitor extends MethodVisitor {
        private final String methodName;
        private int offset = 0;

        ExplainingMethodVisitor(String methodName) {
            super(Opcodes.ASM9);
            this.methodName = methodName;
        }

        @Override
        public void visitInsn(int opcode) {
            // Zero-operand instructions
            System.out.printf("  %3d: %-16s  %s%n",
                offset, opcodeName(opcode), explain(opcode, -1, null));
            offset++;
        }

        @Override
        public void visitVarInsn(int opcode, int varIndex) {
            // Load/store instructions that reference a local variable slot
            System.out.printf("  %3d: %-12s %d   %s%n",
                offset, opcodeName(opcode), varIndex, explainVar(opcode, varIndex));
            offset += (opcode == Opcodes.IINC) ? 3 : 2;
        }

        @Override
        public void visitMethodInsn(int opcode, String owner,
                String name, String descriptor, boolean isInterface) {
            String kind = switch (opcode) {
                case Opcodes.INVOKEVIRTUAL   -> "virtual dispatch (polymorphic)";
                case Opcodes.INVOKESPECIAL   -> "direct call: constructor or super";
                case Opcodes.INVOKESTATIC    -> "static method, no receiver";
                case Opcodes.INVOKEINTERFACE -> "interface method dispatch";
                default                      -> "dynamic (bootstrap method)";
            };
            System.out.printf("  %3d: %-16s  → %s.%s%s  [%s]%n",
                offset, opcodeName(opcode), owner.replace('/', '.'), name, descriptor, kind);
            offset += 3;
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            String kind = switch (opcode) {
                case Opcodes.GETFIELD  -> "read instance field";
                case Opcodes.PUTFIELD  -> "write instance field";
                case Opcodes.GETSTATIC -> "read static field";
                case Opcodes.PUTSTATIC -> "write static field";
                default -> "";
            };
            System.out.printf("  %3d: %-16s  %s.%s  [%s]%n",
                offset, opcodeName(opcode), owner.replace('/', '.'), name, kind);
            offset += 3;
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            System.out.printf("  %3d: %-16s  → branch if condition met%n",
                offset, opcodeName(opcode));
            offset += 3;
        }

        @Override
        public void visitEnd() { System.out.println(); }

        // Human-readable opcode names (subset covering most real code)
        static String opcodeName(int op) {
            return switch (op) {
                case Opcodes.ILOAD   -> "iload";
                case Opcodes.ISTORE  -> "istore";
                case Opcodes.LLOAD   -> "lload";
                case Opcodes.LSTORE  -> "lstore";
                case Opcodes.FLOAD   -> "fload";
                case Opcodes.DLOAD   -> "dload";
                case Opcodes.ALOAD   -> "aload";
                case Opcodes.ASTORE  -> "astore";
                case Opcodes.IADD    -> "iadd";
                case Opcodes.IMUL    -> "imul";
                case Opcodes.ISUB    -> "isub";
                case Opcodes.LSUB    -> "lsub";
                case Opcodes.IRETURN -> "ireturn";
                case Opcodes.LRETURN -> "lreturn";
                case Opcodes.ARETURN -> "areturn";
                case Opcodes.RETURN  -> "return";
                case Opcodes.ICONST_1  -> "iconst_1";
                case Opcodes.ICONST_M1 -> "iconst_m1";
                case Opcodes.INVOKEVIRTUAL   -> "invokevirtual";
                case Opcodes.INVOKESPECIAL   -> "invokespecial";
                case Opcodes.INVOKESTATIC    -> "invokestatic";
                case Opcodes.INVOKEINTERFACE -> "invokeinterface";
                case Opcodes.INVOKEDYNAMIC   -> "invokedynamic";
                case Opcodes.GETFIELD  -> "getfield";
                case Opcodes.PUTFIELD  -> "putfield";
                case Opcodes.GETSTATIC -> "getstatic";
                case Opcodes.PUTSTATIC -> "putstatic";
                case Opcodes.IF_ICMPLE -> "if_icmple";
                case Opcodes.IF_ICMPGE -> "if_icmpge";
                case Opcodes.IF_ICMPGT -> "if_icmpgt";
                case Opcodes.IF_ICMPLT -> "if_icmplt";
                case Opcodes.IFLE      -> "ifle";
                case Opcodes.IFGT      -> "ifgt";
                case Opcodes.NEW       -> "new";
                case Opcodes.DUP       -> "dup";
                case Opcodes.POP       -> "pop";
                case Opcodes.LDC       -> "ldc";
                case Opcodes.CHECKCAST -> "checkcast";
                default -> "op(0x" + Integer.toHexString(op) + ")";
            };
        }
        static String explainVar(int opcode, int slot) {
            String slotName = slot == 0 ? "this" : "local[" + slot + "]";
            return switch (opcode) {
                case Opcodes.ILOAD -> "push int " + slotName + " onto operand stack";
                case Opcodes.LLOAD -> "push long " + slotName + " onto operand stack";
                case Opcodes.ALOAD -> "push reference " + slotName + " onto operand stack";
                case Opcodes.ISTORE -> "pop int from stack → " + slotName;
                case Opcodes.LSTORE -> "pop long from stack → " + slotName;
                case Opcodes.ASTORE -> "pop reference from stack → " + slotName;
                default -> "";
            };
        }

        static String explain(int opcode, int operand, Object value) {
            return switch (opcode) {
                case Opcodes.IADD    -> "pop 2 ints, push their sum";
                case Opcodes.IMUL    -> "pop 2 ints, push their product";
                case Opcodes.ISUB    -> "pop 2 ints, push a - b";
                case Opcodes.IRETURN -> "pop int from stack, return to caller";
                case Opcodes.ARETURN -> "pop reference from stack, return to caller";
                case Opcodes.RETURN  -> "void return";
                case Opcodes.ICONST_1 -> "push int constant 1";
                default -> "";
            };
        }
    }
}