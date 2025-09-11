package nl.vpro.swagger.taglib;

import org.webjars.WebJarVersionLocator;

public class Versions {

    static WebJarVersionLocator locator = new WebJarVersionLocator();
    public static String getSwaggerUIVersion() {

        return locator.version("swagger-ui");
    }
}
