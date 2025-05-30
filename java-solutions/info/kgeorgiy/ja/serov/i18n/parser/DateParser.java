package info.kgeorgiy.ja.serov.i18n.parser;

import java.text.BreakIterator;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

/**
 * Date parser from the whole text.
 *
 * @author alnmlbch
 */
public class DateParser extends AbstractTextParser<Date> {

    private final List<DateFormat> formats;

    /** Default locale dependent constructor. */
    public DateParser(final Locale locale) {
        super(BreakIterator.getWordInstance(locale));
        this.formats = IntStream
            .of(
                // :NOTE: order
                DateFormat.SHORT,
                DateFormat.MEDIUM,
                DateFormat.LONG,
                DateFormat.FULL
            )
            .mapToObj(cnst -> DateFormat.getDateInstance(cnst, locale))
            .toList();
    }

    @Override
    public void tryToParse(
        final String text,
        final ParsePosition pos,
        final int end,
        final List<Date> parsed
    ) {
        for (final DateFormat format : formats) {
            final Date date = format.parse(text, pos);
            if (date != null) {
                parsed.add(date);
            }
        }
    }
}
