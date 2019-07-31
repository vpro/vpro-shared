package nl.vpro.xml.util;

import lombok.extern.slf4j.Slf4j;

import javax.xml.stream.XMLStreamException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 2.8
 */
@Slf4j
public class XMLStreamWriterUtilTest {


    StringBuilder builder = new StringBuilder();
    XMLStreamWriterUtil util = new XMLStreamWriterUtil(builder);

    public XMLStreamWriterUtilTest() throws XMLStreamException {
    }


    @Test
    public void writeOneElement() throws Exception {
        try (AutoCloseable closeable = util.writeElement("a")) {
            util.writeAttribute("x", "b");
        }
        assertThat(builder.toString()).isXmlEqualTo("<a x=\"b\"/>");
    }
    @Test
    public void writeAutoClosedNestedElement() throws Exception {
        try (AutoCloseable closeable = util.writeElement("a")) {
            util.writeAttribute("x", "b");
            util.writeElement("b");
        }
        assertThat(builder.toString()).isXmlEqualTo("<a x=\"b\"><b></b></a>");
    }
    @Test
    public void writeDocument() throws Exception {
        util.setDefaultNamespace("urn:bla");


        try (AutoCloseable c1 = util.writeDocument("1.0");
             AutoCloseable c2 = util.writeElement("a");

        ) {
            util.writeAttribute("x", "b");
            util.writeElement("b");
            util.writeCData("cdata");
            util.writeCharacters("characters");
            util.writeEmptyElement("urn:bla", "empty");
            util.writeEntityRef("amp");
            util.writeComment("comment");


        }
        assertThat(builder.toString()).isEqualTo("<?xml version=\"1.0\"?><a x=\"b\"><b><![CDATA[cdata]]>characters<empty/>&amp;<!--comment--></b></a>");
    }
    @Test
    public void writeWithException() throws Exception {

        try (AutoCloseable c1= util.writeElement("a");) {
            util.writeAttribute("x", "b");
            util.writeElement("b");
            util.writeCharacters("BB");
            throw new Exception("bla");
        } catch (Exception e) {
            util.writeComment(e.getMessage());
            log.info("Catched {}", e.getMessage());
        }
        assertThat(builder.toString()).isEqualTo("<a x=\"b\"><b>BB</b></a><!--bla-->");
    }
}
