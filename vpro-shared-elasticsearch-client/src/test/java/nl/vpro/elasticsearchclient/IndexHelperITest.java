package nl.vpro.elasticsearchclient;

import lombok.extern.log4j.Log4j2;
import tools.jackson.databind.JsonNode;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.*;
import org.junit.jupiter.api.*;

import nl.vpro.jackson3.Jackson3Mapper;

@Log4j2
@Disabled
public class IndexHelperITest {
    HttpHost host = HttpHost.create("https://vpc-vproapi-elasticsearch-test-vltgued6xifxoe534422g642ty.eu-central-1.es.amazonaws.com:9240");

    String userName = "elasticsearch";
    String password;

    {
        Properties properties = new Properties();
        try (FileInputStream is = new FileInputStream(System.getProperty("user.home") + File.separator + "conf" + File.separator + "itests.properties")) {
            properties.load(is);
        } catch (IOException e) {
            log.warn(e.getMessage(), e);
        }
        password = properties.getProperty("elasticsearch.password", null);
    }

    String example = """
            {
              "objectType" : "program",
              "mid" : "BINDINC_397900696",
              "type" : "BROADCAST",
              "avType" : "VIDEO",
              "workflow" : "PUBLISHED",
              "sortDate" : 1736542500000,
              "embeddable" : true,
              "crids" : [ "crid://media-press.tv/397900696" ],
              "broadcasters" : [ ]
            }
            """;

    IndexHelper helper;
    @BeforeEach
    public void setup() {

        ClientElasticSearchFactory factory = new ClientElasticSearchFactory();
        factory.setHttpHosts(host);
        //factory.setHttpHosts(HttpHost.create("http://localhost:9200"));
        factory.setBasicUser(userName);
        factory.setBasicPassword(password);
        //factory.setClusterName("597730061590:vproapi-elasticsearch-test");
        //factory.setClusterName("elasticsearch");

        helper = IndexHelper.builder()
            .log(log)
            .client(factory)
            .indexName("apimedia-publish")
            .build();
    }

    @Test
    public void index() {

        JsonNode node = Jackson3Mapper.getLenientInstance().readerFor(JsonNode.class).readValue(example);

        byte[]  s= example.getBytes(StandardCharsets.UTF_8);


        log.info("Length {}", s.length);


        helper.index("BINDINC_397900696", s);
    }


     @Test
    public void index2() throws IOException {


         RestClient client = RestClient
                .builder(host)
             .setHttpClientConfigCallback(config -> {
                 CredentialsProvider credsProvider = new BasicCredentialsProvider();
                 credsProvider.setCredentials(AuthScope.ANY,
                     new UsernamePasswordCredentials(userName, password));

                 config.setDefaultCredentialsProvider(credsProvider);
                 return config;
             })
             .build();
         Request request = new Request("GET", "/apimedia-publish/_doc/BINDINC_397900696");
         //request.setJsonEntity(example);


         Response response = client.performRequest(request);
         log.info("{}", response);

    }

}
