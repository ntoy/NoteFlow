package main.java;

public class Note {

    private Pitch pitch;
    protected AbsoluteTime onsetTime;
    protected Duration duration;
    private TimeSig timeSig;
    private int voice;

    public Note(Pitch pitch, AbsoluteTime onsetTime, Duration duration,
                TimeSig timeSig, int voice) {
        this.pitch = pitch;
        this.onsetTime = onsetTime;
        this.duration = duration;
        this.timeSig = timeSig;
        this.voice = voice;
    }

    public Note(Note that) {
        this.pitch = that.pitch;
        this.onsetTime = that.onsetTime;
        this.duration = that.duration;
        this.timeSig = that.timeSig;
        this.voice = that.voice;
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

    public TimeSig getTimeSig() {
        return timeSig;
    }

    @Override
    public String toString() {
        return pitch.toString() + " @ " + onsetTime.toString() + " for " + duration.toString()
                + " in [voice:" + voice + "]";
    }
}
