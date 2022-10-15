package org.openehr.adoc.magicdraw;

import com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdtemplates.TemplateSignature;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Operation;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Property;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Bostjan Lah
 */
public class ClassInfoBuilder extends AbstractInfoBuilder<com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class> {
    public ClassInfoBuilder(Formatter formatter) {
        super(formatter);
    }

    @Override
    public ClassInfo build (com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class umlClass) {

        String className = umlClass.getName();

        // check for template parts
        TemplateSignature tplSig = umlClass.getOwnedTemplateSignature();
        if (tplSig != null) {
            // List<TemplateParameter>;
            // FIXME: we have to get the name using getHumanName() but remove
            // the 'Class '. There is no other way to get the parameter type name
            String tplParamsStr = tplSig.getOwnedParameter().stream()
                    .map(t -> t.getParameteredElement().getHumanName().replace("Class ", ""))
                    .collect(Collectors.joining(","));
            className = className + '<' + tplParamsStr + '>';
        }

        ClassInfo classInfo = new ClassInfo("Class")
                .setClassTypeName (className)
                .setDocumentation (getDocumentation (umlClass, getFormatter()))
                .setAbstractClass (umlClass.isAbstract());

        setHierarchy (umlClass.getQualifiedName(), classInfo);

        Map<String, Property> superClassAttributes = new HashMap<>();
        Map<String, Operation> superClassOperations = new HashMap<>();

        if (umlClass.hasSuperClass()) {
            for (Class umlSuperClass: umlClass.getSuperClass())
                classInfo.addParentClassName(umlSuperClass.getName());

            getSuperClassData (umlClass, superClassAttributes, superClassOperations);
        }

        if (umlClass.hasOwnedAttribute()) {
            addAttributes (classInfo.getAttributes(), umlClass.getOwnedAttribute(), superClassAttributes);
            addConstants (classInfo.getConstants(), umlClass.getOwnedAttribute(), superClassAttributes);
        }
        if (umlClass.hasOwnedOperation()) {
            addOperations (classInfo.getOperations(), umlClass.getOwnedOperation(), superClassOperations);
        }

        addConstraints(classInfo.getConstraints(), umlClass.get_constraintOfConstrainedElement());

        return classInfo;
    }

    private void getSuperClassData (Class element, Map<String, Property> superClassAttributes, Map<String, Operation> superClassOperations) {
        for (Class superClass : element.getSuperClass()) {
            // note that we use the 3-argument form of the toMap() function below, with the 3rd argument providing a clash resolver
            // to avoid clashing keys, i.e. due to same-named attributes and or operations being put into the Map result.
            superClassAttributes.putAll (superClass.getOwnedAttribute().stream().collect (Collectors.toMap (NamedElement::getName, p -> p, (p1, p2) -> p1)));
            superClassOperations.putAll (superClass.getOwnedOperation().stream().collect (Collectors.toMap (NamedElement::getName, p -> p, (p1, p2) -> p1)));
            getSuperClassData (superClass, superClassAttributes, superClassOperations);
        }
    }
}
