package main.java;

import org.apache.commons.math3.fraction.Fraction;

public class Duration implements Comparable<Duration> {

    private Fraction value;

    public Duration(Fraction value) {
        this.value = value;
    }

    public Duration(int numer, int denom) {
        this.value = new Fraction(numer, denom);
    }

    public Fraction getValue() {
        return value;
    }

    public Duration add(Duration that) {
        return new Duration(this.value.add(that.value));
    }

    public Duration multiply(Duration that) {
        return new Duration(value.multiply(that.value));
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
