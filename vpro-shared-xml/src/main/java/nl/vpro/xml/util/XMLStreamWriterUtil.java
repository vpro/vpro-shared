package nl.vpro.xml.util;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.Deque;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.*;

import org.apache.commons.io.output.StringBuilderWriter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.meeuw.functional.ThrowingConsumer;


/**
 * A wrapper around {@link XMLStreamWriter}, which uses {@link AutoCloseable} to automatically close the needed elements.
 *
 * @author Michiel Meeuwissen
 * @since 2.8
 */
@Slf4j
public class XMLStreamWriterUtil {

    private static final XMLOutputFactory FACTORY = XMLOutputFactory.newInstance();

    private final XMLStreamWriter writer;

    private final Deque<AutoCloseable> closeables = new ArrayDeque<>();

    private int currentDepth = 0;

    public XMLStreamWriterUtil(XMLStreamWriter writer) {
        this.writer = writer;
    }

    public XMLStreamWriterUtil(OutputStream outputStream) throws XMLStreamException {
        this(FACTORY.createXMLStreamWriter(outputStream));
    }
    public XMLStreamWriterUtil(Writer writer) throws XMLStreamException {
        this(FACTORY.createXMLStreamWriter(writer));
    }
     public XMLStreamWriterUtil(StringBuilder builder) throws XMLStreamException {
        this(FACTORY.createXMLStreamWriter(new StringBuilderWriter(builder)));
    }


    public ElementCloser writeElement(final String name) throws XMLStreamException {
        log.info("Opening {}", name);
        writer.writeStartElement(name);
        return new ElementCloser(null, name);
    }
     public ElementCloser writeElement(String namespace, String name) throws XMLStreamException {
         log.info("Opening {}{}", namespace, name);

        writer.writeStartElement(namespace, name);
        return new ElementCloser(namespace, name);

    }
    public ElementCloser writeDocument() throws XMLStreamException {
        writer.writeStartDocument();
        return new ElementCloser(null, "#DOCUMENT", XMLStreamWriter::writeEndDocument);

    }
    public ElementCloser writeDocument(String version) throws XMLStreamException {
        writer.writeStartDocument(version);
        return new ElementCloser(null, "#DOCUMENT", XMLStreamWriter::writeEndDocument);

    }
     public ElementCloser writeDocument(Charset charset, String version) throws XMLStreamException {
        writer.writeStartDocument(charset.toString(), version);
         return new ElementCloser(null, "#DOCUMENT", XMLStreamWriter::writeEndDocument);
    }


    public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
        writer.writeEmptyElement(namespaceURI, localName);
    }

    public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        writer.writeEmptyElement(prefix, localName, namespaceURI);
    }

    public void writeEmptyElement(String localName) throws XMLStreamException {
        writer.writeEmptyElement(localName);
    }

    public void close() throws XMLStreamException {
        writer.close();
    }

    public void flush() throws XMLStreamException {
        writer.flush();
    }

    public void writeAttribute(String localName, String value) throws XMLStreamException {
        writer.writeAttribute(localName, value);
    }

    public void writeAttribute(String prefix, String namespaceURI, String localName, String value) throws XMLStreamException {
        writer.writeAttribute(prefix, namespaceURI, localName, value);
    }

    public void writeAttribute(String namespaceURI, String localName, String value) throws XMLStreamException {
        writer.writeAttribute(namespaceURI, localName, value);
    }

    public void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
        writer.writeNamespace(prefix, namespaceURI);
    }

    public void writeDefaultNamespace(String namespaceURI) throws XMLStreamException {
        writer.writeDefaultNamespace(namespaceURI);
    }

    public void writeComment(String data) throws XMLStreamException {
        writer.writeComment(data);
    }

    public void writeProcessingInstruction(String target) throws XMLStreamException {
        writer.writeProcessingInstruction(target);
    }

    public void writeProcessingInstruction(String target, String data) throws XMLStreamException {
        writer.writeProcessingInstruction(target, data);
    }

    public void writeCData(String data) throws XMLStreamException {
        writer.writeCData(data);
    }

    public void writeDTD(String dtd) throws XMLStreamException {
        writer.writeDTD(dtd);
    }

    public void writeEntityRef(String name) throws XMLStreamException {
        writer.writeEntityRef(name);
    }

    public void writeCharacters(String text) throws XMLStreamException {
        writer.writeCharacters(text);
    }

    public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
        writer.writeCharacters(text, start, len);
    }

    public String getPrefix(String uri) throws XMLStreamException {
        return writer.getPrefix(uri);
    }

    public void setPrefix(String prefix, String uri) throws XMLStreamException {
        writer.setPrefix(prefix, uri);
    }

    public void setDefaultNamespace(String uri) throws XMLStreamException {
        writer.setDefaultNamespace(uri);
    }

    public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
        writer.setNamespaceContext(context);
    }

    public NamespaceContext getNamespaceContext() {
        return writer.getNamespaceContext();
    }

    public Object getProperty(String name) throws IllegalArgumentException {
        return writer.getProperty(name);
    }


    /**
     * @since 2.34
     */
    public class ElementCloser implements AutoCloseable {
        @Nullable
        final String nameSpace;
        final String name;
        final int closeTo = ++currentDepth;

        final ThrowingConsumer<XMLStreamWriter, XMLStreamException> close;


        protected ElementCloser(@Nullable String nameSpace, String name, ThrowingConsumer<XMLStreamWriter, XMLStreamException> close) {
            this.nameSpace = nameSpace;
            this.name = name;
            this.close = close;
            closeables.push(this);
        }

        protected ElementCloser(@Nullable String nameSpace, String name) {
            this(nameSpace, name, XMLStreamWriter::writeEndElement);
        }


        @Override
        public void close() throws Exception {
            log.trace("Closing {}", name);
            while (currentDepth > closeTo) {
                currentDepth--;
                log.trace("And also {}", closeables.peek());
                closeables.pop().close();
            }
            currentDepth--;
            close.accept(writer);
        }

        @Override
        public String toString() {
            return nameSpace + ":" + name;
        }
    }


}
