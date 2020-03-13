package nl.vpro.logging;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Michiel Meeuwissen
 */
@Slf4j
public class LoggerOutputStreamTest {


    @Test
    public void test() throws IOException {
        String testString = "bla bla\n\nbloe bloe ";
        final StringBuilder buf = new StringBuilder();
        LoggerOutputStream instance = new LoggerOutputStream(false) {
            @Override
            void log(String line) {
                buf.append(line).append("\n");
            }
        };
        instance.write(testString.getBytes());
        instance.close();
        assertEquals(testString + "\n", buf.toString());
    }



    @Test
    public void performance() throws IOException {
        long nano = System.nanoTime();
        LoggerOutputStream out = LoggerOutputStream.info(log);
        for (int i = 0 ; i < 10000; i++) {
            out.write(("bal bla bla bla " + i + "\n").getBytes());
        }
        log.info(""+ Duration.ofNanos(System.nanoTime() - nano));
    }

}
