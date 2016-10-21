package nl.vpro.jpa;

import javax.persistence.*;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
@MappedSuperclass
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class A {

    @Id
    @SequenceGenerator(name = "hibernate_sequences", sequenceName = "hibernate_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequences")
    public Long id;


    protected String aField = "a";

    A() {

    }

}
