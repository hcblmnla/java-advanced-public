package info.kgeorgiy.ja.serov.i18n.render;

import info.kgeorgiy.ja.serov.i18n.statistics.CountedStat;

import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

abstract class AbstractReportRenderer<S extends CountedStat> implements ReportRenderer<S> {

    private final ResourceBundle bundle;
    private final NumberFormat numberFormat;
    private final NumberFormat countFormat;

    AbstractReportRenderer(
        final ResourceBundle bundle,
        final NumberFormat numberFormat,
        final NumberFormat countFormat
    ) {
        this.bundle = bundle;
        this.numberFormat = numberFormat;
        this.countFormat = countFormat;
    }

    abstract Stream<String> renderNonEmpty(S statistic, String topic);

    @Override
    public String render(final S statistic) {
        final String topic = statistic.getTopic();
        if (statistic.getCount() == 0) {
            return """
                %s
                    %s"""
                .formatted(
                    bundle("statistics." + topic),
                    fmt(topic, "count", number(0), number(0))
                );
        }
        return Stream
            .concat(
                Stream.of(bundle("statistics." + topic)),
                Stream
                    .concat(
                        Stream.of(fmt(
                            topic,
                            "count",
                            number(statistic.getCount()),
                            number(statistic.getDifferent())
                        )),
                        renderNonEmpty(statistic, topic)
                    )
                    .map("\t%s"::formatted)
            )
            .collect(Collectors.joining(System.lineSeparator()));
    }

    String bundle(final String key) {
        return bundle.getString(key);
    }

    String number(final Number number) {
        return numberFormat.format(number);
    }

    String count(final Number number) {
        return countFormat.format(number);
    }

    String fmt(final String topic, final String param, final Object... args) {
        return MessageFormat.format(bundle(topic + "." + param), args);
    }
}
