package main.java;

import org.apache.commons.math3.fraction.Fraction;
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
import java.util.Comparator;

import static main.java.Constants.MAX_VOICES;

public class MusicXMLNoteReader implements Runnable {

    private static final Comparator<Note> compareByVoice = Comparator.comparingInt(Note::getVoice);
    private static final Comparator<Note> compareByPitch = (o1, o2) -> {
        if (o1.isGhost() && o2.isGhost()) return 0;
        else if (o1.isGhost()) return -1;
        else if (o2.isGhost()) return 1;
        else return o1.getPitch().compareTo(o2.getPitch());
    };
    private static final Comparator<Note> compareByOnsetTime = Comparator.comparing(Note::getOnsetTime);

    private static final XPathExpression measureExpr, partExpr, divisionsExpr, beatsExpr, beatTypeExpr,
    noteBackupExpr, pitchStepExpr, pitchOctaveExpr, pitchAlterExpr, durationExpr, restExpr, voiceExpr,
    chordExpr, tieExpr, tupletExpr, actualNotesExpr, normalNotesExpr, normalTypeExpr, tupletTypeExpr,
    graceExpr, altEndExpr;

    static {
        final XPath xPath = XPathFactory.newInstance().newXPath();
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
            tupletExpr = xPath.compile("./notations/tuplet");
            actualNotesExpr = xPath.compile("./time-modification/actual-notes");
            normalNotesExpr = xPath.compile("./time-modification/normal-notes");
            normalTypeExpr = xPath.compile("./time-modification/normal-type");
            tupletTypeExpr = xPath.compile("./notations/tuplet/tuplet-actual/tuplet-type");
            graceExpr = xPath.compile("./grace");
            altEndExpr = xPath.compile("./part/barline/ending");
        } catch (XPathExpressionException e) {
            throw new RuntimeException("Failed to compile XPath expressions");
        }
    }

    private File inputFile;
    private Pipe<Note>.PipeSource outputPipe;
    private DecentArrayList<Note> notesToOutput;
    private ArrayList<ArrayList<Note>> outstandingTiesPerVoice;
    private AbsoluteTime curTime;
    private Duration prevDur;
    private TimeSig timeSig;
    private TimeSig prevTimeSig;

    public MusicXMLNoteReader(File inputFile, Pipe<Note>.PipeSource outputPipe) {
        this.inputFile = inputFile;
        this.outputPipe = outputPipe;
    }

    @Override
    public void run() {
        int divisions = 0;
        timeSig = null;
        prevTimeSig = null;

        // invariant: each list in outstandingTiesPerVoice is sorted by onset time
        outstandingTiesPerVoice = new ArrayList<>(MAX_VOICES);
        for (int i = 0; i < MAX_VOICES; i++) {
            outstandingTiesPerVoice.add(new ArrayList<>());
        }

        notesToOutput = new DecentArrayList<>();
        // position in notesToOutput where notes found in this measure start
        int startOfNewNotes = 0;
        // necessary in case we discover we're in a chord
        prevDur = Duration.ZERO;

        curTime = AbsoluteTime.ZERO;

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
            Element altEndingNode = null; // node indicating alternate ending for this measure
            Element nextAltEndingNode = null; // node indicating alternate ending for next measure

            // get list of parts
            NodeList partList = null;
            try {
                partList = (NodeList) partExpr.evaluate(measureNode, XPathConstants.NODESET);
            } catch (XPathExpressionException e) {
                failEvaluate();
            }

            // find out if this is an alternate ending that is not the last
            try {
                altEndingNode = (Element) altEndExpr.evaluate(measureNode, XPathConstants.NODE);
                if (i < measureList.getLength() - 1) {
                    nextAltEndingNode = (Element)
                            altEndExpr.evaluate(measureList.item(i+1), XPathConstants.NODE);
                }
            } catch (XPathExpressionException e) {
                failEvaluate();
            }
            if (altEndingNode != null && nextAltEndingNode != null) {
                continue;
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
                    timeSig = new TimeSig(Integer.parseInt(timeSigBeatsNode.getTextContent()),
                            Integer.parseInt(timeSigBeatTypeNode.getTextContent()));
                }
                if (timeSig == null) {
                    timeSig = new TimeSig(4, 4);
                }

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
                                chordElement = null, tupletElement = null,
                                graceElement = null;
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
                            tupletElement = (Element)
                                    tupletExpr.evaluate(noteNode, XPathConstants.NODE);
                            graceElement = (Element)
                                    graceExpr.evaluate(noteNode, XPathConstants.NODE);
                        } catch (XPathExpressionException e) {
                            failEvaluate();
                        }

                        if (graceElement != null) {
                            continue;
                        }

                        // only increment time if this note is not on top of the previous
                        if (chordElement == null) {
                            curTime = curTime.add(prevDur);
                        }

                        int durInDivs = Integer.parseInt(durationElement.getTextContent());
                        Duration duration = new Duration(durInDivs * timeSig.getBeatType(),
                                4 * divisions * timeSig.getBeats());

                        String tupletStartOrStop = "";
                        if (tupletElement != null) {
                            tupletStartOrStop = tupletElement.getAttribute("type");
                        }

                        // if starting tuplet
                        if (tupletStartOrStop.equals("start")) {
                            int numDivs = findTupletNumDivs(noteNode);
                            int oldNumDivs = findTupletOldNumDivs(noteNode);
                            String divUnit = findTupletDivUnit(noteNode);
                            timeSig = timeSig.ofTuplet(numDivs, oldNumDivs, divUnit, duration, curTime);
                        }

                        if (!timeSig.equals(prevTimeSig)) {
                            // force-close all outstanding ties
                            for (ArrayList<Note> outstandingTies : outstandingTiesPerVoice) {
                                notesToOutput.addAll(outstandingTies);
                                outstandingTies.clear();
                            }
                            // insert ghost to indicate location of time sig change
                            notesToOutput.add(Note.newGhost(curTime, timeSig));
                        }

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
                            if (ties != null && ties.getLength() > 0) {
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

                            Note continuedNote;
                            ArrayList<Note> outstandingTies = outstandingTiesPerVoice.get(voice);
                            int continuedNoteIdx = 0;

                            if (isTieStop && isTieStart) {
                                // find which note we are continuing
                                while (continuedNoteIdx < outstandingTies.size() &&
                                        !(outstandingTies.get(continuedNoteIdx).getPitch().equals(pitch))) {
                                    continuedNoteIdx++;
                                }
                                if (continuedNoteIdx == outstandingTies.size()) {
                                    // tie must have been cut short cause of time sig change
                                    // so treat as just tie start
                                    continuedNote = new Note(pitch, curTime, duration, timeSig, voice);
                                    outstandingTies.add(continuedNote);
                                }
                                else {
                                    // extend duration of tie chain by that of new note
                                    continuedNote = outstandingTies.get(continuedNoteIdx);
                                    continuedNote.setDuration(continuedNote.getDuration().add(duration));
                                    outstandingTies.set(continuedNoteIdx, continuedNote);
                                }
                            }
                            else if (isTieStop) {
                                // find which note we are ending
                                while (continuedNoteIdx < outstandingTies.size() &&
                                        !(outstandingTies.get(continuedNoteIdx).getPitch().equals(pitch))) {
                                    continuedNoteIdx++;
                                }
                                if (continuedNoteIdx == outstandingTies.size()) {
                                    // tie must have been cut short cause of time sig change
                                    // so treat as untied note
                                    notesToOutput.add(new Note(pitch, curTime, duration, timeSig, voice));
                                }
                                else {
                                    // extend duration of tie chain by that of new note, and output
                                    continuedNote = outstandingTies.remove(continuedNoteIdx);
                                    continuedNote.setDuration(continuedNote.getDuration().add(duration));
                                    notesToOutput.add(continuedNote);
                                }
                            }
                            else if (isTieStart) {
                                continuedNote = new Note(pitch, curTime, duration, timeSig, voice);
                                outstandingTies.add(continuedNote);
                            }
                            else {
                                notesToOutput.add(new Note(pitch, curTime, duration, timeSig, voice));
                            }
                        }

                        // if ending tuplet
                        if (tupletStartOrStop.equals("stop")) {
                            //cutTiesAndInsertGhost();
                            timeSig = timeSig.getParent();
                        }

                        prevDur = duration;
                        prevTimeSig = timeSig;
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

            // cancel all fake-news ties
            for (ArrayList<Note> outstandingTies : outstandingTiesPerVoice) {
                int numRemoved = 0;
                int initialSize = outstandingTies.size();
                for (int noteIndex = 0; noteIndex < initialSize; noteIndex++) {
                    Note n = outstandingTies.get(noteIndex - numRemoved);
                    if (n.getOnsetTime().add(n.getDuration()).compareTo(curTime.add(prevDur)) < 0) {
                        outstandingTies.remove(noteIndex - numRemoved);
                        notesToOutput.add(n);
                        numRemoved++;
                    }
                }
            }

            // if first measure is not full, assume it is a pickup
            if (i == 0) {
                Fraction measurePosition = curTime.add(prevDur).getMeasureFrac();
                if (!measurePosition.equals(Fraction.ZERO)) {
                    Duration increase = new Duration(Fraction.ONE.subtract(measurePosition));
                    for (int n = startOfNewNotes; n < notesToOutput.size(); n++) {
                        Note note = notesToOutput.get(n);
                        note.setOnsetTime(note.getOnsetTime().add(increase));
                    }
                    AbsoluteTime ghostTime = curTime.add(prevDur).subtract(new Duration(measurePosition));
                    notesToOutput.add(Note.newGhost(ghostTime, timeSig));
                    curTime = curTime.add(increase);
                }
            }

            // lexicographically sort by (onset time, pitch, voice)
            notesToOutput.sort(compareByVoice);
            notesToOutput.sort(compareByPitch);
            notesToOutput.sort(compareByOnsetTime);

            // find oldest outstanding tie onset across all voices
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

            // output all notes occurring before the oldest outstanding tie
            int n;
            for (n = 0; n < notesToOutput.size(); n++) {
                Note note = notesToOutput.get(n);
                if (note.getOnsetTime().compareTo(oldestOutstandingTieOnset) >= 0)
                    break;
                outputPipe.write(note);
            }
            notesToOutput.removeRange(0, n);
            startOfNewNotes = notesToOutput.size();
        }
        outputPipe.close();
    }

    private static int findTupletNumDivs(Node noteNode) {
        Element actualNotes = null;
        try {
            actualNotes = (Element) actualNotesExpr.evaluate(noteNode, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            failEvaluate();
        }
        return Integer.parseInt(actualNotes.getTextContent());
    }

    private static int findTupletOldNumDivs(Node noteNode) {
        Element normalNotes = null;
        try {
            normalNotes = (Element) normalNotesExpr.evaluate(noteNode, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            failEvaluate();
        }
        return Integer.parseInt(normalNotes.getTextContent());
    }

    private static String findTupletDivUnit(Node noteNode) {
        Element normalType = null;
        try {
            normalType = (Element) normalTypeExpr.evaluate(noteNode, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            failEvaluate();
        }
        if (normalType != null) {
            return normalType.getTextContent();
        }
        else {
            Element tupletType = null;
            try {
                tupletType = (Element) tupletTypeExpr.evaluate(noteNode, XPathConstants.NODE);
            } catch (XPathExpressionException e) {
                failEvaluate();
            }
            if (tupletType != null) {
                return tupletType.getTextContent();
            }
            else {
                return null;
            }
        }
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
