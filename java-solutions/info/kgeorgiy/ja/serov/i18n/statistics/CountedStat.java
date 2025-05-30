package info.kgeorgiy.ja.serov.i18n.statistics;

/**
 * Count values known statistics.
 *
 * @author alnmlbch
 */
public interface CountedStat {

    /** Returns numbers count. */
    int getCount();

    /** Returns distinct numbers count. */
    int getDifferent();

    /** Returns topic name of statistic. */
    String getTopic();
}
