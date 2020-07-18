package org.openehr.adoc.magicdraw;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @author Bostjan Lah
 */
public class ClassInfo implements Comparable<ClassInfo> {
    private final String metaType;         // "Class", "Interface", "Enumeration" etc
    private String classTypeName = "";     // including any generics
    private String className = "";         // root class name
    private String documentation = "";
    private List<String> parentClassNames  = new ArrayList<>();
    private String specComponent = "";
    private String classPackage = "";
    private String classSubPackage = ""; // generally equals spec name
    private String specNameOverride;     // an override for the spec, if not = subPackage
    private boolean abstractClass;

    private final List<ClassFeatureInfo> attributes = new ArrayList<>();
    private final List<ClassFeatureInfo> constants = new ArrayList<>();
    private final List<ClassFeatureInfo> operations = new ArrayList<>();
    private final List<ConstraintInfo> constraints = new ArrayList<>();

    public ClassInfo(String type) {
        this.metaType = type;
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

    public List<String> getParentClassNames() {
        return parentClassNames;
    }

    public ClassInfo addParentClassName (String aParentClassName) {
        parentClassNames.add (aParentClassName);
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

    public String getClassPackage() {
        return classPackage;
    }

    public void setClassPackage(String aPackage) {
        classPackage = aPackage;
    }

    public String getClassSubPackage() {
        return classSubPackage;
    }

    public void setClassSubPackage(String aSubPackage) {
        classSubPackage = aSubPackage;
    }

    public String getSpecName() {
        return specNameOverride == null ? classSubPackage : specNameOverride;
    }

    public void setSpecName (String aSpecName) {
        specNameOverride = aSpecName;
    }

    // Output a URL for the class of the form:
    //   '/releases/<component>>/<release>/<spec>>.html#<fragment>'
    // where <release> is an Asciidoctor variable ref like '{am_release}'
    // e.g.
    //   '/releases/AM/{am_release}/AOM2.html#_c_object_class'
    String urlPath (String aRelease) {
        return "/releases/" + specComponent + "/" + aRelease + "/" + // path
                getSpecName() + ".html" +                            // doc
                "#" + localRef();                                    // fragment
    }

    // Output an internal document ref:
    //   /releases/AM/{am_release}/AOM2.html#_c_object_class
    String localRef () {
        return "_" + className.toLowerCase() + "_" + metaType.toLowerCase();
    }

    @Override
    public int compareTo (@Nonnull ClassInfo o) {
        int i = specComponent.compareTo(o.specComponent);
        if (i != 0)
            return i;

        int j = classPackage.compareTo(o.classPackage);
        if (j != 0)
            return j;

        int k = classSubPackage.compareTo(o.classSubPackage);
        if (k != 0)
            return k;

        return classTypeName.compareTo(o.classTypeName);
    }
}
