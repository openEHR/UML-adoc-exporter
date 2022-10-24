package org.openehr.adoc.magicdraw;

/**
 * @author Bostjan Lah
 */
public class ClassFeatureInfo {
    private String cardinality = "";
    private String status = "";
    private String signature = "";
    private String documentation = "";

    public String getCardinality() {
        return cardinality;
    }

    public ClassFeatureInfo setCardinality (String aCardinality) {
        cardinality = aCardinality;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public ClassFeatureInfo setStatus(String aStatus) {
        status = aStatus;
        return this;
    }

    public String getSignature() {
        return signature;
    }

    public ClassFeatureInfo setSignature(String aSignature) {
        signature = aSignature;
        return this;
    }

    public String getDocumentation() {
        return documentation;
    }

    public ClassFeatureInfo setDocumentation(String aDocumentation) {
        documentation = aDocumentation;
        return this;
    }

}
