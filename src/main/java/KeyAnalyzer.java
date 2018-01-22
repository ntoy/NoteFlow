package main.java;

import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;

public class KeyAnalyzer {
    private ArrayBlockingQueue<Optional<NoteInContext>> queue;
    private Boolean streamOver; // whether we're done with all notes to read
    private Thread noteProducer;
    private volatile boolean keepGoing; // whether the client still wishes to keep reading
    private MusicXMLDur radius;

    private static final int BUFFER_SIZE = 32;
    // Major: tonic1.0 dominant0.8 leading0.6 mediant0.6 subdom0.4 submed0.4 supertonic0.2 4#0.1 7b0.05 5#0.05
    // Minor: tonic1.0 dominant0.8 leading0.6 7th0.4 mediant0.6 subdom0.4 submed0.4 supert0.2 4#0.1
    private static final float[] MAJ_DEGREE_SCORES =
            {1.0f, 0.0f, 0.2f, 0.0f, 0.6f, 0.4f, 0.1f, 0.8f, 0.05f, 0.4f, 0.0f, 0.6f};
    private static final double[] MIN_DEGREE_SCORES =
            {1.0f, 0.0f, 0.2f, 0.6f, 0.0f, 0.4f, 0.1f, 0.8f, 0.4f, 0.0f, 0.4f, 0.6f};

    public KeyAnalyzer(String filename, MusicXMLDur radius) {
        streamOver = false;
        keepGoing = true;
        this.radius = radius;
        queue = new ArrayBlockingQueue<>(BUFFER_SIZE);
        // Create noteProducer thread
        noteProducer = new Thread(new NoteProducerRunnable(filename));
        noteProducer.start();
    }

    public NoteInContext getNext() {
        if (keepGoing) {
            // Create consumer thread
            if (streamOver) {
                return null;
            } else {
                Optional<NoteInContext> nextNoteContainer = Optional.empty();
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
            float[] keyScores = new float[24];
            MusicXMLNoteReader noteReader = new MusicXMLNoteReader(filename);
            Node windowCenter = new Node(noteReader.getNext());
            Node windowStart = windowCenter, windowEnd = windowCenter;
            Note nextNote = null;

            while (windowCenter != null) {
                // shrink window from the left
                while (windowStart.content.getOnsetTime().add(radius).compareTo(windowCenter.content.getOnsetTime()) < 0) {
                    for (int key = 0; key < 12; key++) {
                        int keyDegree = (windowStart.content.getPitch().getPitchIndex() + 12 - key) % 12;
                        keyScores[key] -= MAJ_DEGREE_SCORES[keyDegree];
                        keyScores[key + 12] -= MIN_DEGREE_SCORES[keyDegree];
                    }
                    windowStart = windowStart.next;
                }
                // expand window to the right
                while (windowCenter.content.getOnsetTime().add(radius).compareTo(windowEnd.content.getOnsetTime()) > 0) {
                    nextNote = noteReader.getNext();
                    if (nextNote == null)
                        break;
                    windowEnd.next = new Node(nextNote);
                    windowEnd = windowEnd.next;
                    for (int key = 0; key < 12; key++) {
                        int keyDegree = (windowEnd.content.getPitch().getPitchIndex() + 12 - key) % 12;
                        keyScores[key] += MAJ_DEGREE_SCORES[keyDegree];
                        keyScores[key + 12] += MIN_DEGREE_SCORES[keyDegree];
                    }
                }
                // find key with max score
                int maxScoreKey = -1;
                float maxScore = -1.0f;
                for (int key = 0; key < 24; key++) {
                    if (keyScores[key] > maxScore) {
                        maxScore = keyScores[key];
                        maxScoreKey = key;
                    }
                }

                // associate note with this key
                try {
                    queue.put(Optional.of(new NoteInContext(windowCenter.content, maxScoreKey)));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.exit(0);
                }
                windowCenter = windowCenter.next;
            }
            try {
                queue.put(Optional.empty());
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    private class Node {
        private Note content;
        private Node next;

        private Node(Note content) {
            this.content = content;
            this.next = null;
        }
    }

    public static void main(String[] args) {
        String path = "/Users/Nico/Princeton/Thesis/Thesis_project/out/production/Thesis_project/main/res/MusicXML/timewise/example"
                + args[0] + "_timewise.xml";
        KeyAnalyzer keyAnalyzer = new KeyAnalyzer(path, new MusicXMLDur(8, 1));

        NoteInContext note;
        while((note = keyAnalyzer.getNext()) != null) {
            System.out.println(note);
        }
    }
}
