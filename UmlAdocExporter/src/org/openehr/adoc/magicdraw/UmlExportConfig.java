package org.openehr.adoc.magicdraw;

import java.util.*;

import static java.lang.String.join;

/**
 * Config params object
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class UmlExportConfig {

    // Asciidoctor document heading level to generate for class texts
    private int headingLevel;

    /**
     * depth to build package name qualifiers for classes. Avoids having to use very
     * deep package hierarchies as qualifiers. Class qualifiers are used
     * a) as the key for classes in global hashmap for internal comparisons
     * b) to build filenames for writing out classes to.
     */
    private int packageDepth = 4;

    // root package name to filter on - avoid packages not under this root
    private String rootPackageName = UmlExporterDefinitions.ROOT_PACKAGE_NAME_DEFAULT;

    // List of package names under root representing 'components'
    private final Set<String> componentPackageNames = new HashSet<>();

    /**
     * this may be empty; if so, use specComponentName in links
     */
    private String componentPackageNamePrefix = "";

    /**
     * This is a printf pattern giving the form of an Asciidoctor variable name containing
     * a single '%s' for substitution, e.g. "{%s_release}", where the %s will be substituted
     * by the component name (lower case) of each class, some of which are in the core package
     *  others of which are in other components.
     */
    private String specLinkTemplate = UmlExporterDefinitions.DEFAULT_SPEC_LINK_TEMPLATE;

    private Map<String, Integer> imageFormats = new HashMap<>();

    private boolean qualifiedClassNames;

    private UmlExportConfig() {};

    private static UmlExportConfig INSTANCE;

    public static UmlExportConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new UmlExportConfig();
        }
        return INSTANCE;
    }


    public int getHeadingLevel() {
        return headingLevel;
    }

    public void setHeadingLevel(int headingLevel) {
        this.headingLevel = headingLevel;
    }

    public int getPackageDepth() {
        return packageDepth;
    }

    public void setPackageDepth(int packageDepth) {
        this.packageDepth = packageDepth;
    }

    public Set<String> getComponentPackageNames() {
        return componentPackageNames;
    }

    public String getComponentPackageNamePrefix() {
        return componentPackageNamePrefix;
    }

    public void setComponentPackageNamePrefix(String componentPackageNamePrefix) {
        this.componentPackageNamePrefix = componentPackageNamePrefix;
    }

    public String getSpecLinkTemplate() {
        return specLinkTemplate;
    }

    public void setSpecLinkTemplate(String specLinkTemplate) {
        this.specLinkTemplate = specLinkTemplate;
    }

    public Map<String, Integer> getImageFormats() {
        return imageFormats;
    }

    public void setImageFormats(Map<String, Integer> imageFormats) {
        this.imageFormats = imageFormats;
    }

    public boolean hasQualifiedClassNames() {
        return qualifiedClassNames;
    }

    public void setQualifiedClassNames(boolean qualifiedClassNames) {
        this.qualifiedClassNames = qualifiedClassNames;
    }

    public String getRootPackageName() {
        return rootPackageName;
    }

    public void setRootPackageName(String rootPackageName) {
        this.rootPackageName = rootPackageName;
    }

    // Need these for type name injection in the AbstractINfoBuilder.correctType routine.
    // Hard-wired for now because a two-pass approach needed to look them up before
    // processing everything.
    public String listClassQualifiedName() {
        return (UmlExporterDefinitions.STRUCTURES_PACKAGE_NAME_TEMPLATE + ".List").replace("$root_package", rootPackageName);
    }
    public String hashClassQualifiedName() {
        return (UmlExporterDefinitions.STRUCTURES_PACKAGE_NAME_TEMPLATE + ".Hash").replace("$root_package", rootPackageName);
    }
}
