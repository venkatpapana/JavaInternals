import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import java.lang.reflect.Method;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Stage3_AsmTransformer {

    public static void main(String[] args) throws Exception {
        byte[] original = Files.readAllBytes(Path.of("out/Calculator.class"));

        System.out.println("=== Stage 3: Bytecode rewriting with ASM ===\n");
        System.out.println("Original class size: " + original.length + " bytes");

        // Transform the bytecode in memory
        byte[] transformed = addTimingInstructions(original);
        System.out.println("Transformed class size: " + transformed.length + " bytes");

        // Save the rewritten class so we can inspect it with javap
        Files.write(Path.of("out/Calculator_timed.class"), transformed);
        System.out.println("Saved → out/Calculator_timed.class");
        System.out.println("Run: javap -c out/Calculator_timed.class to see injected instructions\n");

        // Load and run the transformed class in the same JVM
        runTransformed(transformed);
    }

    // --- The ASM transformation pipeline ---
    static byte[] addTimingInstructions(byte[] classBytes) {
        ClassReader reader  = new ClassReader(classBytes);
        ClassWriter writer  = new ClassWriter(
            ClassWriter.COMPUTE_FRAMES |    // ASM recalculates stack frames for us
            ClassWriter.COMPUTE_MAXS       // ASM recalculates max stack/locals
        );
        // Chain: reader → our visitor → writer
        reader.accept(new TimingClassVisitor(writer), ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    // --- ClassVisitor: intercepts class-level events ---
    static class TimingClassVisitor extends ClassVisitor {
        TimingClassVisitor(ClassVisitor next) { super(Opcodes.ASM9, next); }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

            // Don't instrument constructors or synthetic methods
            if (name.equals("<init>") || name.equals("<clinit>")) return mv;

            System.out.println("[ASM] Instrumenting method: " + name + descriptor);
            return new TimingMethodVisitor(mv, name);
        }
    }

    // --- MethodVisitor: intercepts instruction-level events ---
    static class TimingMethodVisitor extends MethodVisitor {
        private final String methodName;
        // Local variable slot where we store the start timestamp
        // Slots 0..N are taken by 'this' and parameters — we use a high index
        private static final int START_TIME_SLOT = 10;

        TimingMethodVisitor(MethodVisitor next, String methodName) {
            super(Opcodes.ASM9, next);
            this.methodName = methodName;
        }

        @Override
        public void visitCode() {
            // Called at the start of the method body — inject BEFORE first instruction:
            //   long startTime = System.nanoTime();
            super.visitCode();
            super.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/lang/System",       // owner class
                "nanoTime",               // method name
                "()J",                    // descriptor: no args, returns long
                false
            );
            // Store the long result into local variable slot 10
            // LSTORE because it's a long (64-bit → takes 2 slots: 10 and 11)
            super.visitVarInsn(Opcodes.LSTORE, START_TIME_SLOT);
        }

        @Override
        public void visitInsn(int opcode) {
            // Intercept every RETURN variant — inject timing print BEFORE the return
            boolean isReturn = (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN);
            if (isReturn) {
                injectTimingPrint();
            }
            super.visitInsn(opcode);
        }

        private void injectTimingPrint() {
            // Inject equivalent of:
            //   System.out.println("[TIMER] methodName took " +
            //       (System.nanoTime() - startTime) + " ns");

            // 1. Get System.out (a PrintStream)
            super.visitFieldInsn(Opcodes.GETSTATIC,
                "java/lang/System", "out", "Ljava/io/PrintStream;");

            // 2. Build the message string with StringBuilder
            super.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
            super.visitInsn(Opcodes.DUP);
            super.visitLdcInsn("[TIMER] " + methodName + " took ");
            super.visitMethodInsn(Opcodes.INVOKESPECIAL,
                "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false);

            // 3. Compute elapsed: System.nanoTime() - startTime
            super.visitMethodInsn(Opcodes.INVOKESTATIC,
                "java/lang/System", "nanoTime", "()J", false);
            super.visitVarInsn(Opcodes.LLOAD, START_TIME_SLOT);
            super.visitInsn(Opcodes.LSUB);  // nanoTime() - startTime

            // 4. Append elapsed long to StringBuilder
            super.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder", "append", "(J)Ljava/lang/StringBuilder;", false);

            // 5. Append " ns"
            super.visitLdcInsn(" ns");
            super.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder", "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);

            // 6. toString()
            super.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);

            // 7. PrintStream.println(String)
            super.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
        }
    }

    // --- Load the transformed bytes and run them ---
    static void runTransformed(byte[] bytes) throws Exception {
        // Simple anonymous subclass — defineClass is protected so we expose it
        ClassLoader loader = new ClassLoader(Stage3_AsmTransformer.class.getClassLoader()) {
            public Class<?> define(String name, byte[] b) {
                return defineClass(name, b, 0, b.length);
            }
        };

        // Invoke our exposed define() via reflection (it's not on ClassLoader's public API)
        java.lang.reflect.Method define = loader.getClass().getMethod("define", String.class, byte[].class);
        Class<?> clazz = (Class<?>) define.invoke(loader, "Calculator", bytes);

        Object calc    = clazz.getDeclaredConstructor(String.class).newInstance("Lab");
        Method add     = clazz.getMethod("add", int.class, int.class);
        Method factorial = clazz.getMethod("factorial", int.class);

        System.out.println("--- Calling add(3, 5) ---");
        System.out.println("Result: " + add.invoke(calc, 3, 5));

        System.out.println("\n--- Calling factorial(10) ---");
        System.out.println("Result: " + factorial.invoke(calc, 10));
    }
}