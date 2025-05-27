package org.openehr.adoc.magicdraw.imm;

import org.openehr.adoc.magicdraw.FeatureDefinitionStatus;
import org.openehr.adoc.magicdraw.amm.AmmOperation;

public class ImmOperation extends AmmOperation {
    private final ImmFeature representation = new ImmFeature();
    public String getCardinality() {
        return representation.getCardinality();
    }

    public ImmOperation setCardinality (int lower, int upper) {
        representation.setCardinality(lower, upper);
        return this;
    }
    public String getStatus() {
        return representation.getStatus();
    }

    public ImmOperation setStatus(FeatureDefinitionStatus opStatus) {
        representation.setStatus(opStatus);
        return this;
    }

    public String getSignature() {
        return representation.getSignature();
    }

    public ImmOperation setSignature(String aSignature) {
        representation.setSignature(aSignature);
        return this;
    }

    public String getDocumentation() {
        return representation.getDocumentation();
    }

    public ImmOperation setDocumentation(String aDocumentation) {
        representation.setDocumentation(aDocumentation);
        return this;
    }

    public ImmFeature getRepresentation() {
        return representation;
    }
}
