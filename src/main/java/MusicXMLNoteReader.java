package main.java;

import org.javatuples.Pair;
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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MusicXMLNoteReader implements Runnable {

    private static final int MAX_VOICES = 8;

    private File inputFile;
    private Pipe<Note>.PipeSource outputPipe;

    public MusicXMLNoteReader(File inputFile, Pipe<Note>.PipeSource outputPipe) {
        this.inputFile = inputFile;
        this.outputPipe = outputPipe;
    }

    @Override
    public void run() {
        // TODO: handle tied notes (extra complication for those that go over a barline)
        // TODO: handle time sig changes (currently assuming constant time sig)

        int divisions = 0;
        TimeSig timeSig = null;
        ArrayList<ArrayList<Note>> outstandingTiesPerVoice = new ArrayList<>(MAX_VOICES);
        for (int i = 0; i < MAX_VOICES; i++) {
            outstandingTiesPerVoice.add(new ArrayList<>());
        }

        // says whether time sig or number of divisions per quarter note has changed
//        boolean timeChange = false;

        // list of measure-relative dur/dur in divisions correspondences at each time change
//        ArrayList<Pair<Integer, AbsoluteTime>> durCheckpoints = new ArrayList<>();

        CompareByVoice compareByVoice = new CompareByVoice();
        CompareByOnsetTime compareByOnsetTime = new CompareByOnsetTime();

        // start at time zero
//        int curTimeInDivs = 0;
        AbsoluteTime curTime = AbsoluteTime.ZERO;

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
                Element timeSigBeatsNode= null;
                Element timeSigBeatTypeNode = null;

                // get number of divisions in part in measure
                try {
                    divisionsNode = (Element) xPath.compile("./attributes/divisions")
                            .evaluate(partNode, XPathConstants.NODE);
                } catch (XPathExpressionException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
                if (divisionsNode != null) {
//                    int newDivisions = Integer.parseInt(divisionsNode.getTextContent());
//                    if (newDivisions != divisions) {
//                        timeChange = true;
//                        divisions = newDivisions;
//                    }
                    divisions = Integer.parseInt(divisionsNode.getTextContent());
                }

                // get time signature
                try {
                    timeSigBeatsNode = (Element) xPath.compile("./attributes/time/beats").evaluate(
                            partNode, XPathConstants.NODE);
                } catch (XPathExpressionException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
                try {
                    timeSigBeatTypeNode = (Element) xPath.compile("./attributes/time/beat-type").evaluate(
                            partNode, XPathConstants.NODE);
                } catch (XPathExpressionException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
                if (timeSigBeatsNode != null && timeSigBeatTypeNode != null) {
//                    TimeSig newTimeSig = new TimeSig(Integer.parseInt(timeSigBeatsNode.getTextContent()),
//                            Integer.parseInt(timeSigBeatTypeNode.getTextContent()));
//                    if (!newTimeSig.equals(timeSig)) {
//                        timeChange = true;
//                        timeSig = newTimeSig;
//                    }
                    timeSig = new TimeSig(Integer.parseInt(timeSigBeatsNode.getTextContent()),
                            Integer.parseInt(timeSigBeatTypeNode.getTextContent()));
                }
                if (timeSig == null)
                    throw new IllegalStateException("No time signature found");

//                if (timeChange) {
//                    durCheckpoints.add(new Pair<>(curTimeInDivs, curTime));
//                    timeChange = false;
//                }

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
                        NodeList ties = null;

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
                            ties = (NodeList) xPath.compile("./tie")
                                    .evaluate(noteNode, XPathConstants.NODESET);
                        } catch (XPathExpressionException e) {
                            e.printStackTrace();
                            System.exit(1);
                        }

                        int durInDivs = Integer.parseInt(durationElement.getTextContent());
                        Duration duration = new Duration(durInDivs * timeSig.getBeatType(),
                                4 * divisions * timeSig.getBeats());

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

                            // handle ties
                            boolean isTieStart = false, isTieStop = false;
                            if (ties != null) {
                                // figure out what types of ties we have (start, stop...)
                                assert (ties.getLength() == 1 || ties.getLength() == 2);
                                for (int m = 0; m < ties.getLength(); m++) {
                                    Element tie = (Element) ties.item(m);
                                    String tieType = tie.getAttribute("type");
                                    if (tieType.equals("start"))
                                        isTieStart = true;
                                    else if (tieType.equals("stop"))
                                        isTieStop = true;
                                    else
                                        throw new RuntimeException("Illegal tie type");
                                }
                            }

                            Note continuedNote = null;
                            ArrayList<Note> outstandingTies = outstandingTiesPerVoice.get(voice);
                            int continuedNoteIdx = 0;

                            if (isTieStop && isTieStart) {
                                // find which note we are continuing
                                while (!(outstandingTies.get(continuedNoteIdx).getPitch().equals(pitch))) {
                                    continuedNoteIdx++;
                                }
                                if (continuedNoteIdx == outstandingTies.size()) {
                                    throw new RuntimeException("Found tie stop without corresponding tie start");
                                }

                                // extend duration of tie chain by that of new note
                                continuedNote = outstandingTies.get(continuedNoteIdx);
                                continuedNote.setDuration(continuedNote.getDuration().add(duration));
                                outstandingTies.set(continuedNoteIdx, continuedNote);
                            }
                            else if (isTieStop) {
                                // find which note we are ending
                                while (!(outstandingTies.get(continuedNoteIdx).getPitch().equals(pitch))) {
                                    continuedNoteIdx++;
                                }
                                if (continuedNoteIdx == outstandingTies.size()) {
                                    throw new RuntimeException("Found tie stop without corresponding tie start");
                                }

                                // extend duration of tie chain by that of new note, and output
                                continuedNote = outstandingTies.remove(continuedNoteIdx);
                                continuedNote.setDuration(continuedNote.getDuration().add(duration));
                                notes.add(continuedNote);
                            }
                            else if (isTieStart) {
                                continuedNote = new Note(pitch, curTime, duration, timeSig, voice);
                                outstandingTies.add(continuedNote);
                            }
                            else {
                                notes.add(new Note(pitch, curTime, duration, timeSig, voice));
                            }
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
                        int backupDurInDivs = Integer.parseInt(backupDurElement.getTextContent());
                        curTime = curTime.add(new Duration(-backupDurInDivs * timeSig.getBeatType(),
                                4 * divisions * timeSig.getBeats()));
                    }
                }
            }
            // lexicographically sort by onset time (major) and voice (minor)
            Collections.sort(notes, compareByVoice);
            Collections.sort(notes, compareByOnsetTime);

            for (Note note : notes) {
                outputPipe.write(note);
            }
        }
        outputPipe.close();
    }

    public static void main(String[] args) {
        MusicXMLPreprocess.preprocessMusicXMLs("main/res/MusicXML/original",
                "main/res/MusicXML/partwise",
                "main/res/MusicXML/timewise",
                "main/res/musicxml30");
        File inputFile = new File(args[0]);
        Pipe<Note> notePipe = new Pipe<>(16);
        Thread musicMXLNoteReader = new Thread(new MusicXMLNoteReader(inputFile, notePipe.source));
        Thread printer = new Thread(new Printer(notePipe.sink));
        musicMXLNoteReader.start();
        printer.start();
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
}
