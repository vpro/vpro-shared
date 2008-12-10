/**
 * Copyright (C) 2008 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.shared.bind;

import nl.vpro.domain.PublishableObject;

import javax.xml.bind.annotation.XmlElement;
import java.util.List;
import java.util.SortedSet;
import java.util.ArrayList;

public class ListWithPORefTypes {

    private List<PORefType> poObjects;

    @XmlElement(name = "item")
    public List<PORefType> getPOObjects() {
        return poObjects;
    }

    public void setPOObjects(List<PORefType> poObjects) {
        this.poObjects = poObjects;
    }

    public static <T extends PublishableObject> ListWithPORefTypes createPORefType(List<T> list) {
        if (list.size() > 0) {
            List<PORefType> listOut = new ArrayList<PORefType>();
            for (T poObject : list) {
                listOut.add(PORefType.createPORefType(poObject));
            }
            ListWithPORefTypes listWithPORefTypes = new ListWithPORefTypes();
            listWithPORefTypes.setPOObjects(listOut);
            return listWithPORefTypes;
        } else {
            return null;
        }
    }

}