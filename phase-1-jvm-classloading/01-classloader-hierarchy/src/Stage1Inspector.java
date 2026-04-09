// src/Stage1Inspector.java
public class Stage1Inspector {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Stage 1: ClassLoader Hierarchy ===\n");

        // --- 1. Walk YOUR class up through the hierarchy ---
        printLoaderChain("Your own class", Stage1Inspector.class);

        // --- 2. A JDK class from the Application layer ---
        printLoaderChain("ArrayList (classpath)", java.util.ArrayList.class);

        // --- 3. A core JDK class loaded by Bootstrap ---
        printLoaderChain("String (bootstrap)", String.class);

        // --- 4. The context loader (what Spring Boot uses) ---
        ClassLoader ctx = Thread.currentThread().getContextClassLoader();
        System.out.println("\nThread context classloader: " + ctx);
        System.out.println("Its parent:                 " + ctx.getParent());
        printLoaderChain("Thread context classloader", ctx.getClass());

        // --- 5. What does the app loader's classpath look like? ---
        System.out.println("\n=== Classpath entries visible to AppClassLoader ===");
        ClassLoader appLoader = ClassLoader.getSystemClassLoader();
        // In Java 9+ this is a jdk.internal.loader.ClassLoaders$AppClassLoader
        // getURLs() is gone — but we can ask the system property
        String cp = System.getProperty("java.class.path");
        for (String entry : cp.split(System.getProperty("path.separator"))) {
            System.out.println("  " + entry);
        }
        System.out.println("\ngetSystemClassLoader: " + appLoader);
        System.out.println("Its parent:                 " + appLoader.getParent());
         printLoaderChain("SystemClassLoader", appLoader.getClass());

    }

    static void printLoaderChain(String label, Class<?> clazz) {
        System.out.println("--- " + label + " ---");
        ClassLoader loader = clazz.getClassLoader();

        // Bootstrap-loaded classes return null here — that's intentional
        if (loader == null) {
            System.out.println("  Loaded by: Bootstrap (null — it's a JVM built-in)");
            return;
        }

        int depth = 0;
        while (loader != null) {
            System.out.println("  ".repeat(depth) + "→ " + loader.getClass().getName()
                    + " [" + loader + "]");
            loader = loader.getParent();
            depth++;
        }
        System.out.println("  ".repeat(depth) + "→ Bootstrap (null)");
        System.out.println();
    }
}