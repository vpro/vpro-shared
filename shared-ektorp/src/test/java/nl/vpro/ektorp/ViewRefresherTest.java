package nl.vpro.ektorp;

import org.ektorp.CouchDbConnector;
import org.ektorp.support.View;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Michiel Meeuwissen
 * @since 0.43
 */
public class ViewRefresherTest {

    public static class TestClass {

        private static String DOC = "bla";
        @View(name = "test", map = "classpath:/META-INF/couchapp/MediaObject/views/test/map.js", reduce = "classpath:/META-INF/couchapp/MediaObject/views/test/reduce.js")
        public void someMethod(String mid) {

        }
    }
    @Test
    public void test() {
        CouchDbConnector connector = mock(CouchDbConnector.class);
        ViewRefresher refresher = new ViewRefresher(connector, 1, "DOC", TestClass.class);
        assertThat(refresher.views).containsExactly("test");
        assertThat(refresher.designDocumentId).isEqualTo("bla");

    }

    @Test
    public void test2() {
        CouchDbConnector connector = mock(CouchDbConnector.class);
        ViewRefresher refresher = new ViewRefresher(connector, 1, String.class, TestClass.class);
        assertThat(refresher.views).containsExactly("test");
        assertThat(refresher.designDocumentId).isEqualTo("_design/String");

    }

}
