package nl.vpro.swagger.taglib;

import org.junit.jupiter.api.Test;

class VersionsTest {


    @Test
    public void version() {
        Versions v = new Versions();
        System.out.println(v.getSwaggerUIVersion());
    }
}
