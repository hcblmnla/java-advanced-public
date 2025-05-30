package info.kgeorgiy.ja.serov.i18n.statistics;

import java.util.Date;
import java.util.List;

/**
 * Dates statistics evaluator.
 *
 * @author alnmlbch
 */
public class DateStat extends AbstractNumericStat<Date> {

    /** Default constructor from dates list. */
    public DateStat(final List<Date> dates, final String topic) {
        super(dates, topic, Date::getTime, date -> new Date(date.longValue()));
    }
}
