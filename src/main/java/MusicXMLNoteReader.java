package main.java;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;


// TODO: handle backup
// TODO: handle transposition
// TODO: handle chords
// TODO: handler multiple parts
// TODO: find out what other special cases there may be
// currently assuming single voice, no transposition, constant tempo

public class MusicXMLNoteReader {

    private enum ReaderState {
        DEFAULT, IN_NOTE, IN_STEP, IN_OCTAVE, IN_DURATION, IN_DIVS, IN_ALTER
    }

    private ArrayBlockingQueue<Optional<Note>> queue;
    private Boolean streamOver; // whether we're done with all notes to read
    private Thread noteProducer;
    private volatile boolean keepGoing; // whether the client still wishes to keep reading

    private static final int BUFFER_SIZE = 32;

    public MusicXMLNoteReader(String filename) {
        streamOver = false;
        keepGoing = true;
        queue = new ArrayBlockingQueue<>(BUFFER_SIZE);
        // Create noteProducer thread
        noteProducer = new Thread(new NoteProducerRunnable(filename));
        noteProducer.start();
    }

    public Note getNext() {
        if (keepGoing) {
            // Create consumer thread
            if (streamOver) {
                return null;
            } else {
                Optional<Note> nextNoteContainer = Optional.empty();
                try {
                    nextNoteContainer = queue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!nextNoteContainer.isPresent()) {
                    streamOver = true;
                    try {
                        noteProducer.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
                else {
                    return nextNoteContainer.get();
                }
            }
        }
        else return null; // if client asked to stop feed, they get nothing
    }

    public void close() {
        keepGoing = false;
        queue.clear();
        try {
            noteProducer.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        queue = null;
    }

    private class NoteProducerRunnable implements Runnable {

        private String filename;

        public NoteProducerRunnable(String filename) {
            this.filename = filename;
        }

        @Override
        public void run() {
            // start at time zero
            MusicXMLAbsTime curTime = new MusicXMLAbsTime(0, 0, 1);

            File inputFile = new File(filename);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = null;

            try {
                dBuilder = dbFactory.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            }

            Document doc = null;
            try {
                doc = dBuilder.parse(inputFile);
            } catch (SAXException e) {
                e.printStackTrace();
                System.exit(1);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
            doc.getDocumentElement().normalize();

            XPath xPath = XPathFactory.newInstance().newXPath();

            // get list of measures
            String expression = "//measure";
            NodeList measureList = null;
            try {
                measureList = (NodeList) xPath.compile(expression).evaluate(
                        doc, XPathConstants.NODESET);
            } catch (XPathExpressionException e) {
                e.printStackTrace();
                System.exit(1);
            }

            Element measureDivisionsElement = null;
            int divisions = 0;

            // iterate over measures
            for (int i = 0; i < measureList.getLength(); i++) {
                Node measureNode = measureList.item(i);

                // get number of divisions in measure
                try {
                    measureDivisionsElement = (Element) xPath.compile("./part/attributes/divisions")
                            .evaluate(measureNode, XPathConstants.NODE);
                } catch (XPathExpressionException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
                if (measureDivisionsElement != null)
                    divisions = Integer.parseInt(measureDivisionsElement.getTextContent());

                // get list of notes in measure
                NodeList noteList = null;
                try {
                    noteList = (NodeList) xPath.compile("./part/note").evaluate(
                            measureNode, XPathConstants.NODESET);
                } catch (XPathExpressionException e) {
                    e.printStackTrace();
                    System.exit(1);
                }

                // iterate over notes in measure
                for (int j = 0; j < noteList.getLength(); j++) {
                    Node noteNode = noteList.item(j);
                    // TODO: optimize by compiling expressions outside of loop
                    Element pitchStepElement = null, pitchOctaveElement = null, pitchAlterElement = null,
                            durationElement = null, restElement = null, backupElement;



                    try {
                        pitchStepElement = (Element) xPath.compile("./pitch/step")
                                .evaluate(noteNode, XPathConstants.NODE);
                        pitchOctaveElement = (Element) xPath.compile("./pitch/octave")
                                .evaluate(noteNode, XPathConstants.NODE);
                        pitchAlterElement = (Element) xPath.compile("./pitch/alter")
                                .evaluate(noteNode, XPathConstants.NODE);
                        durationElement = (Element) xPath.compile("./duration")
                                .evaluate(noteNode, XPathConstants.NODE);
                        restElement = (Element) xPath.compile("./rest")
                                .evaluate(noteNode, XPathConstants.NODE);
                    } catch (XPathExpressionException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }

                    MusicXMLDur duration = new MusicXMLDur(Integer.parseInt(durationElement.getTextContent()),
                            divisions);

                    // if this is not a rest
                    if (restElement == null) {
                        String pitchStep = pitchStepElement.getTextContent();
                        int pitchOctave = Integer.parseInt(pitchOctaveElement.getTextContent());
                        // presumably null iff not found?
                        int alter = 0;
                        if (pitchAlterElement != null) {
                            alter = Integer.parseInt(pitchAlterElement.getTextContent());
                        }
                        Pitch pitch = new Pitch(pitchOctave, pitchStep, alter);
                        Note note = new Note(pitch, curTime, duration);
                        try {
                            queue.put(Optional.of(note));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            System.exit(1);
                        }
                    }

                    // advance time by duration of note
                    curTime = curTime.add(duration);
                }
            }
            try {
                queue.put(Optional.empty());
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    public static void main(String args[]) {
        String path = "/Users/Nico/Princeton/Thesis/Thesis_project/out/production/Thesis_project/main/res/MusicXML/timewise/example"
                + args[0] + "_timewise.xml";
        MusicXMLNoteReader noteReader = new MusicXMLNoteReader(path);

        Note note;
        while((note = noteReader.getNext()) != null) {
            System.out.println(note);
        }
    }
}
