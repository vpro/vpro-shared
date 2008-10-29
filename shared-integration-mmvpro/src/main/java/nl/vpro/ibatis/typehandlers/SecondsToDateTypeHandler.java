/*
 * Copyright (C) 2008 All rights reserved
 * VPRO The Netherlands
 * Creation date 23 okt 2008.
 */
package nl.vpro.ibatis.typehandlers;

import java.sql.SQLException;
import java.util.Date;

import com.ibatis.sqlmap.client.extensions.ParameterSetter;
import com.ibatis.sqlmap.client.extensions.ResultGetter;
import com.ibatis.sqlmap.client.extensions.TypeHandlerCallback;

/**
 * Typehandler that maps MMBase time in seconds to date objects and vice
 * versa. Null values are being mapped to -1 database values.
 *
 * @author roekoe
 *
 */
public class SecondsToDateTypeHandler implements TypeHandlerCallback {

    /*
     * (non-Javadoc)
     *
     * @see
     * com.ibatis.sqlmap.client.extensions.TypeHandlerCallback#getResult(com
     * .ibatis.sqlmap.client.extensions.ResultGetter)
     */
    public Object getResult(ResultGetter getter) throws SQLException {
        return secondsToDate(getter.getLong());
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.ibatis.sqlmap.client.extensions.TypeHandlerCallback#setParameter(
     * com.ibatis.sqlmap.client.extensions.ParameterSetter, java.lang.Object)
     */
    public void setParameter(ParameterSetter setter, Object date)
            throws SQLException {
        if (date instanceof Date) {
            if (!date.equals(null)) {
                setter.setLong(Math.round(((Date)date).getTime() / 1000));
            } else {
                setter.setLong(-1);
            }
        } else {
            throw new SQLException("Expected java.util.Date instead of "
                    + date.getClass().getName());
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.ibatis.sqlmap.client.extensions.TypeHandlerCallback#valueOf(java.
     * lang.String)
     */
    public Object valueOf(String string) {
        return secondsToDate(Long.parseLong(string));
    }

    private Date secondsToDate(long seconds) {
        if (seconds >= 0) {
            return new Date(1000 * seconds);
        } else {
            return null;
        }
    }

}
