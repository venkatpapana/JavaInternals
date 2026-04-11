import org.objectweb.asm.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;

public class Stage4_LoaderIntegration {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Stage 4: ASM + ClassLoader integration ===\n");

        Path pluginDir = Path.of("plugins");
        InstrumentingLoader loader = new InstrumentingLoader(pluginDir,
                Stage4_LoaderIntegration.class.getClassLoader());

        // Load Calculator — it gets transformed transparently before defineClass
        Class<?> clazz   = loader.loadClass("Calculator");
        Object   calc    = clazz.getDeclaredConstructor(String.class).newInstance("Integrated");
        Method   add     = clazz.getMethod("add", int.class, int.class);
        Method   fact    = clazz.getMethod("factorial", int.class);

        System.out.println("add(7, 8) = " + add.invoke(calc, 7, 8));
        System.out.println("factorial(5) = " + fact.invoke(calc, 5));
        System.out.println("\nLoader: " + clazz.getClassLoader());
    }

    // Your Phase 1 loader — now with ASM transformation baked in
    static class InstrumentingLoader extends ClassLoader {
        private final Path dir;

        InstrumentingLoader(Path dir, ClassLoader parent) {
            super(parent);
            this.dir = dir;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            Path file = dir.resolve(name.replace('.', '/') + ".class");
            if (!Files.exists(file)) throw new ClassNotFoundException(name);

            try {
                byte[] original    = Files.readAllBytes(file);
                System.out.println("[Loader] Read " + original.length
                        + " bytes for " + name);

                // Transform BEFORE defineClass — this is the key integration point
                byte[] transformed = Stage3_AsmTransformer.addTimingInstructions(original);
                System.out.println("[Loader] Transformed to " + transformed.length + " bytes");

                return defineClass(name, transformed, 0, transformed.length);
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }
    }
}