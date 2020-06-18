package nl.vpro.jmx;

import lombok.SneakyThrows;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.management.*;

/**
 * @author Michiel Meeuwissen
 * @since 2.12
 */
@MXBean
public class MBeansUtils implements MBeansUtilsMXBean {

    public MBeansUtils() throws MalformedObjectNameException {
        this(true);
    }


    public MBeansUtils(boolean register) throws MalformedObjectNameException {
        if (register) {
            MBeans.registerBean(new ObjectName("nl.vpro.MBeansUtils:name=" + UUID.randomUUID()), this);
        }
    }

    @Override
    @SneakyThrows
    public String cancel(String key) {
        return MBeans.cancel(key).get();
    }

    @Override
    public Map<String, String> getRunning() {
        return MBeans.locks.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, x -> x.getValue().toString()));

    }

}
