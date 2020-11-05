package nl.vpro.util;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class CallbackIteratorTest {


    @Test
    public void test() {
        Runnable runnable = mock(Runnable.class);
        CallbackIterator<String> i = CallbackIterator.<String>builder()
            .wrapped(Arrays.asList("A", "B").iterator())
            .callback(runnable)
            .build()
            ;
        verifyNoInteractions(runnable);
        i.next();
        assertThatThrownBy(i::remove).isInstanceOf(UnsupportedOperationException.class);
        verifyNoInteractions(runnable);
        i.next();
        verify(runnable).run();
        assertThat(i.hasNext()).isFalse();

        assertThatThrownBy(i::getCount).isInstanceOf(UnsupportedOperationException.class);
        assertThat(i.getSize()).isNotPresent();
        assertThat(i.getTotalSize()).isNotPresent();
    }


    @Test
    public void withCounted() {
        Runnable runnable = mock(Runnable.class);
        CallbackIterator<String> i = CallbackIterator.<String>builder()
            .wrapped(BasicWrappedIterator.<String>builder()
                .wrapped(Arrays.asList("A", "B").iterator()).size(2L).build()
            )
            .callback(runnable)
            .build()
            ;
        assertThat(i.getCount()).isEqualTo(0);

        verifyNoInteractions(runnable);
        i.next();
        verifyNoInteractions(runnable);
        i.next();
        verify(runnable).run();
        assertThat(i.hasNext()).isFalse();

        assertThat(i.getSize()).contains(2L);
        assertThat(i.getTotalSize()).contains(2L);
    }



}
