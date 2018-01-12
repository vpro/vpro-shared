package nl.vpro.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TailAdderTest {

    @Test
    public void addTo() {
        Iterator<String> i = Arrays.asList("a", "b").iterator();
        TailAdder<String> adder = new TailAdder<>(i, () -> "c");
        assertEquals("a", adder.next());
        assertEquals("b", adder.next());
        assertEquals("c", adder.next());
        assertEquals(false, adder.hasNext());
    }

    @Test
    public void onlyIfEmptyOnNotEmpty() {
        Iterator<String> i = Arrays.asList("a", "b").iterator();
        TailAdder<String> adder = new TailAdder<>(i, true, () -> "c");
        assertEquals("a", adder.next());
        assertEquals("b", adder.next());
        assertEquals(false, adder.hasNext());
    }


    @Test
    public void onlyIfEmptyOnEmpty() {
        Iterator<String> i = Collections.<String>emptyList().iterator();
        TailAdder<String> adder = new TailAdder<>(i, true, () -> "c");
        assertEquals("c", adder.next());
        assertEquals(false, adder.hasNext());
    }


    @Test
    public void onlyIfNotEmptyOnNotEmpty() {
        Iterator<String> i = Arrays.asList("a", "b").iterator();
        TailAdder<String> adder = TailAdder.<String>builder().wrapped(i).onlyIfNotEmpty(true).adder((s) -> "c").build();
        assertEquals("a", adder.next());
        assertEquals("b", adder.next());
        assertEquals(true, adder.hasNext());
        assertEquals("c", adder.next());

    }


    @Test
    public void onlyIfNotEmptyOnEmpty() {
        Iterator<String> i = Collections.<String>emptyList().iterator();
        TailAdder<String> adder = TailAdder.<String>builder().wrapped(i).onlyIfNotEmpty(true).adder((s) -> "c").build();
        assertEquals(false, adder.hasNext());
    }

    @Test
    public void tailNull() {
        Iterator<String> i = Collections.<String>emptyList().iterator();
        TailAdder<String> adder = new TailAdder<>(i, () -> null);
        assertEquals(null, adder.next());
        assertEquals(false, adder.hasNext());
    }


    @Test
    public void tailException() {
        Iterator<String> i = Collections.<String>emptyList().iterator();
        TailAdder<String> adder = new TailAdder<>(i, () -> {
            throw new Exception();
        });
        assertEquals(false, adder.hasNext());
    }
}
