package nl.vpro.swagger.model;

import io.swagger.v3.jaxrs2.ReaderListener;
import io.swagger.v3.oas.integration.api.OpenApiReader;
import io.swagger.v3.oas.models.OpenAPI;

/**
 * @author Michiel Meeuwissen
 * @since 0.49
 */
public class SwaggerReaderListener implements ReaderListener {

    {
        //SwaggerApplication.inject(this);
    }
    @Override
    public void beforeScan(OpenApiReader reader, OpenAPI swagger) {
        //swagger.get
    }

    @Override
    public void afterScan(OpenApiReader reader, OpenAPI swagger) {

        //swagger.getExtensions().get("Locale").getProperties().clear();
        //swagger.getDefinitions().get("Locale").setExample("nl-NL");
        //swagger.getDefinitions().get("Locale").setDescription("ISO 639 language codes");


    }
}
