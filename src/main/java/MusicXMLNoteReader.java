package main.java;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MusicXMLNoteReader implements Runnable {

    private static final int MAX_VOICES = 8;
    private static final Comparator<Note> compareByVoice = Comparator.comparingInt(Note::getVoice);
    private static final Comparator<Note> compareByPitch = Comparator.comparing(Note::getPitch);
    private static final Comparator<Note> compareByOnsetTime = Comparator.comparing(Note::getOnsetTime);
    private static final XPath xPath = XPathFactory.newInstance().newXPath();

    private static XPathExpression measureExpr, partExpr, divisionsExpr, beatsExpr, beatTypeExpr,
    noteBackupExpr, pitchStepExpr, pitchOctaveExpr, pitchAlterExpr, durationExpr, restExpr, voiceExpr,
    chordExpr, tieExpr;

    static {
        try {
            measureExpr = xPath.compile("//measure");
            partExpr = xPath.compile("./part");
            divisionsExpr = xPath.compile("./attributes/divisions");
            beatsExpr = xPath.compile("./attributes/time/beats");
            beatTypeExpr = xPath.compile("./attributes/time/beat-type");
            noteBackupExpr = xPath.compile("./note | ./backup");
            pitchStepExpr = xPath.compile("./pitch/step");
            pitchOctaveExpr = xPath.compile("./pitch/octave");
            pitchAlterExpr = xPath.compile("./pitch/alter");
            durationExpr = xPath.compile("./duration");
            restExpr = xPath.compile("./rest");
            voiceExpr = xPath.compile("./voice");
            chordExpr = xPath.compile("./chord");
            tieExpr = xPath.compile("./tie");
        } catch (XPathExpressionException e) {
            throw new RuntimeException("Failed to compile XPath expressions");
        }
    }

    private File inputFile;
    private Pipe<Note>.PipeSource outputPipe;

    public MusicXMLNoteReader(File inputFile, Pipe<Note>.PipeSource outputPipe) {
        this.inputFile = inputFile;
        this.outputPipe = outputPipe;
    }

    @Override
    public void run() {
        // TODO: handle time sig changes (currently assuming constant time sig)

        int divisions = 0;
        TimeSig timeSig = null;

        // invariant: each list in outstandingTiesPerVoice is sorted by onset time
        ArrayList<ArrayList<Note>> outstandingTiesPerVoice = new ArrayList<>(MAX_VOICES);
        for (int i = 0; i < MAX_VOICES; i++) {
            outstandingTiesPerVoice.add(new ArrayList<>());
        }

        DecentArrayList<Note> notesToOutput = new DecentArrayList<>();
        Duration prevDur = Duration.ZERO; // necessary in case we discover we're in a chord

        // says whether time sig or number of divisions per quarter note has changed
//        boolean timeChange = false;

        // list of measure-relative dur/dur in divisions correspondences at each time change
//        ArrayList<Pair<Integer, AbsoluteTime>> durCheckpoints = new ArrayList<>();

        // start at time zero
//        int curTimeInDivs = 0;
        AbsoluteTime curTime = AbsoluteTime.ZERO;

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = null;

        try {
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            System.exit(1);
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

        // get list of measures
        NodeList measureList = null;
        try {
            measureList = (NodeList) measureExpr.evaluate(doc, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            failEvaluate();
        }

        // iterate over measures
        for (int i = 0; i < measureList.getLength(); i++) {
            Node measureNode = measureList.item(i);

            // get list of parts
            NodeList partList = null;
            try {
                partList = (NodeList) partExpr.evaluate(measureNode, XPathConstants.NODESET);
            } catch (XPathExpressionException e) {
                failEvaluate();
            }

            // iterate over parts
            for (int j = 0; j < partList.getLength(); j++) {
                Node partNode = partList.item(j);

                Element divisionsNode = null;
                Element timeSigBeatsNode= null;
                Element timeSigBeatTypeNode = null;

                // get number of divisions in part in measure
                try {
                    divisionsNode = (Element) divisionsExpr.evaluate(partNode, XPathConstants.NODE);
                } catch (XPathExpressionException e) {
                    failEvaluate();
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
                    timeSigBeatsNode = (Element) beatsExpr.evaluate(partNode, XPathConstants.NODE);
                } catch (XPathExpressionException e) {
                    failEvaluate();
                }
                try {
                    timeSigBeatTypeNode = (Element) beatTypeExpr.evaluate(
                            partNode, XPathConstants.NODE);
                } catch (XPathExpressionException e) {
                    failEvaluate();
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
                    noteBackupList =
                            (NodeList) noteBackupExpr.evaluate(partNode, XPathConstants.NODESET);
                } catch (XPathExpressionException e) {
                    failEvaluate();
                }

                // iterate over notes/backups in measure
                for (int k = 0; k < noteBackupList.getLength(); k++) {
                    Node noteOrBackup = noteBackupList.item(k);

                    if (noteOrBackup.getNodeName().equals("note")) {
                        Node noteNode = noteOrBackup;

                        Element pitchStepElement = null, pitchOctaveElement = null,
                                pitchAlterElement = null, durationElement = null,
                                restElement = null, voiceElement = null,
                                chordElement = null;
                        NodeList ties = null;

                        try {
                            pitchStepElement = (Element)
                                    pitchStepExpr.evaluate(noteNode, XPathConstants.NODE);
                            pitchOctaveElement = (Element)
                                    pitchOctaveExpr.evaluate(noteNode, XPathConstants.NODE);
                            pitchAlterElement = (Element)
                                    pitchAlterExpr.evaluate(noteNode, XPathConstants.NODE);
                            durationElement = (Element)
                                    durationExpr.evaluate(noteNode, XPathConstants.NODE);
                            restElement = (Element)
                                    restExpr.evaluate(noteNode, XPathConstants.NODE);
                            voiceElement = (Element)
                                    voiceExpr.evaluate(noteNode, XPathConstants.NODE);
                            chordElement = (Element)
                                    chordExpr.evaluate(noteNode, XPathConstants.NODE);
                            ties = (NodeList) tieExpr.evaluate(noteNode, XPathConstants.NODESET);
                        } catch (XPathExpressionException e) {
                            failEvaluate();
                        }

                        // only increment time if this note is not on top of the previous
                        if (chordElement == null) {
                            curTime = curTime.add(prevDur);
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
                                notesToOutput.add(continuedNote);
                            }
                            else if (isTieStart) {
                                continuedNote = new Note(pitch, curTime, duration, timeSig, voice);
                                outstandingTies.add(continuedNote);
                            }
                            else {
                                notesToOutput.add(new Note(pitch, curTime, duration, timeSig, voice));
                            }
                        }

                        prevDur = duration;
                    }
                    else { // if is backup
                        Node backupNode = noteOrBackup;
                        Element backupDurElement = null;
                        try {
                            backupDurElement = (Element)
                                    durationExpr.evaluate(backupNode, XPathConstants.NODE);
                        } catch (XPathExpressionException e) {
                            failEvaluate();
                        }
                        int backupDurInDivs = Integer.parseInt(backupDurElement.getTextContent());
                        curTime = curTime.add(new Duration(-backupDurInDivs * timeSig.getBeatType(),
                                4 * divisions * timeSig.getBeats()));
                    }
                }
            }
            // lexicographically sort by (onset time, pitch, voice)
            Collections.sort(notesToOutput, compareByVoice);
            Collections.sort(notesToOutput, compareByPitch);
            Collections.sort(notesToOutput, compareByOnsetTime);

            AbsoluteTime oldestOutstandingTieOnset = curTime.add(prevDur);
            for (int v = 0; v < MAX_VOICES; v++) {
                ArrayList<Note> outstandingTies = outstandingTiesPerVoice.get(v);
                if (outstandingTies.size() > 0) {
                    AbsoluteTime oldestForThisVoice = outstandingTies.get(0).getOnsetTime();
                    if (oldestForThisVoice.compareTo(oldestOutstandingTieOnset) < 0) {
                        oldestOutstandingTieOnset = oldestForThisVoice;
                    }
                }
            }

            int n;
            for (n = 0; n < notesToOutput.size(); n++) {
                Note note = notesToOutput.get(n);
                if (note.getOnsetTime().compareTo(oldestOutstandingTieOnset) >= 0)
                    break;
                outputPipe.write(note);
            }
            notesToOutput.removeRange(0, n);
        }
        outputPipe.close();
    }

    private static void failEvaluate() {
        throw new RuntimeException("Failed to evaluate XPath expression");
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
}
