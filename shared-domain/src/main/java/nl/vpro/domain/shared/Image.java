/*
 * Copyright (C) 2006/2007 All rights reserved
 * VPRO The Netherlands
 * Creation date 15-nov-2006.
 */

package nl.vpro.domain.shared;

import java.sql.Blob;

import javax.xml.bind.annotation.*;

import org.apache.log4j.Logger;

import nl.vpro.domain.PublishableObject;

/**
 * An image.
 *
 * @author arne
 * @author peter
 * @version $Id$
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {
        "title",
        "description",
        "width",
        "height",
        "imageType"})
public class Image extends PublishableObject {
    private static final Logger log = Logger.getLogger(Image.class);
    private static final long serialVersionUID = 2182582685395751329L;
    public static final String SCHEMA_NAME = "image";

    @XmlElement(namespace = "urn:vpro:media:2009")
    private String title;
    private String description;
    @XmlElement(namespace = "urn:vpro:media:2009")
    private Integer width;
    @XmlElement(namespace = "urn:vpro:media:2009")
    private Integer height;
    @XmlElement(namespace = "urn:vpro:media:2009")
    private ImageType imageType;
    @XmlTransient
    private Blob data;

    /**
     * @return the data
     */
    public Blob getData() {
        return data;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
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

    public Integer getHeight() {
        return height;
    }

    public ImageType getImageType() {
        return imageType;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    public Integer getWidth() {
        return width;
    }

    /**
     * @param data the data to set
     */
    public void setData(Blob data) {
        this.data = data;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @param height The height to set.
     */
    public void setHeight(Integer height) {
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
    public void setWidth(Integer width) {
        this.width = width;
    }

    public String getUrn() {
        return "urn:" + getDomain() + ":" + SCHEMA_NAME + ":" + getId();
    }
}
