package nl.vpro.util;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class CallbackIteratorTest {


    @Test
    public void test() {
        Runnable runnable = mock(Runnable.class);
        CallbackIterator<String> i = new CallbackIterator<>(Arrays.asList("A", "B").iterator(), runnable);
        verifyNoInteractions(runnable);
        i.next();
        verifyNoInteractions(runnable);
        i.next();
        verify(runnable).run();
        assertThat(i.hasNext()).isFalse();
    }



}
