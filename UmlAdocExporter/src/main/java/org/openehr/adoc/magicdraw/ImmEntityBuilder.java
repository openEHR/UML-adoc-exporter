package org.openehr.adoc.magicdraw;

import com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdtemplates.*;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.*;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class;

import java.util.*;
import java.util.function.Function;

/**
 * @author Bostjan Lah
 */
public abstract class ImmEntityBuilder<T> extends AmmEntityBuilder<T> {

    protected ImmEntityBuilder(Formatter formatter, Function<String, Class> getUMLClassByQualifiedName) {
        super (formatter, getUMLClassByQualifiedName);
    }


    // Add class- or routine-level constraints
    protected void addConstraints (List<ImmConstraint> constraints, Collection<Constraint> constraintOfConstrainedElement) {
        for (Constraint constraint : constraintOfConstrainedElement) {
            constraints.add(new ImmConstraint().setDocumentation(formatConstraint(constraint)));
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

    /**
     * Build a ClassFeatureInfo object for property and add it to the attributes list.
     * @param attributes List of ClassFeatureInfo objects for this class so far built.
     * @param umlProperty the property to add.
     * @param attrStatus Status of attribute in this class: defined, redefined etc.
     */
    protected void addAttribute (List<ImmClassFeature> attributes, Property umlProperty, FeatureDefinitionStatus attrStatus) {
        // create a ClassFeatureInfo with attribute documentation, occurrences and redefined marker
        ImmClassFeature immClassFeature = new ImmClassFeature()
                .setDocumentation(getUmlDocumentation(umlProperty, formatter))
                .setCardinality(umlProperty.getLower(), umlProperty.getUpper())
                .setStatus(attrStatus);

        // attribute signature
        StringBuilder sigBuilder = new StringBuilder(formatter.bold(umlProperty.getName()));
        sigBuilder.append(": ");

        // determine the type in qualified form
        String propertyQualifiedTypeName = "";
        String propertyUmlQualifiedTypeName = "";
        if (umlProperty.getType() != null) {
            propertyUmlQualifiedTypeName = umlProperty.getType().getQualifiedName();
            propertyQualifiedTypeName = convertToQualified (propertyUmlQualifiedTypeName);
        }

        // if there is a qualifier on the property, get it, since this will modify the type
        Property umlQualifier = umlProperty.getAssociation() != null && umlProperty.hasQualifier() ? umlProperty.getQualifier().get(0) : null;

        // Now create a proper type string, with corrections to inject List<> and Hash<> where needed
        StringBuilder typeInfo = new StringBuilder (correctType (propertyQualifiedTypeName, umlQualifier, umlProperty.getLower(), umlProperty.getUpper()));

        // If there is a default value defined, attach it to the type, on a new line.
        ValueSpecification defaultValue = umlProperty.getDefaultValue();
        if (defaultValue != null) {
            // if the property is a constant, then
            // output '=' + <default value>; else
            // output \n {default '=' + <default value>}
            if (!umlProperty.isReadOnly())
                typeInfo.append(formatter.hardLineBreak()).append("{default");

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
            else if (defaultValue instanceof EnumerationLiteral) {
                EnumerationLiteral value = (EnumerationLiteral) defaultValue;
                typeInfo.append("{nbsp}={nbsp}").append(value);
            }
            else if (defaultValue instanceof InstanceValue) {
                InstanceValue value = (InstanceValue) defaultValue;
                String spec = value.getInstance().getName();
                if (spec != null)
                    typeInfo.append("{nbsp}={nbsp}").append(spec);
                else
                    typeInfo.append("{nbsp}={nbsp}(unknown)");
            }
            else {
                // This case is not yet working; idea is to generate the symbol string
                // for a constant whose 'value' is another property or constant
                Expression expr = defaultValue.getExpression();
                if (expr != null)
                    typeInfo.append("{nbsp}={nbsp}").append(expr.getSymbol());
            }

            if (!umlProperty.isReadOnly())
                typeInfo.append("}");
        }

        // If there is any type information, append it
        if (typeInfo.length() > 0)
            sigBuilder.append (formatter.monospace (typeInfo.toString()));

        immClassFeature.setSignature(sigBuilder.toString());

        attributes.add(immClassFeature);
    }


    /**
     * Build a ClassFeatureInfo for operation, and append it to the features list so far built.
     * @param features List of class features so far built.
     * @param umlOperation UML operation definition.
     * @param opStatus Status of operation in this class: abstract, effected, defined etc.
     */
    protected void addOperation(List<ImmClassFeature> features, Operation umlOperation, FeatureDefinitionStatus opStatus) {
        // Create the main documentation.
        StringBuilder opDocBuilder = new StringBuilder(getUmlDocumentation(umlOperation, formatter));
        opDocBuilder.append(System.lineSeparator());

        // Start building the operation signature
        // append the operation name, bolded
        StringBuilder opSigBuilder = new StringBuilder(formatter.bold(umlOperation.getName()));

        // see if the operation has stereotype <<Operator>>, which has tag ops: List<String>
        // or <<Symbolic_operator>>, which has tag sym_ops: List<String>
        // See comment above for stereotypeTagNames for details
        StringBuilder opAliasBuilder = new StringBuilder();
        List<TaggedValue> taggedValues = umlOperation.getTaggedValue();

        Iterator<TaggedValue> taggedValuesIt = taggedValues.iterator();
        while (taggedValuesIt.hasNext()) {
            TaggedValue taggedValue = taggedValuesIt.next();
            Property tagProp = taggedValue.getTagDefinition();

            // Here we check that the slot attribute name is one of the ones we want
            if (stereotypeTagNames.contains(tagProp.getName()) &&
                    taggedValue instanceof StringTaggedValue &&
                    taggedValue.hasValue()) {
                // Now we know we have the operator tags, we can output the 'alias' line
                // (Use the first variant to put it on a new line, plus uncomment the
                // post-loop statement to add another NL)
                // opSigBuilder.append(formatter.hardLineBreak() + formatter.italic("alias") + " ");
                if (opAliasBuilder.length() == 0)
                    opAliasBuilder.append(" " + formatter.italic("alias") + " ");

                StringTaggedValue stringTaggedValue = (StringTaggedValue) taggedValue;

                Iterator<String> stringTaggedValueIt = stringTaggedValue.getValue().iterator();
                while (stringTaggedValueIt.hasNext()) {
                    String opStr = stringTaggedValueIt.next();
                    opAliasBuilder.append(formatter.escapeLiteral(opStr));
                    if (taggedValuesIt.hasNext() || stringTaggedValueIt.hasNext())
                        opAliasBuilder.append(", ");
                }
            }
        }
        opSigBuilder.append(opAliasBuilder);

        // If there are parameters, output them within parentheses; also
        // add the parameter documentation to the documentary text
        if (umlOperation.hasOwnedParameter()) {
            opDocBuilder.append(System.lineSeparator());
            addSignatureParameters(opSigBuilder, opDocBuilder, umlOperation.getOwnedParameter());
        }

        // If there is a return type, append it to the signature in monospace.
        String qualifiedTypeName = umlOperation.getType() == null ? "" : convertToQualified (umlOperation.getType().getQualifiedName());
        StringBuilder fullSigBuilder = qualifiedTypeName.isEmpty()
                ? opSigBuilder
                : new StringBuilder(opSigBuilder + ": " + formatter.monospace(correctType(qualifiedTypeName, null, umlOperation.getLower(), umlOperation.getUpper())));

        // Output any operation pre- and post-conditions (UML constraints)
        addOperationConstraint(umlOperation, fullSigBuilder);

        // Create and set the error documentation, if there is any.
        String errStr = getUmlErrorDocumentation(umlOperation, formatter);
        if (!errStr.isEmpty()) {
            opDocBuilder.append(System.lineSeparator())
                .append (formatter.errorDelimiterLine())
                .append (getUmlErrorDocumentation(umlOperation, formatter))
                .append (System.lineSeparator());
        }

        ImmClassFeature immClassFeature = new ImmClassFeature()
                .setCardinality (umlOperation.getLower(), umlOperation.getUpper())
                .setStatus (opStatus)
                .setDocumentation (opDocBuilder.toString())
                .setSignature (fullSigBuilder.toString());

        features.add(immClassFeature);
    }

    /**
     * Add parameters for a UML method in a class definition to the operation string.
     * @param parameters UML parameter definitions.
     * @param sigBuilder string builder containing method definition as a string.
     */
    protected void addSignatureParameters(StringBuilder sigBuilder, StringBuilder docBuilder, List<Parameter> parameters) {
        List<String> parameterSigs = new ArrayList<>();
        List<String> parameterDocs = new ArrayList<>();
        for (Parameter parameter : parameters) {
            String paramName = parameter.getName();
            if (!"return".equals(paramName) && !paramName.isEmpty()) {
                if (parameter.getType() == null)
                    parameterSigs.add(paramName);
                else {
                    parameterSigs.add(
                            paramName + ": " + formatter.monospace(
                                    correctType(convertToQualified (parameter.getType().getQualifiedName()),
                                            null, parameter.getLower(), parameter.getUpper()) +
                                            '[' + formatInlineOccurences (parameter.getLower(), parameter.getUpper()) + ']'
                            )
                    );
                }

                String paramComment = getUmlDocumentation(parameter, formatter);
                if (!paramComment.isEmpty()) {
                    parameterDocs.add(System.lineSeparator() + formatter.italicMonospace(paramName)  + ":: " + paramComment);
                }
            }
        }

        // if there are parameters, put them out on different lines, else just output "()"
        if (!parameterSigs.isEmpty()) {
            sigBuilder.append(" (").append(formatter.hardLineBreak());
            sigBuilder.append(String.join("," + formatter.hardLineBreak(), parameterSigs)).append(formatter.hardLineBreak());
            sigBuilder.append(')');
        }
        else
            sigBuilder.append(" ()");

        if (!parameterDocs.isEmpty()) {
            docBuilder.append (".Parameters");
            docBuilder.append(formatter.hardLineBreak());
            docBuilder.append ("[horizontal]");
            docBuilder.append(String.join("\n", parameterDocs));
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
     * Extract package information from a string like
     * "RM::org::openehr::rm::common
     */
    protected void setHierarchy (String umlQualifiedName, int pkgDepth, ImmClass immClass) {
        String[] parts = umlQualifiedName.split ("::");
        int depth = Math.min(parts.length, pkgDepth);
        if (parts.length > depth) {
            immClass.setSpecComponent(parts[0]);
            immClass.setClassPackage(parts[depth]);

            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < depth; i++)
                sb.append('.').append (parts[i]);
            immClass.setComponentPackage(sb.substring(1));
        }
    }

}
