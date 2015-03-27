/*
 * Copyright (C) 2008 All rights reserved
 * VPRO The Netherlands
 * Creation date 1 nov 2008.
 */
package nl.vpro.domain;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

/**
 * <p>The workflow status for publishable items.</p>
 *
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * <pre>
 * &lt;simpleType name="workflowEnumType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="DRAFT"/&gt;
 *     &lt;enumeration value="FOR_APPROVAL"/&gt;
 *     &lt;enumeration value="PUBLISHED"/&gt;
 *     &lt;enumeration value="REFUSED"/&gt;
 *     &lt;enumeration value="DELETED"/&gt;
 *     &lt;enumeration value="MERGED"/&gt;
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 *
 *
 * @author arne
 * @author roekoe
 * @version $Id$
 */
@XmlEnum
public enum SharedWorkflow {

    DRAFT("ontwerp"),
    @XmlEnumValue("FOR APPROVAL")
    FOR_APPROVAL("ter goedkeuring"),
    PUBLISHED("gepubliceerd"),
    REFUSED("afgewezen"),
    DELETED("verwijderd"),
    MERGED("samengevoegd");

    private String description;

    /**
     * Sole constructor
     */
    SharedWorkflow(String description) {
        this.description = description;
    }

    /**
     * @return Returns the description.
     */
    public String getDescription() {
        return description;
    }

    public static SharedWorkflow fromValue(String s) {
        for (SharedWorkflow w: SharedWorkflow.values()) {
            if (w.description.equals(s)) {
                return w;
            }
        }
        throw new IllegalArgumentException(s.toString());
    }
}
