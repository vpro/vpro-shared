package nl.vpro.jpa;

import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
public class AListener {

    @PreUpdate
    @PrePersist
    public void update(A a) {
        a.aField = "AA";
    }
}
