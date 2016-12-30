package nl.vpro.util;

import java.util.Arrays;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class CallbackIteratorTest {


    @Test
    public void test() {
        Runnable runnable = mock(Runnable.class);
        CallbackIterator<String> i = new CallbackIterator<>(Arrays.asList("A", "B").iterator(), runnable);
        verifyZeroInteractions(runnable);
        i.next();
        verifyZeroInteractions(runnable);
        i.next();
        verify(runnable).run();
        assertThat(i.hasNext()).isFalse();
    }



}
