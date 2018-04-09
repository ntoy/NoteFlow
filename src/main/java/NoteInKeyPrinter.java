package main.java;

import static main.java.PrintUtil.byteArrayToString;

public class NoteInKeyPrinter implements Runnable {
    private Pipe<NoteInKey>.PipeSink inputPipe;

    public NoteInKeyPrinter(Pipe<NoteInKey>.PipeSink inputPipe) {
        this.inputPipe = inputPipe;
    }

    @Override
    public void run() {
        Pitch startRefPitch = new Pitch(4, "C", 0);
        NoteInKey prev = null;
        NoteInKey note;
        while ((note = inputPipe.read()) != null) {
            System.out.print(note.getKeyCircleFifths() + "\t");
            System.out.print(note.getKeyMode() + "\t");
            System.out.print(note.getRelPitchPlain() + "\t");

            System.out.print(note.getPitch().getOctaveOffset(
                    prev == null ? startRefPitch : prev.getPitch()) + "\t");
            System.out.print(byteArrayToString(note.getTimeSig().getBasis()) + "\t");

            HierarchicalRelTime onset = note.getOnsetTime();
            System.out.print(onset.getDepth() + "\t");
            System.out.print(onset.getIncrement() + "\t");

            HierarchicalDur duration = note.getDuration();
            System.out.print(duration.getDepth() + "\t");
            System.out.print(duration.getIncrement() + "\t");

            System.out.print(note.getVoice() + "\t");
            System.out.println();

            prev = note;
        }
    }
}
