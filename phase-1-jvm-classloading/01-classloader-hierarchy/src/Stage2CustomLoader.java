// src/Stage2CustomLoader.java
import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;

public class Stage2CustomLoader extends ClassLoader {

    private final Path pluginDir;

    public Stage2CustomLoader(Path pluginDir, ClassLoader parent) {
        super(parent); // honour the parent delegation model
        this.pluginDir = pluginDir;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // Convert class name → file path: "com.foo.Bar" → "com/foo/Bar.class"
        String fileName = name.replace('.', File.separatorChar) + ".class";
        Path classFile = pluginDir.resolve(fileName);

        if (!Files.exists(classFile)) {
            // We can't find it either — give up (parent already said no)
            throw new ClassNotFoundException("Not found in plugin dir: " + name);
        }

        System.out.println("[CustomLoader] Reading bytes from: " + classFile);
        try {
            byte[] bytes = Files.readAllBytes(classFile);
            // defineClass transforms raw bytes → a Class object
            return defineClass(name, bytes, 0, bytes.length);
        } catch (IOException e) {
            throw new ClassNotFoundException("IO error loading " + name, e);
        }
    }

    // --- Demo ---
    public static void main(String[] args) throws Exception {
        System.out.println("=== Stage 2: Custom ClassLoader ===\n");
        System.out.println("Stage2CustomLoader.class.getClassLoader(): " + Stage2CustomLoader.class.getClassLoader());

        Path pluginDir = Path.of("plugins");
        Stage2CustomLoader loader = new Stage2CustomLoader(pluginDir,
                Stage2CustomLoader.class.getClassLoader());

        // loadClass() follows the delegation chain FIRST
        // Only if parent fails does it call our findClass()
        Class<?> greeterClass = loader.loadClass("Greeter");

        System.out.println("Loaded class:  " + greeterClass);
        System.out.println("Loaded by:     " + greeterClass.getClassLoader());
        System.out.println("Parent loader: " + greeterClass.getClassLoader().getParent());

        // Invoke via reflection — we can't cast to Greeter (it's not on our classpath)
        Object instance = greeterClass.getDeclaredConstructor().newInstance();
        Method greet = greeterClass.getMethod("greet", String.class);
        String result = (String) greet.invoke(instance, "JVM explorer");
        System.out.println("\nResult: " + result);
    }
}