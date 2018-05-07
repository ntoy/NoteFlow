package main.java;

import javax.sound.midi.*;
import java.io.*;
import java.util.Arrays;

import static javax.sound.midi.ShortMessage.NOTE_OFF;
import static javax.sound.midi.ShortMessage.NOTE_ON;
import static main.java.Constants.*;

public class Decoder {
    private static int TICKS_PER_MEASURE = 1440;

    public static void main(String[] args) {
        String inputFilename;
        int homeKey;
        String outputFilename;
        try {
            inputFilename = args[0];
            homeKey = Integer.parseInt(args[1]);
            outputFilename = args[2];
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
            System.out.println("Usage: java Decoder [input file] [home key] [output file]");
            return;
        }

        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(inputFilename));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Input file not found");
        }

        File midiFile = new File(outputFilename);
        Sequence midiSeq = null;
        try {
            midiSeq = new Sequence(Sequence.PPQ,TICKS_PER_MEASURE/4, MAX_VOICES);
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException("Failed to make midi sequence");
        }
        Track[] tracks = midiSeq.getTracks();

        int[][] curTreeNotationByVoice = new int[MAX_VOICES][BASIS_LENGTH + 1];
        byte[] prevBasis = new byte[BASIS_LENGTH];
        for (int i = 0; i < prevBasis.length; i++) {
            prevBasis[i] = 2;
        }

        String line;

        try {
            while ((line = reader.readLine()) != null) {
                String[] list = line.split("\\s+");
                int[] vector = new int[VECTOR_LENGTH];
                if (list.length != VECTOR_LENGTH) {
                    throw new RuntimeException("Illegal vector length");
                }
                try {
                    for (int i = 0; i < VECTOR_LENGTH; i++) {
                        vector[i] = Integer.parseInt(list[i]);
                    }
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Vector contains non-integer");
                }
                for (int i = 0; i < VECTOR_LENGTH; i++) {
                    System.out.println(i + ": " + vector[i]);
                }

                // parse
                int keyCircleFifths = vector[0];
                int keyMode = vector[1];
                int relPitch = vector[2];
                int octave = vector[3];
                int[] basisInt = Arrays.copyOfRange(vector, 4, 4 + BASIS_LENGTH);
                byte[] basis = new byte[BASIS_LENGTH];
                for (int i = 0; i < BASIS_LENGTH; i++) {
                    basis[i] = (byte) basisInt[i];
                }
                int onsetDepth = vector[BASIS_LENGTH + 4];
                int onsetIncrement = vector[BASIS_LENGTH + 5];
                int durDepth = vector[BASIS_LENGTH + 6];
                int durIncrement = vector[BASIS_LENGTH + 7];
                int voice = vector[BASIS_LENGTH + 8];

                int[] curTreeNotation = curTreeNotationByVoice[voice];

                // determine absolute pitch
                Pitch pitch = new Pitch(octave, relPitch, keyCircleFifths, homeKey, keyMode);
                int midiIndex = pitch.getMidiIndex();
                // correct octave
                while (midiIndex < 0)
                    midiIndex += 12;
                while (midiIndex >= 128)
                    midiIndex -= 12;

                // round up time in case of basis change
                int depthOfChange = firstDifferingIndex(basis, prevBasis);
                if (depthOfChange >= 0) {
                    curTreeNotation = roundUpTreeNotationAtDepth(curTreeNotation, basis, depthOfChange);
                }

                // determine onset time of new note in tree notation
                curTreeNotation = roundUpTreeNotationAtDepth(curTreeNotation, basis, onsetDepth);
                int[] incrementor = new int[curTreeNotation.length];
                incrementor[onsetDepth] = onsetIncrement;
                curTreeNotation = HierarchicalRelTime.treeNotationSum(curTreeNotation, incrementor, basis);
                long onsetTicks = ticksOfTreeNotation(curTreeNotation, basis, TICKS_PER_MEASURE);

                // determine offset time of new note in tree notation
                int[] offsetTreeNotation = roundUpTreeNotationAtDepth(curTreeNotation, basis, durDepth);
                if (Arrays.equals(offsetTreeNotation, curTreeNotation)) {
                    durIncrement++;
                }
                incrementor = new int[curTreeNotation.length];
                incrementor[durDepth] = durIncrement;
                offsetTreeNotation = HierarchicalRelTime.treeNotationSum(offsetTreeNotation, incrementor, basis);
                long offsetTicks = ticksOfTreeNotation(offsetTreeNotation, basis, TICKS_PER_MEASURE);

                tracks[voice].add(new MidiEvent(makeNote(midiIndex, NOTE_ON), onsetTicks));
                tracks[voice].add(new MidiEvent(makeNote(midiIndex, NOTE_OFF), offsetTicks));

                curTreeNotationByVoice[voice] = curTreeNotation;
                prevBasis = basis;
            }

            MidiSystem.write(midiSeq, 1, midiFile);
        } catch (IOException e) {
            throw new RuntimeException("IO Exception!");
        }
    }

    static int firstDifferingIndex(byte[] a, byte[] b) {
        if (a.length != b.length)
            return a.length < b.length ? a.length : b.length;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return i;
            }
        }
        return -1;
    }

    static int[] roundUpTreeNotationAtDepth(int[] treeNotation, byte[] basis, int depth) {
        int[] treeNotationCopy = treeNotation.clone();
        boolean aligned = true;
        for (int i = depth + 1; i < treeNotationCopy.length; i++) {
            if (treeNotationCopy[i] != 0) {
                aligned = false;
                treeNotationCopy[i] = 0;
            }
        }
        if (aligned) {
            return treeNotationCopy;
        }
        else {
            int[] incrementor = new int[treeNotationCopy.length];
            incrementor[depth] = 1;
            return HierarchicalRelTime.treeNotationSum(treeNotationCopy, incrementor, basis);
        }
    }

    static long ticksOfTreeNotation(int[] treeNotation, byte[] basis, int ticksPerMeasure) {
        long multiplier = ticksPerMeasure;
        long ticks = 0;
        for (int i = 0; i < treeNotation.length; i++) {
            ticks += treeNotation[i] * multiplier;
            if (i < treeNotation.length - 1) {
                multiplier /= basis[i];
            }
        }
        return ticks;
    }

    private static ShortMessage makeNote(int midiIndex, int type) {
        if (midiIndex < 0 || midiIndex >= 128) {
            throw new IllegalArgumentException("midiIndex must be in [0, 127)");
        }
        if (type != NOTE_ON && type != NOTE_OFF) {
            throw new IllegalArgumentException("type must be NOTE_ON or NOTE_OFF");
        }
        ShortMessage message = null;
        try {
            message = new ShortMessage(type, 0, midiIndex, 127);
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }
        return message;
    }
}
