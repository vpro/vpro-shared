package nl.vpro.util;

import java.util.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TailAdderTest {

    @Test
    public void addTo() {
        Iterator<String> i = Arrays.asList("a", "b").iterator();
        TailAdder<String> adder = TailAdder.<String>builder().wrapped(i).adder((s) -> "c").build();
        assertEquals("a", adder.next());
        assertEquals(1, adder.getCount());
        assertEquals("b", adder.next());
        assertEquals(2, adder.getCount());
        assertEquals("c", adder.next());
        assertEquals(3, adder.getCount());
        assertFalse(adder.hasNext());
    }

    @Test
    public void onlyIfEmptyOnNotEmpty() {
        Iterator<String> i = Arrays.asList("a", "b").iterator();
        TailAdder<String> adder = new TailAdder<>(i, true, () -> "c");
        assertEquals("a", adder.next());
        assertEquals("b", adder.next());
        assertFalse(adder.hasNext());
    }


    @Test
    public void onlyIfEmptyOnEmpty() {
        Iterator<String> i = Collections.<String>emptyList().iterator();
        TailAdder<String> adder = new TailAdder<>(i, true, () -> "c");
        assertEquals("c", adder.next());
        assertFalse(adder.hasNext());
    }


    @Test
    public void onlyIfNotEmptyOnNotEmpty() {
        Iterator<String> i = Arrays.asList("a", "b").iterator();
        TailAdder<String> adder = TailAdder.<String>builder().wrapped(i).onlyIfNotEmpty(true).adder((s) -> "c").build();
        assertEquals("a", adder.next());
        assertEquals("b", adder.next());
        assertTrue(adder.hasNext());
        assertEquals("c", adder.next());

    }


    @Test
    public void onlyIfNotEmptyOnEmpty() {
        Iterator<String> i = Collections.<String>emptyList().iterator();
        TailAdder<String> adder = TailAdder.<String>builder().wrapped(i).onlyIfNotEmpty(true).adder((s) -> "c").build();
        assertFalse(adder.hasNext());
    }

    @Test
    public void tailNull() {
        Iterator<String> i = Collections.<String>emptyList().iterator();
        TailAdder<String> adder = new TailAdder<>(i, () -> null);
        assertNull(adder.next());
        assertFalse(adder.hasNext());
    }


    @Test
    public void tailException() {
        Iterator<String> i = Collections.<String>emptyList().iterator();
        TailAdder<String> adder = new TailAdder<>(i, () -> {
            throw new Exception();
        });
        assertFalse(adder.hasNext());
    }
}
