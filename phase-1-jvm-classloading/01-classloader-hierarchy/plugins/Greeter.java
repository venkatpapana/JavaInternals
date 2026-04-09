// plugins/Greeter.java  (compile separately!)
public class Greeter {
    public String greet(String name) {
        return "Hello from dynamically loaded class, " + name + "! "
             + "My loader: " + getClass().getClassLoader();
    }
}