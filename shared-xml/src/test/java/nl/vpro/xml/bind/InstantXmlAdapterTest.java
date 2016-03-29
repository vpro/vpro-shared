package nl.vpro.xml.bind;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 0.32
 */
@RunWith(Parameterized.class)
public class InstantXmlAdapterTest {

    private Instant in;
    private String out;
    private boolean marshal;

    InstantXmlAdapter instance = new InstantXmlAdapter();

    static Object[][] CASES = {
        // Instant                   XML-value
        {"2016-01-20T10:48:09.954Z", "2016-01-20T11:48:09.954+01:00", true},
        {"2016-01-20T10:48:00.000Z", "2016-01-20T11:48:00.000+01:00", true},
        {"2016-01-20T10:59:00.733Z", "1453287540733", false},
        {"2016-01-19T23:00:00Z", "2016-01-20", false},
        {"2016-01-19T23:00:00Z", "2016-01-20T00:00:00", false}
    };


    public InstantXmlAdapterTest(String in,
                                 String out,
                                 boolean marshal) {
        this.in = Instant.parse(in);
        this.out = out;
        this.marshal = marshal;
    }

    @Parameterized.Parameters
    public static Collection primeNumbers() {
        return Arrays.asList(CASES);
    }


    @Test
    public void testMarshal() {
        Assume.assumeTrue(marshal);
        assertThat(instance.marshal(in)).isEqualTo(out);

    }
    @Test
    public void testUnmarshal() {
        assertThat(instance.unmarshal(out)).isEqualTo(in);
    }

}
