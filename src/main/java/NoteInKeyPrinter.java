package main.java;

import static main.java.PrintUtil.byteArrayToString;

public class NoteInKeyPrinter implements Runnable {
    private Pipe<NoteInKey>.PipeSink inputPipe;

    public NoteInKeyPrinter(Pipe<NoteInKey>.PipeSink inputPipe) {
        this.inputPipe = inputPipe;
    }

    @Override
    public void run() {
        NoteInKey note;
        while ((note = inputPipe.read()) != null) {
            System.out.print(note.getKey() + "\t");
            System.out.print(note.getPitch().getPitchIndex() + "\t");
            System.out.print(note.getPitch().getOctave() + "\t");
            System.out.print(byteArrayToString(note.getTimeSig().getBasis()) + "\t");

            HierarchicalRelTime onset = note.getOnsetTime();
            System.out.print(onset.getLevel() + "\t");
            System.out.print(onset.getIncrement() + "\t");

            HierarchicalDur duration = note.getDuration();
            System.out.print(duration.getLevel() + "\t");
            System.out.print(duration.getIncrement() + "\t");

            System.out.print(note.getVoice() + "\t");
            System.out.println();
        }
    }
}