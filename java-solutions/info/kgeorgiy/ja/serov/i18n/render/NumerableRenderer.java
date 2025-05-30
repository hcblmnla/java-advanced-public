package info.kgeorgiy.ja.serov.i18n.render;

import info.kgeorgiy.ja.serov.i18n.statistics.NumerableStat;

import java.text.NumberFormat;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Numeric statistics renderer.
 *
 * @param <N> number type
 * @author alnmlbch
 */
public class NumerableRenderer<N> extends AbstractReportRenderer<NumerableStat<N>> {

    private final Function<? super N, String> formatter;

    /** Default constructor using created beans with specified formatter. */
    public NumerableRenderer(
        final ResourceBundle bundle,
        final NumberFormat numberFormat,
        final NumberFormat countFormat,
        final Function<? super N, String> formatter
    ) {
        super(bundle, numberFormat, countFormat);
        this.formatter = formatter;
    }

    @Override
    public Stream<String> renderNonEmpty(final NumerableStat<N> statistic, final String topic) {
        return Stream.of(
            fmt(topic, "min", formatted(statistic.getMin())),
            fmt(topic, "max", formatted(statistic.getMax())),
            fmt(topic, "avg", formatted(statistic.getAvg()))
        );
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private String formatted(final Optional<N> param) {
        return formatter.apply(param.orElseThrow());
    }
}
