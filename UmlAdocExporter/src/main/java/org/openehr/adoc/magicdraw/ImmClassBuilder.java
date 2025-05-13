package org.openehr.adoc.magicdraw;

import com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdtemplates.TemplateSignature;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Operation;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Property;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Bostjan Lah
 */
public class ImmClassBuilder extends ImmEntityBuilder<Class> {
    public ImmClassBuilder(Formatter formatter, Function<String, Class> getUMLClassByQualifiedName) {
        super(formatter, getUMLClassByQualifiedName);
    }

    @Override
    public ImmClass build (com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class umlClass) {

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

        ImmClass immClass = new ImmClass("Class")
                .setClassTypeName (className)
                .setDocumentation (getUmlDocumentation(umlClass, getFormatter()))
                .setAbstractClass (umlClass.isAbstract());

        setHierarchy (umlClass.getQualifiedName(), UmlExportConfig.getInstance().getPackageDepth(), immClass);

        Map<String, Property> superClassAttributes = new HashMap<>();
        Map<String, Operation> superClassOperations = new HashMap<>();

        if (umlClass.hasSuperClass()) {
            for (Class umlSuperClass: umlClass.getSuperClass())
                immClass.addQualifiedParentClassName (convertToQualified (umlSuperClass.getQualifiedName()));

            getSuperClassData (umlClass, superClassAttributes, superClassOperations);
        }

        if (umlClass.hasOwnedAttribute()) {
            addAttributes (immClass.getAttributes(), umlClass.getOwnedAttribute(), superClassAttributes);
            addConstants (immClass.getConstants(), umlClass.getOwnedAttribute(), superClassAttributes);
        }
        if (umlClass.hasOwnedOperation()) {
            addOperations (immClass.getOperations(), umlClass.getOwnedOperation(), superClassOperations);
        }

        addConstraints(immClass.getConstraints(), umlClass.get_constraintOfConstrainedElement());

        return immClass;
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
