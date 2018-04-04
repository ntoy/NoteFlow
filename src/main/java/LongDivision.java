package main.java;

public class LongDivision {
    public static int quotient(int a, int b) {
        if (b < 0) return -quotient(a, -b);
        int x = a / b;
        if (a % b != 0 && a < 0) return x - 1;
        else return x;
    }

    public static int quotient(int a, int b, int remainderLow) {
        return quotient(a - remainderLow, b);
    }

    public static int remainder(int a, int b) {
        return a - b * quotient(a, b);
    }

    public static int remainder(int a, int b, int remainderLow) {
        return remainder(a - remainderLow, b) + remainderLow;
    }

    private LongDivision() {}
}
