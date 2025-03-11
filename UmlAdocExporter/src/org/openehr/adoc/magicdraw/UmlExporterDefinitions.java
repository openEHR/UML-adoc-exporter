package org.openehr.adoc.magicdraw;

public abstract class UmlExporterDefinitions {

    public static final String TYPE_QUOTE_REGEX = "@[A-Za-z0-9_. ]+@";

    public static final String BARE_QUOTE_REGEX = "[A-Za-z0-9_][A-Za-z0-9_. ]+";

    public static final String DEFAULT_SPEC_LINK_TEMPLATE ="/releases/${component_prefix}${component}/{${component_lower}_release}/${spec_name}.html";

    public static final String ROOT_PACKAGE_NAME_DEFAULT = "openehr";

    // wrap class names in @@ except if they are single letter names, which are formal generic parameters
    // aQualifiedTypeName is of form pkg.pkg.pkg.name
    protected String quoteTypeName (String aQualifiedTypeName) {
        return aQualifiedTypeName.lastIndexOf(".") + 2 == aQualifiedTypeName.length() ?
                aQualifiedTypeName.substring(aQualifiedTypeName.length()-1) :
                "@" + aQualifiedTypeName + "@";
    }

    // Need these for type name injection in the AbstractINfoBuilder.correctType routine.
    // Hard-wired for now because a two-pass approach needed to look them up before
    // processing everything.
    public static String listClassQualifiedName = "org.openehr.base.foundation_types.List";
    public static String hashClassQualifiedName = "org.openehr.base.foundation_types.Hash";

}
