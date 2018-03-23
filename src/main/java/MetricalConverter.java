package main.java;

public class MetricalConverter implements Runnable {
    private Pipe<Note>.PipeSink inputPipe;
    private Pipe<NoteInRhythm>.PipeSource outputPipe;

    public MetricalConverter(Pipe<Note>.PipeSink inputPipe, Pipe<NoteInRhythm>.PipeSource outputPipe) {
        this.inputPipe = inputPipe;
        this.outputPipe = outputPipe;
    }

    @Override
    public void run() {
        Note cur, prev = null;
        while ((cur = inputPipe.read()) != null) {
            if (!cur.isGhost()) {
                outputPipe.write(new NoteInRhythm(cur, prev));
            }
            prev = cur;
        }
        outputPipe.close();
    }
}
