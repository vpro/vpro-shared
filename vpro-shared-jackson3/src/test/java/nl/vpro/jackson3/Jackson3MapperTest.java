package nl.vpro.jackson3;

import lombok.extern.log4j.Log4j2;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.introspect.AnnotationIntrospectorPair;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationIntrospector;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import jakarta.xml.bind.annotation.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;

import nl.vpro.jackson.Views;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Log4j2
public class Jackson3MapperTest {

    public enum EnumValues {
        a,
        b
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @XmlRootElement
    public static class A {
        @XmlElement
        int integer;

        @XmlAttribute
        EnumValues enumValue;

        @XmlElement
        Optional<Integer> optional;
    }

    /**
     * This is not actually testing Jackson3Mapper itself, but is used to test things out with jackson3 itself.
     * So, this test can be changed without affecting coverage.
     */
    @Test
    public void basicJackson3() {
        JsonMapper mapper = JsonMapper.builder()
            .enable(MapperFeature.DEFAULT_VIEW_INCLUSION)

            .annotationIntrospector(new AnnotationIntrospectorPair(
            new JacksonAnnotationIntrospector(),
            new JakartaXmlBindAnnotationIntrospector(false)
            ))
            .changeDefaultPropertyInclusion(v -> v.withValueInclusion(JsonInclude.Include.NON_EMPTY))
            .build();

        A a = mapper.readerWithView(Views.Normal.class).forType(A.class)
            .readValue("""
        {"integer": 2, "optional": 3}
        """);
        assertThat(a.integer).isEqualTo(2);
        assertThat(a.optional).contains(3);

        assertThat(mapper.writer().writeValueAsString(a)).isEqualTo("{\"integer\":2,\"optional\":3}");
    }


    @Test
    public void read() {
        //String example = "/* leading comments */\n{'integer': 2 /* ignore comments */, 'optional': 3}";
        String example ="""
        {"integer": 2, "optional": 3}
        """;
        A a = Jackson3Mapper.INSTANCE
            .readerFor(A.class)
            .readValue(example);
        assertThat(a.integer).isEqualTo(2);
        assertThat(a.optional).isPresent();
        assertThat(a.optional.get()).isEqualTo(3);

        Jackson3Mapper.LENIENT.readerFor(A.class).readValue(example.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void readChange() {
        String change = """
             {
                    "sequence": 4000,
                    "revision": 1,
                    "mid": "POMS_VPRO_1139774",
                    "deleted": false,
                    "media": {
                        "objectType": "program",
                        "mid": "POMS_VPRO_1139774",
                        "creationDate": 1396061002808,
                        "lastModified": 1398759143568,
                        "sortDate": 1398722400000,
                        "urn": "urn:vpro:media:program:39326270",
                        "embeddable": true,
                        "descriptions": [1]

                    }
                }
            """;
        JsonArrayIteratorTest.Change a = Jackson3Mapper.INSTANCE
            .reader()
            .forType(JsonArrayIteratorTest.Change.class)
            .readValue(change);
        assertThat(a.getMedia()).isNotNull();
        assertThat(a.getMedia()).isInstanceOf(Map.class);
        log.info("Change media: {}", a.getMedia());
    }

    @Test
    public void readIntFromString() {
        A a = Jackson3Mapper.INSTANCE.readerFor(A.class).readValue("{'integer': '2'}");
        assertThat(a.integer).isEqualTo(2);
    }

    @Test
    public void readEnumValue() {
        A a = Jackson3Mapper.INSTANCE.readerFor(A.class).readValue("{'enumValue': 'a'}");
        assertThat(a.enumValue).isEqualTo(EnumValues.a);
    }

    @Test
    public void readUnknownEnumValue() {
        assertThatThrownBy(() -> {
            A a = Jackson3Mapper.INSTANCE.readerFor(A.class).readValue("{'enumValue': 'c'}");
        }).isInstanceOf(InvalidFormatException.class);
    }

    @Test
    public void readUnknownEnumValueLenient() {
        A a = Jackson3Mapper.LENIENT.readerFor(A.class).readValue("{'enumValue': 'c'}");
        assertThat(a.enumValue).isNull();
    }

    @Test
    public void write() throws JacksonException {
        A a = new A();
        a.integer = 2;
        a.optional = Optional.of(3);
        assertThat(Jackson3Mapper.INSTANCE.writer().writeValueAsString(a)).isEqualTo("{\"integer\":2,\"optional\":3}");
    }

    @Test
    public void writeWithEmptyOptional() throws JacksonException {
        A a = new A();
        a.integer = 2;
        a.optional = Optional.empty();
        assertThat(Jackson3Mapper.INSTANCE.writer().writeValueAsString(a)).isEqualTo("{\"integer\":2}");
    }

}
