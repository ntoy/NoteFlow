package main.java;

import java.util.Arrays;

import static main.java.HierarchicalRelTime.*;

public class HierarchicalDur extends Duration {
    private int level;
    private int increment;

    public HierarchicalDur(AbsoluteTime onsetTime, Duration duration, TimeSig timeSig) {
        super(duration);
        // offsetTime = new HierarchicalRelTime(onsetTime.add(duration), onsetTime, timeSig);
        byte[] basis = timeSig.getBasis();
        int[] offsetTree = treeNotation(onsetTime.add(duration), basis);
        int[] onsetTree = treeNotation(onsetTime, basis);

        // get index of last nonzero digit for curTree (or 0 if all zeros)
        int l = offsetTree.length - 1;
        while (offsetTree[l] == 0 && l > 0) {
            l--;
        }
        level = l;

        byte[] basisTruncated = Arrays.copyOfRange(basis, 0, level);
        int[] curTruncated = Arrays.copyOfRange(offsetTree, 0, level + 1);

        // find smallest tree notated time with all zeros beyond index [level]
        // that is (STRICTLY) greater than [prevTree]
        // (all zeros beyond index [level] cropped off)
        int[] ceilingTruncated = Arrays.copyOfRange(onsetTree, 0, level + 1);
        l = onsetTree.length - 1;
        while (onsetTree[l] == 0 && l > level) {
            l--;
        }
        int[] one = new int[level + 1];
        one[level] = 1;
        ceilingTruncated = treeNotationSum(ceilingTruncated, one, basisTruncated);

        increment = treeNotationIntValue(treeNotationDiff(curTruncated, ceilingTruncated, basisTruncated), basisTruncated);
    }

    public int getLevel() {
        return level;
    }

    public int getIncrement() {
        return increment;
    }
}
