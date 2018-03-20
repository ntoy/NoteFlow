package main.java;

public class SmartNote extends NoteInKey {

    public SmartNote(NoteInKey cur, NoteInKey prev) {
        super(cur);
        if (prev == null) {
            onsetTime = new HierarchicalRelTime(cur.onsetTime, AbsoluteTime.ZERO, cur.getTimeSig());
        }
        else {
            onsetTime = new HierarchicalRelTime(cur.onsetTime, prev.onsetTime.add(prev.duration), prev.getTimeSig());
        }
        duration = new HierarchicalDur(cur.onsetTime, cur.duration, cur.getTimeSig());
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
