package nl.vpro.jpa;

import lombok.Data;

import java.net.URI;
import java.util.UUID;

import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import nl.vpro.hibernate.FalseToNullType;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
@MappedSuperclass
@Data
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

    @Column
    protected UUID uuidField;

    @Column
    protected URI uriField;

    @Column
    @Type(FalseToNullType.class)
    protected Boolean falseToNull;

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
