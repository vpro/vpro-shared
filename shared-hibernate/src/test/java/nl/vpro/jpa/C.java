package nl.vpro.jpa;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class C extends B {

    protected String cField = "c";

    @ManyToMany
    @OrderColumn(name = "list_index", nullable = false)
    protected List<X> xs;

    public C() {

    }

    public List<X> getXs() {
        if (xs == null) {
            xs = new ArrayList<>();
        }
        return xs;
    }

    public void setBroadcasters(List<X> xs) {
        this.xs= xs;
    }


}
