package nl.vpro.logging.simple;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 1.77
 */
public class ToStringBuilderSimpleLoggerTest {

    @Test
    public void test() {
        StringBuilderSimpleLogger logger = new StringBuilderSimpleLogger();
        logger.info("x:{}", "y", new Exception());
        assertThat(logger.getStringBuilder().toString()).startsWith("INFO x:y\n" +
            "java.lang.Exception");
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
        logger.info("e");
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
}
