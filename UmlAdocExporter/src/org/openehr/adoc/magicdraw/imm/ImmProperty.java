package org.openehr.adoc.magicdraw.imm;

import org.openehr.adoc.magicdraw.FeatureDefinitionStatus;
import org.openehr.adoc.magicdraw.amm.AmmProperty;

public class ImmProperty extends AmmProperty {
    private final ImmFeature representation = new ImmFeature();
    public String getCardinality() {
        return representation.getCardinality();
    }

    public ImmProperty setCardinality (int lower, int upper) {
        representation.setCardinality(lower, upper);
        return this;
    }
    public String getStatus() {
        return representation.getStatus();
    }

    public ImmProperty setStatus(FeatureDefinitionStatus opStatus) {
        representation.setStatus(opStatus);
        return this;
    }

    public String getSignature() {
        return representation.getSignature();
    }

    public ImmProperty setSignature(String aSignature) {
        representation.setSignature(aSignature);
        return this;
    }

    public String getDocumentation() {
        return representation.getDocumentation();
    }

    public ImmProperty setDocumentation(String aDocumentation) {
        representation.setDocumentation(aDocumentation);
        return this;
    }

    public ImmFeature getRepresentation() {
        return representation;
    }
}
