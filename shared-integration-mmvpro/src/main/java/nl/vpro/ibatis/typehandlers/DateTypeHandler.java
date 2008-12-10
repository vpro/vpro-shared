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
public class DateTypeHandler implements TypeHandlerCallback {

    public Object getResult(ResultGetter getter) throws SQLException {
        return secondsToDate(getter.getLong());
    }

    public void setParameter(ParameterSetter setter, Object date)
            throws SQLException {
        if (date instanceof Date) {
            if (!(date == null)) {
                setter.setLong(Math.round(((Date)date).getTime() / 1000));
            } else {
                setter.setLong(-1);
            }
        } else {
            throw new SQLException("Expected java.util.Date instead of "
                    + date.getClass().getName());
        }
    }

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
