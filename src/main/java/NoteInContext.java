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
}
