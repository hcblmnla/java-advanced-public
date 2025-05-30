package info.kgeorgiy.ja.serov.i18n.parser;

import java.text.BreakIterator;
import java.text.ParsePosition;
import java.util.List;
import java.util.Locale;

/**
 * Word parser from the whole text.
 *
 * @author alnmlbch
 */
public class WordParser extends AbstractTextParser<String> {

    /** Default locale dependent constructor. */
    public WordParser(final Locale locale) {
        super(BreakIterator.getWordInstance(locale));
    }

    @Override
    public void tryToParse(
        final String text,
        final ParsePosition pos,
        final int end,
        final List<String> parsed
    ) {
        final String word = text.substring(pos.getIndex(), end).trim();
        if (!word.isEmpty() && word.codePoints().anyMatch(Character::isAlphabetic)) {
            pos.setIndex(end);
            parsed.add(word);
        }
    }
}
