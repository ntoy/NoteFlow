package main.java;

import org.apache.commons.math3.fraction.Fraction;

import java.util.ArrayList;
import java.util.Arrays;

import static main.java.PrintUtil.byteArrayToString;
import static main.java.PrintUtil.intArrayToString;

public class HierarchicalRelTime extends AbsoluteTime {
    private int depth;
    private int increment;

    public HierarchicalRelTime(AbsoluteTime cur, AbsoluteTime prev, TimeSig timeSig) {
        super(cur);
        byte[] basis = timeSig.getBasis();
        int[] curTree = treeNotation(cur, basis);
        int[] prevTree = treeNotation(prev, basis);

        // get index of last nonzero digit for curTree (or 0 if all zeros)
        int d = curTree.length - 1;
        while (curTree[d] == 0 && d > 0) {
            d--;
        }
        depth = d;

        byte[] basisTruncated = Arrays.copyOfRange(basis, 0, depth);
        int[] curTruncated = Arrays.copyOfRange(curTree, 0, depth + 1);

        // find smallest tree notated time with all zeros beyond index [depth]
        // that is greater than or equal to [prevTree]
        // (all zeros beyond index [depth] cropped off)
        int[] ceilingTruncated = Arrays.copyOfRange(prevTree, 0, depth + 1);
        d = prevTree.length - 1;
        while (prevTree[d] == 0 && d > depth) {
            d--;
        }
        // increment at [depth] if prevTree is not its own "ceiling"
        if (d != depth) {
            int[] one = new int[depth + 1];
            one[depth] = 1;
            ceilingTruncated = treeNotationSum(ceilingTruncated, one, basisTruncated);
        }

        increment = treeNotationIntValue(treeNotationDiff(curTruncated, ceilingTruncated, basisTruncated), basisTruncated);
    }

    public int getDepth() {
        return depth;
    }

    public int getIncrement() {
        return increment;
    }

    static int[] treeNotation(AbsoluteTime time, byte[] basis) {
        int[] treeNotation = new int[basis.length + 1];
        treeNotation[0] = time.getMeasureOffset();
        Fraction remainingFrac = time.getMeasureFrac();
        int i = 0;

        while (!remainingFrac.equals(Fraction.ZERO)) {
            if (i >= basis.length)
                throw new RuntimeException("Basis of insufficient length to represent this time");
            Fraction timeInDivs = remainingFrac.multiply(basis[i]);
            int timeInWholeDivs = timeInDivs.intValue();
            assert timeInWholeDivs <= Byte.MAX_VALUE;
            treeNotation[i+1] = timeInWholeDivs;
            remainingFrac = timeInDivs.subtract(timeInWholeDivs);
            i++;
        }
        return treeNotation;
    }

    static int[] treeNotationDiff(int[] a, int[] b, byte[] basis) {
        assert a.length == basis.length + 1;
        assert b.length == a.length;
        int[] result = new int[a.length];
        int steal = 0;

        for (int i = result.length - 1; i >= 0; i--) {
            int difference = a[i] - steal - b[i];
            if (i == 0) {
                if (difference < 0)
                    throw new RuntimeException("Difference is negative");
                result[i] = difference;
            }
            else {
                if (difference >= 0) {
                    result[i] = difference;
                    steal = 0;
                } else {
                    result[i] = basis[i - 1] + difference;
                    steal = 1;
                }
            }
        }

        return result;
    }

    static int[] treeNotationSum(int[] a, int[] b, byte[] basis) {
        assert a.length == basis.length + 1;
        assert b.length == a.length;

        int[] result = new int[a.length];
        int carry = 0;

        for (int i = result.length - 1; i >= 0; i--) {
            result[i] = a[i] + b[i] + carry;
            if (i > 0) {
                carry = result[i] / basis[i-1];
                result[i] %= basis[i-1];
            }
        }
        return result;
    }

    static int treeNotationIntValue(int[] treeNotation, byte[] basis) {
        assert treeNotation.length == basis.length + 1;
        int result = treeNotation[0];
        for (int i = 1; i < treeNotation.length; i++) {
            result *= basis[i-1];
            result += treeNotation[i];
        }
        return result;
    }

    public static void main(String args[]) {
        TimeSig timeSig = new TimeSig(12, 8);
        System.out.println(byteArrayToString(timeSig.getBasis()));
        AbsoluteTime cur = new AbsoluteTime(13, 0, 1);
        for (int i = 0; i < 49; i++) {
            System.out.println(i);
            System.out.println(intArrayToString(treeNotation(cur, timeSig.getBasis())));
            cur = cur.add(new Duration(1, 24));
        }

        System.out.println();

        byte[] basis = {3, 4, 5, 4};
        int[] a = {16, 0, 0, 3, 2};
        int[] b = {16, 0, 0, 3, 2};

        int[] treeNotation = treeNotationDiff(a, b, basis);

        System.out.println(intArrayToString(treeNotation));
        System.out.println(treeNotationIntValue(treeNotation, basis));
        System.out.println();

        int[] c = {15, 1, 2, 4, 3};
        int[] d = {15, 0, 3, 3, 1};

        int[] sum = treeNotationSum(c, d, basis);
        System.out.println(intArrayToString(sum));

        System.out.println("\nSTART MAIN TEST\n");

        ArrayList<AbsoluteTime> absoluteTimes = new ArrayList<>();
        absoluteTimes.add(new AbsoluteTime(0, 0, 1));
        absoluteTimes.add(new AbsoluteTime(0, 4, 12));
        absoluteTimes.add(new AbsoluteTime(0, 9, 24));
        absoluteTimes.add(new AbsoluteTime(0, 1, 2));
        absoluteTimes.add(new AbsoluteTime(0, 8, 12));
        absoluteTimes.add(new AbsoluteTime(0, 10, 12));

        for (int i = 1; i < absoluteTimes.size(); i++) {
            HierarchicalRelTime time =
                new HierarchicalRelTime(absoluteTimes.get(i), absoluteTimes.get(i-1), timeSig);
            System.out.println("lev: " + time.depth);
            System.out.println("inc: " + time.increment);
        }
    }
}
