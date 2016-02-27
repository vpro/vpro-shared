package nl.vpro.hibernate;

import java.util.HashMap;
import java.util.Map;

import org.fest.assertions.Assertions;
import org.junit.Test;

import nl.vpro.test.util.jackson2.Jackson2TestUtil;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 4.3
 */
public class JsonBridgeTest {

    @Test
    public void testObjectToString() throws Exception {
        String string = "[{\"programUrl\":\"http://content.omroep.nl/avrotros/transcoding/communicatie/1422371188061/320x180_180.m4v\",\"avAttributes\":{\"bitrate\":180000,\"avFileFormat\":\"M4V\",\"videoAttributes\":{\"width\":320,\"heigth\":180}},\"createdBy\":{\"principalId\":\"avrotros-importer\",\"displayName\":\"AVROTROS importer\",\"email\":\"media-beheer@omroep.nl\",\"lastLogin\":1429686787549,\"id\":\"avrotros-importer\"},\"lastModifiedBy\":{\"principalId\":\"avrotros-importer\",\"displayName\":\"AVROTROS importer\",\"email\":\"media-beheer@omroep.nl\",\"lastLogin\":1429686787549,\"id\":\"avrotros-importer\"},\"owner\":\"BROADCASTER\",\"creationDate\":1422371236107,\"lastModified\":1422371240979,\"workflow\":\"PUBLISHED\",\"urn\":\"urn:vpro:media:location:50725180\"},{\"programUrl\":\"http://content.omroep.nl/avrotros/transcoding/communicatie/1422371188061/640x360_1000.m4v\",\"avAttributes\":{\"bitrate\":990000,\"avFileFormat\":\"M4V\",\"videoAttributes\":{\"width\":640,\"heigth\":360}},\"createdBy\":{\"principalId\":\"avrotros-importer\",\"displayName\":\"AVROTROS importer\",\"email\":\"media-beheer@omroep.nl\",\"lastLogin\":1429686787549,\"id\":\"avrotros-importer\"},\"lastModifiedBy\":{\"principalId\":\"avrotros-importer\",\"displayName\":\"AVROTROS importer\",\"email\":\"media-beheer@omroep.nl\",\"lastLogin\":1429686787549,\"id\":\"avrotros-importer\"},\"owner\":\"BROADCASTER\",\"creationDate\":1422371236107,\"lastModified\":1422371240994,\"workflow\":\"PUBLISHED\",\"urn\":\"urn:vpro:media:location:50725183\"},{\"programUrl\":\"http://content.omroep.nl/avrotros/transcoding/communicatie/1422371188061/854x480_1500.m4v\",\"avAttributes\":{\"bitrate\":1500000,\"avFileFormat\":\"M4V\",\"videoAttributes\":{\"width\":854,\"heigth\":480}},\"createdBy\":{\"principalId\":\"avrotros-importer\",\"displayName\":\"AVROTROS importer\",\"email\":\"media-beheer@omroep.nl\",\"lastLogin\":1429686787549,\"id\":\"avrotros-importer\"},\"lastModifiedBy\":{\"principalId\":\"avrotros-importer\",\"displayName\":\"AVROTROS importer\",\"email\":\"media-beheer@omroep.nl\",\"lastLogin\":1429686787549,\"id\":\"avrotros-importer\"},\"owner\":\"BROADCASTER\",\"creationDate\":1422371236107,\"lastModified\":1422371240996,\"workflow\":\"PUBLISHED\",\"urn\":\"urn:vpro:media:location:50725186\"}]";

        JsonBridge bridge = new JsonBridge();
        Map<String, String> params = new HashMap<>();
        params.put("class", Location[].class.toGenericString());

        bridge.setParameterValues(params);

        Location[] o = (Location[]) bridge.stringToObject(string);
        

        Jackson2TestUtil.roundTripAndSimilar(o, string);


    }
}
