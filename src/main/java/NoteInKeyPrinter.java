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
            System.out.print(note.getRelKeyCircleFifths() + "\t");
            System.out.print(note.getKeyMode() + "\t");
            System.out.print(note.getRelPitchPlain() + "\t");

            System.out.print(note.getPitch().getKeyRelOctave(note.getHomeKeyCircleFifths()) + "\t");
            System.out.print(byteArrayToString(note.getTimeSig().getBasis()) + "\t");

            HierarchicalRelTime onset = note.getOnsetTime();
            System.out.print(onset.getDepth() + "\t");
            System.out.print(onset.getIncrement() + "\t");

            HierarchicalDur duration = note.getDuration();
            System.out.print(duration.getDepth() + "\t");
            System.out.print(duration.getIncrement() + "\t");

            System.out.print(note.getVoice());
            System.out.println();
        }
    }
}
