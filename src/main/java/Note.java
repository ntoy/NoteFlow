package main.java;

public class Note {

    private Pitch pitch;
    private AbsoluteTime onsetTime;
    private Duration duration;
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
