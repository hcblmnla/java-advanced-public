package info.kgeorgiy.ja.serov.i18n.parser;

import java.text.BreakIterator;
import java.text.ParsePosition;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Sentence parser from the whole text.
 *
 * @author alnmlbch
 */
public class SentenceParser extends AbstractTextParser<String> {

    private static final Pattern SENTENCE_WS = Pattern.compile("\\s+");

    /** Default locale dependent constructor. */
    public SentenceParser(final Locale locale) {
        super(BreakIterator.getSentenceInstance(locale));
    }

    @Override
    public void tryToParse(
        final String text,
        final ParsePosition pos,
        final int end,
        final List<String> parsed
    ) {
        final String sentence = SENTENCE_WS
            .matcher(text.substring(pos.getIndex(), end))
            .replaceAll(" ")
            .trim();
        if (!sentence.isEmpty()) {
            pos.setIndex(end);
            parsed.add(sentence);
        }
    }
}
