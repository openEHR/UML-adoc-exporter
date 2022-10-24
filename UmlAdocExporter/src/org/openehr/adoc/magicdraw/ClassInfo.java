package org.openehr.adoc.magicdraw;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Bostjan Lah
 */
public class ClassInfo implements Comparable<ClassInfo> {
    private final String metaType;          // "Class", "Interface", "Enumeration" etc
    private String classTypeName = "";      // including any generics
    private String className = "";          // root class name
    private String documentation = "";      // class documentation
    private final List<String> qualifiedParentClassNames = new ArrayList<>();
    private String specComponent = "";      // specification component, extracted from package hierarchy
    private String componentPackage = "";   // Usually something like org.openehr.rm
    private String classPackage = "";       // generally equals spec name, e.g. 'common'
    private String specNameOverride;        // an override for the spec, if not = subPackage
    private boolean abstractClass;          // True if abstract

    private final List<ClassFeatureInfo> attributes = new ArrayList<>();
    private final List<ClassFeatureInfo> constants = new ArrayList<>();
    private final List<ClassFeatureInfo> operations = new ArrayList<>();
    private final List<ConstraintInfo> constraints = new ArrayList<>();

    public ClassInfo(String metaType) {
        this.metaType = metaType;
    }

    // return a reliable unique key to ensure no clashes of same-named classes
    public String getClassKey() {
        return getQualifiedClassName();
    }

    public String getMetaType() {
        return metaType;
    }

    public String getClassTypeName() {
        return classTypeName;
    }

    public String getClassName() {
        return className;
    }

    public ClassInfo setClassTypeName (String aTypeName) {
        classTypeName = aTypeName;
        className = aTypeName.contains("<") ? aTypeName.substring(0, aTypeName.indexOf('<')): aTypeName;
        return this;
    }

    public String getDocumentation() {
        return documentation;
    }

    public ClassInfo setDocumentation(String aDocumentation) {
        documentation = aDocumentation;
        return this;
    }

    public List<String> getQualifiedParentClassNames() {
        return qualifiedParentClassNames;
    }

    public ClassInfo addQualifiedParentClassName(String aParentClassQualifiedName) {
        qualifiedParentClassNames.add (aParentClassQualifiedName);
        return this;
    }

    public List<ClassFeatureInfo> getAttributes() {
        return attributes;
    }

    public List<ClassFeatureInfo> getConstants() {
        return constants;
    }

    public List<ClassFeatureInfo> getOperations() {
        return operations;
    }

    public List<ConstraintInfo> getConstraints() {
        return constraints;
    }

    public boolean isAbstractClass() {
        return abstractClass;
    }

    public ClassInfo setAbstractClass (boolean anAbstractClass) {
        abstractClass = anAbstractClass;
        return this;
    }

    public String getSpecComponent() {
        return specComponent;
    }

    public void setSpecComponent(String aComponent) {
        specComponent = aComponent;
    }

    public String getComponentPackage() {
        return componentPackage;
    }

    public void setComponentPackage(String aPackage) {
        componentPackage = aPackage;
    }

    public String getClassPackage() {
        return classPackage;
    }

    public void setClassPackage(String aSubPackage) {
        classPackage = aSubPackage;
    }

    public String getSpecName() {
        return specNameOverride == null ? classPackage : specNameOverride;
    }

    public void setSpecName (String aSpecName) {
        specNameOverride = aSpecName;
    }

    public String getQualifiedClassName() {
        StringBuilder sb = new StringBuilder();
        sb.append(componentPackage != null? componentPackage + "." : "");
        sb.append(classPackage != null? classPackage + "." : "");
        sb.append(className);
        return sb.toString();
    }

    // Output a URL for the class of the form:
    //   "/releases/<component>>/<release_ref>/<spec>>.html#<fragment>"
    // where <release_ref> is an Asciidoctor variable ref like '{am_release}'
    // e.g.
    //   "/releases/AM/{am_release}/AOM2.html#_c_object_class"
    String urlPath (String aSpecReleaseVarPattern) {
        // in the below, we rewrite the release ref to match the spec component i.e. so that a
        // link to a BASE component class will have a release ref like
        // "{base_release}", and not the ref for the component for which this
        // extraction was invoked (e.g. "{rm_release}" or whatever

        return "/releases/" + specComponent + "/" +                  // component e.g. "AM"
                String.format(aSpecReleaseVarPattern,
                          specComponent.toLowerCase()) + "/" +       // release
                getSpecName() + ".html" +                            // doc
                "#" + localRef();                                    // fragment
    }

    // Output an internal document ref, e.g.:
    //   "_c_object_class"
    String localRef () {
        return "_" + className.toLowerCase() + "_" + metaType.toLowerCase();
    }

    @Override
    public int compareTo (@Nonnull ClassInfo o) {
        int i = specComponent.compareTo(o.specComponent);
        if (i != 0)
            return i;

        int j = componentPackage.compareTo(o.componentPackage);
        if (j != 0)
            return j;

        int k = classPackage.compareTo(o.classPackage);
        if (k != 0)
            return k;

        return classTypeName.compareTo(o.classTypeName);
    }
}
