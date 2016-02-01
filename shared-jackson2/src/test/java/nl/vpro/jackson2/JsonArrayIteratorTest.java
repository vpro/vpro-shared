package nl.vpro.jackson2;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.junit.Test;


import static org.fest.assertions.Assertions.assertThat;

public class JsonArrayIteratorTest {


    @Test
    public void test() throws IOException {

        //Jackson2Mapper.getInstance().writeValue(System.out, new Change("bla", false));
        JsonArrayIterator<Change> it = new JsonArrayIterator<>(getClass().getResourceAsStream("/changes.json"), Change.class, null);
        assertThat(it.next().getMid()).isEqualTo("POMS_NCRV_1138990"); // 1
        assertThat(it.getSize().get()).isEqualTo(11);
        for (int i = 0; i < 9; i++) {
            assertThat(it.hasNext()).isTrue();

            Change change = it.next(); // 10
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
       JsonArrayIterator<Change> it = new JsonArrayIterator<>(new ByteArrayInputStream("{\"array\":[]}".getBytes()), Change.class, null);
        assertThat(it.hasNext()).isFalse();
        assertThat(it.hasNext()).isFalse();
    }

    @Test
    public void testNulls() throws IOException {
        JsonArrayIterator<Change> it = new JsonArrayIterator<>(new ByteArrayInputStream("{\"array\":[null, {}, null, {}]}".getBytes()), Change.class, null);
        assertThat(it.hasNext()).isTrue();
        it.next();
        assertThat(it.hasNext()).isTrue();
        it.next();
        assertThat(it.hasNext()).isFalse();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Change {

        private String mid;
        private Boolean deleted;
        private Object media;

        public Change() {

        }
        public Change(String mid, Boolean deleted) {
            this.mid = mid;
            this.deleted = deleted;
        }

        public String getMid() {
            return mid;
        }

        public void setMid(String mid) {
            this.mid = mid;
        }

        public Boolean isDeleted() {
            return deleted;
        }

        public void setDeleted(Boolean deleted) {
            this.deleted = deleted;
        }

        public Boolean getDeleted() {
            return deleted;
        }

        public Object getMedia() {
            return media;
        }

        public void setMedia(Object media) {
            this.media = media;
        }
    }
}
