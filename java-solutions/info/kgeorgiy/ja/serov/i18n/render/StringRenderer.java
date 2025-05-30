package info.kgeorgiy.ja.serov.i18n.render;

import info.kgeorgiy.ja.serov.i18n.statistics.StringStat;

import java.text.NumberFormat;
import java.util.ResourceBundle;
import java.util.stream.Stream;

/**
 * String statistics renderer.
 *
 * @author alnmlbch
 */
public class StringRenderer extends AbstractReportRenderer<StringStat> {

    /** Default constructor using created beans. */
    public StringRenderer(
        final ResourceBundle bundle,
        final NumberFormat numberFormat,
        final NumberFormat countFormat
    ) {
        super(bundle, numberFormat, countFormat);
    }

    @Override
    public Stream<String> renderNonEmpty(final StringStat statistic, final String topic) {
        final String shortest = statistic.getShortest().orElseThrow();
        final String longest = statistic.getLongest().orElseThrow();
        return Stream.of(
            fmt(topic, "min", statistic.getMin().orElseThrow()),
            fmt(topic, "max", statistic.getMax().orElseThrow()),
            fmt(topic, "min.length", number(shortest.length()), shortest),
            fmt(topic, "max.length", number(longest.length()), longest),
            fmt(topic, "avg.length", count(statistic.getAvg().orElseThrow()))
        );
    }
}
