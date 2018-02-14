package main.java;

public class Note {

    private Pitch pitch;
    private AbsoluteTime onsetTime;
    private Duration duration;

    private int voice;

    public Note(Pitch pitch, AbsoluteTime onsetTime, Duration duration, int voice) {
        this.pitch = pitch;
        this.onsetTime = onsetTime;
        this.duration = duration;
        this.voice = voice;
    }

    public Pitch getPitch() {
        return pitch;
    }

    public AbsoluteTime getOnsetTime() {
        return onsetTime;
    }

    public Duration getDuration() {
        return duration;
    }

    public int getVoice() {
        return voice;
    }

    public void setPitch(Pitch pitch) {
        this.pitch = pitch;
    }

    public void setOnsetTime(AbsoluteTime onsetTime) {
        this.onsetTime = onsetTime;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public void setVoice(int voice) {
        this.voice = voice;
    }

    @Override
    public String toString() {
        return pitch.toString() + " @ " + onsetTime.toString() + " for " + duration.toString()
                + " in [voice:" + voice + "]";
    }
}
