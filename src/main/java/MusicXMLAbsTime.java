package main.java;

import org.apache.commons.math3.fraction.Fraction;

/**
 * class to store time since beginning of piece as whole part + fractional part
 * to avoid overflow
 */
public class MusicXMLAbsTime implements Comparable<MusicXMLAbsTime> {

    // Assumption made: piece is always at same tempo
    private int quarterNoteOffset;
    private Fraction quarterNoteFrac;

    public MusicXMLAbsTime(int quarterNoteOffset, Fraction quarterNoteFrac) {
        int wholePartChange = quarterNoteFrac.intValue();
        // floor of real number = integer value if positive or whole, o/w integer value - 1
        if (quarterNoteFrac.getNumerator() * quarterNoteFrac.getDenominator() < 0
                && quarterNoteFrac.getNumerator() % quarterNoteFrac.getDenominator() != 0)
            wholePartChange--;
        this.quarterNoteOffset = quarterNoteOffset + wholePartChange;
        this.quarterNoteFrac = quarterNoteFrac.subtract(wholePartChange);
    }

    public MusicXMLAbsTime(int quarterNoteOffset, int numer, int denom) {
        this(quarterNoteOffset, new Fraction(numer, denom));
    }

    public int getQuarterNoteOffset() {
        return quarterNoteOffset;
    }

    public Fraction getQuarterNoteFrac() {
        return quarterNoteFrac;
    }

    public MusicXMLAbsTime add(MusicXMLDur duration) {
        return new MusicXMLAbsTime(quarterNoteOffset, quarterNoteFrac.add(duration.getValue()));
    }

    @Override
    public String toString() {
        return "[time:" + quarterNoteOffset + "+" + quarterNoteFrac + "]";
    }

    // compare with lexicographical order
    @Override
    public int compareTo(MusicXMLAbsTime that) {
        // invariant assumed: fractional parts are always less than one
        if (this.quarterNoteOffset != that.quarterNoteOffset)
            return this.quarterNoteOffset - that.quarterNoteOffset;
        else
            return this.quarterNoteFrac.compareTo(that.quarterNoteFrac);
    }

    // Unit testing
    public static void main(String[] args) {
        MusicXMLAbsTime absTime;
        MusicXMLDur dur;
        Fraction frac;
        int offset;

        absTime = new MusicXMLAbsTime(3, 1, 6);
        dur = new MusicXMLDur(1, 2);
        offset = absTime.add(dur).getQuarterNoteOffset();
        frac = absTime.add(dur).getQuarterNoteFrac();
        System.out.println(offset + ", " + frac);

        absTime = new MusicXMLAbsTime(3, 5, 6);
        dur = new MusicXMLDur(11, 6);
        offset = absTime.add(dur).getQuarterNoteOffset();
        frac = absTime.add(dur).getQuarterNoteFrac();
        System.out.println(offset + ", " + frac);

        absTime = new MusicXMLAbsTime(3, 1, 6);
        dur = new MusicXMLDur(-11, 6);
        offset = absTime.add(dur).getQuarterNoteOffset();
        frac = absTime.add(dur).getQuarterNoteFrac();
        System.out.println(offset + ", " + frac);
    }
}
