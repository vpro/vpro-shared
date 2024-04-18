package nl.vpro.hibernate.search6;

import org.junit.jupiter.api.Test;

import java.util.Arrays;


import nl.vpro.test.util.jackson2.Jackson2TestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Michiel Meeuwissen
 * @since 4.3
 */
public class JsonBridgeTest {

    /* 100 chars */
    private static final String LONG_STRING = "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890";

    @Test
    public void testObjectToString() {
        String string = "[{\"programUrl\":\"http://content.omroep.nl/avrotros/transcoding/communicatie/1422371188061/320x180_180.m4v\"},{\"programUrl\":\"http://content.omroep.nl/avrotros/transcoding/communicatie/1422371188061/640x360_1000.m4v\"},{\"programUrl\":\"http://content.omroep.nl/avrotros/transcoding/communicatie/1422371188061/854x480_1500.m4v\"}]\n"
            ;

        try (JsonBridge<Location[]> bridge = new JsonBridge<>(Location[].class)) {

            Location[] o = bridge.fromIndexedValue(string, null);


            Jackson2TestUtil.roundTripAndSimilar(o, string);
        }
    }


    @Test
    public void testObjectToStringWithDuration() {
        String string = "[{\"programUrl\":\"http://content.omroep.nl/avrotros/transcoding/communicatie/1422371188061/320x180_180.m4v\"},{\"programUrl\":\"http://content.omroep.nl/avrotros/transcoding/communicatie/1422371188061/640x360_1000.m4v\"},{\"programUrl\":\"http://content.omroep.nl/avrotros/transcoding/communicatie/1422371188061/854x480_1500.m4v\"}]\n";

        try (JsonBridge<LocationWithDurationField[]> bridge = new JsonBridge<>(LocationWithDurationField[].class)) {

            LocationWithDurationField[] o = (LocationWithDurationField []) bridge.fromIndexedValue(string, null);


            Jackson2TestUtil.roundTripAndSimilar(o, string);
        }
    }

    @Test
    public void testTooLargeArray() {
        /* Array with 1000 elements each 100 chars long, cannot serialize to JSON to store in Lucene, use shorter version */
        String[] array = new String[1000];
        Arrays.fill(array, LONG_STRING);

        try (JsonBridge<String[]> bridge = new JsonBridge<>(String[].class)) {
            String ret = bridge.toIndexedValue(array, null);
            /* Result will be anywhere between MAX_LENGTH +/- LONG_STRING.length() characters */
            assertTrue(ret.length() >= JsonBridge.MAX_LENGTH - LONG_STRING.length());
            assertTrue(ret.length() <= JsonBridge.MAX_LENGTH + LONG_STRING.length());
        }
    }

    @Test
    public void testOKArray100() {
        /* Array with 100 elements each 100 chars long, can serialize to JSON to store in Lucene */
        String[] array = new String[100];
        Arrays.fill(array, LONG_STRING);

        try (JsonBridge<String[]> bridge = new JsonBridge<>(String[].class)) {
            String ret = bridge.toIndexedValue(array, null);
            /* Prefix '[' + 100 * ('"' + string length + '",') - last comma + ']' */
            assertEquals(1 + array.length * (LONG_STRING.length() + 3) - 1 + 1, ret.length());
        }
    }

    @Test
    public void testOKArray1() {
        /* Array with 1 element of  100 chars long, can serialize to JSON to store in Lucene */
        try (JsonBridge<String[]> bridge = new JsonBridge<>(String[].class)) {
            String ret = bridge.toIndexedValue(new String[]{LONG_STRING}, null);
            /* Prefix '["' + string length + '"]' */
            assertEquals(1 + (LONG_STRING.length() + 2) + 1, ret.length());
        }
    }

    @Test
    public void testTooLargeObject() {
        /* Single object 100 * 1000 chars long which is too long to serialize to JSON to store in Lucene */
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            buf.append(LONG_STRING);
        }

        try (JsonBridge<String> bridge = new JsonBridge<>(String.class)) {
            String ret = bridge.toIndexedValue(buf.toString(), null);
            assertEquals("{}", ret);
        }
    }

    @Test
    public void testTooLargeArray1() {
        /* Array with single object 100 * 1000 chars long which is too long to serialize to JSON to store in Lucene */
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            buf.append(LONG_STRING);
        }

        try (JsonBridge<String[]> bridge = new JsonBridge<>(String[].class)) {
            String ret = bridge.toIndexedValue(new String[]{buf.toString()}, null);
            assertEquals("[]", ret);
        }
    }
}
