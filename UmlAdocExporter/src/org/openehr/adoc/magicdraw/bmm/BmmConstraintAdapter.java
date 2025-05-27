package org.openehr.adoc.magicdraw.bmm;

import org.openehr.adoc.magicdraw.amm.AmmConstraint;
import org.openehr.bmm.core.BmmAssertion;

public class BmmConstraintAdapter extends AmmConstraint {

    private final BmmAssertion bmmAssertion;

    public BmmConstraintAdapter(BmmAssertion bmmAssertion) {
        this.bmmAssertion = bmmAssertion;
    }

    public BmmAssertion getBmmAssertion() {
        return bmmAssertion;
    }
}
