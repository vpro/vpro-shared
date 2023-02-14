/*
 * Copyright (C) 2010 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.hibernate.search6;

import java.util.Map;

import org.hibernate.search.bridge.ParameterizedBridge;
import org.hibernate.search.bridge.TwoWayStringBridge;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;


/**
 * @since 3.5
 */
public class EnumToLowerCaseBridge implements ValueBridge<Enum<?>, String> {

    private Class enumClazz;

    @Override
    public Object stringToObject(String stringValue) {
        if (stringValue == null) {
            return null;
        }
        return Enum.valueOf(enumClazz, stringValue.toUpperCase());
    }

    @Override
    public String objectToString(Object object) {
        if (object == null) {
            return null;
        }
        return String.valueOf(object).toLowerCase();
    }

    @Override
    public void setParameterValues(Map<String, String> parameters) {
        try {
            String className = parameters.get("class");
            enumClazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
