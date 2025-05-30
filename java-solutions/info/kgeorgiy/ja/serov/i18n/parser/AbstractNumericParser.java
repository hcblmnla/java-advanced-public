package info.kgeorgiy.ja.serov.i18n.parser;

import java.text.BreakIterator;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.List;
import java.util.Locale;

// :NOTE: class
abstract class AbstractNumericParser extends AbstractTextParser<Double> {

    private final NumberFormat parser;

    AbstractNumericParser(final Locale locale, final NumberFormat parser) {
        super(BreakIterator.getCharacterInstance(locale));
        this.parser = parser;
    }

    @Override
    public void tryToParse(
        final String text,
        final ParsePosition pos,
        final int end,
        final List<Double> parsed
    ) {
        final Number number = parser.parse(text, pos);
        if (number != null) {
            parsed.add(Double.parseDouble(number.toString()));
        }
    }
}
