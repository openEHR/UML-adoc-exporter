package org.openehr.adoc.magicdraw.bmm;

import org.openehr.adoc.magicdraw.UmlExporterDefinitions;
import org.openehr.adoc.magicdraw.amm.AmmClass;
import org.openehr.adoc.magicdraw.amm.AmmConstraint;
import org.openehr.adoc.magicdraw.amm.AmmOperation;
import org.openehr.adoc.magicdraw.amm.AmmProperty;
import org.openehr.adoc.magicdraw.imm.ImmConstraint;
import org.openehr.adoc.magicdraw.imm.ImmOperation;
import org.openehr.adoc.magicdraw.imm.ImmProperty;
import org.openehr.bmm.core.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BmmClassAdapter extends AmmClass {

    private BmmClass bmmClass;

    private final List<BmmPropertyAdapter> attributes = new ArrayList<>();
    private final List<BmmPropertyAdapter> constants = new ArrayList<>();
    private final List<BmmFunctionAdapter> operations = new ArrayList<>();
    private final List<BmmConstraintAdapter> constraints = new ArrayList<>();
    private static final String GEN_PARAM_LETTERS = "TUV";

    public BmmClassAdapter(String name, String documentation, boolean isAbstract) {
        BmmSimpleClass bmmSimpleClass = new BmmSimpleClass (name, documentation, isAbstract);
        bmmClass = bmmSimpleClass;
    }

    public BmmClassAdapter(String name, String documentation, boolean isAbstract, List<String> genericParamTypes) {
        BmmGenericClass bmmGenericClass = new BmmGenericClass (name, documentation, isAbstract);
        HashMap<String, BmmParameterType> genParamTypesMap = new HashMap<>;
        int i = 0;
        for (String genParamType: genericParamTypes) {
            genParamTypesMap.put (GEN_PARAM_LETTERS.substring(i, i+1), new BmmParameterType (genParamType, UmlExporterDefinitions.AnyType));
            i++;
        }
        bmmGenericClass.setGenericParameters (genParamTypesMap);
        bmmClass = bmmGenericClass;
    }

    @Override
    public List<BmmPropertyAdapter> getAttributes() {
        return attributes;
    };

    @Override
    public List<BmmPropertyAdapter> getConstants() {
        return constants;
    };

    @Override
    public List<BmmFunctionAdapter> getOperations() {
        return operations;
    };

    @Override
    public List<BmmConstraintAdapter> getConstraints() {
        return constraints;
    };

    @Override
    public void addAttribute(AmmProperty ammProperty) {
        getAttributes().add((BmmPropertyAdapter) ammProperty);
    }

    @Override
    public void addConstant(AmmProperty ammConstant) {
        getConstants().add((BmmPropertyAdapter) ammConstant);
    }

    @Override
    public void addOperation(AmmOperation ammOperation) {
        getOperations().add((BmmFunctionAdapter) ammOperation);
    }

    @Override
    public void addConstraint(AmmConstraint ammConstraint) {
        getConstraints().add((BmmConstraintAdapter) ammConstraint);
    }

}
