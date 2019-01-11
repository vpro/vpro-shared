/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.xml.bind;

import java.util.Date;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Roelof Jan Koekoek
 * @since 2.1
 */
@SuppressWarnings("deprecation")
public class SecondsToDateAdapterTest {

    @Test
    public void testUnmarshalOnWHoleNumbers() {
        Date date = new SecondsToDateAdapter().unmarshal("11");
        assertThat(date.getTime()).isEqualTo(11000l);
    }

    @Test
    public void testUnmarshalWithMillisRoundedToTens() {
        Date date = new SecondsToDateAdapter().unmarshal("10.5");
        assertThat(date.getTime()).isEqualTo(10500l);
    }

    @Test
    public void testUnmarshalWithMillisRoundedToHundreds() {
        Date date = new SecondsToDateAdapter().unmarshal("10.55");
        assertThat(date.getTime()).isEqualTo(10550l);
    }

    @Test
    public void testUnmarshalWithMillisFull() {
        Date date = new SecondsToDateAdapter().unmarshal("10.555");
        assertThat(date.getTime()).isEqualTo(10555l);
    }

    @Test
    public void testUnmarshalOmmitHigherPrecision() {
        Date date = new SecondsToDateAdapter().unmarshal("10.5558");
        assertThat(date.getTime()).isEqualTo(10555l);
    }

    @Test
    public void testUnmarshalOnLargeNumbers() {
        Date date = new SecondsToDateAdapter().unmarshal("7777777777777777.999111");
        assertThat(date.getTime()).isEqualTo(7777777777777777999l);
    }
}
