package nl.vpro.jackson2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author Michiel Meeuwissen
 * @since 2.9
 */
public class Utils {


	@SuppressWarnings("unchecked")
	public static MapDifference<String, Object> flattenedDifference(
			JsonNode j1, JsonNode j2)  {
		ObjectMapper mapper = Jackson2Mapper.getPublisherInstance();
		Map<String, Object> map1 = mapper.convertValue(j1, Map.class);
		Map<String, Object> flatten1= flatten(map1);
		Map<String, Object> map2 = mapper.convertValue(j2, Map.class);
		Map<String, Object> flatten2 = flatten(map2);

		return Maps.difference(flatten1, flatten2);
	}

	static Map<String, Object> flatten(Map<String, Object> map) {
        return map.entrySet().stream()
                .flatMap(Utils::flatten)
                .collect(LinkedHashMap::new, (m, e) -> m.put("/" + e.getKey(), e.getValue()), LinkedHashMap::putAll);
    }

    private static Stream<Map.Entry<String, Object>> flatten(Map.Entry<String, Object> entry) {

        if (entry == null) {
            return Stream.empty();
        }

        if (entry.getValue() instanceof Map<?, ?>) {
            return ((Map<?, ?>) entry.getValue()).entrySet().stream()
                    .flatMap(e -> flatten(new AbstractMap.SimpleEntry<>(entry.getKey() + "/" + e.getKey(), e.getValue())));
        }
        if (entry.getValue() instanceof Integer) {
            entry.setValue(((Integer) entry.getValue()).longValue());
        }

        if (entry.getValue() instanceof List<?>) {
            List<?> list = (List<?>) entry.getValue();
            return IntStream.range(0, list.size())
                    .mapToObj(i -> new AbstractMap.SimpleEntry<String, Object>(entry.getKey() + "/" + i, list.get(i)))
                    .flatMap(Utils::flatten);
        }

        return Stream.of(entry);
    }

}
