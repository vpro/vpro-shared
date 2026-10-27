package nl.vpro.util;

import java.util.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class HeadAdderTest {

    @Test
    void addTo() {
        Iterator<String> i = Arrays.asList("a", "b").iterator();
        org.meeuw.collections.HeadAdder<String> adder = HeadAdder.<String>builder().wrapped(i).adder((s) -> s + "c").build();
        assertEquals("ac", adder.next());
        assertEquals("a", adder.next());
        assertEquals("b", adder.next());
        assertEquals(false, adder.hasNext());
    }

    @Test
    void onlyIfEmptyOnNotEmpty() {
        Iterator<String> i = Arrays.asList("a", "b").iterator();
        org.meeuw.collections.HeadAdder<String> adder = HeadAdder.<String>builder().wrapped(i).adder((s) -> s + "c").onlyIfEmpty(true).build();
        assertEquals("a", adder.next());
        assertEquals("b", adder.next());
        assertEquals(false, adder.hasNext());
    }


    @Test
    void onlyIfEmptyOnEmpty() {
        Iterator<String> i = Collections.<String>emptyList().iterator();
        org.meeuw.collections.HeadAdder<String> adder = HeadAdder.<String>builder().wrapped(i).adder((s) -> s + "c").onlyIfEmpty(true).build();
        assertEquals("nullc", adder.next());
        assertEquals(false, adder.hasNext());
    }


    @Test
    void onlyIfNotEmptyOnNotEmpty() {
        Iterator<String> i = Arrays.asList("a", "b").iterator();
        org.meeuw.collections.HeadAdder<String> adder = HeadAdder.<String>builder().wrapped(i).adder((s) -> s + "c").onlyIfNotEmpty(true).build();
        assertEquals("ac", adder.next());
        assertEquals("a", adder.next());
        assertEquals("b", adder.next());

    }


    @Test
    void onlyIfNotEmptyOnEmpty() {
        Iterator<String> i = Collections.<String>emptyList().iterator();
        org.meeuw.collections.HeadAdder<String> adder = HeadAdder.<String>builder().wrapped(i).adder((s) -> s + "c").onlyIfNotEmpty(true).build();
        assertEquals(false, adder.hasNext());
    }

    @Test
    void headNull() {
        Iterator<String> i = Collections.<String>emptyList().iterator();
        org.meeuw.collections.HeadAdder<String> adder = HeadAdder.<String>builder().wrapped(i).adder((s) -> null).build();

        assertEquals(null, adder.next());
        assertEquals(false, adder.hasNext());
    }


    @Test
    void headException() {
        Iterator<String> i = Collections.<String>emptyList().iterator();
        org.meeuw.collections.HeadAdder<String> adder = HeadAdder.<String>builder().wrapped(i).adder((s) ->{
            throw new RuntimeException();
        }).build();

        assertEquals(false, adder.hasNext());
    }
}
