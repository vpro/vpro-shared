package nl.vpro.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 1.68
 */
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

		assertThat(i).containsExactly(result.toArray(new String[result.size()]));

	}

}
