/*
 * Copyright (C) 2007/2008 All rights reserved
 * VPRO Omroepvereniging, The Netherlands
 * Creation date 18 sep 2008
 */

package nl.vpro.domain;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAccessType;

/**
 *
 * @author roekoe
 * @version $Id$
 */
@SuppressWarnings("serial")
@XmlAccessorType(XmlAccessType.FIELD)
public abstract class SharedDomainObject implements Serializable {

    @XmlAttribute
    protected Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

}
