/**
 * Copyright (C) 2008 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.shared.bind;

import nl.vpro.domain.PublishableObject;
import nl.vpro.domain.shared.Image;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSeeAlso;

@XmlSeeAlso({Image.class})
public class PORefType {

    private String urn;

    public PORefType() {
    }

    @XmlAttribute
    public String getUrn() {
        return urn;
    }

    public void setUrn(String urn) {
        this.urn = urn;
    }

    public static <T extends PublishableObject> PORefType createPORefType(T publishableObject) {
        PORefType poRefType = new PORefType();
        poRefType.setUrn(publishableObject.getUrn());
        return poRefType;
    }
}
