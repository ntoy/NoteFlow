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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MusicXMLNoteReader implements Runnable {

    private File inputFile;
    private Pipe<Note>.PipeSource outputPipe;

    public MusicXMLNoteReader(File inputFile, Pipe<Note>.PipeSource outputPipe) {
        this.inputFile = inputFile;
        this.outputPipe = outputPipe;
    }

    @Override
    public void run() {
        int divisions = 0;

        CompareByVoice compareByVoice = new CompareByVoice();
        CompareByOnsetTime compareByOnsetTime = new CompareByOnsetTime();

        // start at time zero
        MusicXMLAbsTime curTime = new MusicXMLAbsTime(0, 0, 1);

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
                outputPipe.write(note);
            }
        }
        outputPipe.close();
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
