package org.openehr.adoc.magicdraw;

import com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdtemplates.ParameterableElement;
import com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdtemplates.TemplateBinding;
import com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdtemplates.TemplateParameterSubstitution;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.*;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class;
import org.openehr.adoc.magicdraw.*;
import org.openehr.adoc.magicdraw.Formatter;
import org.openehr.adoc.magicdraw.exception.UmlAdocExporterException;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AmmEntityBuilder<T> {
    static String DOC_ERROR_DELIM = ".Errors";

    // The following are names of attributes in the openEHR profile stereotypes
    // <<Operator>> and <<Symbolic_operator>> respectively, which become UML 'tags'
    // in the Operation elements to which those stereotypes are applied. These
    // stereotypes have the effect of adding two possible meta-attributes, i.e.
    // ops and sym_ops, both defined as List<String>, which may contain string
    // names of operators (e.g. "+") to be associated with the operation (e.g. add()).
    static List<String> stereotypeTagNames = Arrays.asList("ops", "sym_ops");
    protected final Formatter formatter;
    protected final Function<String, Class> getUMLClassByQualifiedName;

    public AmmEntityBuilder(Formatter formatter, Function<String, Class> getUMLClassByQualifiedName) {
        this.formatter = formatter;
        this.getUMLClassByQualifiedName = getUMLClassByQualifiedName;
    }


    /**
     * Build a descriptor object from element
     * @param element class or similar object from the UML model
     * @return
     */
    public abstract ImmClass build (T element);

    /**
     * Extract comment text, but remove any section starting with the line
     * ".Error"; this is split out by getErrorDocumentation()
     */
    @SuppressWarnings("HardcodedLineSeparator")
    protected String getUmlDocumentation(Element umlElement, org.openehr.adoc.magicdraw.Formatter formatter) {
        List<String> allLines = umlElement.getOwnedComment().stream()
                .map(Comment::getBody)
                .flatMap(body -> Stream.of(body.split("\n")))
                .map(formatter::escape)
                .collect(Collectors.toList());
        List<String> resultLines = new ArrayList<>();
        for (String s : allLines) {
            if (s.equalsIgnoreCase(DOC_ERROR_DELIM))
                break;
            resultLines.add(s);
        }
        return String.join(System.lineSeparator(), resultLines);

    }

    /**
     * Extract error comment text, which is delimited by a line containing
     * ".Error", if it exists.
     * The delimiter line is not included in the result. If there is no
     * error text, the result is an empty string.
     */
    @SuppressWarnings("HardcodedLineSeparator")
    protected String getUmlErrorDocumentation(Element umlElement, org.openehr.adoc.magicdraw.Formatter formatter) {
        List<String> allLines = umlElement.getOwnedComment().stream()
                .map(Comment::getBody)
                .flatMap(body -> Stream.of(body.split("\n")))
                .map(formatter::escape)
                .collect(Collectors.toList());
        List<String> resultLines = new ArrayList<>();
        boolean found = false;
        for (String s : allLines) {
            if (found)
                resultLines.add(s);
            if (s.equalsIgnoreCase(DOC_ERROR_DELIM))
                found = true;
        }
        if (!resultLines.isEmpty())
            return String.join(System.lineSeparator(), resultLines);
        else
            return "";
    }

    protected Formatter getFormatter() {
        return formatter;
    }

    protected void addAttributes(List<ImmClassFeature> attributes, List<Property> umlProperties, Map<String, Property> umlSuperClassAttributes) {
        umlProperties.stream()
                .filter(p -> !umlSuperClassAttributes.containsKey(p.getName()))        // if not in inherited properties, it's 'new'
                .filter(p -> !p.isReadOnly())                                       // treat as a constant; do below
                .forEach(p -> addAttribute(attributes, p, FeatureDefinitionStatus.DEFINED));// group 'new' properties here
        umlProperties.stream()
                .filter(p -> umlSuperClassAttributes.containsKey(p.getName()))         // if in inherited properties, it's a redefine
                .filter(p -> !p.isReadOnly())
                .forEach(p -> addAttribute(attributes, p, FeatureDefinitionStatus.REDEFINED));// group redefined properties here
    }

    protected abstract void addAttribute(List<ImmClassFeature> attributes, Property umlProperty, FeatureDefinitionStatus attrStatus);

    protected void addConstants(List<ImmClassFeature> attributes, List<Property> umlProperties, Map<String, Property> umlSuperClassAttributes) {
        umlProperties.stream()
                .filter(p -> !umlSuperClassAttributes.containsKey(p.getName()))
                .filter(StructuralFeature::isReadOnly)
                .forEach(p -> addAttribute(attributes, p, FeatureDefinitionStatus.DEFINED));
        umlProperties.stream()
                .filter(p -> umlSuperClassAttributes.containsKey(p.getName()))
                .filter(StructuralFeature::isReadOnly)
                .forEach(p -> addAttribute(attributes, p, FeatureDefinitionStatus.REDEFINED));
    }

    /**
     * Convert a UML style qualified class name string like
     * RM::org::openehr::rm::entity::physical_entity::...::class_name
     * to a fixed depth form like
     * org.openehr.rm.entity.class_name (for pkgDepth = 4)
     * <p>
     * For generic types, the result will be something like
     * org.openehr.rm.data_types.DV_INTERVAL<DV_DATE>
     * what we need is org.openehr.rm.data_types.DV_INTERVAL<org.openehr.rm.data_types.DV_DATE>;
     * So we also inject the qualifiers into the generic parts
     * <p>
     * ASSUMPTION: only one level of generics!
     */
    String convertToQualified(String umlQualifiedTypeName) {
        String qualifiedTypeName = packageQualifiedClassName(umlQualifiedTypeName, UmlExportConfig.getInstance().getPackageDepth());
        if (qualifiedTypeName.contains("<")) {
            StringBuilder genericTypeNameSb = new StringBuilder(qualifiedTypeName.substring(0, qualifiedTypeName.indexOf("<") + 1));
            Class typeClass = getUMLClassByQualifiedName.apply(umlQualifiedTypeName);
            if (typeClass != null) {
                Collection<TemplateBinding> tplBindings = typeClass.getTemplateBinding();
                if (!tplBindings.isEmpty()) {
                    for (TemplateBinding tplBinding : tplBindings) {
                        List<TemplateParameterSubstitution> tplParamSubsts = new ArrayList<>(tplBinding.getParameterSubstitution());
                        if (!tplParamSubsts.isEmpty()) {
                            // We should be using the following line of code instead of the for loop below, but...
                            // for (TemplateParameterSubstitution tplParamSubst: tplParamSubstsList) {
                            // TODO: for whatever reasons, the result of tplBinding.getParameterSubstitution() appears to be
                            // in reverse order to that declared in the UML model. Here we iterate backwards through it.
                            for (int i = tplParamSubsts.size() - 1; i >= 0; i--) {
                                ParameterableElement pElem = tplParamSubsts.get(i).getActual();
                                if (pElem == null)
                                    throw new UmlAdocExporterException("Null actual generic class parameter in " + umlQualifiedTypeName + "; check model");
                                else if (pElem instanceof NamedElement) {
                                    String qName = ((NamedElement) pElem).getQualifiedName();
                                    if (qName.contains("<"))
                                        genericTypeNameSb.append(convertToQualified(qName));
                                    else if (pElem instanceof Class)
                                        genericTypeNameSb.append(packageQualifiedClassName(((Class) pElem).getQualifiedName(), UmlExportConfig.getInstance().getPackageDepth()));
                                    else
                                        genericTypeNameSb.append(((NamedElement) pElem).getName());

                                    genericTypeNameSb.append(", ");
                                } else
                                    throw new UmlAdocExporterException("Couldn't find meta-type for generic class parameter in " +
                                            umlQualifiedTypeName + "; Java type " + pElem.getClass() + "; check model");
                            }
                            // Replace final trailing ', ' with final '>'
                            genericTypeNameSb.delete(genericTypeNameSb.lastIndexOf(", "), genericTypeNameSb.length()).append(">");
                        } else
                            throw new UmlAdocExporterException("Couldn't find any template param substitutions for generic class " + umlQualifiedTypeName);
                    }
                } else
                    throw new UmlAdocExporterException("Couldn't find any template bindings for generic class " + umlQualifiedTypeName);
            } else
                throw new UmlAdocExporterException("Couldn't find MD UML Class object for type: " + umlQualifiedTypeName);

            return genericTypeNameSb.toString();
        } else
            return qualifiedTypeName;
    }

    protected void addOperations(List<ImmClassFeature> features, List<Operation> umlOperations, Map<String, Operation> umlSuperClassOperations) {
        for (Operation umlOperation : umlOperations) {
            if (umlSuperClassOperations.containsKey(umlOperation.getName())) {
                if (umlSuperClassOperations.get(umlOperation.getName()).isAbstract())
                    addOperation(features, umlOperation, FeatureDefinitionStatus.EFFECTED);
                else
                    addOperation(features, umlOperation, FeatureDefinitionStatus.REDEFINED);
            } else
                addOperation(features, umlOperation, umlOperation.isAbstract() ? FeatureDefinitionStatus.ABSTRACT : FeatureDefinitionStatus.DEFINED);
        }
    }

    protected abstract void addOperation(List<ImmClassFeature> features, Operation umlOperation, FeatureDefinitionStatus opStatus);

    /**
     * Here we will do a trick: we will wrap every type name in @@, e.g.
     * "@List@<@ITEM@>" in preparation for post processing, which will
     * replace each "@Type@" with a linked version
     *
     * @param qualifiedTypeName
     * @param qualifier
     * @param lower
     * @param upper
     * @return
     */
    protected String correctType(String qualifiedTypeName, Property qualifier, int lower, int upper) {
        String result;

        String quotedQualifiedTypeName = qualifiedTypeName.contains("<") ? quotedClassNames(qualifiedTypeName) : quoteTypeName(qualifiedTypeName);

        // if there is no qualifier, output either the UML relation target type or List<target type>
        if (qualifier == null) {
            // synthesise List<> wrapper where cardinality indicates a container
            result = upper == -1 || upper > 1 ? quoteTypeName(UmlExportConfig.getInstance().listClassQualifiedName()) +
                    '<' + quotedQualifiedTypeName + '>' : quotedQualifiedTypeName;
        } else {
            String quotedQualifierType = quoteTypeName(convertToQualified(qualifier.getType().getQualifiedName()));
            String qualifierName = qualifier.getName();

            // if there is a qualifier, but with no name, the output type is either the UML
            // qualifier type of List<qualifier type>
            if (qualifierName == null || qualifierName.isEmpty())
                result = upper == -1 || upper > 1 ? quoteTypeName(UmlExportConfig.getInstance().listClassQualifiedName()) +
                        '<' + quotedQualifierType + '>' : quotedQualifierType;
                // else if there is a qualifier name, it stands for a Hash key, and we output a Hash type sig
                // This should only occur with multiple relationships.
            else
                result = upper == -1 || upper > 1 ? quoteTypeName(UmlExportConfig.getInstance().hashClassQualifiedName()) +
                        '<' + quotedQualifierType + ',' + quotedQualifiedTypeName + '>' : quotedQualifierType;
        }
        return result;
    }

    /**
     * Add "@TypeName@" quoting to each bare type name in a generic type name
     */
    private String quotedClassNames(String typeName) {
        Pattern p = Pattern.compile(UmlExporterDefinitions.BARE_QUOTE_REGEX);
        Matcher m = p.matcher(typeName);
        StringBuffer sb = new StringBuffer();
        while (m.find())
            m.appendReplacement(sb, quoteTypeName(m.group()));
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Convert a UML style qualified class name string like
     * "RM::org::openehr::rm::entity::physical_entity::...::class_name
     * to a fixed depth form like org.openehr.rm.entity.class_name for pkgDepth = 4
     */
    protected String packageQualifiedClassName(String umlQualifiedName, int pkgDepth) {
        String[] parts = umlQualifiedName.split("::");
        int depth = Math.min(parts.length, pkgDepth);
        if (parts.length > depth) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= depth; i++)
                sb.append('.').append(parts[i]);
            // now add the class name from the end
            sb.append('.').append(parts[parts.length - 1]);
            return sb.substring(1);
        } else
            return parts[parts.length - 1];
    }

    // wrap class names in @@ except if they are single letter names, which are formal generic parameters
    // aQualifiedTypeName is of form pkg.pkg.pkg.name
    protected String quoteTypeName(String aQualifiedTypeName) {
        return aQualifiedTypeName.lastIndexOf(".") + 2 == aQualifiedTypeName.length() ?
                aQualifiedTypeName.substring(aQualifiedTypeName.length() - 1) :
                "@" + aQualifiedTypeName + "@";
    }
}
