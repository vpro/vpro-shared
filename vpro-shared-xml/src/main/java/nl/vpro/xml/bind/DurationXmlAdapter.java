package nl.vpro.xml.bind;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Date;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import nl.vpro.util.BindingUtils;
import nl.vpro.util.TimeUtils;

/**
 * https://bugs.openjdk.java.net/browse/JDK-8042456
 * @author Michiel Meeuwissen
 * @since 0.21
 */
@Slf4j
public class DurationXmlAdapter extends XmlAdapter<javax.xml.datatype.Duration, Duration> {


    static final DatatypeFactory DATATYPE_FACTORY;
    static {
        try {
            DATATYPE_FACTORY = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException();
        }
    }
    @Override
    public Duration unmarshal(javax.xml.datatype.Duration xmlDurationValue) {
        if (xmlDurationValue != null) {
            final String string;
            if (xmlDurationValue.getYears() > 0) {
                string = "" + xmlDurationValue.getTimeInMillis(new Date(0));
            } else {
                string = xmlDurationValue.toString();
            }

            try {
                return TimeUtils.parseDuration(string, Instant.EPOCH.atZone(BindingUtils.DEFAULT_ZONE)).orElseGet(() -> {
                    log.warn("Could not parse '" + string + "'");
                    return null;
                });
            } catch (DateTimeParseException dateTimeParseException) {
                throw new DateTimeParseException("Could not parse " + string + " to duration: " + dateTimeParseException.getMessage(), dateTimeParseException.getParsedString(), dateTimeParseException.getErrorIndex());
            }
        } else {
            return null;
        }
    }

    @Override
    public javax.xml.datatype.Duration marshal(Duration value) {
        return value != null ? (value.toDays() < 30 ? marshalDayTime(value.toMillis()) : marshal(value.toMillis())) : null;
    }

    protected javax.xml.datatype.Duration marshalDayTime(long time)  {
        return DATATYPE_FACTORY.newDurationDayTime(time);
    }

    protected javax.xml.datatype.Duration marshal(long time)  {
        return DATATYPE_FACTORY.newDuration(time);
    }
}
