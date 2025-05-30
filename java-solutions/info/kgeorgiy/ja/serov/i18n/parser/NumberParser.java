package info.kgeorgiy.ja.serov.i18n.parser;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Number parser from the whole text.
 *
 * @author alnmlbch
 */
// :NOTE: class??
public class NumberParser extends AbstractNumericParser {

    /** Default locale dependent constructor. */
    public NumberParser(final Locale locale) {
        super(locale, NumberFormat.getNumberInstance(locale));
    }
}
