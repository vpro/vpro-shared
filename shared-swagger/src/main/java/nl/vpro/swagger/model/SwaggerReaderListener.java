package nl.vpro.swagger.model;

import io.swagger.jaxrs.Reader;
import io.swagger.jaxrs.config.ReaderListener;
import io.swagger.models.Swagger;

import javax.ws.rs.ext.Provider;

import nl.vpro.swagger.SwaggerApplication;

/**
 * @author Michiel Meeuwissen
 * @since 0.49
 */
public class SwaggerReaderListener implements ReaderListener{
    
    {
        SwaggerApplication.inject(this);
    }
    @Override
    public void beforeScan(Reader reader, Swagger swagger) {

    }

    @Override
    public void afterScan(Reader reader, Swagger swagger) {
        
        swagger.getDefinitions().get("Locale").getProperties().clear();
        swagger.getDefinitions().get("Locale").setExample("nl-NL");
        swagger.getDefinitions().get("Locale").setDescription("ISO 639 language codes");


    }
}
