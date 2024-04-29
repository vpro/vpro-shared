package nl.vpro.hibernate;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.*;
import java.time.Instant;

/**
 * @author Michiel Meeuwissen
 * @since 0.36
 */
public class InstantToTimestampType implements UserType<Instant> {

    public static final InstantToTimestampType INSTANCE = new InstantToTimestampType();


    public InstantToTimestampType() {
        super();
    }


    @Override
    public int getSqlType() {
        return Types.TIMESTAMP;
    }

    @Override
    public Class<Instant> returnedClass() {
        return Instant.class;
    }

    @Override
    public boolean equals(Instant x, Instant y) throws HibernateException {
        if (x == null) {
            return y == null;
        }
        return x.equals(y);

    }

    @Override
    public int hashCode(Instant x) throws HibernateException {
        if (x == null) {
            return 0;
        }
        return x.hashCode();
    }

    @Override
    public Instant nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) throws SQLException {
        Timestamp ts = rs.getTimestamp(position);
        if (ts == null) {
            return null;
        }
        return ts.toInstant();
    }


    @Override
    public void nullSafeSet(PreparedStatement st, Instant value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
        if (value == null) {
            st.setNull(index, Types.TIMESTAMP);
        } else {
            st.setTimestamp(index, Timestamp.from((Instant) value));
        }
    }

    @Override
    public Instant deepCopy(Instant value) throws HibernateException {
        return value;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(Instant value) throws HibernateException {
        return value;
    }

    @Override
    public Instant assemble(Serializable cached, Object owner) throws HibernateException {
        return (Instant) cached;
    }

    @Override
    public Instant replace(Instant original, Instant target, Object owner) throws HibernateException {
        return original;
    }
}

