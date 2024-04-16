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
public abstract class EnumToLowerCaseBridge<E extends Enum<E>> implements ValueBridge<E, String> {

    private Class<E> enumClazz;


    public EnumToLowerCaseBridge(Class<E> enumClazz) {
        this.enumClazz = enumClazz;
    }


    @Override
    public E fromIndexedValue(String stringValue, ValueBridgeFromIndexedValueContext context) {

        if (stringValue == null) {
            return null;
        }
        return Enum.valueOf(enumClazz, stringValue.toUpperCase());
    }

    @Override
    public String toIndexedValue(E object, ValueBridgeToIndexedValueContext valueBridgeToIndexedValueContext) {
        if (object == null) {
            return null;
        }
        return String.valueOf(object).toLowerCase();
    }

    public void setClass(String className) {
        try {
            enumClazz = (Class<E>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
