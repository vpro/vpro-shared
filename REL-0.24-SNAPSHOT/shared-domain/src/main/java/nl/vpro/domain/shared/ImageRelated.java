/*
 * Copyright (C) 2005/2006/2007 All rights reserved
 * VPRO The Netherlands
 * Creation date 15-jan-2007.
 */

package nl.vpro.domain.shared;

import nl.vpro.domain.SharedDomainObject;


/**
 * Related image wrapper. It's only use is making JAXB generated XML backward compatibel for
 * the Flash Media speler.
 *
 * @author roekoe
 * @version $Id$
 */
public class ImageRelated extends SharedDomainObject {

    private static final long serialVersionUID = 1667066853154310116L;

    private Image image;

    /**
     * Default constructor.
     */
    public ImageRelated() {
        // default constructor
    }

    /**
     * @param image The image to create te related for
     */
    public ImageRelated(Image image) {
        this.image = image;
    }

    /**
     * @return Returns the image.
     */
    public Image getImage() {
        return image;
    }

    /**
     * @param image The image to set.
     */
    public void setImage(Image image) {
        this.image = image;
    }

}
