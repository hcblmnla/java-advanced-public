package info.kgeorgiy.ja.serov.i18n.statistics;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Function;

/**
 * Strings statistics evaluator.
 *
 * @author alnmlbch
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class StringStat extends AbstractStat {

    private final Optional<String> min;
    private final Optional<String> max;

    private final Optional<String> shortest;
    private final Optional<String> longest;
    private final OptionalDouble avg;

    /**
     * Locale dependent constructor from strings list.
     *
     * @param strings given data-list
     * @param locale  locale
     */
    public StringStat(final List<String> strings, final String topic, final Locale locale) {
        super(strings, topic);
        final Collator collator = Collator.getInstance(locale);
        collator.setStrength(Collator.IDENTICAL);

        final Comparator<String> collating = collator::compare;
        final Comparator<String> sizer = Comparator.comparingInt(String::length);

        final Function<Comparator<String>, Optional<String>> min = cmp -> strings.stream().min(cmp);
        final Function<Comparator<String>, Optional<String>> max = min.compose(Comparator::reversed);

        this.min = min.apply(collating);
        this.max = max.apply(collating);

        this.shortest = min.apply(sizer);
        this.longest = max.apply(sizer);
        this.avg = strings.stream().mapToInt(String::length).average();
    }

    /** Returns lexical minimal string. */
    public Optional<String> getMin() {
        return min;
    }

    /** Returns lexical maximal string. */
    public Optional<String> getMax() {
        return max;
    }

    /** Returns shortest string. */
    public Optional<String> getShortest() {
        return shortest;
    }

    /** Returns longest string. */
    public Optional<String> getLongest() {
        return longest;
    }

    /** Returns average by length string. */
    public OptionalDouble getAvg() {
        return avg;
    }
}
