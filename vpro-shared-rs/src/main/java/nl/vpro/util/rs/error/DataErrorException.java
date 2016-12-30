package nl.vpro.util.rs.error;

/**
 * Date: 2-5-12
 * Time: 14:11
 *
 * @author Ernst Bunders
 */
public class DataErrorException extends RuntimeException {
    private DataError dataError;

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

    public DataError getDataError() {
        return dataError;
    }
}
