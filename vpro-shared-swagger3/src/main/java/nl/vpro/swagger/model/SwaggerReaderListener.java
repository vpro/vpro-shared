package nl.vpro.swagger.model;

import io.swagger.v3.jaxrs2.ReaderListener;
import io.swagger.v3.oas.integration.api.OpenApiReader;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.log4j.Log4j2;

/**
 * @author Michiel Meeuwissen
 */
@Log4j2
public class SwaggerReaderListener implements ReaderListener {

    @Override
    public void beforeScan(OpenApiReader reader, OpenAPI swagger) {
        //swagger.get
        log.info("Hoi");
    }

    @Override
    public void afterScan(OpenApiReader reader, OpenAPI swagger) {

        //swagger.getExtensions().get("Locale").getProperties().clear();
        //swagger.getDefinitions().get("Locale").setExample("nl-NL");
        //swagger.getDefinitions().get("Locale").setDescription("ISO 639 language codes");


    }
}
