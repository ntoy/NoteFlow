package main.java;

import org.apache.commons.math3.fraction.Fraction;

public class Duration implements Comparable<Duration> {

    public static final Duration ZERO = new Duration(Fraction.ZERO);
    public static final Duration ONE = new Duration(Fraction.ONE);

    private Fraction value;

    public Duration(Fraction value) {
        this.value = value;
    }

    public Duration(int numer, int denom) {
        this.value = new Fraction(numer, denom);
    }

    public Duration(Duration that) {
        this.value = that.value;
    }

    public Fraction getValue() {
        return value;
    }

    public Duration add(Duration that) {
        return new Duration(this.value.add(that.value));
    }

    public Duration subtract(Duration that) {
        return new Duration(this.value.subtract(that.value));
    }

    public Duration multiply(Duration that) {
        return new Duration(value.multiply(that.value));
    }

    public Duration multiply(int factor) {
        return new Duration(value.multiply(factor));
    }

    @Override
    public String toString() {
        return "[dur:" + value + "]";
    }

    @Override
    public int compareTo(Duration that) {
        return this.value.compareTo(that.value);
    }
}
