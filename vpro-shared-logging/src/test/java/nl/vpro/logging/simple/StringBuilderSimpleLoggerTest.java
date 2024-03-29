package nl.vpro.logging.simple;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 1.77
 */
public class StringBuilderSimpleLoggerTest {

    @Test
    public void test() {
        StringBuilderSimpleLogger logger = new StringBuilderSimpleLogger();
        logger.info("x:{}", "y", new Exception());
        assertThat(logger.getStringBuilder().toString()).startsWith("INFO x:y\n" +
            "java.lang.Exception");
    }

    @Test
    public void test2() {
        StringBuilderSimpleLogger logger = new StringBuilderSimpleLogger();
        Map<String, Object> map = new HashMap<>();
        map.put("foo", 1);
        logger.info("map:{}", map);

        assertThat(logger.getStringBuilder().toString()).startsWith("INFO map:{foo=1}");
    }


    @Test
    public void testTruncate() {
        StringBuilderSimpleLogger logger = StringBuilderSimpleLogger
            .builder()
            .maxLength(4L)
            .build();
        logger.info("a");
        logger.info("b");
        logger.info("c");
        logger.info("d");
        logger.info(() -> "e");
        assertThat(logger.getStringBuilder().toString()).isEqualTo("...\n" +
            "INFO b\n" +
            "INFO c\n" +
            "INFO d\n" +
            "INFO e");
         logger.info("f");
         assertThat(logger.getStringBuilder().toString()).isEqualTo("...\n" +
             "INFO c\n" +
             "INFO d\n" +
             "INFO e\n" +
             "INFO f");

    }

    @Test
    public void testFormat() {
        StringBuilderSimpleLogger logger = StringBuilderSimpleLogger
            .builder()
            .maxLength(4L)
            .build();
        logger.info("{} + {} = 30", "10", 20);
        assertThat(logger.getStringBuilder().toString()).isEqualTo("INFO 10 + 20 = 30");
    }
}
