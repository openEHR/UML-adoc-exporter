package org.openehr.adoc.magicdraw;

import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Enumeration;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.EnumerationLiteral;

import java.util.List;
import java.util.function.Function;

/**
 * @author Bostjan Lah
 */
public class ImmEnumerationBuilder extends ImmEntityBuilder<Enumeration> {
    public ImmEnumerationBuilder(Formatter formatter, Function<String, Class> getUMLClassByQualifiedName) {
        super(formatter, getUMLClassByQualifiedName);
    }

    @Override
    public ImmClass build (Enumeration element) {

        String className = element.getName();

        ImmClass immClass = new ImmClass("Enumeration")
                .setClassTypeName (className)
                .setDocumentation (getUmlDocumentation(element, getFormatter()));

        setHierarchy (element.getQualifiedName(), UmlExportConfig.getInstance().getPackageDepth(), immClass);

        if (element.hasOwnedLiteral()) {
            addLiterals(immClass.getAttributes(), element.getOwnedLiteral(), getFormatter());
        }

        return immClass;
    }

    private void addLiterals(List<ImmClassFeature> attributes, List<EnumerationLiteral> ownedLiteral, Formatter formatter) {
        for (EnumerationLiteral literal : ownedLiteral) {
            attributes.add(new ImmClassFeature()
                                   .setSignature(literal.getName())
                                   .setDocumentation(getUmlDocumentation(literal, formatter)));
        }
    }
}
