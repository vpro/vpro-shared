/**
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.hibernate;

import org.hibernate.search.bridge.StringBridge;

public class LowerCaseBridge implements StringBridge {

    public static String parse(String input) {
        return input.toLowerCase();
    }

    @Override
    public String objectToString(Object object) {
        return parse((String)object);
    }
}
