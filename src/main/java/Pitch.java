package main.java;

public class Pitch implements Comparable<Pitch> {
    private int midiIndex;

    private static String noteNames = "CDEFGAB";
    private static int[] noteNameOffsets = {0, 2, 4, 5, 7, 9, 11};

    public Pitch(int midiIndex) {
        this.midiIndex = midiIndex;
    }

    public Pitch(int octave, int pitchIndex, int alter) {
        this.midiIndex = 12 * (octave + 1) + pitchIndex + alter;
    }

    public Pitch(int octave, String pitchName, int alter) {
        this.midiIndex = 12 * (octave + 1) + noteNameOffsets[noteNames.indexOf(pitchName)] + alter;
    }

    public int getOctave() {
        return midiIndex / 12 - 1;
    }

    public int getPitchIndex() {
        return mod(midiIndex, 12);
    }

    public String getPitchNameSharp() {
        int pitchIndex = mod(midiIndex, 12);
        int minNumSharps = 12;
        int argmin = -1;
        for (int i = 0; i < 7; i++) {
            int numSharps = mod((pitchIndex - noteNameOffsets[i]), 12);
            if (numSharps < minNumSharps) {
                minNumSharps = numSharps;
                argmin = i;
            }
        }
        String name = noteNames.substring(argmin, argmin + 1);
        return minNumSharps  == 0 ? name : name + "#";
    }

    public String getPitchNameFlat() {
        int pitchIndex = mod(midiIndex, 12);
        int minNumFlats = 12;
        int argmin = -1;
        for (int i = 0; i < 7; i++) {
            int numFlats = mod((noteNameOffsets[i] - pitchIndex), 12);
            if (numFlats < minNumFlats) {
                minNumFlats = numFlats;
                argmin = i;
            }
        }
        String name = noteNames.substring(argmin, argmin + 1);
        return minNumFlats  == 0 ? name : name + "b";
    }

    @Override
    public int compareTo(Pitch that) {
        return this.midiIndex - that.midiIndex;
    }

    @Override
    public boolean equals(Object that) {
        if (that == this) return true;
        if (!(that instanceof Pitch)) return false;
        Pitch thatPitch = (Pitch) that;
        return thatPitch.midiIndex == this.midiIndex;
    }

    @Override
    public String toString() {
        return getPitchNameSharp() + getOctave();
    }

    private static int mod(int a, int b) {
        int x = a % b;
        return x >= 0 ? x : x + b;
    }

    public static void main(String args[]) {
        Pitch pitch = new Pitch(61);
        System.out.println(pitch.getOctave());
        System.out.println(pitch.getPitchIndex());
        System.out.println(pitch.getPitchNameFlat());
        System.out.println(pitch.getPitchNameSharp());
        System.out.println(pitch.equals("hello"));
        System.out.println(pitch.equals(new Pitch(60)));
        System.out.println(pitch.equals(new Pitch(61)));
        System.out.println(pitch.equals(pitch));
    }
}
