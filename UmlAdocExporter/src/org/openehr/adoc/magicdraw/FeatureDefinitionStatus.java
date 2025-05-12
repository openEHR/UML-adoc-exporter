package org.openehr.adoc.magicdraw;

public enum FeatureDefinitionStatus {
    // operation is abstract in current class
    ABSTRACT ("abstract"),

    // operation effects previous abstract definition
    EFFECTED ("effected"),

    // operation effects previous abstract definition
    REDEFINED ("redefined"),

    // operation is defined in current class
    DEFINED ("");

    private String literalName;
    FeatureDefinitionStatus(String litName) {
        this.literalName = litName;
    }

    @Override
    public String toString(){
        return literalName;
    }
}
