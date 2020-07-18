package org.openehr.adoc.magicdraw;

public abstract class UmlExporterDefinitions {

    public static final String TYPE_QUOTE_REGEX = "@[A-Za-z0-9_]+@";

    public static final String BARE_QUOTE_REGEX = "[A-Za-z0-9_]+";

    protected String quoteTypeName (String aTypeName) {
        return aTypeName.length() == 1 ? aTypeName : "@" + aTypeName + "@";
    }

}
