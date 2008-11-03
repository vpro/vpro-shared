/*
 * Copyright (C) 2008 All rights reserved
 * VPRO The Netherlands
 * Creation date 1 nov 2008.
 */
package nl.vpro.domain;

/**
 * The workflow status for publishable items.
 *
 * @author arne
 * @author roekoe
 * @version $Id$
 */
public enum Workflow {
    DRAFT("ontwerp"), FOR_APPROVAL("ter goedkeuring"), PUBLISHED("gepubliceerd"), REFUSED("afgewezen"), DELETED("verwijderd"), MERGED("samengevoegd");

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


}
