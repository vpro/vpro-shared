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
        ToStringBuilderSimpleLogger logger = new ToStringBuilderSimpleLogger();
        logger.info("x:{}", "y", new Exception());
        assertThat(logger.getStringBuilder().toString()).startsWith("INFO x:y\n" +
            "java.lang.Exception");
    }

}
