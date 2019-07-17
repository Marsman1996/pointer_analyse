package test;

import benchmark.internal.Benchmark;
import benchmark.objects.A;

class Value {
    int v;
    Value(int n) {v = n;};
}

public class Test1 {

    public Test1() {}
    private static Value assign_test(Value v) {
        return v;
    }

    private static A assign(A v) {
        return v;
    }

    public static void main(String[] args) {
        Benchmark.alloc(1);
        A a = new A();
        A b = assign(a);

        Benchmark.test(1, a);
        Benchmark.test(2, b);
        System.out.println("Hello");
    }

}
