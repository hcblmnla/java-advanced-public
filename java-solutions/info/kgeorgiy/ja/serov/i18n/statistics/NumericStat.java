package info.kgeorgiy.ja.serov.i18n.statistics;

import java.util.List;
import java.util.function.Function;

/**
 * Numbers statistics evaluator.
 *
 * @author alnmlbch
 */
public class NumericStat extends AbstractNumericStat<Double> {

    /** Default constructor from numbers list. */
    public NumericStat(final List<Double> numbers, final String topic) {
        super(numbers, topic, Double::doubleValue, Function.identity());
    }
}
