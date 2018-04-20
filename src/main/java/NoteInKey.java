package main.java;

import static main.java.LongDivision.*;

/**
 * A note paired with the musical key of the section it lies in.
 * The key is represented as an integer between 0 and 23.
 */
public class NoteInKey extends NoteInRhythm {
    private int key;

    public NoteInKey(NoteInRhythm note, int key) {
        super(note);
        this.key = key;
    }

    public NoteInKey(NoteInKey that) {
        super(that);
        this.key = that.key;
    }

    // 0-11 represent C Major - B Major, 12-23 represent C minor - B minor
    public int getKeyPlain() {
        return key;
    }

    // 0 is C major or A minor, 1 is G major or E minor, etc.
    public int getKeyCircleFifths() {
        return circle5(key) - (key >= 12 ? 3 : 0);
    }

    public int getKeyMode() {
        return key < 12 ? 0 : 1;
    }

    public int getRelPitchPlain() {
        return remainder(this.getPitch().getPitchIndex() - key, 12);
    }

    public int getRelPitchCircleFifths() {
        return circle5(getKeyPlain());
    }

    public void setKey(int key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return super.toString() + " in " + new Pitch(0, key, 0).getPitchNameSharp()
                + (key < 12 ? " major" : " minor");
    }

    public static void main(String[] args) {
        Note note = new Note(new Pitch(60), new AbsoluteTime(2, 1, 4),
                new Duration(1, 4),
                new TimeSig(4, 4), 1);
        NoteInRhythm noteInRhythm = new NoteInRhythm(note, null);
        NoteInKey noteInKey = new NoteInKey(noteInRhythm, 3);
        System.out.println(noteInKey.getKeyCircleFifths());
    }

    static int circle5(int plainIndex) {
        int targetIndex = plainIndex % 12;
        int index = 0;
        int circle5Index = 0;
        while (index != targetIndex) {
            index = (index + 7) % 12;
            circle5Index++;
        }
        return circle5Index;
    }
}
