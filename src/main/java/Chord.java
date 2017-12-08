package main.java;

import java.util.Iterator;
import java.util.LinkedList;

public class Chord implements Iterable<Pitch> {

    private LinkedList<Pitch> pitches;
    private int[] pitchIndexVector;

    @Override
    public Iterator<Pitch> iterator() {
        return pitches.iterator();
    }

    private int[] getPitchIndexVector() {
        int[] pitchIndexVector = new int[12];
        for (Pitch p : pitches) {
            pitchIndexVector[p.getPitchIndex()] = 1;
        }
        return pitchIndexVector;
    }
}
