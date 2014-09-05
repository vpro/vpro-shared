package nl.vpro.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.Callable;

import org.junit.Test;

import static org.junit.Assert.*;

public class TailAdderTest {

    @Test
    public void addTo() {
        Iterator<String> i = Arrays.asList("a", "b").iterator();
        TailAdder<String> adder = new TailAdder<>(i, new Callable<String>() {
            @Override
            public String call() throws Exception {
                return "c";
            }
        });
        assertEquals("a", adder.next());
        assertEquals("b", adder.next());
        assertEquals("c", adder.next());
        assertEquals(false, adder.hasNext());
    }

    @Test
    public void onlyIfEmptyOnNotEmpty() {
        Iterator<String> i = Arrays.asList("a", "b").iterator();
        TailAdder<String> adder = new TailAdder<>(i, true, new Callable<String>() {
            @Override
            public String call() throws Exception {
                return "c";
            }
        });
        assertEquals("a", adder.next());
        assertEquals("b", adder.next());
        assertEquals(false, adder.hasNext());
    }


    @Test
    public void onlyIfEmptyOnEmpty() {
        Iterator<String> i = Collections.<String>emptyList().iterator();
        TailAdder<String> adder = new TailAdder<>(i, true, new Callable<String>() {
            @Override
            public String call() throws Exception {
                return "c";
            }
        });
        assertEquals("c", adder.next());
        assertEquals(false, adder.hasNext());
    }

}
