package nl.vpro.jpa;

import jakarta.persistence.*;

/**
 * @author Michiel Meeuwissen
 * @since 1.55
 */
@MappedSuperclass
public abstract class B extends A {


    protected String bField = "b";



    public B() {

    }
}
