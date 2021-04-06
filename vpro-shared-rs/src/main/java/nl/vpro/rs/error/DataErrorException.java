package nl.vpro.rs.error;

import lombok.Getter;

/**
 *
 * @author Ernst Bunders
 */
public class DataErrorException extends RuntimeException {
    @Getter
    private final DataError dataError;

    public DataErrorException(DataError dataError) {
        super();
        this.dataError = dataError;
    }

    public DataErrorException(String s, DataError dataError) {
        super(s);
        this.dataError = dataError;
    }

    public DataErrorException(String s, Throwable throwable, DataError dataError) {
        super(s, throwable);
        this.dataError = dataError;
    }

    public DataErrorException(Throwable throwable, DataError dataError) {
        super(throwable);
        this.dataError = dataError;
    }
}
