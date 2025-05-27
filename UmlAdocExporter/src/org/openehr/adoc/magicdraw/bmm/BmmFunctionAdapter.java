package org.openehr.adoc.magicdraw.bmm;

import org.openehr.adoc.magicdraw.amm.AmmOperation;
import org.openehr.bmm.core.BmmFunction;

public class BmmFunctionAdapter extends AmmOperation {
    private BmmFunction<?> bmmFunction;

    public BmmFunctionAdapter(BmmFunction<?> bmmFunction) {
        this.bmmFunction = bmmFunction;
    }

    public BmmFunction<?> getBmmFunction() {
        return bmmFunction;
    }
}
