package main.java;

public class NoteInRhythm extends Note {

    public NoteInRhythm(Note cur, Note prev) {
        super(cur);
        if (prev == null) {
            onsetTime = new HierarchicalRelTime(cur.onsetTime, AbsoluteTime.ZERO, cur.getTimeSig());
        }
        else {
            onsetTime = new HierarchicalRelTime(cur.onsetTime, prev.onsetTime, prev.getTimeSig());
        }
        duration = new HierarchicalDur(cur.onsetTime, cur.duration, cur.getTimeSig());
    }

    public NoteInRhythm(NoteInRhythm that) {
        super(that);
        this.onsetTime = that.onsetTime;
        this.duration = that.duration;
    }

    @Override
    public HierarchicalRelTime getOnsetTime() {
        return (HierarchicalRelTime) super.getOnsetTime();
    }

    @Override
    public HierarchicalDur getDuration() {
        return (HierarchicalDur) super.getDuration();
    }
}
