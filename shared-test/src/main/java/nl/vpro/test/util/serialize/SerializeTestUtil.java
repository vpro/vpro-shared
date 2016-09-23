/**
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.test.util.serialize;

import java.io.*;


/**
 *
 * @author Michiel Meeuwissen
 * @since 0.50
 */
public class SerializeTestUtil {

    public static <T> byte[] serialize(T object) throws IOException {

        ByteArrayOutputStream bytes  = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bytes);
        out.writeObject(object);
        out.close();
        return bytes.toByteArray();

    }

    @SuppressWarnings("unchecked")
    public static <T> T deserialize(byte[] bytes, Class<T> clazz) throws IOException {
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
        T result;
        try {
            result = (T) in.readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        in.close();
        return result;
    }

    public static  <T> T roundTrip(T input) throws IOException {
        return (T) deserialize(serialize(input), input.getClass());
    }


}
