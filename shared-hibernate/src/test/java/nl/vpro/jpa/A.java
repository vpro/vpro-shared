package nl.vpro.jpa;

import javax.persistence.*;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
@MappedSuperclass
public abstract class A implements I {

    @Id
    @SequenceGenerator(name = "hibernate_sequences", sequenceName = "hibernate_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequences")
    protected Long id;


    @Column(nullable = false)
    protected String aField;

    @Column(nullable = false)
    protected String a2Field;

    @Column(nullable = false)
    protected String a3Field;

    A() {

    }
    @Override
    public Long getId() {
        return id;
    }

    @Override
    public A setId(Long id) {
        this.id = id;
        return this;
    }

    @PrePersist
    public void setA2() {
        a2Field = "AAA2";
    }
}
