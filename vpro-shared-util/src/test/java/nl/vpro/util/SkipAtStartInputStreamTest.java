package nl.vpro.util;

import java.io.ByteArrayInputStream;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Michiel Meeuwissen
 * @since 2.5
 */
public class SkipAtStartInputStreamTest {

    @Test
    public void skipUTF8ByteOrderMarks() {

        byte[] withUtf8 = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'a', 'b'};

        assertThat(new SkipAtStartInputStream(new ByteArrayInputStream(withUtf8), SkipAtStartInputStream.UTF8_BYTE_ORDER_MARK)).hasSameContentAs(new ByteArrayInputStream(new byte[]{'a', 'b'}));


    }
    @Test
    public void skipUTF8ByteOrderMarks2() {

        byte[] withUtf8 = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'a', 'b'};

        assertThat(SkipAtStartInputStream.skipUnicodeByteOrderMarks(new ByteArrayInputStream(withUtf8))).hasSameContentAs(new ByteArrayInputStream(new byte[]{'a', 'b'}));
    }

     @Test
    public void dontskipUnicodeByteOrderMarks() {
         byte[] withunrecognized =  {(byte) 0xFF, (byte) 0xFF, 'x', 'y'};


        assertThat(SkipAtStartInputStream.skipUnicodeByteOrderMarks(new ByteArrayInputStream(withunrecognized))).hasSameContentAs(new ByteArrayInputStream(withunrecognized));


    }

    @Test
    public void noskipifnotrecognized() {
         byte[] withunrecognized =  {(byte) 0xEF, (byte) 0xFF, 'c', 'd'}; // looks a bit like UTF8

        assertThat(new SkipAtStartInputStream(new ByteArrayInputStream(withunrecognized), SkipAtStartInputStream.UTF8_BYTE_ORDER_MARK)).hasSameContentAs(new ByteArrayInputStream(withunrecognized));


    }

    @Test
    public void skipUnicodeByteOrderMarks() {

        byte[] withUtf8 = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'a', 'b'};

        byte[] withbe =  {(byte) 0xFE, (byte) 0xFF, 'c', 'd'};
        byte[] withle =  {(byte) 0xFF, (byte) 0xFE, 'c', 'd'};
        byte[] withunrecognized =  {(byte) 0xFF, (byte) 0xFF, 'x', 'y'};
        byte[] withnothingspecial= {'x'};



        assertThat(SkipAtStartInputStream.skipUnicodeByteOrderMarks(new ByteArrayInputStream(withnothingspecial))).hasSameContentAs(new ByteArrayInputStream(withnothingspecial));

        assertThat(SkipAtStartInputStream.skipUnicodeByteOrderMarks(new ByteArrayInputStream(withUtf8))).hasSameContentAs(new ByteArrayInputStream(new byte[]{'a', 'b'}));
        assertThat(SkipAtStartInputStream.skipUnicodeByteOrderMarks(new ByteArrayInputStream(withbe))).hasSameContentAs(new ByteArrayInputStream(new byte[]{'c', 'd'}));
        assertThat(SkipAtStartInputStream.skipUnicodeByteOrderMarks(new ByteArrayInputStream(withle))).hasSameContentAs(new ByteArrayInputStream(new byte[]{'c', 'd'}));
        assertThat(SkipAtStartInputStream.skipUnicodeByteOrderMarks(new ByteArrayInputStream(withunrecognized))).hasSameContentAs(new ByteArrayInputStream(withunrecognized));

    }
}
