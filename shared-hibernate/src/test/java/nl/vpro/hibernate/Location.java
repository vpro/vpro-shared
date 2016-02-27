package nl.vpro.hibernate;

import javax.persistence.Column;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.*;
import java.util.Date;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import nl.vpro.jackson2.XMLDurationToJsonTimestamp;

 
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "locationType",
    propOrder = {"programUrl",
     
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
    @JsonSerialize(using = XMLDurationToJsonTimestamp.Serializer.class, include = JsonSerialize.Inclusion.NON_NULL)
    @JsonDeserialize(using = XMLDurationToJsonTimestamp.DeserializerDate.class)
    protected Date offset;

    @Temporal(TemporalType.TIME)
    @XmlElement
    @JsonSerialize(using = XMLDurationToJsonTimestamp.Serializer.class, include = JsonSerialize.Inclusion.NON_NULL)
    @JsonDeserialize(using = XMLDurationToJsonTimestamp.DeserializerDate.class)
    protected Date duration;

    

    @Column(nullable = true)
    @XmlTransient
    protected Long neboId;

   

    @XmlTransient
    private boolean ceresUpdate = false;

    public Location() {
    }
    

    
    public String getProgramUrl() {
        return programUrl;
    }

    public Location setProgramUrl(String url) {
        this.programUrl = url == null ? null : url.trim();
        return this;
    }
  

    public String getSubtitles() {
        return subtitles;
    }

    public Location setSubtitles(String subtitles) {
        this.subtitles = subtitles;
        return this;
    }

    public Date getOffset() {
        return offset;
    }

    public Location setOffset(Date offset) {
        this.offset = offset;
        return this;
    }

    public Date getDuration() {
        return duration;
    }

    public Location setDuration(Date duration) {
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
   

}
