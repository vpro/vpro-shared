package nl.vpro.xml.bind;

import lombok.extern.java.Log;

import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.Date;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import static nl.vpro.xml.util.XmlUtils.DEFAULT_ZONE;
import static org.meeuw.time.TimeUtils.parseDuration;

/**
 * <a href="https://bugs.openjdk.java.net/browse/JDK-8042456">JDK-8042456</a>
 * @author Michiel Meeuwissen
 * @since 0.21
 */
@Log
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
                string = String.valueOf(xmlDurationValue.getTimeInMillis(new Date(0)));
            } else {
                string = xmlDurationValue.toString();
            }

            try {
                return parseDuration(string, Instant.EPOCH.atZone(DEFAULT_ZONE)).orElseGet(() -> {
                    log.warning("Could not parse '" + string + "'");
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
        if (value == null) {
            return null;
        }
        return value.toDays()  < 30 ? marshalDayTime(value.toMillis()) : marshal(value.toMillis());
    }

    protected javax.xml.datatype.Duration marshalDayTime(long time)  {
        return DATATYPE_FACTORY.newDurationDayTime(time);
    }

    protected javax.xml.datatype.Duration marshal(long time)  {
        return DATATYPE_FACTORY.newDuration(time);
    }
}
