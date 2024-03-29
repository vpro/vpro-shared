package nl.vpro.jackson2;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.Range;

import static org.assertj.core.api.Assertions.assertThat;

class GuavaRangeModuleTest {

    ObjectMapper mapper = new ObjectMapper();
    {
        mapper.registerModule(new DateModule());
        mapper.registerModule(new GuavaRangeModule());
    }


    static class WithoutSerializer {
        @JsonProperty
        Range<Integer> range;
        @JsonProperty
        int anotherField = 1;

    }
    static class WithIntegerRange {
        @JsonSerialize(using = GuavaRangeModule.Serializer.class) Range<Integer> range;
    }

    static class WithInstantRange {
        @JsonSerialize(using = GuavaRangeModule.Serializer.class)
        Range<Instant> range;
    }


    @Test
    public void without() throws JsonProcessingException {
        WithoutSerializer a = new WithoutSerializer();
        a.range = Range.closedOpen(1, 2);
        String example = "{\"range\":{\"lowerEndpoint\":1,\"lowerBoundType\":\"CLOSED\",\"upperEndpoint\":2,\"upperBoundType\":\"OPEN\",\"type\":\"java.lang.Integer\"},\"anotherField\":1}";
        assertThat(mapper.writeValueAsString(a)).isEqualTo(example);

        WithoutSerializer ab = mapper.readValue(example, WithoutSerializer.class);
        assertThat(ab.range).isEqualTo(a.range);

    }


    @Test
    public void empty() throws JsonProcessingException {
        WithIntegerRange a = new WithIntegerRange();
        assertThat(mapper.writeValueAsString(a)).isEqualTo("{\"range\":null}");

    }

    @Test
    public void filled() throws JsonProcessingException {
        WithIntegerRange a = new WithIntegerRange();
        a.range = Range.closedOpen(1, 10);

        assertThat(mapper.writeValueAsString(a)).isEqualTo("{\"range\":{\"lowerEndpoint\":1,\"lowerBoundType\":\"CLOSED\",\"upperEndpoint\":10,\"upperBoundType\":\"OPEN\",\"type\":\"java.lang.Integer\"}}");

    }

    @Test
    public void instant() throws JsonProcessingException {
        WithInstantRange a = new WithInstantRange();
        a.range = Range.closedOpen(
            Instant.parse("2021-12-24T10:00:00Z"),
            Instant.parse("2021-12-25T10:00:00Z")
        );

        assertThat(mapper.writeValueAsString(a)).isEqualTo(
            "{\"range\":{\"lowerEndpoint\":1640340000000,\"lowerBoundType\":\"CLOSED\",\"upperEndpoint\":1640426400000,\"upperBoundType\":\"OPEN\",\"type\":\"java.time.Instant\"}}");

    }

}
