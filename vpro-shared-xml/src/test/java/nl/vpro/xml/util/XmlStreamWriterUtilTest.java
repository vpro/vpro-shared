package nl.vpro.xml.util;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.junit.Test;

/**
 * @author Michiel Meeuwissen
 * @since 2.8
 */
@Slf4j
public class XmlStreamWriterUtilTest {

    final XMLOutputFactory factory = XMLOutputFactory.newInstance();




    @Test
    public void writeElement() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        XMLStreamWriter writer = factory.createXMLStreamWriter(outputStream);
        XmlStreamWriterUtil util = new XmlStreamWriterUtil(writer);

        try (AutoCloseable closeable = util.writeElement("a")) {
            util.writeAttribute("x", "b");
            try (AutoCloseable b = util.writeElement("b")) {

            }
        }
        log.info("{}", outputStream.toString());
    }
}
