package test;

import java.util.*;

import benchmark.internal.Benchmark;
import benchmark.objects.A;
import benchmark.objects.B;

public class Test6 {

    public static void newA() {
        new A();
    }
    public static void main(String[] args) {
        int i = 10;
        while (i > 100) {
            i = i - 1;
        }
    }
}