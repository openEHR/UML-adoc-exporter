package org.openehr.adoc.magicdraw.amm;

import java.util.List;

public abstract class  AmmClass {

    public abstract List<? extends AmmProperty> getAttributes();

    public abstract List<? extends AmmProperty> getConstants();

    public abstract List<? extends AmmOperation> getOperations();

    public abstract List<? extends AmmConstraint> getConstraints();

    public abstract void addAttribute(AmmProperty ammProperty);

    public abstract void addConstant(AmmProperty ammConstant);

    public abstract void addOperation(AmmOperation ammOperation);

    public abstract void addConstraint(AmmConstraint ammConstraint);

}
