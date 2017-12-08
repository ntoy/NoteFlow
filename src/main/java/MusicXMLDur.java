package main.java;

import org.apache.commons.math3.fraction.Fraction;

public class MusicXMLDur implements Comparable<MusicXMLDur> {

    private Fraction value;

    public MusicXMLDur(Fraction value) {
        this.value = value;
    }

    public MusicXMLDur(int numer, int denom) {
        this.value = new Fraction(numer, denom);
    }

    public Fraction getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "[dur:" + value + "]";
    }

    @Override
    public int compareTo(MusicXMLDur that) {
        return this.value.compareTo(that.value);
    }
}
