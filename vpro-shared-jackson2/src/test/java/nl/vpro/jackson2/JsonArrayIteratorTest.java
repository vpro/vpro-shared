package nl.vpro.jackson2;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

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
    public void test() throws IOException {

        //Jackson2Mapper.getInstance().writeValue(System.out, new Change("bla", false));
        try (JsonArrayIterator<Change> it = JsonArrayIterator.<Change>builder().inputStream(getClass().getResourceAsStream("/changes.json")).valueClass(Change.class).objectMapper(Jackson2Mapper.getInstance()).build()) {
            assertThat(it.next().getMid()).isEqualTo("POMS_NCRV_1138990"); // 1
            assertThat(it.getCount()).isEqualTo(1);
            assertThat(it.getSize()).hasValueSatisfying(size -> assertThat(size).isEqualTo(14));
            for (int i = 0; i < 9; i++) {
                assertThat(it.hasNext()).isTrue();

                Change change = it.next(); // 10
                Optional<Long> size = it.getSize();
                if (size.isPresent()) {
                    log.info(it.getCount() + "/" + size.get() + " :" + change);
                }
                if (!change.isDeleted()) {
                    assertThat(change.getMedia()).isNotNull();
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
    public void testIncompleteJson() throws IOException {
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


    @SuppressWarnings("FinalizeCalledExplicitly")
    @Test
    public void testZeroBytes() throws IOException {

        JsonArrayIterator<Change> it = new JsonArrayIterator<>(new ByteArrayInputStream(new byte[0]), Change.class);

        assertThat(it.hasNext()).isFalse();
        assertThatThrownBy(it::next).isInstanceOf(NoSuchElementException.class);
        it.finalize();
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
            .objectMapper(Jackson2Mapper.getInstance())
            .skipNulls(false)
            .build();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            it.write(out, (c) -> {
                log.info("{}", c);
            });
            String expected = "{'array': " + IOUtils.resourceToString("/array_from_changes.json", StandardCharsets.UTF_8) + "}";
            JSONAssert.assertEquals(expected, out.toString(), JSONCompareMode.STRICT);
        }
    }

    @Test
    public void writeArray() throws IOException, JSONException {
        try (JsonArrayIterator<Change> it = JsonArrayIterator
            .<Change>builder()
            .inputStream(getClass().getResourceAsStream("/changes.json"))
            .valueClass(Change.class)
            .objectMapper(Jackson2Mapper.getInstance())
            .logger(log)
            .skipNulls(false)
            .build();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            it.writeArray(out, (c) -> {
                log.info("{}", c);
            });
            String expected = IOUtils.resourceToString("/array_from_changes.json", StandardCharsets.UTF_8);
            JSONAssert.assertEquals(expected, out.toString(), JSONCompareMode.STRICT);
        }
    }

    @Test
    public void illegalConstruction() {
        assertThatThrownBy(() -> {
            try (JsonArrayIterator<Object> js = JsonArrayIterator.builder().build()) {
                log.info("{}", js);
            }
        }).isInstanceOf(IllegalArgumentException.class);

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
    public void interrupt() throws IOException {
        byte[] bytes = "[{},{},{},{},{},{}]".getBytes(StandardCharsets.UTF_8);
        final String[] callback = new String[1];
        try (JsonArrayIterator<Simple> i = JsonArrayIterator.<Simple>builder()
            .inputStream(new InputStream() {
                int i = 0;
                @Override
                public int read() throws IOException {
                    if (i == 6) {
                        throw new InterruptedIOException();
                    }
                    if (i < bytes.length) {
                        return bytes[i++];
                    } else {
                        return -1;
                    }
                }
            })
            .callback(() -> {
                callback[0] = "called";
            })
            .valueClass(Simple.class)
            .build()) {
            log.info("{}", i.next());
            log.info("{}", i.next());
            assertThatThrownBy(i::next)
                .isInstanceOf(RuntimeException.class);
            assertThat(callback[0]).isEqualTo("called");
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @Getter
    @Setter
    private static class Simple {
        private String value;
    }



    @XmlAccessorType(XmlAccessType.FIELD)
    @Getter
    @Setter
    @EqualsAndHashCode
    private static class Change {

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
