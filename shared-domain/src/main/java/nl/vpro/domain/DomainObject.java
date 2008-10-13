/*
 * Copyright (C) 2007/2008 All rights reserved
 * VPRO Omroepvereniging, The Netherlands
 * Creation date 18 sep 2008
 */

package nl.vpro.domain;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;

/**
 *
 * @author roekoe
 * @version $Id$
 */
public abstract class DomainObject implements Serializable {

    private static final long serialVersionUID = 247492039472015107L;

    private Long id;

    /**
     * @return the id
     */
    @XmlAttribute
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

}
