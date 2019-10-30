package nl.vpro.test.util.jaxb.test;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * This has no XmlRootElement
 * @author Michiel Meeuwissen
 * @since ...
 */
@XmlType(name = "bType")
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
public class B {
    String value = "bb";


    @XmlAttribute
    Integer i = 1;


    @XmlAttribute
    Integer j = 2;
/*

    @XmlJavaTypeAdapter(MapAdapter.class)
    Map<String, String> map = new HashMap<>();

    {
        map.put("x", "y");
    }
*/

    C c = new C();

    public static class MapAdapter  extends XmlAdapter<MapAdapter.AdaptedMap, Map<String, String>> {

        public static class Entry {

            public String k;

            public String v;

        }
        public static class AdaptedMap {
            public List<Entry> e = new ArrayList<>();
        }

        @Override
        public Map<String, String > unmarshal(AdaptedMap adaptedMap) {
            Map<String, String> map = new HashMap<>();
            for(Entry entry : adaptedMap.e) {
                map.put(entry.k, entry.v);
            }
            return map;
        }

        @Override
        public AdaptedMap marshal(Map<String, String> map) {
        AdaptedMap adaptedMap = new AdaptedMap();
        for(Map.Entry<String, String> mapEntry : map.entrySet()) {
            Entry entry = new Entry();
            entry.k= mapEntry.getKey();
            entry.v= mapEntry.getValue();
            adaptedMap.e.add(entry);
        }
        return adaptedMap;
    }

    }

}
