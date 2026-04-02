package nl.vpro.jackson3;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.exc.MismatchedInputException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("DataFlowIssue")
@Slf4j
public class JsonArrayIteratorTest {


    @Test
    public void simple() {
        try (JsonArrayIterator<Change> iterator = JsonArrayIterator.builder(Change.class)
            .objectMapper(Jackson3Mapper.INSTANCE.withSourceInLocation())
            .inputStream(
                new ByteArrayInputStream("""
            {
                "size": 1,
                "changes": [
                {
                    "sequence": 724,
                    "revision": 2,
                    "mid": "POMS_NCRV_1138990",
                    "deleted": true
                }
                ]
            }
            """.getBytes(StandardCharsets.UTF_8))).build()) {

            assertThat(iterator.getProperty()).isEqualTo("changes");
            assertThat(iterator.hasNext()).isTrue();
            assertThat(iterator.getSize()).hasValue(1L);
            Change change = iterator.next();
            log.info("{}", change);
            assertThat(iterator.hasNext()).isFalse();
        }
    }

    @Test
    public void specifyField() {
        try (JsonArrayIterator<JsonNode> iterator = JsonArrayIterator.builder(JsonNode.class)
            .property("array2")
            .inputStream(
                new ByteArrayInputStream("""
            {
                "size": 3,
                "array1": [ "a", "b" ],
                "array2": [ "c", "d", "e" ]
            }
            """.getBytes(StandardCharsets.UTF_8))).build()) {

            assertThat(iterator.hasNext()).isTrue();
            assertThat(iterator.getSize()).hasValue(3L);
            JsonNode change = iterator.next();
            log.info("{}", change);
        }
    }

