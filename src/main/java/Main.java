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
        Pipe<NoteInKey> noteInKeyPipe = new Pipe<>(16);
        Pipe<SmartNote> smartNotePipe = new Pipe<>(16);

        Thread musicMXLNoteReader = new Thread(new MusicXMLNoteReader(inputFile, notePipe.source));
        Thread keyAnalyzer = new Thread(new KeyAnalyzer(notePipe.sink, noteInKeyPipe.source,
                new Duration(2, 1)));
        Thread metricalConverter = new Thread(new MetricalConverter(noteInKeyPipe.sink, smartNotePipe.source));
        Thread smartNotePrinter = new Thread(new SmartNotePrinter(smartNotePipe.sink));

        musicMXLNoteReader.start();
        keyAnalyzer.start();
        metricalConverter.start();
        smartNotePrinter.start();
    }
}
