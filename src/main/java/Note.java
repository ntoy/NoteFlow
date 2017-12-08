package main.java;

public class Note {

    private Pitch pitch;
    private MusicXMLAbsTime onsetTime;
    private MusicXMLDur duration;

    public Note(Pitch pitch, MusicXMLAbsTime onsetTime, MusicXMLDur duration) {
        this.pitch = pitch;
        this.onsetTime = onsetTime;
        this.duration = duration;
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

    public void setPitch(Pitch pitch) {
        this.pitch = pitch;
    }

    public void setOnsetTime(MusicXMLAbsTime onsetTime) {
        this.onsetTime = onsetTime;
    }

    public void setDuration(MusicXMLDur duration) {
        this.duration = duration;
    }

    @Override
    public String toString() {
        return pitch.toString() + " @ " + onsetTime.toString() + " for " + duration.toString();
    }
}
