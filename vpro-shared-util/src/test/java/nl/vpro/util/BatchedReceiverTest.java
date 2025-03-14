package nl.vpro.util;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 1.68
 */
@Log4j2
public class BatchedReceiverTest {


    @Test
    public void test() {
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
                .build();

        assertThat(i).toIterable().containsExactly(result.toArray(new String[0]));

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
    public void testWithoutBatch() {
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
    static class WithToken {
        final List<String> result;
        final Integer token;

        WithToken(List<String> result, Integer token) {
            this.result = result;
            this.token = token;
        }

        static WithToken forToken(Integer token) {
            if (token == null) {
                return new WithToken(List.of("0", "a"), 1);
            } else {
                if (token < 10) {
                    return new WithToken(List.of("a" + token , "b" + token), token + 1);
                } else {
                    return null;
                }

            }
        }
    }



    @Test
    public void testWithTokens() {
        AtomicInteger offset = new AtomicInteger(0);
        BatchedReceiver<String> i =
            BatchedReceiver.<String>builder()
                .initialAndResumption(
                    () -> WithToken.forToken(null),
                    (withToken) -> WithToken.forToken(withToken.token),
                    (withToken) -> withToken.result.iterator())
                .build();

        assertThat(i).toIterable().containsExactly("0", "a", "a1", "b1", "a2", "b2", "a3", "b3", "a4", "b4", "a5", "b5", "a6", "b6", "a7", "b7", "a8", "b8", "a9", "b9");



    }

}
