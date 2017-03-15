package nl.vpro.jpa;

import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
public class A3Listener {

    @PreUpdate
    @PrePersist
    public void update(A a) {
        a.a3Field = "AA3";
    }
}
