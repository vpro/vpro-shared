package nl.vpro.logging.log4j2;

import lombok.extern.log4j.Log4j2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2

class CaptureStringFromLoggerTest {


    @Test
    public void log() {
        try (CaptureStringFromLogger capture = new CaptureStringFromLogger()){
            log.info("foo");
            log.info("bar");

            assertEquals("foo\nbar", capture.get());
        }
    }

}
