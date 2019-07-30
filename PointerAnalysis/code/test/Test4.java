package test;

import benchmark.internal.Benchmark;
import benchmark.objects.A;
import benchmark.objects.B;

/*
 * @testcase FieldSensitivity2
 *
 * @version 1.0
 *
 * @author Johannes Späth, Nguyen Quang Do Lisa (Secure Software Engineering Group, Fraunhofer
 * Institute SIT)
 *
 * @description Field Sensitivity without static method
 */
public class Test4 {

    public Test4() {}

    private void assign(A x, A y) {
        y.f = x.f;
    }
    private B assign2(A x) {A y = x; return y.f;}

    private void test() {

    }

    public void main(String[] args) {
        Benchmark. alloc(1);
        B b = new B();
        Benchmark.alloc(2);
        A a = new A();
        Benchmark.alloc(3);
        A c = new A();
        int i = 10;
        if (i > 0) {
            a.f = b;
        }
        do {
            i = i - 1;
            c.f = b;
        } while (i > 0);
        Benchmark.test(1, a.f);
        Benchmark.test(2, c.f);
    }
}
