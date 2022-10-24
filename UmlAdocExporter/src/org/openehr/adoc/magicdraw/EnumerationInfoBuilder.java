package org.openehr.adoc.magicdraw;

import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Enumeration;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.EnumerationLiteral;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Bostjan Lah
 */
public class EnumerationInfoBuilder extends AbstractInfoBuilder<Enumeration> {
    public EnumerationInfoBuilder(Formatter formatter, int pkgDepth, Function<String, Class> getUMLClassByQualifiedName) {
        super(formatter, pkgDepth, getUMLClassByQualifiedName);
    }

    @Override
    public ClassInfo build (Enumeration element) {

        String className = element.getName();

        ClassInfo classInfo = new ClassInfo("Enumeration")
                .setClassTypeName (className)
                .setDocumentation (getDocumentation (element, getFormatter()));

        setHierarchy (element.getQualifiedName(), packageDepth, classInfo);

        if (element.hasOwnedLiteral()) {
            addLiterals(classInfo.getAttributes(), element.getOwnedLiteral(), getFormatter());
        }

        return classInfo;
    }

    private void addLiterals(List<ClassFeatureInfo> attributes, List<EnumerationLiteral> ownedLiteral, Formatter formatter) {
        for (EnumerationLiteral literal : ownedLiteral) {
            attributes.add(new ClassFeatureInfo()
                                   .setSignature(literal.getName())
                                   .setDocumentation(getDocumentation(literal, formatter)));
        }
    }
}
