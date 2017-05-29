/*
 * Copyright (C) 2008 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.shared.bind;

import nl.vpro.domain.SharedPublishableObject;
import nl.vpro.domain.shared.Image;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSeeAlso;

@XmlSeeAlso({Image.class})
public class PORefType {

    private String urn;
    private String type;

    public PORefType() {
    }

    @XmlAttribute
    public String getUrn() {
        return urn;
    }

    public void setUrn(String urn) {
        this.urn = urn;
    }

    @XmlAttribute
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public static <T extends SharedPublishableObject> PORefType createPORefType(T publishableObject) {
        PORefType poRefType = new PORefType();
        poRefType.setUrn(publishableObject.getUrn());
        poRefType.setType(publishableObject.getClass().getSimpleName().toLowerCase());
        return poRefType;
    }
}
