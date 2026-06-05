package nl.vpro.swagger.taglib;

import org.junit.jupiter.api.Test;


class VersionsTest {


    @Test
    public void version() {
        System.getLogger("test").log(System.Logger.Level.INFO, "swagger version: " + Versions.getSwaggerUIVersion());
    }
}
