package info.kgeorgiy.ja.serov.i18n.statistics;

import java.util.HashSet;
import java.util.List;

abstract class AbstractStat implements CountedStat {

    private final int count;
    private final int different;

    private final String topic;

    AbstractStat(final List<?> values, final String topic) {
        this.count = values.size();
        this.different = new HashSet<>(values).size();
        this.topic = topic;
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public int getDifferent() {
        return different;
    }

    @Override
    public String getTopic() {
        return topic;
    }
}
