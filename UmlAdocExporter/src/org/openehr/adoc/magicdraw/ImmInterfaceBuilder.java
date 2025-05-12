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
public class ImmInterfaceBuilder extends ImmEntityBuilder<Interface> {

    public ImmInterfaceBuilder(Formatter formatter, Function<String, Class> getUMLClassByQualifiedName) {
        super(formatter, getUMLClassByQualifiedName);
    }

    @Override
    public ImmClass build (Interface element) {

        String className = element.getName();

        ImmClass immClass = new ImmClass("Interface")
                .setClassTypeName (className)
                .setDocumentation (getUmlDocumentation(element, getFormatter()))
                .setAbstractClass (element.isAbstract());

        setHierarchy(element.getQualifiedName(), UmlExportConfig.getInstance().getPackageDepth(), immClass);

        Map<String, Property> superClassAttributes = new HashMap<>();
        Map<String, Operation> superClassOperations = new HashMap<>();

        if (element.hasOwnedAttribute())
            addAttributes (immClass.getAttributes(), element.getOwnedAttribute(), superClassAttributes);

        if (element.hasOwnedOperation())
            addOperations (immClass.getOperations(), element.getOwnedOperation(), superClassOperations);

        addConstraints (immClass.getConstraints(), element.get_constraintOfConstrainedElement());

        return immClass;
    }

}
