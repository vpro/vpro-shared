package nl.vpro.elasticsearch;

import nl.vpro.jmx.Description;
import nl.vpro.jmx.Units;

import javax.management.MXBean;
import java.util.Date;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
@MXBean
public interface ElasticSearchIteratorMXBean {

	@Description("When iteration started")

	Date getStartDate();

	@Description("The current estimation for when iteration will be ready")
	Date getETADate();

	@Description("The current estimation for when iteration will be ready, expressed in seconds since start")
	Long getETASeconds();

	@Description("The current count")
	Long getCount();

	@Description("The total iteration size. As far as that is known")
	Long getTotalSizeLong();

	@Description("The number of elements read per second")
	@Units("/s")
	double getSpeed();

	@Description("The fraction of the time that this iterator busy")
	default float getFraction() {
		throw new UnsupportedOperationException();
	}


}
