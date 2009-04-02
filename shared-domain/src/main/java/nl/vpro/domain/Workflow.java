/*
 * Copyright (C) 2008 All rights reserved
 * VPRO The Netherlands
 * Creation date 1 nov 2008.
 */
package nl.vpro.domain;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

/**
 * <p>The workflow status for publishable items.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="workflowEnumType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="DRAFT"/>
 *     &lt;enumeration value="FOR_APPROVAL"/>
 *     &lt;enumeration value="PUBLISHED"/>
 *     &lt;enumeration value="REFUSED"/>
 *     &lt;enumeration value="DELETED"/>
 *     &lt;enumeration value="MERGED"/>
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
public enum Workflow {

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
    Workflow(String description) {
        this.description = description;
    }

    /**
     * @return Returns the description.
     */
    public String getDescription() {
        return description;
    }

    public static Workflow fromValue(String s) {
        for (Workflow w: Workflow.values()) {
            if (w.description.equals(s)) {
                return w;
            }
        }
        throw new IllegalArgumentException(s.toString());
    }
}
