package nl.vpro.test.jupiter;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.extension.ExtendWith;

import static java.lang.Thread.sleep;

@ExtendWith(TimingExtension.class)
class TimingExtensionTest {

    @RepeatedTest(10)
    public void test() throws InterruptedException {
        sleep(10);
    }

}
