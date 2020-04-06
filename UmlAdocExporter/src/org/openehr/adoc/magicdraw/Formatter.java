package org.openehr.adoc.magicdraw;

/**
 * @author Bostjan Lah
 */
public interface Formatter {
    String bold(String text);

    String monospace(String text);

    String italic(String text);

    String italicMonospace(String text);

    String boldMonospace(String text);

    String italicBoldMonospace(String text);

    String italicBold(String text);

    String hardLineBreak();

    String escapeLiteral(String value);

    String escape(String value);

    String escapeColumnSeparator(String value);

    String normalizeLines(String doc);

    String errorDelimiterLine();

}
