package info.kgeorgiy.ja.serov.i18n.parser;

import java.util.List;

/**
 * Common text parser.
 *
 * @param <T> parsed token type
 * @author alnmlbch
 */
@FunctionalInterface
public interface TextParser<T> {

    /** Parses the given text into tokens list. */
    List<T> parseText(String text);
}
