package nl.vpro.hibernate;

import java.io.Serializable;
import java.sql.*;

import org.hibernate.HibernateException;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.UserType;

/**
 *
 * @author Michiel Meeuwissen
 */
public class FalseToNullType implements UserType<Boolean> {

    public static final FalseToNullType INSTANCE = new FalseToNullType();


    public FalseToNullType() {
        super();
    }


    @Override
    public int getSqlType() {
        return Types.BOOLEAN;
    }

    @Override
    public Class<Boolean> returnedClass() {
        return Boolean.class;
    }

    @Override
    public boolean equals(Boolean x, Boolean y) throws HibernateException {
        if(x == null) {
            x = Boolean.FALSE;
        }
        if(y == null) {
            y = Boolean.FALSE;
        }
        return x.equals(y);

    }

    @Override
    public int hashCode(Boolean x) throws HibernateException {
        return 0;
    }

    @Override
    public Boolean nullSafeGet(ResultSet rs, int position, WrapperOptions wrapperOptions) throws SQLException {
        Boolean result = rs.getBoolean(position);
        if (!result) return null;
        return result;
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Boolean value, int index, WrapperOptions wrapperOptions) throws HibernateException, SQLException {
        if (value == null) {
            st.setBoolean(index, false);
        } else {
            st.setBoolean(index, (Boolean) value);
        }
    }

    @Override
    public Boolean deepCopy(Boolean value) throws HibernateException {
        return value;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(Boolean value) throws HibernateException {
        return value;
    }

    @Override
    public Boolean assemble(Serializable cached, Object owner) {
        return (Boolean) cached;
    }


    @Override
    public Boolean replace(Boolean original, Boolean target, Object owner) throws HibernateException {
        return original;
    }
}