    @Test
    public void changes() {
        String[] mids = new String[] {
            "POMS_NCRV_1138990",
            null,
            null,
            "POMS_AVRO_1138559"
        };
        //Jackson2Mapper.getInstance().writeValue(System.out, new Change("bla", false));
        try (JsonArrayIterator<Change> it = JsonArrayIterator.<Change>builder()
            .inputStream(getClass().getResourceAsStream("/changes.json"))
            .valueClass(Change.class)
            .objectMapper(Jackson3Mapper.INSTANCE.withSourceInLocation())
            .build()) {
            assertThat(it.next().getMid()).isEqualTo(
                mids[0]
            ); // 1
            assertThat(it.getCount()).isEqualTo(1);
            assertThat(it.getSize()).hasValueSatisfying(size -> assertThat(size).isEqualTo(14));
            for (int i = 0; i < 9; i++) {
                assertThat(it.hasNext()).isTrue();

                Change change = it.next(); // 10
                Optional<Long> size = it.getSize();
                size.ifPresent(aLong ->
                    log.info("{}/{} :{}", it.getCount(), aLong, change)
                );
                if (!change.isDeleted()) {
                    assertThat(change.getMedia()).isNotNull();
                    assertThat(change.getMedia()).isInstanceOf(Map.class);
                }
            }
            assertThat(it.hasNext()).isTrue(); // 11
            assertThat(it.next().getMid()).isEqualTo("POMS_VPRO_1139788");
            assertThat(it.hasNext()).isFalse();
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testEmpty() throws IOException {
        try (JsonArrayIterator<Change> it = new JsonArrayIterator<>(new ByteArrayInputStream("{\"array\":[]}".getBytes()), Change.class)) {
            assertThat(it.hasNext()).isFalse();
            assertThat(it.hasNext()).isFalse();
            assertThat(it.getCount()).isEqualTo(0);
            assertThatThrownBy(it::next).isInstanceOf(NoSuchElementException.class);
        }
    }

    @Test
    public void testNulls() throws IOException {
        JsonArrayIterator<Change> it = new JsonArrayIterator<>(new ByteArrayInputStream("{\"array\":[null, {}, null, {}]}".getBytes()), Change.class);
        assertThat(it.hasNext()).isTrue();
        it.next();
        assertThat(it.peek()).isEqualTo(new Change());
        assertThat(it.getCount()).isEqualTo(2);
        assertThat(it.hasNext()).isTrue();
        it.next();
        assertThat(it.getCount()).isEqualTo(4);
        assertThat(it.hasNext()).isFalse();
        assertThat(it.getCount()).isEqualTo(4);
        assertThatThrownBy(it::next).isInstanceOf(NoSuchElementException.class);

    }

    @Test
    public void testIncompleteJson() {
        InputStream input = getClass().getResourceAsStream("/incomplete_changes.json");
        assert input != null;
        try (JsonArrayIterator<Change> it = JsonArrayIterator.<Change>builder()
            .inputStream(input)
            .valueClass(Change.class)
            .build()) {

            assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> {
                while (it.hasNext()) it.next();
            });
            assertThat(it.hasNext()).isFalse();
            assertThatThrownBy(it::next).isInstanceOf(NoSuchElementException.class);
        }
    }


    @Test
    public void testZeroBytes() throws IOException {

        JsonArrayIterator<Change> it = new JsonArrayIterator<>(new ByteArrayInputStream(new byte[0]), Change.class);

        assertThat(it.hasNext()).isFalse();
        assertThatThrownBy(it::next).isInstanceOf(NoSuchElementException.class);
        it.close();
    }

    @Test
    public void callback() throws IOException {
        Runnable callback = mock(Runnable.class);
        JsonArrayIterator<Change> it = new JsonArrayIterator<>(getClass().getResourceAsStream("/changes.json"), Change.class, callback);
        while (it.hasNext()) {
            verify(callback, times(0)).run();
            it.next();
        }
        assertThat(it.getSize()).hasValueSatisfying(size -> assertThat(size).isEqualTo(it.getCount()));
        verify(callback, times(1)).run();
    }

    @Test
    public void write() throws IOException, JSONException {
        try (JsonArrayIterator<Change> it = JsonArrayIterator
            .<Change>builder()
            .inputStream(getClass().getResourceAsStream("/changes.json"))
            .valueClass(Change.class)
            .objectMapper(Jackson3Mapper.INSTANCE)
            .skipNulls(false)
            .skipErrors(true)
            .build();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            it.write(out, (c) -> {
                log.info("{}", c);
            });
            String expected = "{'array': " + IOUtils.resourceToString("/array_from_changes.json", StandardCharsets.UTF_8) + "}";
            JSONAssert.assertEquals(expected, out.toString(), JSONCompareMode.LENIENT);
        }
    }

    @Test
    public void writeArray() throws IOException, JSONException {
        try (JsonArrayIterator<Change> it = JsonArrayIterator
            .<Change>builder()
            .inputStream(getClass().getResourceAsStream("/changes.json"))
            .valueClass(Change.class)
            .objectMapper(Jackson3Mapper.INSTANCE)
            .logger(log)
            .skipNulls(false)
            .build();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            it.writeArray(out, (c) ->
                log.info("{}", c)
            );
            String expected = IOUtils.resourceToString("/array_from_changes.json", StandardCharsets.UTF_8);
            JSONAssert.assertEquals(expected, out.toString(), JSONCompareMode.LENIENT);
        }
    }

    @Test
    public void illegalConstruction() {
        assertThatThrownBy(() -> {
            try (JsonArrayIterator<Object> js = JsonArrayIterator.builder().build()) {
                log.info("{}", js);
            }
        }).isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> {
            try (JsonArrayIterator<Change> js = JsonArrayIterator
                .<Change>builder()
                .inputStream(requireNonNull(getClass().getResourceAsStream("/changes.json")))
                .valueClass(Change.class)
                .valueCreator((jp, on) -> null)
                .build()) {
                log.info("{}", js);

            }
        }).isInstanceOf(IllegalArgumentException.class);
    }


