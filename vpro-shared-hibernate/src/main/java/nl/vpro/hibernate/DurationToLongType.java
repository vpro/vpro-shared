package nl.vpro.hibernate;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.*;
import java.time.Duration;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

/**
 * @author Michiel Meeuwissen
 * @since 0.36
 */
public class DurationToLongType implements UserType<Duration> {

    public static final DurationToLongType INSTANCE = new DurationToLongType();


    public DurationToLongType() {
        super();
    }


    @Override
    public int getSqlType() {
        return Types.BIGINT;
    }

    @Override
    public Class<Duration> returnedClass() {
        return Duration.class;
    }

    @Override
    public boolean equals(Duration x, Duration y) throws HibernateException {
        if (x == null) {
            return y == null;
        }
        return x.equals(y);

    }

    @Override
    public int hashCode(Duration x) throws HibernateException {
        if (x == null) {
            return 0;
        }
        return x.hashCode();
    }

    @Override
    public Duration nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) throws SQLException {
        BigDecimal ts = rs.getBigDecimal(position);
        if (ts == null) {
            return null;
        }
        return Duration.ofMillis(ts.toBigInteger().longValue());
    }


    @Override
    public void nullSafeSet(PreparedStatement st, Duration value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
        if (value == null) {
            st.setNull(index, Types.TIMESTAMP);
        } else {
            st.setBigDecimal(index, BigDecimal.valueOf(value.toMillis()));
        }
    }

    @Override
    public Duration deepCopy(Duration value) throws HibernateException {
        return value;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(Duration value) throws HibernateException {
        return value;
    }

    @Override
    public Duration assemble(Serializable cached, Object owner) throws HibernateException {
        return (Duration) cached;
    }

    @Override
    public Duration replace(Duration original, Duration target, Object owner) throws HibernateException {
        return original;
    }
}

