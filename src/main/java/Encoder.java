package main.java;

import java.io.File;

public class Encoder {
    public static void main(String[] args) {
        Thread.UncaughtExceptionHandler handler = (t, e) -> {
            System.err.println(e.getMessage());
            System.exit(1);
        };

        File inputFile = new File(args[0]);
        Pipe<Note> notePipe = new Pipe<>(128);
        Pipe<NoteInRhythm> noteInRhythmPipe = new Pipe<>(128);
        Pipe<NoteInKey> noteInKeyPipe = new Pipe<>(128);

        Thread musicMXLNoteReader = new Thread(new MusicXMLNoteReader(inputFile, notePipe.source));
        Thread metricalConverter =
                new Thread(new MetricalConverter(notePipe.sink, noteInRhythmPipe.source));
        Thread keyAnalyzer = new Thread(new KeyAnalyzer(noteInRhythmPipe.sink, noteInKeyPipe.source,
                new Duration(2, 1)));
        Thread smartNotePrinter = new Thread(new NoteInKeyPrinter(noteInKeyPipe.sink));

        musicMXLNoteReader.setUncaughtExceptionHandler(handler);
        keyAnalyzer.setUncaughtExceptionHandler(handler);
        metricalConverter.setUncaughtExceptionHandler(handler);
        smartNotePrinter.setUncaughtExceptionHandler(handler);

        musicMXLNoteReader.start();
        keyAnalyzer.start();
        metricalConverter.start();
        smartNotePrinter.start();

        try {
            musicMXLNoteReader.join();
            keyAnalyzer.join();
            metricalConverter.join();
            smartNotePrinter.join();
        } catch (InterruptedException e) {
            return;
        }
    }
}
