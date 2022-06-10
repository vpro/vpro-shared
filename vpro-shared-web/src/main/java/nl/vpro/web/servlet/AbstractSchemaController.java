package nl.vpro.web.servlet;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.util.Calendar;
import java.util.Date;
import java.util.function.BiFunction;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.checkerframework.checker.nullness.qual.NonNull;

import nl.vpro.util.SchemaType;

/**
 * @author Michiel Meeuwissen
 * @since 3.4
 */
@Slf4j
public abstract class AbstractSchemaController<M extends BiFunction<String, SchemaType, File>> {

    protected M mappings;

    protected File getFileForNamespace(@NonNull String namespace) {
        return mappings.apply(namespace, SchemaType.XSD);
    }

    protected void el(XMLStreamWriter w, String name, String chars) throws XMLStreamException {
        w.writeStartElement(name);
        w.writeCharacters(chars);
        w.writeEndElement();
    }

    protected void h2(XMLStreamWriter w, String chars) throws XMLStreamException {
        el(w, "h2", chars);
    }

    protected void a(XMLStreamWriter w, String href, String chars) throws XMLStreamException {
        w.writeStartElement("a");
        w.writeAttribute("href", href);
        w.writeCharacters(chars);
        w.writeEndElement();
    }

    protected void li_a(XMLStreamWriter w, String href, String chars, String after) throws XMLStreamException {
        w.writeStartElement("li");
        a(w, href, chars);
        if (after != null) {
            w.writeCharacters(after);
        }
        w.writeEndElement();
    }


    protected void li_a(XMLStreamWriter w, String href, String chars) throws XMLStreamException {
        li_a(w, href, chars, null);
    }

    protected void li(XMLStreamWriter w, String chars) throws XMLStreamException {
        w.writeStartElement("li");
        w.writeCharacters(chars);
        w.writeEndElement();
    }

    protected void getXSD(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final String namespace) throws IOException {
        File file = getFileForNamespace(namespace);
        serveXml(file, request, response);
    }


    protected void getJsonSchema(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final String namespace) throws IOException {
        File file = getFileForNamespace(namespace);
        serveJsonSchema(file, request, response);
    }


    protected void serveXml(File file, HttpServletRequest request, HttpServletResponse response) throws IOException {
        serveFile(file, "application/xml", request, response);
    }


    protected void serveJsonSchema(File file, HttpServletRequest request, HttpServletResponse response) throws IOException {
        serveFile(file, "application/schema+json", request, response);
    }


    protected void serveFile(File file, String contentType, HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Cache-Control", "public, max-age=86400");
        long ifModifiedSince = request.getDateHeader("If-Modified-Since");
        Date fileDate = DateUtils.round(new Date(file.lastModified()), Calendar.SECOND);
        if (ifModifiedSince > fileDate.getTime()) {
            response.setContentType(contentType);
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            response.setDateHeader("Last-Modified", fileDate.getTime());
        } else {
            response.setContentType(contentType);
            response.setDateHeader("Last-Modified", fileDate.getTime());
            try (InputStream input = Files.newInputStream(file.toPath())) {
                IOUtils.copy(input, response.getOutputStream());
            }
        }
    }


}
