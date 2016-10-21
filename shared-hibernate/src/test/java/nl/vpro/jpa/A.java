package nl.vpro.jpa;

import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.MappedSuperclass;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
@MappedSuperclass
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class A {

    @Id
    public Long id;

    protected String aField = "a";

    A() {

    }
    A(Long i) {
        this.id = i;
    }
}
