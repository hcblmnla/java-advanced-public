package info.kgeorgiy.ja.serov.i18n.statistics;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
abstract class AbstractNumericStat<N extends Comparable<? super N>>
    extends AbstractStat implements NumerableStat<N> {

    private final Optional<N> min;
    private final Optional<N> max;
    private final Optional<N> avg;

    AbstractNumericStat(
        final List<N> values,
        final String topic,
        final ToDoubleFunction<N> toDoubleCoder,
        final Function<Double, N> doubleDecoder
    ) {
        super(values, topic);
        this.min = values.stream().min(Comparator.naturalOrder());
        this.max = values.stream().max(Comparator.naturalOrder());

        this.avg = values.stream()
            .mapToDouble(toDoubleCoder)
            .average()
            .stream()
            .boxed()
            .findAny()
            .map(doubleDecoder);
    }

    @Override
    public Optional<N> getMin() {
        return min;
    }

    @Override
    public Optional<N> getMax() {
        return max;
    }

    @Override
    public Optional<N> getAvg() {
        return avg;
    }
}
