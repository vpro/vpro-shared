
package nl.vpro.domain.shared;

import java.io.File;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 * Supported image types.
 *
 * @author peter
 * @version $Id$
 */
@XmlEnum
@XmlType(name = "imageTypeEnum")
public enum ImageType {
    JPG, GIF, PNG, BMP, PCX, IFF, RAS, PBM, PGM, PPM, PSD;

    private static final String JPEG_EXT = "JPEG";

    private static final String PJPEG_EXT = "PJPEG";

    private boolean allowed = true;

    private ImageType(boolean allowed) {
       this.allowed = allowed;
    }

    private ImageType() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Get the image type from a file.
     *
     * @param f the file
     * @return the imagetype, or <code>null</code> if not found.
     */
    public static ImageType fromFile(File f) {
        return fromFileName(f.getName());
    }

    /**
     * Get the image type from a filename.
     *
     * @param n the filename
     * @return the imagetype, or <code>null</code> if not found.
     */
    public static ImageType fromFileName(String n) {
        String extension = n.substring(n.lastIndexOf(".") + 1, n.length());
        if (isJPGPermutation(extension)) {
            return JPG;
        }
        return ImageType.valueOf(extension.toUpperCase());
    }

    /**
     * @param extension
     * @return
     */
    private static boolean isJPGPermutation(String extension) {
        return JPEG_EXT.equalsIgnoreCase(extension) || PJPEG_EXT.equalsIgnoreCase(extension);
    }

    /**
     * Get the image type from a name.
     *
     * @return the imagetype, or <code>null</code> if not found.
     */
    public static ImageType fromName(String value) {
        if (isJPGPermutation(value)) {
            return JPG;
        }
        return ImageType.valueOf(value.toUpperCase());
    }

    /**
     * Get the image type from a content type.
     *
     * @param n the contenttype
     * @return the imagetype, or <code>null</code> if not found.
     * @throws UnsupportedImageTypeException
     */
    public static ImageType fromContentType(String n) throws UnsupportedImageTypeException {
        String extension = n.substring(n.lastIndexOf("/") + 1, n.length());
        if (isJPGPermutation(extension)) {
            return JPG;
        }
        try {
            return ImageType.valueOf(extension.toUpperCase());
        } catch (Exception e) {
            throw new UnsupportedImageTypeException(extension);
        }
    }

    public boolean isAllowed(){
        return allowed;
    }
}
