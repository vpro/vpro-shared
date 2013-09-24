/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.util;

import java.util.Date;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Roelof Jan Koekoek
 */
public class DateUtilsTest {

    @Test
    public void testLowest() throws Exception {
        assertThat(DateUtils.lowest(null,null)).isNull();
        assertThat(DateUtils.lowest(new Date(0),null)).isEqualTo(new Date(0));
        assertThat(DateUtils.lowest(new Date(0),new Date(1))).isEqualTo(new Date(0));
        assertThat(DateUtils.lowest(null,new Date(0))).isEqualTo(new Date(0));
        assertThat(DateUtils.lowest(new Date(1),new Date(0))).isEqualTo(new Date(0));
    }

    @Test
    public void testHighest() throws Exception {
        assertThat(DateUtils.highest(null,null)).isNull();
        assertThat(DateUtils.highest(new Date(0),null)).isEqualTo(new Date(0));
        assertThat(DateUtils.highest(new Date(0),new Date(1))).isEqualTo(new Date(1));
        assertThat(DateUtils.highest(null,new Date(0))).isEqualTo(new Date(0));
        assertThat(DateUtils.highest(new Date(1),new Date(0))).isEqualTo(new Date(1));
    }
}
