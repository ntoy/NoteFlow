package main.java;

import java.util.Arrays;

import static main.java.HierarchicalRelTime.*;

public class HierarchicalDur extends Duration {
    private int depth;
    private int increment;

    public HierarchicalDur(AbsoluteTime onsetTime, Duration duration, TimeSig timeSig) {
        super(duration);
        // offsetTime = new HierarchicalRelTime(onsetTime.add(duration), onsetTime, timeSig);
        byte[] basis = timeSig.getBasis();
        int[] offsetTree = treeNotation(onsetTime.add(duration), basis);
        int[] onsetTree = treeNotation(onsetTime, basis);

        // get index of last nonzero digit for curTree (or 0 if all zeros)
        int d = offsetTree.length - 1;
        while (offsetTree[d] == 0 && d > 0) {
            d--;
        }
        depth = d;

        byte[] basisTruncated = Arrays.copyOfRange(basis, 0, depth);
        int[] curTruncated = Arrays.copyOfRange(offsetTree, 0, depth + 1);

        // find smallest tree notated time with all zeros beyond index [depth]
        // that is (STRICTLY) greater than [prevTree]
        // (all zeros beyond index [depth] cropped off)
        int[] ceilingTruncated = Arrays.copyOfRange(onsetTree, 0, depth + 1);
        d = onsetTree.length - 1;
        while (onsetTree[d] == 0 && d > depth) {
            d--;
        }
        int[] one = new int[depth + 1];
        one[depth] = 1;
        ceilingTruncated = treeNotationSum(ceilingTruncated, one, basisTruncated);

        increment = treeNotationIntValue(treeNotationDiff(curTruncated, ceilingTruncated, basisTruncated), basisTruncated);
    }

    public int getDepth() {
        return depth;
    }

    public int getIncrement() {
        return increment;
    }
}
