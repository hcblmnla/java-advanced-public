package info.kgeorgiy.ja.serov.i18n.parser;

import java.text.BreakIterator;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

abstract class AbstractTextParser<T> implements TextParser<T> {

    private final BreakIterator iterator;

    AbstractTextParser(final BreakIterator iterator) {
        this.iterator = iterator;
    }

    abstract void tryToParse(String text, ParsePosition pos, int end, List<T> parsed);

    @Override
    public List<T> parseText(final String text) {
        final List<T> parsed = new ArrayList<>();
        iterator.setText(text);

        final ParsePosition pos = new ParsePosition(iterator.first());
        int end = iterator.next();

        while (end != BreakIterator.DONE) {
            tryToParse(text, pos, end, parsed);
            if (pos.getIndex() > end) {
                while (pos.getIndex() > end && end != BreakIterator.DONE) {
                    end = iterator.next();
                }
            } else {
                pos.setIndex(end);
                end = iterator.next();
            }
        }
        // :NOTE: Collections.unmodifiableList
        return Collections.unmodifiableList(parsed);
    }
}
