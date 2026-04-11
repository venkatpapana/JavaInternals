//shell commands + reading output

public class Calculator {

    private String name;

    public Calculator(String name) {
        this.name = name;
    }

    public int add(int a, int b) {
        return a + b;
    }

    public int factorial(int n) {
        if (n <= 1) return 1;
        return n * factorial(n - 1);  // recursive — watch the invoke opcodes
    }

    public String describe(int result) {
        return name + " computed: " + result;  // string concat — javac transforms this
    }
}