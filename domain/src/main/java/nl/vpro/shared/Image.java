/*
 * Copyright (C) 2006/2007 All rights reserved
 * VPRO The Netherlands
 * Creation date 15-nov-2006.
 */

package nl.vpro.domain.shared;

import java.sql.Blob;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.apache.log4j.Logger;

import nl.vpro.domain.DomainObject;

/**
 * An image.
 *
 * @author arne
 * @author peter
 * @version $Id$
 */
@XmlType(propOrder = {"title", "width", "height","imageType"})
public class Image extends DomainObject {

    private static final long serialVersionUID = 2182582685395751329L;

    private static final Logger log = Logger.getLogger(Image.class);

    private Blob data;

    private int height;

    private ImageType imageType;

    private String title;

    private int width;

    /**
     * @return the data
     */
    @XmlTransient
    public Blob getData() {
        return data;
    }

    /**
     * @return The extension (jpg, gif, etc.) to use for this image
     */
    public String getExtension() {
        if (imageType!=null) {
            return imageType.toString().toLowerCase();
        } else {
            return ImageType.JPG.toString().toLowerCase();
        }
    }

    /**
     * @return Returns the height.
     */
    @XmlElement
    public int getHeight() {
        return height;
    }

    /**
     * @return the image type
     */
    public ImageType getImageType() {
        return imageType;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return Returns the width.
     */
    @XmlElement
    public int getWidth() {
        return width;
    }

    /**
     * @param data the data to set
     */
    public void setData(Blob data) {
        this.data = data;
    }

    /**
     * @param height The height to set.
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * @param imageType the image type
     */
    public void setImageType(ImageType imageType) {
        this.imageType = imageType;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @param width The width to set.
     */
    public void setWidth(int width) {
        this.width = width;
    }

}
