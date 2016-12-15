package nl.vpro.jackson2;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

public class JsonArrayIteratorTest {

    @Test
    public void test() throws IOException {

        //Jackson2Mapper.getInstance().writeValue(System.out, new Change("bla", false));
        JsonArrayIterator<Change> it = new JsonArrayIterator<>(getClass().getResourceAsStream("/changes.json"), Change.class);
        assertThat(it.next().getMid()).isEqualTo("POMS_NCRV_1138990"); // 1
        assertThat(it.getCount()).isEqualTo(1);
        assertThat(it.getSize()).hasValueSatisfying(size -> assertThat(size).isEqualTo(14));
        for (int i = 0; i < 9; i++) {
            assertThat(it.hasNext()).isTrue();

            Change change = it.next(); // 10
            Optional<Long> size = it.getSize();
            if (size.isPresent()) {
                System.out.println(it.getCount() + "/" + size.get() + " :" + change);
            }
            if (!change.isDeleted()) {
                assertThat(change.getMedia()).isNotNull();
            }
        }
        assertThat(it.hasNext()).isTrue(); // 11
        assertThat(it.next().getMid()).isEqualTo("POMS_VPRO_1139788");
        assertThat(it.hasNext()).isFalse();
    }

    @Test
    public void testEmpty() throws IOException {
       JsonArrayIterator<Change> it = new JsonArrayIterator<>(new ByteArrayInputStream("{\"array\":[]}".getBytes()), Change.class);
        assertThat(it.hasNext()).isFalse();
        assertThat(it.hasNext()).isFalse();
        assertThat(it.getCount()).isEqualTo(0);
    }

    @Test
    public void testNulls() throws IOException {
        JsonArrayIterator<Change> it = new JsonArrayIterator<>(new ByteArrayInputStream("{\"array\":[null, {}, null, {}]}".getBytes()), Change.class);
        assertThat(it.hasNext()).isTrue();
        it.next();
        assertThat(it.getCount()).isEqualTo(2);
        assertThat(it.hasNext()).isTrue();
        it.next();
        assertThat(it.getCount()).isEqualTo(4);
        assertThat(it.hasNext()).isFalse();
        assertThat(it.getCount()).isEqualTo(4);
    }

    @Test
    public void testIncompleteJson() throws IOException {
        JsonArrayIterator<Change> it = new JsonArrayIterator<>(getClass().getResourceAsStream("/incomplete_changes.json"), Change.class);

        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> { while (it.hasNext()) it.next(); });
        assertThat(it.hasNext()).isFalse();
    }


    @Test
    public void testZeroBytes() throws IOException {
        JsonArrayIterator<Change> it = new JsonArrayIterator<>(new ByteArrayInputStream(new byte[0]), Change.class);

        assertThat(it.hasNext()).isFalse();
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

    @XmlAccessorType(XmlAccessType.FIELD)
    private static class Change {

        private String mid;
        private Boolean deleted;
        private Object media;

        public Change() {

        }
        public Change(String mid, Boolean deleted) {
            this.mid = mid;
            this.deleted = deleted;
        }

        String getMid() {
            return mid;
        }

        public void setMid(String mid) {
            this.mid = mid;
        }

        Boolean isDeleted() {
            return deleted;
        }

        public void setDeleted(Boolean deleted) {
            this.deleted = deleted;
        }

        public Boolean getDeleted() {
            return deleted;
        }

        Object getMedia() {
            return media;
        }

        public void setMedia(Object media) {
            this.media = media;
        }

        @Override
        public String toString() {
            return mid;
        }
    }
}
