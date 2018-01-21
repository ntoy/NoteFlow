package main.java;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;


// TODO: handle transposition
// TODO: handle chords
// TODO: handle multiple parts
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
            int divisions = 0;

            CompareByVoice compareByVoice = new CompareByVoice();
            CompareByOnsetTime compareByOnsetTime = new CompareByOnsetTime();

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

            // iterate over measures
            for (int i = 0; i < measureList.getLength(); i++) {
                Node measureNode = measureList.item(i);
                ArrayList<Note> notes = new ArrayList<>();

                // get list of parts
                NodeList partList = null;
                try {
                    partList = (NodeList) xPath.compile("./part").evaluate(
                            measureNode, XPathConstants.NODESET);
                } catch (XPathExpressionException e) {
                    e.printStackTrace();
                    System.exit(1);
                }

                // iterate over parts
                for (int j = 0; j < partList.getLength(); j++) {
                    Node partNode = partList.item(j);

                    Element divisionsNode = null;

                    // get number of divisions in part in measure
                    try {
                        divisionsNode = (Element) xPath.compile("./attributes/divisions")
                                .evaluate(partNode, XPathConstants.NODE);
                    } catch (XPathExpressionException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                    if (divisionsNode != null)
                        divisions = Integer.parseInt(divisionsNode.getTextContent());

                    // get list of notes in part in measure
                    NodeList noteBackupList = null;
                    try {
                        noteBackupList = (NodeList) xPath.compile("./note | ./backup").evaluate(
                                partNode, XPathConstants.NODESET);
                    } catch (XPathExpressionException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }

                    // iterate over notes/backups in measure
                    for (int k = 0; k < noteBackupList.getLength(); k++) {
                        Node noteOrBackup = noteBackupList.item(k);

                        if (noteOrBackup.getNodeName().equals("note")) {
                            Node noteNode = noteOrBackup;

                            // TODO: optimize by compiling expressions outside of loop
                            Element pitchStepElement = null, pitchOctaveElement = null, pitchAlterElement = null,
                                    durationElement = null, restElement = null, voiceElement = null;

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
                                voiceElement = (Element) xPath.compile("./voice")
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
                                int voice = Integer.parseInt(voiceElement.getTextContent());
                                Note note = new Note(pitch, curTime, duration, voice);
                                notes.add(note);
                            }

                            // advance time by duration of note
                            curTime = curTime.add(duration);
                        }
                        else { // if is backup
                            Node backupNode = noteOrBackup;
                            Element backupDurElement = null;
                            try {
                                backupDurElement = (Element) xPath.compile("./duration")
                                        .evaluate(backupNode, XPathConstants.NODE);
                            } catch (XPathExpressionException e) {
                                e.printStackTrace();
                                System.exit(1);
                            }
                            int backupDur = Integer.parseInt(backupDurElement.getTextContent());
                            curTime = curTime.add(new MusicXMLDur(-backupDur, divisions));
                        }
                    }
                }
                // lexicographically sort by onset time (major) and voice (minor)
                Collections.sort(notes, compareByVoice);
                Collections.sort(notes, compareByOnsetTime);

                for (Note note : notes) {
                    try {
                        queue.put(Optional.of(note));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
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

    private class CompareByVoice implements Comparator<Note> {
        @Override
        public int compare(Note o1, Note o2) {
            return o1.getVoice() - o2.getVoice();
        }
    }

    private class CompareByOnsetTime implements Comparator<Note> {
        @Override
        public int compare(Note o1, Note o2) {
            return o1.getOnsetTime().compareTo(o2.getOnsetTime());
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
