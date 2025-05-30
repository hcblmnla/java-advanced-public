package info.kgeorgiy.ja.serov.i18n.statistics;

import java.util.Optional;

/**
 * Numeric statistics evaluator.
 *
 * @param <N> number type
 * @author alnmlbch
 */
public interface NumerableStat<N> extends CountedStat {

    /** Returns minimal number. */
    Optional<N> getMin();

    /** Returns maximal number. */
    Optional<N> getMax();

    /** Returns average number. */
    Optional<N> getAvg();
}
