package org.openehr.adoc.magicdraw;

import org.openehr.bmm.core.BmmModel;

public final class UmlBmmModel {
    private static UmlBmmModel INSTANCE;

    private static BmmModel bmmModel;
    private UmlBmmModel() {
        bmmModel = new BmmModel();
    }

    public static UmlBmmModel getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new UmlBmmModel();
        }
        return INSTANCE;
    }
}
