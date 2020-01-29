package nl.vpro.rs.error;

/**
 * Date: 20-4-12
 * Time: 8:36
 *
 * @author Ernst Bunders
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException() {
    }

    public NotFoundException(String s) {
        super(s);
    }

    public NotFoundException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public NotFoundException(Throwable throwable) {
        super(throwable);
    }
}
