/*
 * Copyright (C) 2005/2006/2007 All rights reserved
 * VPRO The Netherlands
 * Creation date 15-mrt-2007.
 */

package nl.vpro.domain.shared;

/**
 *
 *
 * @author arne
 * @version $Id$
 */
public class UnsupportedImageTypeException extends Exception {

    /** */
    private static final long serialVersionUID = -5484822919403580553L;
    private String contentType;

    /**
     *
     */
    public UnsupportedImageTypeException(String contentType) {
        super();
        this.contentType = contentType;
    }

    /**
     * @param message
     * @param cause
     */
    public UnsupportedImageTypeException(String contentType, String message, Throwable cause) {
        super(message, cause);
        this.contentType = contentType;
    }

    /**
     * @param message
     */
    public UnsupportedImageTypeException(String contentType, String message) {
        super(message);
        this.contentType = contentType;
    }

    /**
     * @param cause
     */
    public UnsupportedImageTypeException(String contentType, Throwable cause) {
        super(cause);
        this.contentType = contentType;
    }

    /**
     * @return Returns the contentType.
     */
    public String getContentType() {
        return contentType;
    }

}
