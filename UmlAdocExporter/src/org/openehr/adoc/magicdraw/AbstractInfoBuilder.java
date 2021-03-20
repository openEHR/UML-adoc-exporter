package org.openehr.adoc.magicdraw;

import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Bostjan Lah
 */
public abstract class AbstractInfoBuilder<T> extends UmlExporterDefinitions {
    static String DOC_ERROR_DELIM = ".Errors";

    // The following are names of attributes in the openEHR profile stereotypes
    // <<Operator>> and <<Symbolic_operator>> respectively, which become UML 'tags'
    // in the Operation elements to which those stereotypes are applied. These
    // stereotypes have the effect of adding two possible meta-attributes, i.e.
    // ops and sym_ops, both defined as List<String>, which may contain string
    // names of operators (e.g. "+") to be associated with the operation (e.g. add()).
    static List<String> stereotypeTagNames = Arrays.asList("ops", "sym_ops");

    protected final Formatter formatter;

    protected AbstractInfoBuilder (Formatter formatter) {
        this.formatter = formatter;
    }

    /**
     * Extract comment text, but remove any section starting with the line
     * ".Error"; this is split out by getErrorDocumentation()
     */
    @SuppressWarnings("HardcodedLineSeparator")
    protected String getDocumentation(Element element, Formatter formatter) {
        List<String> allLines = element.getOwnedComment().stream()
                .map(Comment::getBody)
                .flatMap(body -> Stream.of(body.split("\n")))
                .map(formatter::escape)
                .collect(Collectors.toList());
        List<String> resultLines = new ArrayList<>();
        for (String s: allLines) {
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
    protected String getErrorDocumentation(Element element, Formatter formatter) {
        List<String> allLines = element.getOwnedComment().stream()
                .map(Comment::getBody)
                .flatMap(body -> Stream.of(body.split("\n")))
                .map(formatter::escape)
                .collect(Collectors.toList());
        List<String> resultLines = new ArrayList<>();
        boolean found = false;
        for (String s: allLines) {
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

    /**
     * Build a descriptor object from element
     * @param element class or similar object from the UML model
     * @return
     */
    public abstract ClassInfo build (T element);

    protected Formatter getFormatter() {
        return formatter;
    }

    // Add class- or routine-level constraints
    protected void addConstraints (List<ConstraintInfo> constraints, Collection<Constraint> constraintOfConstrainedElement) {
        for (Constraint constraint : constraintOfConstrainedElement) {
            constraints.add(new ConstraintInfo().setDocumentation(formatConstraint(constraint)));
        }
    }

    private String formatConstraint (Constraint constraint) {
        StringBuilder builder = new StringBuilder(formatter.italic(constraint.getName())).append(": ");
        if (constraint.getSpecification() instanceof OpaqueExpression) {
            OpaqueExpression opaqueExpression = (OpaqueExpression)constraint.getSpecification();
            if (opaqueExpression.hasBody()) {
                boolean add = false;
                for (String line : opaqueExpression.getBody()) {
                    if (add)
                        builder.append(formatter.hardLineBreak());
                    builder.append(formatter.monospace(formatter.escape(line)));
                    add = true;
                }
            }
        }
        return builder.toString();
    }

    protected void addAttributes (List<ClassFeatureInfo> attributes, List<Property> properties, Map<String, Property> superClassAttributes) {
        properties.stream()
                .filter(p -> !superClassAttributes.containsKey(p.getName()))        // if not in inherited properties, it's 'new'
                .filter(p -> !p.isReadOnly())                                       // treat as a constant; do below
                .forEach(p -> addAttribute(attributes, p, OperationStatus.DEFINED));// group 'new' properties here
        properties.stream()
                .filter(p -> superClassAttributes.containsKey(p.getName()))         // if in inherited properties, it's a redefine
                .filter(p -> !p.isReadOnly())
                .forEach(p -> addAttribute(attributes, p, OperationStatus.REDEFINED));// group redefined properties here
    }

    protected void addConstants(List<ClassFeatureInfo> attributes, List<Property> properties, Map<String, Property> superClassAttributes) {
        properties.stream()
                .filter(p -> !superClassAttributes.containsKey(p.getName()))
                .filter(StructuralFeature::isReadOnly)
                .forEach(p -> addAttribute(attributes, p, OperationStatus.DEFINED));
        properties.stream()
                .filter(p -> superClassAttributes.containsKey(p.getName()))
                .filter(StructuralFeature::isReadOnly)
                .forEach(p -> addAttribute(attributes, p, OperationStatus.REDEFINED));
    }

    /**
     * Build a ClassFeatureInfo object for property and add it to the attributes list.
     * @param attributes List of ClassFeatureInfo objects for this class so far built.
     * @param property the property to add.
     * @param attrStatus Status of attribute in this class: defined, redefined etc.
     */
    private void addAttribute (List<ClassFeatureInfo> attributes, Property property, OperationStatus attrStatus) {
        // create a ClassFeatureInfo with attribute documentation, occurrences and redefined marker
        ClassFeatureInfo classFeatureInfo = new ClassFeatureInfo()
                .setDocumentation(getDocumentation(property, formatter))
                .setCardinality(formatSpecialOccurences(property.getLower(), property.getUpper()))
                .setStatus(attrStatus.toString().isEmpty()? "" : "(" + attrStatus + ")");

        // attribute signature
        StringBuilder sigBuilder = new StringBuilder(formatter.bold(property.getName()));
        sigBuilder.append(": ");

        // determine the type
        String type = property.getType() == null ? "" : property.getType().getName();

        // if there are template parameters, add them to the type name. Because UML doesn't
        // support a proper notion of types, we have to get the qualified type name, then
        // search for that, and then check if it is a template type, and if so obtain the
        // list of parameters and construct a string of the form "<T,U,V>" to add to the root
        // type name.

        // THE FOLLOWING CODE IS AN INITIAL ATTEMPT BUT IS WRONG
        //String type = property.getMetaType() == null ? "" : property.getMetaType().getQualifiedName();
        //TemplateParameter tplParam = property.getClassType().getTypeName();
        //getMetaType(). getOwningTemplateParameter();
//        if (tplParam != null) {
//            List<TemplateParameter> tplParams = tplParam.getSignature().getOwnedParameter();
//            String tplParamList = tplParams.stream()
//                    .map(t -> t.getHumanName())
//                    .collect(Collectors.joining(","));
//            type = type + '<' + tplParamList + '>';
//        }

        // if there is a qualifier on the property, get it, since this will modify the type
        Property qualifier = property.getAssociation() != null && property.hasQualifier() ? property.getQualifier().get(0) : null;
        StringBuilder typeInfo = new StringBuilder (formatType (type, qualifier, property.getLower(), property.getUpper()));

        // If there is a default value defined, output it.
        ValueSpecification defaultValue = property.getDefaultValue();
        if (defaultValue != null) {
            // if the property is a constant, then
            // output '=' + <default value>; else
            // output \n {default '=' + <default value>}
            if (!property.isReadOnly())
                typeInfo.append(formatter.hardLineBreak() + "{default");

            if (defaultValue instanceof LiteralString) {
                LiteralString value = (LiteralString)defaultValue;
                typeInfo.append("{nbsp}={nbsp}").append(formatter.escapeLiteral(value.getValue()));
            }
            else if (defaultValue instanceof LiteralInteger) {
                LiteralInteger value = (LiteralInteger) defaultValue;
                typeInfo.append("{nbsp}={nbsp}").append(value.getValue());
            }
            else if (defaultValue instanceof LiteralReal) {
                LiteralReal value = (LiteralReal) defaultValue;
                typeInfo.append("{nbsp}={nbsp}").append(value.getValue());
            }
            else if (defaultValue instanceof LiteralBoolean) {
                LiteralBoolean value = (LiteralBoolean) defaultValue;
                typeInfo.append("{nbsp}={nbsp}").append(value.isValue());
            }
            else {
                // This case is not yet working; idea is to generate the symbol string
                // for a constant whose 'value' is another property or constant
                Expression expr = defaultValue.getExpression();
                if (expr != null)
                    typeInfo.append("{nbsp}={nbsp}").append(expr.getSymbol());
            }

            if (!property.isReadOnly())
                typeInfo.append("}");
        }

        // If there is any type information, append it
        if (typeInfo.length() > 0)
            sigBuilder.append (formatter.monospace (typeInfo.toString()));

        classFeatureInfo.setSignature(sigBuilder.toString());

        attributes.add(classFeatureInfo);
    }

    protected void addOperations(List<ClassFeatureInfo> features, List<Operation> operations, Map<String, Operation> superClassOperations) {
        for (Operation op : operations) {
            if (superClassOperations.containsKey(op.getName())) {
                if (superClassOperations.get(op.getName()).isAbstract())
                    addOperation(features, op, OperationStatus.EFFECTED);
                else
                    addOperation(features, op, OperationStatus.REDEFINED);
            }
            else
                addOperation(features, op, op.isAbstract()? OperationStatus.ABSTRACT : OperationStatus.DEFINED);
        }
    }

    /**
     * Build a ClassFeatureInfo for operation, and append it to the features list so far built.
     * @param features List of class features so far built.
     * @param operation UML operation definition.
     * @param opStatus Status of operation in this class: abstract, effected, defined etc.
     */
    private void addOperation(List<ClassFeatureInfo> features, Operation operation, OperationStatus opStatus) {
        // Create the main documentation.
        StringBuilder opDocBuilder = new StringBuilder(getDocumentation(operation, formatter));
        opDocBuilder.append(System.lineSeparator());

        // Start building the operation signature
        // append the operation name, bolded
        StringBuilder opSigBuilder = new StringBuilder(formatter.bold(operation.getName()));

        // see if the operation has stereotype <<Operator>>, which has tag ops: List<String>
        // or <<Symbolic_operator>>, which has tag sym_ops: List<String>
        // See comment above for stereotypeTagNames for details
        StringBuilder opAliasBuilder = new StringBuilder();
        InstanceSpecification stereotypeSpec = operation.getAppliedStereotypeInstance();
        if (stereotypeSpec != null) {
            Collection<Slot> slots = stereotypeSpec.getSlot();
            if (!slots.isEmpty()) {
                Iterator<Slot> slots_it = slots.iterator();
                while (slots_it.hasNext()) {
                    Slot tag = slots_it.next();
                    // Here we check that the slot attribute name is one of the ones we want
                    StructuralFeature tagAttr = tag.getDefiningFeature();
                    if (tagAttr instanceof Property) {
                        Property tagProp = (Property) tagAttr;
                        if (stereotypeTagNames.contains(tagProp.getName()) && tag.hasValue()){
                            // Now we know we have the operator tags, we can output the 'alias' line
                            // (Use the first variant to put it on a new line, plus uncomment the
                            // post-loop statement to add another NL)
                            // opSigBuilder.append(formatter.hardLineBreak() + formatter.italic("alias") + " ");
                            if (opAliasBuilder.length() == 0)
                                opAliasBuilder.append(" " + formatter.italic("alias") + " ");

                            List<ValueSpecification> ops = tag.getValue();
                            Iterator<ValueSpecification> ops_it = ops.iterator();

                            while (ops_it.hasNext()) {
                                ValueSpecification op = ops_it.next();
                                if (op instanceof LiteralString) {
                                    LiteralString op_str = (LiteralString) op;
                                    opAliasBuilder.append(formatter.escapeLiteral(op_str.getValue()));
                                    if (ops_it.hasNext() || slots_it.hasNext())
                                        opAliasBuilder.append(", ");
                                }
                            }
                        }
                    }
                }
                // opAliasBuilder.append(formatter.hardLineBreak());
            }
        }
        opSigBuilder.append(opAliasBuilder);

        // If there are parameters, output them within parentheses; also
        // add the parameter documentation to the documentary text
        if (operation.hasOwnedParameter()) {
            addSignatureParameters(opSigBuilder, operation.getOwnedParameter());
            opDocBuilder.append(System.lineSeparator());
            addDocumentParameters(opDocBuilder, operation.getOwnedParameter());
        }

        // If there is a return type, append it to the signature in monospace.
        String type = operation.getType() == null ? "" : operation.getType().getName();
        StringBuilder fullSigBuilder = type.isEmpty()
                ? new StringBuilder(opSigBuilder)
                : new StringBuilder(opSigBuilder + ": " + formatter.monospace(formatType(type, null, operation.getLower(), operation.getUpper())));

        // Output any operation pre- and post-conditions (UML constraints)
        addOperationConstraint(operation, fullSigBuilder);

        // Create and set the error documentation, if there is any.
        String errStr = getErrorDocumentation(operation, formatter);
        if (!errStr.isEmpty()) {
            opDocBuilder.append(System.lineSeparator());
            opDocBuilder.append(formatter.errorDelimiterLine());
            opDocBuilder.append (getErrorDocumentation(operation, formatter));
            opDocBuilder.append(System.lineSeparator());
        }

        ClassFeatureInfo classFeatureInfo = new ClassFeatureInfo()
                .setCardinality(formatSpecialOccurences(operation.getLower(), operation.getUpper()))
                .setStatus(opStatus.toString().isEmpty()? "" : "(" + opStatus + ")")
                .setDocumentation(opDocBuilder.toString());

        classFeatureInfo.setSignature(fullSigBuilder.toString());

        features.add(classFeatureInfo);
    }

    /**
     * Add parameters for a UML method in a class definition to the operation string.
     * @param parameters UML parameter definitions.
     * @param sigBuilder string builder containing method definition as a string.
     */
    protected void addSignatureParameters(StringBuilder sigBuilder, List<Parameter> parameters) {
        List<String> formattedParameters = new ArrayList<>();
        for (Parameter parameter : parameters) {
            String paramSignature = parameter.getName();
            if (!"return".equals(paramSignature) && !paramSignature.isEmpty()) {
                if (parameter.getType() == null)
                    formattedParameters.add(paramSignature);
                else {
                    formattedParameters.add(
                            paramSignature + ": " + formatter.monospace(
                                    formatType(parameter.getType().getName(), null, parameter.getLower(), parameter.getUpper()) +
                                            '[' + formatInlineOccurences(parameter.getLower(), parameter.getUpper()) + ']'
                            )
                    );
                }
            }
        }

        // if there are parameters, put them out on different lines, else just output "()"
        if (!formattedParameters.isEmpty()) {
            sigBuilder.append(" (").append(formatter.hardLineBreak());
            sigBuilder.append(String.join("," + formatter.hardLineBreak(), formattedParameters)).append(formatter.hardLineBreak());
            sigBuilder.append(')');
        }
        else
            sigBuilder.append(" ()");
    }

    /**
     * Add parameters for a UML method in a class definition to the operation string.
     * @param parameters UML parameter definitions.
     * @param docBuilder string builder containing parameter documentation.
     */
    protected void addDocumentParameters(StringBuilder docBuilder, List<Parameter> parameters) {
        List<String> formattedParameters = new ArrayList<>();
        for (Parameter parameter : parameters) {
            String paramName = parameter.getName();
            if (!"return".equals(paramName) && !paramName.isEmpty()) {
                String paramComment = getDocumentation(parameter, formatter);
                if (!paramComment.isEmpty()) {
                    formattedParameters.add(System.lineSeparator() + formatter.italicMonospace(paramName)  + ":: " + paramComment);
                }
            }
        }
        if (!formattedParameters.isEmpty()) {
            docBuilder.append (".Parameters");
            docBuilder.append(formatter.hardLineBreak());
            docBuilder.append ("[horizontal]");
            docBuilder.append(String.join("\n", formattedParameters));
        }
    }

    /**
     * Add the pre- or post-condition constraints attached to a UML method in a class definition
     * to the attribute string, each on a new line.
     * @param operation UML method definition.
     * @param builder string builder containing method definition as a string.
     */
    private void addOperationConstraint(Operation operation, StringBuilder builder) {
        StringBuilder constraintBuilder = new StringBuilder();

        // Pre-conditions first; match by looking for leading "pre" (case-insensitive)
        for (Constraint constraint : operation.get_constraintOfConstrainedElement()) {
            if (constraint.getName().toLowerCase().startsWith("pre"))
                constraintBuilder.append (formatter.hardLineBreak()).append(formatConstraint(constraint));
        }

        // Post-conditions
        for (Constraint constraint : operation.get_constraintOfConstrainedElement()) {
            if (constraint.getName().toLowerCase().startsWith("post"))
                constraintBuilder.append (formatter.hardLineBreak()).append(formatConstraint(constraint));
        }

        // Any others
        for (Constraint constraint : operation.get_constraintOfConstrainedElement()) {
            if (!constraint.getName().toLowerCase().matches("(pre|post).*"))
                constraintBuilder.append (formatter.hardLineBreak()).append(formatConstraint(constraint));
        }

        // if there were any constraints, first output a blank line, then the constraints
        if (constraintBuilder.length() > 0) {
            builder.append (formatter.hardLineBreak());
            builder.append (constraintBuilder);
        }
    }

    /**
     * Here we will do a trick: we will wrap every type name in @@, e.g.
     * "@List@<@ITEM@>" in preparation for post processing, which will
     * replace each "@Type@" with a linked version
     * @param type
     * @param qualifier
     * @param lower
     * @param upper
     * @return
     */
    private String formatType (String type, Property qualifier, int lower, int upper) {
        String result;

        String quotedTypeName = type.contains("<")? quotedClassNames(type) :  quoteTypeName(type);

        // if there is no qualifier, output either the UML relation target type or List<target type>
        if (qualifier == null) {
            // synthesise List<> wrapper where cardinality indicates a container
            result = upper == -1 || upper > 1 ? quoteTypeName("List") +
                        "<" + quotedTypeName + '>' : quotedTypeName;
        }
        else {
            String quotedQualifierType = quoteTypeName (qualifier.getType().getName());
            String qualifierName = qualifier.getName();

            // if there is a qualifier, but with no name, the output type is either the UML
            // qualifier type of List<qualifier type>
            if (qualifierName == null || qualifierName.isEmpty())
                result = upper == -1 || upper > 1 ? quoteTypeName ("List") +
                        "<" + quotedQualifierType + '>' : quotedQualifierType;
                // else if there is a qualifier name, it stands for a Hash key, and we output a Hash type sig
                // This should only occur with multiple relationships.
            else
                result = upper == -1 || upper > 1 ? quoteTypeName("Hash") +
                        "<" + quotedQualifierType + ',' + quotedTypeName + '>' : quotedQualifierType;
        }
        return result;
    }

    /**
     * Add "@TypeName@" quoting to each bare type in a generic type name
     */
    private String quotedClassNames (String typeName) {
        Pattern p = Pattern.compile (BARE_QUOTE_REGEX);
        Matcher m = p.matcher (typeName);
        StringBuffer sb = new StringBuffer();
        while (m.find())
            m.appendReplacement(sb, quoteTypeName(m.group()));
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Format occurrences in the standard way.
     * @param lower lower value of occurrences.
     * @param upper upper value of occurrences.
     */
    private String formatInlineOccurences(int lower, int upper) {
        if (upper == -1)
            return lower == 0 ? "0..1" : "1";
        else
            return lower == upper ? "" + lower : lower + ".." + upper;
    }

    /**
     * Format occurrences in a way that accounts for 0..* in UML being represented as List<T>.
     * @param lower lower value of occurrences.
     * @param upper upper value of occurrences.
     */
    private String formatSpecialOccurences(int lower, int upper) {
        return upper == -1 ? lower + "..1" : lower + ".." + upper;
    }

    /**
     * Extract package information from a string like
     * "RM::org::openehr::rm::common
     */
    protected void setHierarchy (String qualifiedName, ClassInfo classInfo) {
        // this is hard-coded for openehr atm
        String[] parts = qualifiedName.split ("::");
        if (parts.length > 4) {
            classInfo.setSpecComponent(parts[0]);
            classInfo.setClassSubPackage(parts[4]);
            StringBuilder indexPackage = new StringBuilder();
            for (int i = 1; i < 4; i++)
                indexPackage.append('.').append (parts[i]);
            classInfo.setClassPackage (indexPackage.substring(1));
        }
    }
}
