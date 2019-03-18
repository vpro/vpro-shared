package nl.vpro.hibernate.search;

import java.time.Duration;

import javax.persistence.Column;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import nl.vpro.jackson2.XMLDurationToJsonTimestamp;
import nl.vpro.xml.bind.DurationXmlAdapter;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "locationType",
    propOrder = {
        "programUrl",
        "subtitles",
        "offset",
        "duration"})
public class LocationWithDurationField {


    @Column(nullable = false)
    @XmlElement
    protected String programUrl;


    @XmlElement
    protected String subtitles;

    @Temporal(TemporalType.TIME)
    @Column(name = "start_offset")
    @XmlJavaTypeAdapter(DurationXmlAdapter.class)
    @JsonSerialize(using = XMLDurationToJsonTimestamp.Serializer.class)
    @JsonDeserialize(using = XMLDurationToJsonTimestamp.DeserializerDate.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected Duration offset;

    @Temporal(TemporalType.TIME)
    @XmlElement
    @XmlJavaTypeAdapter(DurationXmlAdapter.class)
    @JsonSerialize(using = XMLDurationToJsonTimestamp.Serializer.class)
    @JsonDeserialize(using = XMLDurationToJsonTimestamp.DeserializerDate.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected Duration duration;



    @Column(nullable = true)
    @XmlTransient
    protected Long neboId;



    @XmlTransient
    private boolean ceresUpdate = false;

    public LocationWithDurationField() {
    }



    public String getProgramUrl() {
        return programUrl;
    }

    public LocationWithDurationField setProgramUrl(String url) {
        this.programUrl = url == null ? null : url.trim();
        return this;
    }


    public String getSubtitles() {
        return subtitles;
    }

    public LocationWithDurationField setSubtitles(String subtitles) {
        this.subtitles = subtitles;
        return this;
    }

    public Duration getOffset() {
        return offset;
    }

    public LocationWithDurationField setOffset(Duration offset) {
        this.offset = offset;
        return this;
    }

    public Duration getDuration() {
        return duration;
    }

    public LocationWithDurationField setDuration(Duration duration) {
        this.duration = duration;
        return this;
    }

    public boolean isCeresUpdate() {
        return ceresUpdate;
    }

    public void setCeresUpdate(Boolean ceresUpdate) {
        this.ceresUpdate = ceresUpdate;
    }




    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (this.programUrl != null ? this.programUrl.hashCode() : 0);
        return result;
    }

    public Long getNeboId() {
        return neboId;
    }

    public void setNeboId(Long neboId) {
        this.neboId = neboId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LocationWithDurationField location = (LocationWithDurationField) o;

        if (ceresUpdate != location.ceresUpdate) return false;
        if (programUrl != null ? !programUrl.equals(location.programUrl) : location.programUrl != null) return false;
        if (subtitles != null ? !subtitles.equals(location.subtitles) : location.subtitles != null) return false;
        if (offset != null ? !offset.equals(location.offset) : location.offset != null) return false;
        if (duration != null ? !duration.equals(location.duration) : location.duration != null) return false;
        return neboId != null ? neboId.equals(location.neboId) : location.neboId == null;

    }
}
