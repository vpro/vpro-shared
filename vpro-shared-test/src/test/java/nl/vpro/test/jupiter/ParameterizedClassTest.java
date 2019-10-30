package nl.vpro.test.jupiter;


import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.*;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */

@ExtendWith(ParameterizedClass.class)
@Slf4j
public class ParameterizedClassTest {


    static List<Integer> getParams() {
        return Arrays.asList(1,2,3);
    }

    @MethodSource("getParams")
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ParameterizedTest(name = "Elaborate name listing all {arguments}")
    @interface Params {

    }
    @BeforeEach
    public void beforeEach(int i) {

    }

    @ParameterizedTest
    @Params
    public void atest(int i) {
        log.info("{}", i);
    }
}
