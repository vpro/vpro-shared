package nl.vpro.test.jupiter;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */

@ExtendWith(ParameterizedClass.class)
@Slf4j
 class AbstractParameterizedClassTest {
    @Nested
    @ExtendWith({Browsers.class})
    class Base {
        int i;
        Base(int i) {
            this.i = i;
        }

        @Test
        public void test() {
            log.info("{}", i);
        }
        @BeforeEach
        public void setup() {
            log.info("before : {}", i);
        }
    }
    @Nested
    class Class1 extends Base {
        Class1() {
            super(1);
        }
    }
    @Nested
    class Class2 extends Base {
        Class2() {
            super(2);
        }
    }
}

