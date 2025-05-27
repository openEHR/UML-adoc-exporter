package org.openehr.adoc.magicdraw.imm;

import org.openehr.adoc.magicdraw.amm.AmmConstraint;

/**
 * @author Bostjan Lah
 */
public class ImmConstraint extends AmmConstraint {
    private String documentation = "";

    public String getDocumentation() {
        return documentation;
    }

    public ImmConstraint setDocumentation(String documentation) {
        this.documentation = documentation;
        return this;
    }
}
