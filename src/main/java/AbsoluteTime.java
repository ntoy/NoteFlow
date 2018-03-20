package main.java;

import org.apache.commons.math3.fraction.Fraction;

/**
 * class to store time since beginning of piece as whole part + fractional part
 * to avoid overflow
 */
public class AbsoluteTime implements Comparable<AbsoluteTime> {

    public static AbsoluteTime ZERO = new AbsoluteTime(0, 0, 1);

    // Assumption made: piece is always at same tempo
    private int measureOffset;
    private Fraction measureFrac;

    public AbsoluteTime(int measureOffset, Fraction measureFrac) {
        int wholePartChange = measureFrac.intValue();
        // floor of real number = integer value if positive or whole, o/w integer value - 1
        if (measureFrac.getNumerator() * measureFrac.getDenominator() < 0
                && measureFrac.getNumerator() % measureFrac.getDenominator() != 0)
            wholePartChange--;
        this.measureOffset = measureOffset + wholePartChange;
        this.measureFrac = measureFrac.subtract(wholePartChange);
    }

    public AbsoluteTime(int measureOffset, int numer, int denom) {
        this(measureOffset, new Fraction(numer, denom));
    }

    public AbsoluteTime(AbsoluteTime that) {
        this.measureOffset = that.measureOffset;
        this.measureFrac = that.measureFrac;
    }

    public int getMeasureOffset() {
        return measureOffset;
    }

    public Fraction getMeasureFrac() {
        return measureFrac;
    }

    public AbsoluteTime add(Duration duration) {
        return new AbsoluteTime(measureOffset, measureFrac.add(duration.getValue()));
    }

    @Override
    public String toString() {
        return "[time:" + measureOffset + " + " + measureFrac + "]";
    }

    // compare with lexicographical order
    @Override
    public int compareTo(AbsoluteTime that) {
        // invariant assumed: fractional parts are always less than one
        if (this.measureOffset != that.measureOffset)
            return this.measureOffset - that.measureOffset;
        else
            return this.measureFrac.compareTo(that.measureFrac);
    }

    // Unit testing
    public static void main(String[] args) {
        AbsoluteTime absTime;
        Duration dur;
        Fraction frac;
        int offset;

        absTime = new AbsoluteTime(3, 1, 6);
        dur = new Duration(1, 2);
        offset = absTime.add(dur).getMeasureOffset();
        frac = absTime.add(dur).getMeasureFrac();
        System.out.println(offset + ", " + frac);

        absTime = new AbsoluteTime(3, 5, 6);
        dur = new Duration(11, 6);
        offset = absTime.add(dur).getMeasureOffset();
        frac = absTime.add(dur).getMeasureFrac();
        System.out.println(offset + ", " + frac);

        absTime = new AbsoluteTime(3, 1, 6);
        dur = new Duration(-11, 6);
        offset = absTime.add(dur).getMeasureOffset();
        frac = absTime.add(dur).getMeasureFrac();
        System.out.println(offset + ", " + frac);
    }
}
