package nl.vpro.xml.util;

import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.Deque;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * @author Michiel Meeuwissen
 * @since 2.8
 */
public class XmlStreamWriterUtil {

    final XMLStreamWriter writer;
    final Deque<AutoCloseable> closeables = new ArrayDeque<>();
    int currentDepth = 0;


    public XmlStreamWriterUtil(XMLStreamWriter writer) {
        this.writer = writer;
    }

    public AutoCloseable writeElement(String name) throws XMLStreamException {
        final int closeTo = currentDepth;
        currentDepth++;
        writer.writeStartElement(name);

        AutoCloseable closeable = () -> {
            while (currentDepth > closeTo) {
                currentDepth--;
                closeables.pop().close();
            }
            writer.writeEndElement();
        };
        closeables.push(closeable);
        return closeable;
    }
     public AutoCloseable writeElement(String namespace, String name) throws XMLStreamException {
        writer.writeStartElement(namespace, name);
        return writer::writeEndElement;

    }
    public AutoCloseable writeDocument() throws XMLStreamException {
        writer.writeStartDocument();
        return writer::writeEndDocument;

    }
    public AutoCloseable writeDocument(String version) throws XMLStreamException {
        writer.writeStartDocument(version);
        return writer::writeEndDocument;

    }
     public AutoCloseable writeDocument(Charset charset, String version) throws XMLStreamException {
        writer.writeStartDocument(charset.toString(), version);
        return writer::writeEndDocument;

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


}
