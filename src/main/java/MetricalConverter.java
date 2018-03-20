package main.java;

public class MetricalConverter implements Runnable {
    private Pipe<NoteInKey>.PipeSink inputPipe;
    private Pipe<SmartNote>.PipeSource outputPipe;

    public MetricalConverter(Pipe<NoteInKey>.PipeSink inputPipe, Pipe<SmartNote>.PipeSource outputPipe) {
        this.inputPipe = inputPipe;
        this.outputPipe = outputPipe;
    }

    @Override
    public void run() {
        NoteInKey cur, prev = null;
        while ((cur = inputPipe.read()) != null) {
            outputPipe.write(new SmartNote(cur, prev));
            prev = cur;
        }
        outputPipe.close();
    }
}
