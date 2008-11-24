/*
 * Copyright (C) 2008 All rights reserved
 * VPRO The Netherlands
 * Creation date 1 nov 2008.
 */
package nl.vpro.domain;

import java.util.Date;

import nl.vpro.domain.users.Editor;

import javax.xml.bind.annotation.XmlTransient;

/**
 * Publishable contains all items for Publishables.
 *
 * @author arne
 * @author roekoe
 * @version $Id$
 */
@SuppressWarnings("serial")
public abstract class PublishableObject extends DomainObject {

    private Date lastModified;

    private Date creationDate;

    private Editor lastModifiedBy;

    private Editor createdBy;

    private Date publishStart = new Date();

    private Date publishStop;

    private Workflow workflow = Workflow.DRAFT;

    private Integer viewCounter;

    /**
     * @return the lastModified
     */
    @XmlTransient
    public Date getLastModified() {
        return lastModified;
    }

    /**
     * @param lastModified the lastModified to set
     */
    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * @return the creationDate
     */
    @XmlTransient
    public Date getCreationDate() {
        return creationDate;
    }

    /**
     * @param creationDate the creationDate to set
     */
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    /**
     * @return the lastModifiedBy
     */
    @XmlTransient
    public Editor getLastModifiedBy() {
        return lastModifiedBy;
    }

    /**
     * @param lastModifiedBy the lastModifiedBy to set
     */
    public void setLastModifiedBy(Editor lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    /**
     * @return the createdBy
     */
    @XmlTransient
    public Editor getCreatedBy() {
        return createdBy;
    }

    /**
     * @param createdBy the createdBy to set
     */
    public void setCreatedBy(Editor createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * @return the publishStart
     */
    @XmlTransient
    public Date getPublishStart() {
        return publishStart;
    }

    /**
     * @param publishStart the publishStart to set
     */
    public void setPublishStart(Date publishStart) {
        this.publishStart = publishStart;
    }

    /**
     * @return the publishStop
     */
    @XmlTransient
    public Date getPublishStop() {
        return publishStop;
    }

    /**
     * @param publishStop the publishStop to set
     */
    public void setPublishStop(Date publishStop) {
        this.publishStop = publishStop;
    }

    /**
     * @return the workflow
     */
    @XmlTransient
    public Workflow getWorkflow() {
        return workflow;
    }

    /**
     * @param workflow the workflow to set
     */
    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    /**
     * @return the viewCounter
     */
    @XmlTransient
    public Integer getViewCounter() {
        return viewCounter;
    }

    /**
     * @param viewCounter the viewCounter to set
     */
    public void setViewCounter(Integer viewCounter) {
        this.viewCounter = viewCounter;
    }

}
