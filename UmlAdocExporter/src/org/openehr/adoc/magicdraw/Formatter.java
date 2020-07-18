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

    /**
     * Make text into a heading at headingLevel
     * @param text
     * @param headingLevel
     * @return
     */
    String heading (String text, int headingLevel);

    /**
     * Generate the text for an external link in a
     * @param text the text of the link
     * @param url the link target
     * @return
     */
    String externalLink (String text, String url);

    /**
     * Generate a link to a target within the current document.
     * @param text the text of the link
     * @param ref the target reference
     * @return
     */
    String internalRef (String text, String ref);

    /**
     * Generate text representing a table column header
     * @param text
     * @param mergeCellCount == 1 for single cell width
     * @return
     */
    String tableColHeader (String text, int mergeCellCount);

    /**
     * Generate text representing a centred table column header
     * @param text
     * @param mergeCellCount == 1 for single cell width
     * @return
     */
    String tableColHeaderCentred (String text, int mergeCellCount);

    /**
     * Return a table delimiter
     * @return
     */
    String tableDelimiter ();

    /**
     * Return a table definition taking a string of the form "n,m,..."
     * where "n,m" etc represent relative widths. E.g. "2,3,5"
     * @param colsWidthProportions
     * @return
     */
    String tableDefinition (String colsWidthProportions);

    /**
     * Return a table cell, with optional merged cells
     * @param mergeCellCount == 1 for single cell width
     * @return
     */
    String tableCell (String text, int mergeCellCount);

    /**
     * Return a table cell, with optional merged cells, and passthrough of the
     * native formatter marks that may occur in the source text
     * @param mergeCellCount == 1 for single cell width
     * @return
     */
    String tableCellPassthrough (String text, int mergeCellCount);
}
