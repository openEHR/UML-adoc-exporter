package org.openehr.adoc.magicdraw;

import com.nomagic.magicdraw.export.image.ImageExporter;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UmlExporterDefinitions {

    public static final String TYPE_QUOTE_REGEX = "@[A-Za-z0-9_. ]+@";

    public static final String BARE_QUOTE_REGEX = "[A-Za-z0-9_][A-Za-z0-9_. ]+";

    public static final String DEFAULT_SPEC_LINK_TEMPLATE ="/releases/${component_prefix}${component}/{${component_lower}_release}/${spec_name}.html";

    public static final String ROOT_PACKAGE_NAME_DEFAULT = "openehr";

    public static final String STRUCTURES_PACKAGE_NAME_TEMPLATE = "org.$root_package.base.foundation_types.List";

    // wrap class names in @@ except if they are single letter names, which are formal generic parameters
    // aQualifiedTypeName is of form pkg.pkg.pkg.name
    protected String quoteTypeName (String aQualifiedTypeName) {
        return aQualifiedTypeName.lastIndexOf(".") + 2 == aQualifiedTypeName.length() ?
                aQualifiedTypeName.substring(aQualifiedTypeName.length()-1) :
                "@" + aQualifiedTypeName + "@";
    }

    public static Map<String, Integer> defaultImageFormats = Stream.of(new Object[][] {
            { "svg", ImageExporter.SVG },
            { "png", ImageExporter.PNG }
    }).collect(Collectors.toMap(data -> (String) data[0], data -> (Integer) data[1]));

    public static Set<String> defaultImageFormatNames () {
        return defaultImageFormats.keySet();
    }
}
