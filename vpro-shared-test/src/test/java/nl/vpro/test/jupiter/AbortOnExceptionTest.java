package nl.vpro.test.jupiter;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.Assert.*;

/**
 * @author Michiel Meeuwissen
 * @since 2.9
 */
@ExtendWith(AbortOnException.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
@Disabled
class AbortOnExceptionTest {

    @Order(1)
    @Test
    void firstTest() {
        log.info("a");
    }
    @Order(2)
    @Test
    void secondTest() throws Exception {
        log.info("b");
        throw new Exception();
    }
    @Order(3)
    @Test
    void thirdTest() {
        log.info("c");
        Assertions.fail();
    }

}
