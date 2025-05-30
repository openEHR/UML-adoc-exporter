package org.openehr.adoc.magicdraw;

import com.nomagic.uml2.ext.magicdraw.classes.mdinterfaces.Interface;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Operation;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Property;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Bostjan Lah
 */
public class InterfaceInfoBuilder extends AbstractInfoBuilder<Interface> {

    public InterfaceInfoBuilder (Formatter formatter, Function<String, Class> getUMLClassByQualifiedName) {
        super(formatter, getUMLClassByQualifiedName);
    }

    @Override
    public ClassInfo build (Interface element) {

        String className = element.getName();

        ClassInfo classInfo = new ClassInfo("Interface")
                .setClassTypeName (className)
                .setDocumentation (getDocumentation(element, getFormatter()))
                .setAbstractClass (element.isAbstract());

        setHierarchy(element.getQualifiedName(), UmlExportConfig.getInstance().getPackageDepth(), classInfo);

        Map<String, Property> superClassAttributes = new HashMap<>();
        Map<String, Operation> superClassOperations = new HashMap<>();

        if (element.hasOwnedAttribute())
            addAttributes (classInfo.getAttributes(), element.getOwnedAttribute(), superClassAttributes);

        if (element.hasOwnedOperation())
            addOperations (classInfo.getOperations(), element.getOwnedOperation(), superClassOperations);

        addConstraints (classInfo.getConstraints(), element.get_constraintOfConstrainedElement());

        return classInfo;
    }

}
