/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.util;

import java.util.Date;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Roelof Jan Koekoek
 */
public class DateUtilsTest {

    @Test
    public void testLowest() {
        assertThat(DateUtils.lowest(null,null)).isNull();
        assertThat(DateUtils.lowest(new Date(0),null)).isEqualTo(new Date(0));
        assertThat(DateUtils.lowest(new Date(0),new Date(1))).isEqualTo(new Date(0));
        assertThat(DateUtils.lowest(null,new Date(0))).isEqualTo(new Date(0));
        assertThat(DateUtils.lowest(new Date(1),new Date(0))).isEqualTo(new Date(0));
        assertThat(DateUtils.lowest(DateUtils.MIN_VALUE, new Date(0))).isEqualTo(DateUtils.MIN_VALUE);
    }

    @Test
    public void testHighest() {
        assertThat(DateUtils.highest(null,null)).isNull();
        assertThat(DateUtils.highest(new Date(0),null)).isEqualTo(new Date(0));
        assertThat(DateUtils.highest(new Date(0),new Date(1))).isEqualTo(new Date(1));
        assertThat(DateUtils.highest(null,new Date(0))).isEqualTo(new Date(0));
        assertThat(DateUtils.highest(new Date(1),new Date(0))).isEqualTo(new Date(1));
        assertThat(DateUtils.highest(DateUtils.MAX_VALUE, new Date(0))).isEqualTo(DateUtils.MAX_VALUE);
    }

    @Test
    public void testNullIsMaximal() {
        assertThat(DateUtils.nullIsMaximal(null)).isEqualTo(DateUtils.MAX_VALUE);
        assertThat(DateUtils.nullIsMaximal(new Date(100))).isEqualTo(new Date(100));

        assertThat(DateUtils.maximalIsNull(null)).isNull();
        assertThat(DateUtils.maximalIsNull(DateUtils.MAX_VALUE)).isNull();
        assertThat(DateUtils.maximalIsNull(DateUtils.MIN_VALUE)).isEqualTo(DateUtils.MIN_VALUE);
        assertThat(DateUtils.maximalIsNull(new Date(100))).isEqualTo(new Date(100));

    }



    @Test
    public void testNullIsMinimal() {
        assertThat(DateUtils.nullIsMinimal(null)).isEqualTo(DateUtils.MIN_VALUE);
        assertThat(DateUtils.nullIsMinimal(new Date(100))).isEqualTo(new Date(100));

        assertThat(DateUtils.minimalIsNull(null)).isNull();
        assertThat(DateUtils.minimalIsNull(DateUtils.MAX_VALUE)).isEqualTo(DateUtils.MAX_VALUE);
        assertThat(DateUtils.minimalIsNull(DateUtils.MIN_VALUE)).isNull();
        assertThat(DateUtils.minimalIsNull(new Date(100))).isEqualTo(new Date(100));

    }
}
