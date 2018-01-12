package nl.vpro.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HeadAdderTest {

    @Test
    public void addTo() {
        Iterator<String> i = Arrays.asList("a", "b").iterator();
        HeadAdder<String> adder = HeadAdder.<String>builder().wrapped(i).adder((s) -> s + "c").build();
        assertEquals("ac", adder.next());
        assertEquals("a", adder.next());
        assertEquals("b", adder.next());
        assertEquals(false, adder.hasNext());
    }

    @Test
    public void onlyIfEmptyOnNotEmpty() {
        Iterator<String> i = Arrays.asList("a", "b").iterator();
        HeadAdder<String> adder = HeadAdder.<String>builder().wrapped(i).adder((s) -> s + "c").onlyIfEmpty(true).build();
        assertEquals("a", adder.next());
        assertEquals("b", adder.next());
        assertEquals(false, adder.hasNext());
    }


    @Test
    public void onlyIfEmptyOnEmpty() {
        Iterator<String> i = Collections.<String>emptyList().iterator();
        HeadAdder<String> adder = HeadAdder.<String>builder().wrapped(i).adder((s) -> s + "c").onlyIfEmpty(true).build();
        assertEquals("nullc", adder.next());
        assertEquals(false, adder.hasNext());
    }


    @Test
    public void onlyIfNotEmptyOnNotEmpty() {
        Iterator<String> i = Arrays.asList("a", "b").iterator();
        HeadAdder<String> adder = HeadAdder.<String>builder().wrapped(i).adder((s) -> s + "c").onlyIfNotEmpty(true).build();
        assertEquals("ac", adder.next());
        assertEquals("a", adder.next());
        assertEquals("b", adder.next());

    }


    @Test
    public void onlyIfNotEmptyOnEmpty() {
        Iterator<String> i = Collections.<String>emptyList().iterator();
        HeadAdder<String> adder = HeadAdder.<String>builder().wrapped(i).adder((s) -> s + "c").onlyIfNotEmpty(true).build();
        assertEquals(false, adder.hasNext());
    }

    @Test
    public void headNull() {
        Iterator<String> i = Collections.<String>emptyList().iterator();
        HeadAdder<String> adder = HeadAdder.<String>builder().wrapped(i).adder((s) -> null).build();

        assertEquals(null, adder.next());
        assertEquals(false, adder.hasNext());
    }


    @Test
    public void headException() {
        Iterator<String> i = Collections.<String>emptyList().iterator();
        HeadAdder<String> adder = HeadAdder.<String>builder().wrapped(i).adder((s) ->{
            throw new RuntimeException();
        }).build();

        assertEquals(false, adder.hasNext());
    }
}
