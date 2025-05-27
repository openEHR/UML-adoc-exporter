package org.openehr.adoc.magicdraw.bmm;

import org.openehr.adoc.magicdraw.amm.AmmProperty;
import org.openehr.bmm.core.BmmProperty;

public class BmmPropertyAdapter extends AmmProperty {
    private final BmmProperty<?> bmmProperty;

    public BmmPropertyAdapter(BmmProperty<?> bmmProperty) {
        this.bmmProperty = bmmProperty;
    }

    public BmmProperty<?> getBmmProperty() {
        return bmmProperty;
    }
}
