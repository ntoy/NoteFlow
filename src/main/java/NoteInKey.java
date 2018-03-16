package main.java;


/**
 * A note paired with the musical key of the section it lies in.
 * The key is represented as an integer between 0 and 23.
 * 0-11 represent C Major - B Major, 12-23 represent C minor - B minor.
 */
public class NoteInKey extends Note {
    private int key;

    public NoteInKey(Note note, int key) {
        super(note);
        this.key = key;
    }

    public int getKey() {
        return key;
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
        System.out.println(new NoteInKey(note, 3));
        System.out.println(new NoteInKey(note, 15));
    }
}