    @Test
    public void interrupt() {
        byte[] bytes = "[{},{},{},{},{},{}]".getBytes(StandardCharsets.UTF_8);
        final String[] callback = new String[1];
        try (JsonArrayIterator<Simple> i = JsonArrayIterator.<Simple>builder()
            .inputStream(new InputStream() {
                int i = 0;
                @Override
                public int read() throws IOException {
                    if (i == 6) {
                        throw new InterruptedIOException("INterrupted at 6 bytes, so that would have complete 2 objects");
                    }
                    if (i < bytes.length) {
                        return bytes[i++];
                    } else {
                        return -1;
                    }
                }
            })
            .callback(() ->
                callback[0] = "called"
            )
            .valueClass(Simple.class)
            .build()) {
            log.info("{}", i.next());
            log.info("{}", i.next());
            assertThatThrownBy(i::next)
                .isInstanceOf(RuntimeException.class);
            assertThat(callback[0]).isEqualTo("called");
        }
    }
    @Test
    public void eventsOnObject() {
        byte[] bytes = """
            {
              "array1": [],
              "array2": [1, 2, 3],
              "array3": [4, 5, 6],
              "foo": "bar"
            }
            """.getBytes(StandardCharsets.UTF_8);
        List<JsonArrayIterator<Integer>.Event> events = new ArrayList<>();
        try (JsonArrayIterator<Integer> i = JsonArrayIterator.<Integer>builder()
            .valueCreator((jp, on) -> on.intValue())
            .eventListener(events::add)
            .property("array2")
            .inputStream(new ByteArrayInputStream(bytes))
            .build()) {
            assertThat(i.next()).isEqualTo(1);
            assertThat(i.next()).isEqualTo(2);
            assertThat(i.next()).isEqualTo(3);
            assertThat(i.hasNext()).isFalse();

        }
        for (JsonArrayIterator<Integer>.Event event : events) {
            log.info("{}", event);
        }

    }
    @Test
    public void eventsOnArray() {
        byte[] bytes = """
            [
            {},{},{},
            {"integerValue": 'x'},
            {'value': 'x'},{}
            ]
            """.getBytes(StandardCharsets.UTF_8);
        List<JsonArrayIterator<Simple>.Event> events = new ArrayList<>();
        try (JsonArrayIterator<Simple> i = JsonArrayIterator.<Simple>builder()
            .eventListener(events::add)
            .inputStream(new ByteArrayInputStream(bytes))
            .valueClass(Simple.class)
            .build()) {
            i.forEachRemaining(s -> log.info("{}", s));
            log.info("event" + events);
            assertThat(events).hasSize(10);
            List<JsonArrayIterator<Simple>.Event> errors = events.stream().filter(e -> e instanceof JsonArrayIterator.ValueReadExceptionEvent).toList();
            assertThat(errors).hasSize(1);
            JsonArrayIterator<Simple>.ValueReadExceptionEvent event = (JsonArrayIterator<Simple>.ValueReadExceptionEvent) errors.get(0);
            MismatchedInputException e = event.getException();
            assertThat(e.getMessage()).contains("Cannot deserialize value of type `int` from String \"x\"");

            JsonNode node = event.getJson();
            assertThat(node.toString()).isEqualTo("""
                {"integerValue":"x"}""");

        }
    }


    @Test
    public void exceptionHandler() {
        byte[] bytes = """
            [
            {},{},{},
            ["array"],
            {"integerValue": 'x'},
            {'value': 'x'},
            {}

            ]
            """.getBytes(StandardCharsets.UTF_8);
        List<JsonArrayIterator<Simple>.ValueReadExceptionEvent> errors = new ArrayList<>();
        try (JsonArrayIterator<Simple> i = JsonArrayIterator.<Simple>builder()
            .eventListener(new JsonArrayIterator.ExceptionListener<>() {
                @Override
                public void accept(JsonArrayIterator<Simple>.ValueReadExceptionEvent s) {
                    errors.add(s);
                }
            })
            .inputStream(new ByteArrayInputStream(bytes))
            .skipErrors(false)
            .valueClass(Simple.class)
            .build()) {
            i.forEachRemaining(s -> log.info("{}", s));
            assertThat(errors).hasSize(2);
            {
                JsonArrayIterator<Simple>.ValueReadExceptionEvent event = errors.get(1);
                MismatchedInputException e = event.getException();
                assertThat(e.getMessage()).contains("Cannot deserialize value of type `int` from String \"x\"");
            }
            {
                JsonArrayIterator<Simple>.ValueReadExceptionEvent event = errors.get(0);
                MismatchedInputException e = event.getException();
                assertThat(e.getMessage()).contains("Cannot deserialize value of type `nl.vpro.jackson3.JsonArrayIteratorTest$Simple` from Array value (token `JsonToken.START_ARRAY`)");
            }
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @Getter
    @Setter
    @Data
    private static class Simple {
        private String value;

        private int integerValue;
    }



    @XmlAccessorType(XmlAccessType.FIELD)
    @Getter
    @Setter
    @EqualsAndHashCode
    static class Change {

        private String mid;
        private boolean deleted;
        private Object media;

        public Change() {

        }
        public Change(String mid, boolean deleted) {
            this.mid = mid;
            this.deleted = deleted;
        }

        @Override
        public String toString() {
            return mid;
        }
    }
}
