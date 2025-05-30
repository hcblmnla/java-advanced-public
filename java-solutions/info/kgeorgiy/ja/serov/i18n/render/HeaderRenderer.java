package info.kgeorgiy.ja.serov.i18n.render;

import info.kgeorgiy.ja.serov.i18n.statistics.CountedStat;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Variadic report header helper renderer.
 *
 * @author alnmlbch
 */
public enum HeaderRenderer {
    ;

    /**
     * Pretty renders header with counts.
     *
     * @param header       header name
     * @param description  description
     * @param bundle       locale bundle
     * @param numberFormat number formatter
     * @param statistics   given statistics
     * @return rendered string
     */
    public static String render(
        final String header,
        final String description,
        final ResourceBundle bundle,
        final NumberFormat numberFormat,
        final CountedStat... statistics
    ) {
        final String body = Arrays
            .stream(statistics)
            .map(statistic -> "    %s: %s.".formatted(
                bundle.getString("summary." + statistic.getTopic()),
                numberFormat.format(statistic.getCount())
            ))
            .collect(Collectors.joining(System.lineSeparator()));
        return """
            %s
            %s
            """.formatted(header, description) + body;
    }
}
