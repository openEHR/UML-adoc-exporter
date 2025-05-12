package org.openehr.adoc.magicdraw;

/**
 * @author Bostjan Lah
 */
public class ImmClassFeature {
    private String cardinality = "";
    private String status = "";
    private String signature = "";
    private String documentation = "";

    public String getCardinality() {
        return cardinality;
    }

    public ImmClassFeature setCardinality (int lower, int upper) {
        cardinality = upper == -1 ? lower + "..1" : lower + ".." + upper;
        return this;
    }
    public String getStatus() {
        return status;
    }

    public ImmClassFeature setStatus(FeatureDefinitionStatus opStatus) {
        status = opStatus.toString().isEmpty()? "" : "(" + opStatus + ")";
        return this;
    }

    public String getSignature() {
        return signature;
    }

    public ImmClassFeature setSignature(String aSignature) {
        signature = aSignature;
        return this;
    }

    public String getDocumentation() {
        return documentation;
    }

    public ImmClassFeature setDocumentation(String aDocumentation) {
        documentation = aDocumentation;
        return this;
    }

}
