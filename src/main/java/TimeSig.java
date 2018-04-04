package main.java;

import java.util.Arrays;

import static main.java.NoteLengthTranslation.wordToDur;

public class TimeSig {
    private int beats;
    private int beatType;
    private byte[] parentBasis;
    private byte[] basis;

    private static final int BASIS_LENGTH = 5;

    public TimeSig(int beats, int beatType) {
        this.beats = beats;
        this.beatType = beatType;
        basis = new byte[BASIS_LENGTH];
        parentBasis = null;

        // Find (a, b) such that beats == 2^a * 3^b, assuming n is of that form
        int twoExponent = 0;
        int threeExponent = 0;
        while (beats % 2 == 0) {
            beats /= 2;
            twoExponent++;
        }
        while (beats % 3 == 0) {
            beats /= 3;
            threeExponent++;
        }
        if (beats != 1) {
            throw new RuntimeException("n is not a product of threes and twos");
        }

        if (twoExponent + threeExponent >= BASIS_LENGTH) {
            throw new RuntimeException("Basis container not long enough to represent time signature");
        }
        for (int i = 0; i < twoExponent; i++) {
            basis[i] = 2;
        }
        for (int i = twoExponent; i < twoExponent + threeExponent; i++) {
            basis[i] = 3;
        }
        for (int i = twoExponent + threeExponent; i < BASIS_LENGTH; i++) {
            basis[i] = 2;
        }
    }

    // private copy constructor
    private TimeSig(TimeSig that) {
        this.beats = that.beats;
        this.beatType = that.beatType;
        this.parentBasis = that.parentBasis;
        this.basis = that.basis.clone();
    }

    public TimeSig ofTuplet(int numDivs, int oldNumDivs, String divUnit,
                            Duration firstNoteDur, AbsoluteTime startTime) {
        if (parentBasis != null) {
            throw new IllegalStateException("Already in tuplet; nested tuplets not supported");
        }

        Duration adjustedDivUnit;
        if (divUnit != null) {
            Duration lengthAdjustment = new Duration(getBeatType(), getBeats());
            adjustedDivUnit = wordToDur(divUnit).multiply(lengthAdjustment);
        }
        else if (firstNoteDur != null) {
            adjustedDivUnit = firstNoteDur.multiply(new Duration(numDivs, oldNumDivs));
        }
        else {
            throw new NullPointerException("At least one of divUnit and firstNoteDur must be non-null");
        }

        AbsoluteTime tupletSpan = AbsoluteTime.ZERO.add(adjustedDivUnit.multiply(oldNumDivs));
        HierarchicalRelTime hierachicalSpan =
                new HierarchicalRelTime(tupletSpan, AbsoluteTime.ZERO, this);
        if (hierachicalSpan.getIncrement() != 1) {
            throw new IllegalArgumentException("Tuplet length not aligned to grid: not supported");
        }
        HierarchicalRelTime hierarchicalStartTime =
                new HierarchicalRelTime(startTime, AbsoluteTime.ZERO, this);
        if (hierarchicalStartTime.getLevel() > hierachicalSpan.getLevel()) {
            throw new IllegalArgumentException("Tuplet start not aligned to grid: not supported");
        }

        TimeSig child = new TimeSig(this);
        child.parentBasis = child.basis.clone();
        child.basis[hierachicalSpan.getLevel()] = (byte) numDivs; // no more than 127 divisions hopefully
        for (int i = hierachicalSpan.getLevel() + 1; i < child.basis.length; i++) {
            child.basis[i] = 2;
        }
        return child;
    }

    public TimeSig getParent() {
        if (parentBasis == null) {
            throw new IllegalStateException("Not in tuplet");
        }

        TimeSig parent = new TimeSig(this);
        parent.basis = parent.parentBasis;
        parent.parentBasis = null;
        return parent;
    }

    public int getBeats() {
        return beats;
    }

    public int getBeatType() {
        return beatType;
    }

    public byte[] getBasis() {
        return basis.clone();
    }

    @Override
    public boolean equals(Object that) {
        if (that == this) return true;
        if (!(that instanceof TimeSig)) return false;
        TimeSig thatTimeSig = (TimeSig) that;
        return this.beats == thatTimeSig.beats
                && this.beatType == thatTimeSig.beatType
                && Arrays.equals(this.basis, thatTimeSig.basis)
                && Arrays.equals(this.parentBasis, thatTimeSig.parentBasis);
    }

    @Override
    public String toString() {
        return beats + "|" + beatType;
    }

    public static void main(String[] args) {
        TimeSig timeSig = new TimeSig(4, 8);
        byte[] basis = timeSig.getBasis();
        for (byte b : basis) {
            System.out.println(b);
        }
    }
}
