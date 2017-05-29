package nl.vpro.hibernate;

import java.io.Serializable;
import java.sql.*;
import java.time.Instant;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.UserType;

/**
 * @author Michiel Meeuwissen
 * @since 0.36
 */
public class InstantToTimestampType implements UserType {

    public static final InstantToTimestampType INSTANCE = new InstantToTimestampType();


    public InstantToTimestampType() {
        super();
    }

    @Override
    public int[] sqlTypes() {
        return new int[]{Types.TIMESTAMP};
    }

    @Override
    public Class returnedClass() {
        return Instant.class;
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        if (x == null) {
            return y == null;
        }
        return x.equals(y);

    }

    @Override
    public int hashCode(Object x) throws HibernateException {
        if (x == null) {
            return 0;
        }
        return x.hashCode();
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner) throws HibernateException, SQLException {
        Timestamp ts = rs.getTimestamp(names[0]);
        if (ts == null) {
            return null;
        }
        return ts.toInstant();
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {
        if (value == null) {
            st.setNull(index, Types.TIMESTAMP);
        } else {
            st.setTimestamp(index, Timestamp.from((Instant) value));
        }
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        return value;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        return (Serializable) value;
    }

    @Override
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return cached;
    }

    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }
}

