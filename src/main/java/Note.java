package main.java;

public class Note {

    private Pitch pitch;
    private MusicXMLAbsTime onsetTime;
    private MusicXMLDur duration;

    private int voice;

    public Note(Pitch pitch, MusicXMLAbsTime onsetTime, MusicXMLDur duration, int voice) {
        this.pitch = pitch;
        this.onsetTime = onsetTime;
        this.duration = duration;
        this.voice = voice;
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

    public int getVoice() {
        return voice;
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

    public void setVoice(int voice) {
        this.voice = voice;
    }

    @Override
    public String toString() {
        return pitch.toString() + " @ " + onsetTime.toString() + " for " + duration.toString()
                + "in [voice:" + voice + "]";
    }
}
