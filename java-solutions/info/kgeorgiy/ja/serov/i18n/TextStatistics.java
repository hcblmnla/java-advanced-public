package info.kgeorgiy.ja.serov.i18n;

import info.kgeorgiy.ja.serov.i18n.parser.CurrencyParser;
import info.kgeorgiy.ja.serov.i18n.parser.DateParser;
import info.kgeorgiy.ja.serov.i18n.parser.NumberParser;
import info.kgeorgiy.ja.serov.i18n.parser.SentenceParser;
import info.kgeorgiy.ja.serov.i18n.parser.WordParser;
import info.kgeorgiy.ja.serov.i18n.render.HeaderRenderer;
import info.kgeorgiy.ja.serov.i18n.render.NumerableRenderer;
import info.kgeorgiy.ja.serov.i18n.render.StringRenderer;
import info.kgeorgiy.ja.serov.i18n.statistics.DateStat;
import info.kgeorgiy.ja.serov.i18n.statistics.NumericStat;
import info.kgeorgiy.ja.serov.i18n.statistics.StringStat;

import java.io.IOException;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Simple {@code i18n} text statistics implementation.
 *
 * @author alnmlbch
 */
public enum TextStatistics {
    ;

    private static final String BUNDLE_BASE_NAME
        = TextStatistics.class.getPackageName() + ".messages";

    /**
     * {@link TextStatistics} launcher.
     * <p>
     * Usage: {@code TextStatistics <in_languageTag> <out_languageTag> <input> <output>}
     *
     * @param args command line arguments
     */
    public static void main(final String... args) {
        if (args.length < 4) {
            // :NOTE: not localized
            System.err.println("""
                Usage: TextStatistics <in_languageTag> <out_languageTag> <input> <output>
                """);
            return;
        }
        final Locale inLocale = Locale.forLanguageTag(args[0]);
        final Locale outLocale = Locale.forLanguageTag(args[1]);

        final String input = args[2];
        final Path file;
        final String text;
        try {
            file = Path.of(input);
        } catch (final InvalidPathException e) {
            System.err.println("Invalid in path: " + input);
            return;
        }
        try {
            text = Files.readString(file, StandardCharsets.UTF_8);
        } catch (final IOException e) {
            System.err.println("Error reading file: " + e);
            return;
        }

        final ResourceBundle bundle;
        try {
            bundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, outLocale);
            // impossible but why not
        } catch (final MissingResourceException e) {
            System.err.format(
                "Internal error: missing resource bundle %s for locale %s%n",
                BUNDLE_BASE_NAME,
                outLocale.toLanguageTag()
            );
            return;
        }

        final String outPath = args[3];
        final String statistics = collectStatistics(
            bundle, inLocale, outLocale, text, file.getFileName().toString()
        );
        try {
            final Path path = Path.of(outPath);
            if (Files.notExists(path)) {
                final Path parent = path.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.createFile(path);
            }
            Files.writeString(path, statistics, StandardCharsets.UTF_8);
        } catch (final IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        } catch (final InvalidPathException e) {
            System.err.println("Invalid out path: " + outPath);
        }
    }

    private static String collectStatistics(
        final ResourceBundle bundle,
        final Locale inLocale,
        final Locale outLocale,
        final String text,
        final String filename
    ) {
        // :NOTE: copy-paste
        final StringStat sentences = new StringStat(
            new SentenceParser(inLocale).parseText(text), "sentence", inLocale);
        final StringStat words = new StringStat(
            new WordParser(inLocale).parseText(text), "word", inLocale);

        final NumericStat numbers = new NumericStat(
            new NumberParser(inLocale).parseText(text), "number");
        final NumericStat currencies = new NumericStat(
            new CurrencyParser(inLocale).parseText(text), "currency");
        final DateStat dates = new DateStat(
            new DateParser(inLocale).parseText(text), "date");

        final NumberFormat numberFormat = NumberFormat.getNumberInstance(outLocale);
        final NumberFormat countFormat = configureCountFormat(outLocale);

        final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(outLocale);
        final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.DEFAULT, outLocale);

        final StringRenderer stringRenderer = new StringRenderer(
            bundle, numberFormat, countFormat);

        return Stream
            .of(
                HeaderRenderer.render(
                    MessageFormat.format(bundle.getString("file"), filename),
                    bundle.getString("summary"),
                    bundle,
                    numberFormat,
                    sentences, words, numbers, currencies, dates
                ),
                stringRenderer.render(sentences),
                stringRenderer.render(words),
                // :NOTE: method
                new NumerableRenderer<Double>(
                    // :NOTE: arg object
                    bundle, numberFormat, countFormat, countFormat::format
                ).render(numbers),
                new NumerableRenderer<Double>(
                    bundle, numberFormat, countFormat, currencyFormat::format
                ).render(currencies),
                new NumerableRenderer<Date>(
                    bundle, numberFormat, countFormat, dateFormat::format
                ).render(dates)
            )
            .collect(Collectors.joining(System.lineSeparator()));
    }

    private static NumberFormat configureCountFormat(final Locale locale) {
        final NumberFormat countFormat = NumberFormat.getNumberInstance(locale);
        countFormat.setGroupingUsed(false);
        countFormat.setMinimumFractionDigits(1);
        countFormat.setMaximumFractionDigits(3);
        countFormat.setRoundingMode(RoundingMode.HALF_EVEN);
        return countFormat;
    }
}
