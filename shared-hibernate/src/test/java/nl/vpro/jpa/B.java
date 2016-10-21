package nl.vpro.jpa;

import javax.persistence.Entity;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
@Entity
public class B extends A {

    protected String bField = "b";


    public B() {

    }
    public B(Long i) {
        super(i);
    }
}
