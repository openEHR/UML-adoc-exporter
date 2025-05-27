package org.openehr.adoc.magicdraw.bmm;

import com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdtemplates.TemplateSignature;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Operation;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Property;
import org.openehr.adoc.magicdraw.Formatter;
import org.openehr.adoc.magicdraw.imm.ImmClass;
import org.openehr.adoc.magicdraw.UmlExportConfig;
import org.openehr.bmm.core.BmmClass;
import org.openehr.bmm.core.BmmSimpleClass;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BmmClassBuilder extends BmmEntityBuilder<Class> {
    public BmmClassBuilder(Formatter formatter, Function<String, Class> getUMLClassByQualifiedName) {
        super(formatter, getUMLClassByQualifiedName);
    }

    @Override
    public BmmClassAdapter build(com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class umlClass) {

        String className = umlClass.getName();

        // check for template parts
        TemplateSignature tplSig = umlClass.getOwnedTemplateSignature();
        if (tplSig != null) {
            // List<TemplateParameter>;
            // FIXME: we have to get the name using getHumanName() but remove
            // the 'Class '. There is no other way to get the parameter type name
            List<String> tplParams = tplSig.getOwnedParameter().stream()
                    .map(t -> t.getParameteredElement().getHumanName().replace("Class ", ""))
                    .collect(Collectors.toList());
        }

        BmmClassAdapter bmmClassAdapter = new BmmClassAdapter(className, getUmlDocumentation(umlClass, getFormatter()), umlClass.isAbstract());

        setHierarchy (umlClass.getQualifiedName(), UmlExportConfig.getInstance().getPackageDepth(), bmmClassAdapter);

        Map<String, Property> superClassAttributes = new HashMap<>();
        Map<String, Operation> superClassOperations = new HashMap<>();

        if (umlClass.hasSuperClass()) {
            for (Class umlSuperClass: umlClass.getSuperClass())
                bmmClassAdapter.addQualifiedParentClassName (convertToQualified (umlSuperClass.getQualifiedName()));

            getSuperClassData (umlClass, superClassAttributes, superClassOperations);
        }

        if (umlClass.hasOwnedAttribute()) {
            addAttributes (bmmClassAdapter, umlClass.getOwnedAttribute(), superClassAttributes);
            addConstants (bmmClassAdapter, umlClass.getOwnedAttribute(), superClassAttributes);
        }
        if (umlClass.hasOwnedOperation()) {
            addOperations (bmmClassAdapter, umlClass.getOwnedOperation(), superClassOperations);
        }

        addConstraints(bmmClassAdapter, umlClass.get_constraintOfConstrainedElement());

        return bmmClassAdapter;
    }

}