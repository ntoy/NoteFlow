package main.java;

public class HierarchicalDur extends Duration {
    private HierarchicalRelTime offsetTime;

    public HierarchicalDur(AbsoluteTime onsetTime, Duration duration, TimeSig timeSig) {
        super(duration);
        offsetTime = new HierarchicalRelTime(onsetTime.add(duration), onsetTime, timeSig);
    }

    public int getLevel() {
        return offsetTime.getLevel();
    }

    public int getIncrement() {
        return offsetTime.getIncrement();
    }
}
