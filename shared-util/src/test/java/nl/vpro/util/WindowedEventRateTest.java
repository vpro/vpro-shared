package nl.vpro.util;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 * @author Michiel Meeuwissen
 * @since 0.38
 */
public class WindowedEventRateTest {


    @Test
    public void test() throws InterruptedException {
        WindowedEventRate rate = new WindowedEventRate(1, TimeUnit.SECONDS, 5);
        long start = System.currentTimeMillis();

        for (int i = 0; i < 1000; i++) {
            System.out.println("duration: " + (System.currentTimeMillis() - start) + " ms. Measured rate " + rate.getRate(TimeUnit.SECONDS) + " #/s");
            rate.newEvent();
        }

        Thread.sleep(4800L);
        System.out.println("duration: " + (System.currentTimeMillis() - start) + " ms. Measured rate " + rate.getRate(TimeUnit.SECONDS) + " #/s");

    }

}
