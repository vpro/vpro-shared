package nl.vpro.web.servlet;


import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static nl.vpro.test.util.jackson2.Jackson2TestUtil.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 2.8
 */
@Slf4j
public class ConfigurationServletTest {

	@Test
	public void test() throws IOException {
		ConfigurationServlet configurationServlet = new ConfigurationServlet();
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		Map<String, Object> props = new HashMap<>();
		props.put("a", "b");
		configurationServlet.write(null, bout, new HashMap<>(), props);
		assertThatJson(bout.toString()).isSimilarTo("{\"configuration\":{\"a\":\"b\"}}");

		bout = new ByteArrayOutputStream();
		configurationServlet.write("bla", bout, new HashMap<>(), props);
		assertThat(bout.toString()).isEqualTo("var bla = {\"configuration\":{\"a\":\"b\"}};");


	}

}
