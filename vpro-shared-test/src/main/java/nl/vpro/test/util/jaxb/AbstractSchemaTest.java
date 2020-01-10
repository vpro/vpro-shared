package nl.vpro.test.util.jaxb;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.bind.*;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

import com.google.common.io.Files;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Tests whether the POMS schema's are changed. I
 *
 * @author Michiel Meeuwissen
 * @since 2.8
 */
@Slf4j
public class AbstractSchemaTest {

    @SuppressWarnings("UnstableApiUsage")
    private final static File DIR = Files.createTempDir();

    protected static JAXBContext context;

    @BeforeAll
    public static void generateXSDs() throws JAXBException, IOException {
        context = generate(
        );

    }

    @SneakyThrows
    protected <T extends Enum<T>> void testEnum(String resource, String enumTypeName, Class<T> enumClass) {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document document = dBuilder.parse(getClass().getResourceAsStream(resource));

        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList nodes = (NodeList)xPath.evaluate("/schema/simpleType[@name='" + enumTypeName + "']/restriction/enumeration", document, XPathConstants.NODESET);

        List<String> valuesInXsd = new ArrayList<>();
        for (int i  = 0; i < nodes.getLength(); i++) {
            valuesInXsd.add(nodes.item(i).getAttributes().getNamedItem("value").getTextContent());
        }

        List<String> valuesInEnum = new ArrayList<>();

        T[] values = enumClass.getEnumConstants();
        for (T v : values) {
            XmlEnumValue xmlEnumValue = enumClass.getField(v.name()).getAnnotation(XmlEnumValue.class);
            valuesInEnum.add(xmlEnumValue != null ? xmlEnumValue.value() : v.name());
        }
        assertThat(valuesInXsd)
            .containsExactlyInAnyOrderElementsOf(valuesInEnum);
    }

    private static File getFile(final String namespace) {
        String filename = namespace.replaceAll("/", "_");
        if (StringUtils.isEmpty(namespace)) {
            filename = "absentnamespace";
        }
        return new File(DIR, filename + ".xsd");
    }

    protected void testNamespace(String namespace) throws IOException {
        File file = getFile(namespace);
        InputStream control = getClass().getResourceAsStream("/schema/" + file.getName());
        if (control == null) {
            System.out.println(file.getName());
            IOUtils.copy(new FileInputStream(file), System.out);
            throw new RuntimeException("No file " + file.getName());
        }
        Diff diff = DiffBuilder.compare(control)
            .withTest(file)
            .checkForIdentical()
            .build();

        assertThat(diff.hasDifferences())
            .withFailMessage("" + file + " should be equal to " + getClass().getResource("/schema/" + file.getName())).isFalse();
    }

    protected static JAXBContext generate(Class<?>... classes) throws JAXBException, IOException {
        //DocumentationAdder collector = new DocumentationAdder(classes);

        JAXBContext context = JAXBContext.newInstance(classes);

        context.generateSchema(new SchemaOutputResolver() {
            @Override
            public Result createOutput(String namespaceUri, String suggestedFileName) throws IOException {
                if (XMLConstants.XML_NS_URI.equals(namespaceUri)) {
                    return null;
                }
                File f = getFile(namespaceUri);
                if (f.exists()) {
                    f = File.createTempFile(namespaceUri, "");
                }
                log.info(namespaceUri + " -> " + f);

                StreamResult result = new StreamResult(f);
                result.setSystemId(f);
                FileOutputStream fo = new FileOutputStream(f);
                result.setOutputStream(fo);

                return result;
            }
        });
        return context;
    }
}
