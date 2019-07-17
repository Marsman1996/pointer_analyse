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
public class Test2 {

    public Test2() {}

    private void assign(A x, A y) {
        y.f = x.f;
    }

    private void test() {

    }

    public static void main(String[] args) {

        Benchmark.alloc(1);
        B b = new B();
        Benchmark.alloc(2);
        A a = new A();
        a.f = b;
        Benchmark.alloc(3);
        A c = new A();
        c = a;

        Benchmark.test(1, c.f);
    }

}
