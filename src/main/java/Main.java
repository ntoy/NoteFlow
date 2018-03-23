package main.java;

import java.io.File;

public class Main {
    public static void main(String[] args) {
        MusicXMLPreprocess.preprocessMusicXMLs("main/res/MusicXML/original",
                "main/res/MusicXML/partwise",
                "main/res/MusicXML/timewise",
                "main/res/musicxml30");

        File inputFile = new File(args[0]);
        Pipe<Note> notePipe = new Pipe<>(16);
        Pipe<NoteInRhythm> noteInRhythmPipe = new Pipe<>(16);
        Pipe<NoteInKey> noteInKeyPipe = new Pipe<>(16);

        Thread musicMXLNoteReader = new Thread(new MusicXMLNoteReader(inputFile, notePipe.source));
        Thread metricalConverter =
                new Thread(new MetricalConverter(notePipe.sink, noteInRhythmPipe.source));
        Thread keyAnalyzer = new Thread(new KeyAnalyzer(noteInRhythmPipe.sink, noteInKeyPipe.source,
                new Duration(2, 1)));
        Thread smartNotePrinter = new Thread(new NoteInKeyPrinter(noteInKeyPipe.sink));

        musicMXLNoteReader.start();
        keyAnalyzer.start();
        metricalConverter.start();
        smartNotePrinter.start();
    }
}
