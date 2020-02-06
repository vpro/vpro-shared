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
@Disabled("A test is failing on purpose because we're testing just that")
class AbortOnExceptionTest {
    @Order(1)
    @Test
    void firstTest() {
        log.info("a");
    }
    @Order(2)
    @Test
    void secondTest() {
        log.info("b");
    }
    @Order(3)
    @Test
    @AbortOnException.Except
    void thirdTest() {
        Assertions.assertTrue(1 == 0);
        log.info("c");
    }

    @Order(4)
    @Test
    void fourthTest() throws Exception {
        log.info("d");
        throw new Exception("exception from fourth test");
    }
    @Order(5)
    @Test
    void fifthTest() {
        log.info("e");
        Assertions.fail();
    }

    @Order(6)
    @Test
    void sixthTest() {
        log.info("f");
        Assertions.fail();
    }

    @AfterAll
    public static void shutdown(List<Exception> exceptions) {
        log.info("{}", exceptions);

    }

}
