package nl.vpro.hibernate;

import javax.persistence.*;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Comparator;
import java.util.Date;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOG = LoggerFactory.getLogger(Location.class);

    private static final long serialVersionUID = -140942203904508506L;

    private static final String BASE_URN = "urn:vpro:media:location:";

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

   
  


    public static Long idFromUrn(String urn) {
        final String id = urn.substring(BASE_URN.length());
        return Long.valueOf(id);
    }

    public static String urnForId(long id) {
        return BASE_URN + id;
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
