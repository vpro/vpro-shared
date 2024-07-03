/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.xml.bind;

import java.util.Date;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Roelof Jan Koekoek
 * @since 2.1
 */
@Deprecated
public class SecondsToDateAdapterTest {

    @Test
    public void testUnmarshalOnWHoleNumbers() {
        Date date = new SecondsToDateAdapter().unmarshal("11");
        assertThat(date.getTime()).isEqualTo(11000L);
    }

    @Test
    public void testUnmarshalWithMillisRoundedToTens() {
        Date date = new SecondsToDateAdapter().unmarshal("10.5");
        assertThat(date.getTime()).isEqualTo(10500L);
    }

    @Test
    public void testUnmarshalWithMillisRoundedToHundreds() {
        Date date = new SecondsToDateAdapter().unmarshal("10.55");
        assertThat(date.getTime()).isEqualTo(10550L);
    }

    @Test
    public void testUnmarshalWithMillisFull() {
        Date date = new SecondsToDateAdapter().unmarshal("10.555");
        assertThat(date.getTime()).isEqualTo(10555L);
    }

    @Test
    public void testUnmarshalOmmitHigherPrecision() {
        Date date = new SecondsToDateAdapter().unmarshal("10.5558");
        assertThat(date.getTime()).isEqualTo(10555L);
    }

    @Test
    public void testUnmarshalOnLargeNumbers() {
        Date date = new SecondsToDateAdapter().unmarshal("7777777777777777.999111");
        assertThat(date.getTime()).isEqualTo(7777777777777777999L);
    }
}
