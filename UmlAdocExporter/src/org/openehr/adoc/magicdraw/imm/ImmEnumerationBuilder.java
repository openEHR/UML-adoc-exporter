package org.openehr.adoc.magicdraw.imm;

import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Enumeration;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.EnumerationLiteral;
import org.openehr.adoc.magicdraw.Formatter;
import org.openehr.adoc.magicdraw.UmlExportConfig;

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
            addLiterals(immClass, element.getOwnedLiteral(), getFormatter());
        }

        return immClass;
    }

    private void addLiterals(ImmClass immClass, List<EnumerationLiteral> ownedLiteral, Formatter formatter) {
        for (EnumerationLiteral literal : ownedLiteral) {
            immClass.addAttribute(new ImmProperty()
                                   .setSignature(literal.getName())
                                   .setDocumentation(getUmlDocumentation(literal, formatter)));
        }
    }
}
