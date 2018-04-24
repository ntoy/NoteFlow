package main.java;

public class KeyAnalyzer implements Runnable {

    private Pipe<NoteInRhythm>.PipeSink inputPipe;
    private Pipe<NoteInKey>.PipeSource outputPipe;
    private Duration radius;

    // Major: tonic1.0 dominant0.8 leading0.6 mediant0.6 subdom0.4 submed0.4 supertonic0.2 4#0.1 7b0.05 5#0.05
    // Minor: tonic1.0 dominant0.8 leading0.6 7th0.4 mediant0.6 subdom0.4 submed0.4 supert0.2 4#0.1
    private static final float[] MAJ_DEGREE_SCORES =
            {1.0f, 0.0f, 0.2f, 0.0f, 0.6f, 0.4f, 0.1f, 0.8f, 0.05f, 0.4f, 0.0f, 0.6f};
    private static final double[] MIN_DEGREE_SCORES =
            {1.0f, 0.0f, 0.2f, 0.6f, 0.0f, 0.4f, 0.1f, 0.8f, 0.4f, 0.0f, 0.4f, 0.6f};

    public KeyAnalyzer(Pipe<NoteInRhythm>.PipeSink inputPipe, Pipe<NoteInKey>.PipeSource outputPipe, Duration radius) {
        this.inputPipe = inputPipe;
        this.outputPipe = outputPipe;
        this.radius = radius;
    }

    @Override
    public void run() {
        float[] keyScores = new float[24];
        Node windowCenter = new Node(inputPipe.read());
        Node windowStart = windowCenter, windowEnd = windowCenter;
        // the right side of the window shall be three quarters the size of the left side
        Duration rRadius = radius.multiply(new Duration(3, 4));
        NoteInRhythm nextNote = null;

        while (windowCenter != null && windowCenter.content != null) {
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
            while (windowCenter.content.getOnsetTime().add(rRadius).compareTo(windowEnd.content.getOnsetTime()) > 0) {
                nextNote = inputPipe.read();
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
            outputPipe.write(new NoteInKey(windowCenter.content, maxScoreKey));
            windowCenter = windowCenter.next;
        }
        outputPipe.close();
    }

    private static class Node {
        private NoteInRhythm content;
        private Node next;

        private Node(NoteInRhythm content) {
            this.content = content;
            this.next = null;
        }
    }
}
