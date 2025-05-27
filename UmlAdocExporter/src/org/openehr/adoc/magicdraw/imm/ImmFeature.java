package org.openehr.adoc.magicdraw.imm;

import org.openehr.adoc.magicdraw.FeatureDefinitionStatus;
import org.openehr.adoc.magicdraw.amm.AmmProperty;

/**
 * @author Bostjan Lah
 */
public class ImmFeature {
    private String cardinality = "";
    private String status = "";
    private String signature = "";
    private String documentation = "";

    public String getCardinality() {
        return cardinality;
    }

    public void setCardinality (int lower, int upper) {
        cardinality = upper == -1 ? lower + "..1" : lower + ".." + upper;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(FeatureDefinitionStatus opStatus) {
        status = opStatus.toString().isEmpty()? "" : "(" + opStatus + ")";
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String aSignature) {
        signature = aSignature;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String aDocumentation) {
        documentation = aDocumentation;
    }

}
