package main.java;

public class TimeSig {
    private int beats;
    private int beatType;

    private byte[] basis;

    private static final int BASIS_LENGTH = 6;

    public TimeSig(int beats, int beatType) {
        this.beats = beats;
        this.beatType = beatType;
        basis = new byte[BASIS_LENGTH];

        // Find (a, b) such that beats == 2^a * 3^b, assuming n is of that form
        int twoExponent = 0;
        int threeExponent = 0;
        while (beats % 2 == 0) {
            beats /= 2;
            twoExponent++;
        }
        while (beats % 3 == 0) {
            beats /= 3;
            threeExponent++;
        }
        if (beats != 1) {
            throw new RuntimeException("n is not a product of threes and twos");
        }

        if (twoExponent + threeExponent >= BASIS_LENGTH) {
            throw new RuntimeException("Basis container not long enough to represent time signature");
        }
        for (int i = 0; i < twoExponent; i++) {
            basis[i] = 2;
        }
        for (int i = twoExponent; i < twoExponent + threeExponent; i++) {
            basis[i] = 3;
        }
        for (int i = twoExponent + threeExponent; i < BASIS_LENGTH; i++) {
            basis[i] = 2;
        }
    }

    public int getBeats() {
        return beats;
    }

    public int getBeatType() {
        return beatType;
    }

    public byte[] getBasis() {
        return basis.clone();
    }

    public static void main(String[] args) {
        TimeSig timeSig = new TimeSig(4, 8);
        byte[] basis = timeSig.getBasis();
        for (byte b : basis) {
            System.out.println(b);
        }
    }
}
