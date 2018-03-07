package main.java;

public class NoteInContext {
    private Note note;
    // key < 12 is major, key >= 12 is (key - 12) minor
    private int key;

    // a non-mutation-safe constructor
    public NoteInContext(Note note, int key) {
        this.note = note;
        this.key = key;
    }

    public Note getNote() {
        return note;
    }

    public void setNote(Note note) {
        this.note = note;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return note.toString() + " in " + new Pitch(0, key, 0).getPitchNameSharp()
                + (key < 12 ? " major" : " minor");
    }

    public static void main(String[] args) {
        Note note = new Note(new Pitch(60), new AbsoluteTime(2, 1, 4),
                new Duration(1, 4),
                new TimeSig(4, 4), 1);
        System.out.println(new NoteInContext(note, 3));
        System.out.println(new NoteInContext(note, 15));
    }
}
