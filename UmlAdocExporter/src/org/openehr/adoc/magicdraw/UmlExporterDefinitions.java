package org.openehr.adoc.magicdraw;

public abstract class UmlExporterDefinitions {

    public static final String TYPE_QUOTE_REGEX = "@[A-Za-z0-9_]+@";

    public static final String BARE_QUOTE_REGEX = "[A-Za-z0-9_]+";

    public static final String SPEC_RELEASE_PATTERN_DEFAULT = "{%s_release}";

    protected String quoteTypeName (String aTypeName) {
        return aTypeName.length() == 1 ? aTypeName : "@" + aTypeName + "@";
    }

}
