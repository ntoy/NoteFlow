package main.java;

import java.util.HashMap;
import java.util.Map;

public class NoteLengthTranslation {
    public static final Map<String, Duration> wordToDurMap;

    static {
        HashMap<String, Duration> mapping = new HashMap<>();
        mapping.put("half", new Duration(1, 2));
        mapping.put("quarter", new Duration(1, 4));
        mapping.put("eighth", new Duration(1, 8));
        mapping.put("16th", new Duration(1, 16));
        mapping.put("32nd", new Duration(1, 32));
        mapping.put("64th", new Duration(1, 64));
        wordToDurMap = mapping;
    }

    public static Duration wordToDur(String word) {
        Duration dur = wordToDurMap.get(word);
        if (dur == null)
            throw new IllegalArgumentException("Word supplied does not correspond to any duration");
        return dur;
    }

    // cannot instantiate
    private NoteLengthTranslation() {}
}
