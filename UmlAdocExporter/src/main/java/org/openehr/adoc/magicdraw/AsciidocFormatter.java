package org.openehr.adoc.magicdraw;

/**
 * @author Bostjan Lah
 */
public class AsciidocFormatter implements Formatter {
    @Override
    public String bold(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        return '*' + text + '*';
    }

    @Override
    public String monospace(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        return '`' + text + '`';
    }

    @Override
    public String italicMonospace(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        return "`_" + text + "_`";
    }

    @Override
    public String italic(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        return "__" + text + "__";
    }

    @Override
    public String italicBoldMonospace(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        return "`*_" + text + "_*`";
    }

    @Override
    public String boldMonospace(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        return "`*" + text + "*`";
    }

    @Override
    public String italicBold(String text) {
        return "*_" + text + "_*";
    }

    @Override
    public String hardLineBreak() {
        return " +" + System.lineSeparator();
    }

    /**
     * Do any escaping needed for AsciiDoc processing within literal strings occurring in type signatures.
     * @param value documentation string.
     */
    @Override
    public String escapeLiteral(String value) {
        return value.replace("|", "&#124;").replace("*", "&#42;").replace("<=", "\\<=");
    }

    /**
     * Do any escaping needed for AsciiDoc processing.
     * @param value documentation string.
     */
    @Override
    public String escape(String value) {
        return value.replace("<=", "\\<=");
    }

    /**
     * Convert pipe characters in text to their char code equivalent, to prevent being processed as
     * an AsciiDoc table delimiter.
     * @param value documentation string.
     */
    @Override
    public String escapeColumnSeparator(String value) {
        return value.replace("|", "&#124;");
    }

    /**
     * Removing leading and trailing spaces from lines, except in literal (code etc) blocks
     * and line continuation lines (i.e. " +")
     * @param doc documentation string.
     */
    @Override
    public String normalizeLines (String doc) {
        StringBuilder sb = new StringBuilder();
        boolean inLiteralBlock = false;
        for (String line : doc.split("\n")) {
            if (line.trim().startsWith("----") || line.equals(" +"))
                inLiteralBlock = !inLiteralBlock;
            sb.append (inLiteralBlock ? line : line.trim()).append(System.lineSeparator());
        }
        return sb.toString().trim();
    }

    /**
     * Generate the line of text ".Errors", which will be interpreted by Asciidoctor
     * as a special heading.
     */
    @Override
    public String errorDelimiterLine() {
        return (System.lineSeparator() + ".Errors" + System.lineSeparator());
    }

    @Override
    public String heading (String text, int headingLevel) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < headingLevel; i++)
            sb.append('=');
        return sb.toString() + " " + text;
    }

    @Override
    public String externalLink (String text, String url) {
        String linkTemplate = "link:%s[%s^]";
        return String.format (linkTemplate, url, text);
    }

    @Override
    public String internalRef (String text, String ref) {
        return "<<" + ref + "," + text + ">>";
    }

    @Override
    public String tableColHeader (String text, int mergeCellCount) {
        return (mergeCellCount > 1 ? String.valueOf(mergeCellCount) + "+" : "") + "h|" + bold(text);
    }

    @Override
    public String tableColHeaderCentred (String text, int mergeCellCount) {
        return (mergeCellCount > 1 ? String.valueOf (mergeCellCount) + "+" : "") + "^h|" + bold(text);
    }

    @Override
    public String tableDelimiter () {
        return "|===";
    }

    @Override
    public String tableDefinition (String colsWidthProportions) {
        return "[cols=\"^" + colsWidthProportions + "\"]";
    }

    /*
     * if mergeCellCount == 1, just output "|text", else output
     * "N+|text"
     */
    @Override
    public String tableCell (String text, int mergeCellCount) {
        return (mergeCellCount > 1 ? String.valueOf(mergeCellCount) + "+" : "") + "|" + escapeColumnSeparator (normalizeLines (text));
    }

    @Override
    public String tableCellPassthrough (String text, int mergeCellCount) {
        return (mergeCellCount > 1 ? String.valueOf(mergeCellCount) + "+" : "") + "a|" + escapeColumnSeparator (normalizeLines (text));
    }

}
