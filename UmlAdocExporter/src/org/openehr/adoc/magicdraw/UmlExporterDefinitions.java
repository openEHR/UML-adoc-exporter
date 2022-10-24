package org.openehr.adoc.magicdraw;

public abstract class UmlExporterDefinitions {

    public static final String TYPE_QUOTE_REGEX = "@[A-Za-z0-9_. ]+@";

    public static final String BARE_QUOTE_REGEX = "[A-Za-z0-9_. ]+";

    public static final String SPEC_RELEASE_PATTERN_DEFAULT = "{%s_release}";

    public static final String ROOT_PACKAGE_NAME_DEFAULT = "openehr";

    protected String quoteTypeName (String aTypeName) {
        return aTypeName.length() == 1 ? aTypeName : "@" + aTypeName + "@";
    }

    // Need these for type name injection in the AbstractINfoBuilder.correctType routine.
    // Hard-wired for now because a two-pass approach needed to look them up before
    // processing everything.
    public static String listClassQualifiedName = "org.openehr.base.foundation_types.List";
    public static String hashClassQualifiedName = "org.openehr.base.foundation_types.Hash";

}
