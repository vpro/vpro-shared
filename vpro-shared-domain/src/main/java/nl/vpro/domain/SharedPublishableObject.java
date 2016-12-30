/*
 * Copyright (C) 2008 All rights reserved
 * VPRO The Netherlands
 * Creation date 1 nov 2008.
 */
package nl.vpro.domain;

import java.util.Date;

import nl.vpro.domain.users.Editor;

import javax.xml.bind.annotation.*;

/**
 * Publishable contains all items for Publishables.
 *
 * @author arne
 * @author roekoe
 * @version $Id$
 */
@SuppressWarnings("serial")
@XmlAccessorType(XmlAccessType.FIELD)
public abstract class SharedPublishableObject extends SharedDomainObject {

    @XmlAttribute
    protected Date creationDate;
    @XmlAttribute
    protected Date lastModified;
    @XmlTransient
    protected Editor createdBy;
    @XmlAttribute
    protected Date publishStart = new Date();
    @XmlTransient
    protected Editor lastModifiedBy;
    @XmlAttribute
    protected Date publishStop;
    @XmlAttribute
    protected SharedWorkflow workflow = SharedWorkflow.DRAFT;
    @XmlTransient
    protected Integer viewCounter;
    @XmlTransient
    protected String domain;

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public Editor getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(Editor lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public Editor getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Editor createdBy) {
        this.createdBy = createdBy;
    }

    public Date getPublishStart() {
        return publishStart;
    }

    public void setPublishStart(Date publishStart) {
        this.publishStart = publishStart;
    }

    public Date getPublishStop() {
        return publishStop;
    }

    public void setPublishStop(Date publishStop) {
        this.publishStop = publishStop;
    }

    @XmlAttribute(name = "urn")
    abstract public String getUrn();

    public SharedWorkflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(SharedWorkflow workflow) {
        this.workflow = workflow;
    }

    public Integer getViewCounter() {
        return viewCounter;
    }

    public void setViewCounter(Integer viewCounter) {
        this.viewCounter = viewCounter;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }
}
