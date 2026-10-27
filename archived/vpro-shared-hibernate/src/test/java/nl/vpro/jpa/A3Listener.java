package nl.vpro.jpa;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

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
