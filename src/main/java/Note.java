package main.java;

import static main.java.PrintUtil.byteArrayToString;

public class Note {

    private Pitch pitch;
    protected AbsoluteTime onsetTime;
    protected Duration duration;
    private TimeSig timeSig;
    private int voice;

    public Note(Pitch pitch, AbsoluteTime onsetTime, Duration duration,
                TimeSig timeSig, int voice) {
        // unspecified pitch and duration are reserved for ghost notes
        if (pitch == null) {
            throw new NullPointerException("pitch cannot be null");
        }
        if (duration == null) {
            throw new NullPointerException("duration cannot be null");
        }
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

    public void setOnsetTime(AbsoluteTime onsetTime) {
        this.onsetTime = onsetTime;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public int getVoice() {
        return voice;
    }

    public boolean isGhost() {
        return (pitch == null);
    }

    public TimeSig getTimeSig() {
        return timeSig;
    }

    public static Note newGhost(AbsoluteTime onsetTime, TimeSig timeSig) {
        Note ghost = new Note();
        ghost.pitch = null;
        ghost.onsetTime = onsetTime;
        ghost.timeSig = timeSig;
        ghost.duration = Duration.ZERO;
        ghost.voice = -1;
        return ghost;
    }

    @Override
    public String toString() {
        if (isGhost()) {
            return "Ghost @ " + onsetTime.toString() + " in time sig " + timeSig.toString();
        }
        return pitch.toString() + " @ " + onsetTime.toString() + " for " + duration.toString()
                + " in [voice:" + voice + "] in " + timeSig.toString() + " w/ basis " + byteArrayToString(timeSig.getBasis());
    }

    private Note() {}
}
