package info.kgeorgiy.ja.serov.i18n.parser;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Currency parser from the whole text.
 *
 * @author alnmlbch
 */
public class CurrencyParser extends AbstractNumericParser {

    /** Default locale dependent constructor. */
    public CurrencyParser(final Locale locale) {
        super(locale, NumberFormat.getCurrencyInstance(locale));
    }
}
