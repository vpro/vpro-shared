package nl.vpro.rs.error;

/**
 * @author Ernst Bunders
 */
public class ServerErrorException extends RuntimeException {
    public ServerErrorException() {
    }

    public ServerErrorException(String s) {
        super(s);
    }

    public ServerErrorException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public ServerErrorException(Throwable throwable) {
        super(throwable);
    }
}
