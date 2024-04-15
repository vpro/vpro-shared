/*
 * Copyright (C) 2010 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.hibernate.search6;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.common.annotation.Param;


/**
 * @since 3.5
 */
@Param(name = "class", value = "java.lang.String")
public class EnumToLowerCaseBridge implements ValueBridge<Object, String> {

    private Class enumClazz;


    public EnumToLowerCaseBridge(Class<? extends Enum<?>> enumClazz) {
        this.enumClazz = enumClazz;
    }


    @Override
    public Object fromIndexedValue(String stringValue, ValueBridgeFromIndexedValueContext context) {

        if (stringValue == null) {
            return null;
        }
        return Enum.valueOf(enumClazz, stringValue.toUpperCase());
    }

    @Override
    public String toIndexedValue(Object object, ValueBridgeToIndexedValueContext valueBridgeToIndexedValueContext) {
        if (object == null) {
            return null;
        }
        return String.valueOf(object).toLowerCase();
    }

    public void setClass(String className) {
        try {
            enumClazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
