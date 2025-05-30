package info.kgeorgiy.ja.serov.i18n.render;

import info.kgeorgiy.ja.serov.i18n.statistics.CountedStat;

/**
 * Common statistics renderer.
 *
 * @param <S> statistic type
 * @author alnmlbch
 */
@FunctionalInterface
public interface ReportRenderer<S extends CountedStat> {

    /**
     * Renders given statistic using specified topic.
     *
     * @param statistic statistics
     * @return rendered string
     */
    String render(S statistic);
}
