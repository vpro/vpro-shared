package nl.vpro.jpa;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

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
