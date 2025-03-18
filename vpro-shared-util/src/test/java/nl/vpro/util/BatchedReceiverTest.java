package nl.vpro.util;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Michiel Meeuwissen
 * @since 1.68
 */
@Log4j2
public class BatchedReceiverTest {

    @Test
    public void illegalConstruction() {
        assertThatThrownBy(() ->
            BatchedReceiver.builder().build()).isInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() ->
            BatchedReceiver.builder()
                .supplier(Optional::empty)
                .batchGetter((offset, max) -> Collections.emptyIterator())
                .build()).isInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() ->
            BatchedReceiver.builder()
                .supplier(Optional::empty)
                .batchSize(100)
                .build()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testWithOffsetBatchGetter() {
        final List<String> result = new ArrayList<>();
        for (int i = 0; i < 23; i++) {
            result.add(String.valueOf((char) ('a' + i)));
        }
        BatchedReceiver<String> i =
            BatchedReceiver.<String>builder()
                .batchGetter((offset, max) ->
                    result.subList(
                        Math.min(offset.intValue(), result.size()),
                        Math.min(offset.intValue() + max, result.size())).iterator())
                .batchSize(6)
                .build();

        assertThat(i).toIterable().containsExactly(result.toArray(new String[0]));

        assertThatThrownBy(i::next).isInstanceOf(NoSuchElementException.class);
    }


    @Test
    public void testWithOffset() {
        final List<String> result = new ArrayList<>();
        for (int i = 0; i < 23; i++) {
            result.add("a" + i);
        }
        BatchedReceiver<String> i =
            BatchedReceiver.<String>builder()
                .batchGetter((offset, max) ->
                    result.subList(
                        Math.min(offset.intValue(), result.size()),
                        Math.min(offset.intValue() + max, result.size())).iterator())
                .batchSize(6)
                .offset(10L)
                .build();

        assertThat(i)
            .toIterable().containsExactly(result.subList(10, result.size()).toArray(new String[result.size() - 10]));

    }


    @Test
    public void testWithoutBatchSize() {
        final List<String> result = new ArrayList<>();
        for (int i = 0; i < 23; i++) {
            result.add("a" + i);
        }
        AtomicInteger offset = new AtomicInteger(0);
        BatchedReceiver<String> i =
            BatchedReceiver.<String>builder()
                .batchGetter(() -> {
                    int of = offset.getAndAdd(5);
                    return result.subList(
                        Math.min(of, result.size()),
                        Math.min(of + 5, result.size())).iterator();
                })
                .build();

        assertThat(i)
            .toIterable().containsExactly(result.toArray(new String[0]));

    }

    @Getter
    static class WithToken implements Iterable<String> {
        final List<String> result;
        final Integer token;

        WithToken(List<String> result, Integer token) {
            this.result = result;
            this.token = token;
        }

        @Override
        public Iterator<String> iterator() {
            return result.iterator();
        }
        static WithToken initial() {
                return new WithToken(List.of("0", "a"), 1);
        }

        static Optional<WithToken> forToken(Integer token) {
            if (token < 10) {
                return Optional.of(new WithToken(List.of("a" + token , "b" + token), token + 1));
            } else {
                return Optional.empty();
            }
        }
    }


    @Test
    public void testWithTokens() {
        BatchedReceiver<String> i =
            BatchedReceiver.<String>builder()
                .initialAndResumption(WithToken::initial,
                    (withToken) -> WithToken.forToken(withToken.token))
                .build();

        assertThat(i).toIterable().containsExactly(
            "0", "a", "a1", "b1", "a2", "b2", "a3", "b3", "a4", "b4", "a5", "b5", "a6", "b6", "a7", "b7", "a8", "b8", "a9", "b9");



    }

     @Test
    public void testWithSupplier() {
         Supplier<Optional<Iterator<String>>> supplier = new Supplier<>() {
             int i = 10;
             @Override
             public Optional<Iterator<String>> get() {
                 if (i-- > 0 ) {
                     if (i % 2 == 0) {
                         return Optional.of(Collections.emptyIterator());
                     }
                     return Optional.of(List.of(String.valueOf((char) ('a' + i)), "x" + i).iterator());
                 } else {
                     return Optional.empty();
                 }
             }
         };
         BatchedReceiver<String> i =
            BatchedReceiver.<String>builder()
                .supplier(supplier)
                .build();

        assertThat(i).toIterable().containsExactly(
            "j", "x9", "h", "x7", "f", "x5", "d", "x3", "b", "x1");
    }

}
