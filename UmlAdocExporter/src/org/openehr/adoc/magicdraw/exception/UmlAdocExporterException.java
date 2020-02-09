package org.openehr.adoc.magicdraw.exception;

/**
 * @author Bostjan Lah
 */
public class UmlAdocExporterException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public UmlAdocExporterException(String message) {
        super(message);
    }

    public UmlAdocExporterException(Throwable cause) {
        super(cause);
    }

    public UmlAdocExporterException(String message, Throwable cause) {
        super(message, cause);
    }
}
