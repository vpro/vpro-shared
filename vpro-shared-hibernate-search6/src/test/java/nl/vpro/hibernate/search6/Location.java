package nl.vpro.hibernate.search6;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

import jakarta.persistence.*;
import jakarta.xml.bind.annotation.*;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import nl.vpro.jackson2.XMLDurationToJsonTimestamp;

/**
 * Used in {@link JsonBridgeTest}
 */

@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "locationType",
    propOrder = {
        "programUrl",
        "subtitles",
        "offset",
        "duration"})
public class Location  {


    @Column(nullable = false)
    @XmlElement
    protected String programUrl;


    @XmlElement
    protected String subtitles;

    @Temporal(TemporalType.TIME)
    @Column(name = "start_offset")
    @JsonSerialize(using = XMLDurationToJsonTimestamp.Serializer.class)
    @JsonDeserialize(using = XMLDurationToJsonTimestamp.DeserializerDate.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected Date offset;

    @Temporal(TemporalType.TIME)
    @XmlElement
    @JsonSerialize(using = XMLDurationToJsonTimestamp.Serializer.class)
    @JsonDeserialize(using = XMLDurationToJsonTimestamp.DeserializerDate.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected Date duration;



    @Setter
    @Column(nullable = true)
    @XmlTransient
    protected Long neboId;



    @XmlTransient
    private boolean ceresUpdate = false;

    public Location() {
    }


    public Location setProgramUrl(String url) {
        this.programUrl = url == null ? null : url.trim();
        return this;
    }


    public Location setSubtitles(String subtitles) {
        this.subtitles = subtitles;
        return this;
    }

    public Location setOffset(Date offset) {
        this.offset = offset;
        return this;
    }

    public Location setDuration(Date duration) {
        this.duration = duration;
        return this;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Location location = (Location) o;

        if (ceresUpdate != location.ceresUpdate) return false;
        if (programUrl != null ? !programUrl.equals(location.programUrl) : location.programUrl != null) return false;
        if (subtitles != null ? !subtitles.equals(location.subtitles) : location.subtitles != null) return false;
        if (offset != null ? !offset.equals(location.offset) : location.offset != null) return false;
        if (duration != null ? !duration.equals(location.duration) : location.duration != null) return false;
        return neboId != null ? neboId.equals(location.neboId) : location.neboId == null;

    }
}
