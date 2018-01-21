/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.test.util.serialize;

import java.io.*;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;

import static org.assertj.core.api.Assertions.assertThat;


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
        try(ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (T) in.readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T roundTrip(T input) throws IOException {
        return (T) deserialize(serialize(input), input.getClass());
    }

    @SuppressWarnings("unchecked")
    public static <T> T roundTripAndEquals(T input) throws IOException {
        T newT = (T) deserialize(serialize(input), input.getClass());
        assertThat(newT).isEqualTo(input);
        return newT;

    }

    public static <T> SerializeTestUtil.ObjectAssert assertThatSerialized(T o) {
        return new SerializeTestUtil.ObjectAssert<>(o);
    }


    public static class ObjectAssert<S extends SerializeTestUtil.ObjectAssert<S, A>, A> extends AbstractObjectAssert<S, A> {

        A rounded;

        protected ObjectAssert(A actual) {
            super(actual, SerializeTestUtil.ObjectAssert.class);
        }



        public AbstractObjectAssert<?, A> andRounded() throws IOException {
            if (rounded == null) {
                rounded = roundTrip(actual);
            }
            return Assertions.assertThat(rounded);
        }

        public A get() throws IOException {
            if (rounded == null) {
                rounded = roundTrip(actual);
            }
            return rounded;
        }

    }

}
