package nl.vpro.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 1.68
 */
public class BatchedReceiverSpliteratorTest {


	@Test
	public void test() {
		final List<String> result = new ArrayList<>();
		for (int i = 0; i < 23; i++) {
			result.add("a" + i);
		}
		BatchedReceiverSpliterator<String> i =
				BatchedReceiverSpliterator.<String>builder()._build().<String>builder()
				.batchGetter((offset, max) ->
						result.subList(
								Math.min(offset.intValue(), result.size()),
								Math.min(offset.intValue() + max, result.size())).iterator())
				.batchSize(6)
				.build();

		assertThat(i).asList().containsExactly(result.toArray(new String[0]));

	}


	@Test
	public void testWithOffset() {
		final List<String> result = new ArrayList<>();
		for (int i = 0; i < 23; i++) {
			result.add("a" + i);
		}
		BatchedReceiverSpliterator<String> i =
			BatchedReceiverSpliterator.<String>builder()
				.batchGetter((offset, max) ->
					result.subList(
						Math.min(offset.intValue(), result.size()),
						Math.min(offset.intValue() + max, result.size())).iterator())
				.batchSize(6)
                .offset(10L)
				.build();

		assertThat(i)
			.asList().containsExactly(result.subList(10, result.size()).toArray(new String[result.size() -10]));

	}



	@Test
	public void testWithoutBatch() {
		final List<String> result = new ArrayList<>();
		for (int i = 0; i < 23; i++) {
			result.add("a" + i);
		}
		AtomicInteger offset = new AtomicInteger(0);
		BatchedReceiverSpliterator<String> i =
			BatchedReceiverSpliterator.<String>builder()
				.batchGetter(() -> {
					int of = offset.getAndAdd(5);
					return result.subList(
						Math.min(of, result.size()),
						Math.min(of + 5, result.size())).iterator();
				})
				.build();

		//assertThat(i).extracting().containsExactly(result.toArray(new String[0]));

	}

}
