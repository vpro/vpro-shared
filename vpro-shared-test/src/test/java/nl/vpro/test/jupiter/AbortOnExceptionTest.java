package nl.vpro.test.jupiter;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * @author Michiel Meeuwissen
 * @since 2.9
 */
@ExtendWith(AbortOnException.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
//@Disabled("A test is failing on purpose because we're testing just that")
class AbortOnExceptionTest {
    @Order(1)
    @Test
    void firstTest() {
        log.info("a");
    }
    @Order(2)
    @Test
    void secondTest() throws Exception {
        Assumptions.assumeTrue(1 == 0);
        log.info("b");
    }
    @Order(3)
    @Test
    void thirdTest() throws Exception {
        log.info("c");
        throw new Exception("exception from third test");
    }
    @Order(3)
    @Test
    void fourthTest() {
        log.info("d");
        Assertions.fail();
    }

    @Order(5)
    @Test
    void fifthTest() {
        log.info("e");
        Assertions.fail();
    }

    @AfterAll
    public static void shutdown(List<Exception> exceptions) {
        log.info("{}", exceptions);

    }

}
