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
        Thread musicMXLNoteReader = new Thread(new MusicMXLNoteReader(inputFile, notePipe.source));
        Pipe<NoteInContext> noteInContextPipe = new Pipe<>(16);
        Thread keyAnalyzer = new Thread(new KeyAnalyzer(notePipe.sink, noteInContextPipe.source,
                new MusicXMLDur(8, 1)));
        Thread printer = new Thread(new Printer(noteInContextPipe.sink));

        musicMXLNoteReader.start();
        keyAnalyzer.start();
        printer.start();
    }
}
