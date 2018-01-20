package main.java;

public class Note {

    private Pitch pitch;
    private MusicXMLAbsTime onsetTime;
    private MusicXMLDur duration;

    private int part;

    public Note(Pitch pitch, MusicXMLAbsTime onsetTime, MusicXMLDur duration, int part) {
        this.pitch = pitch;
        this.onsetTime = onsetTime;
        this.duration = duration;
        this.part = part;
    }

    public Pitch getPitch() {
        return pitch;
    }

    public MusicXMLAbsTime getOnsetTime() {
        return onsetTime;
    }

    public MusicXMLDur getDuration() {
        return duration;
    }

    public int getPart() {
        return part;
    }

    public void setPitch(Pitch pitch) {
        this.pitch = pitch;
    }

    public void setOnsetTime(MusicXMLAbsTime onsetTime) {
        this.onsetTime = onsetTime;
    }

    public void setDuration(MusicXMLDur duration) {
        this.duration = duration;
    }

    public void setPart(int part) {
        this.part = part;
    }

    @Override
    public String toString() {
        return pitch.toString() + " @ " + onsetTime.toString() + " for " + duration.toString()
                + "in [part:" + part + "]";
    }
}
